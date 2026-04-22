package com.hui.mapsystem.view;

import com.hui.mapsystem.model.GameMap;
import com.hui.mapsystem.model.MapEntity;
import com.hui.mapsystem.model.MoveComponent;
import com.hui.mapsystem.model.MoveType;
import com.hui.mapsystem.model.SquareCoordinate;
import com.hui.mapsystem.model.VisionComponent;
import com.hui.mapsystem.path.AStarPathFinder;
import com.hui.mapsystem.path.PathFinder;
import com.hui.mapsystem.vision.IncrementalVisionService;
import com.hui.mapsystem.vision.VisionDelta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 游戏会话，管理地图状态、实体位置与视野。
 * 所有游戏状态变更通过单线程 LogicThread 串行执行，避免并发修改共享游戏数据。
 * 外部线程（HTTP 处理线程）通过 {@link #submitMove} 投递任务，通过 {@link #snapshot} 读取状态。
 */
public final class GameSession {

    private final GameMap gameMap;
    private final Map<String, MapEntity> entities = new LinkedHashMap<>();
    private final Map<String, SquareCoordinate> positions = new HashMap<>();
    private final Map<String, Set<SquareCoordinate>> currentVisibleCells = new HashMap<>();
    private final IncrementalVisionService visionService = new IncrementalVisionService();
    private final ExecutorService logicThread;
    private List<SquareCoordinate> lastPath = List.of();

    /**
     * 创建游戏会话并加载地图模板，初始放置侦察兵单位。
     *
     * @param templateId 地图模板 id。
     */
    public GameSession(String templateId) {
        this.gameMap = GameMap.fromTemplate(templateId);
        PathFinder.bind(this.gameMap);
        this.logicThread = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "LogicThread");
            t.setDaemon(true);
            return t;
        });
        // 构造器中直接调用，对象尚未发布给其他线程，无需同步
        placeEntity("scout-1", new SquareCoordinate(0, 0), MoveType.GROUND, 2);
    }

    /**
     * 提交移动指令到 LogicThread。
     * HTTP 线程调用后通过 {@code Future.get()} 等待结果，不阻塞 LogicThread 的后续任务。
     *
     * @param entityId 实体 id。
     * @param to 目标坐标。
     * @return 实际行走路径的 Future；目标不可达时路径为空列表。
     */
    public Future<List<SquareCoordinate>> submitMove(String entityId, SquareCoordinate to) {
        return logicThread.submit(() -> {
            MapEntity entity = entities.get(entityId);
            if (entity == null) {
                return List.of();
            }
            SquareCoordinate from = positions.get(entityId);
            List<SquareCoordinate> path = PathFinder.findPath(from, to, entity.getMoveType());
            if (path.isEmpty()) {
                return List.of();
            }
            VisionDelta delta = visionService.updateVision(gameMap, entity, to);
            synchronized (this) {
                positions.put(entityId, to);
                Set<SquareCoordinate> visible = currentVisibleCells.computeIfAbsent(entityId, k -> new HashSet<>());
                visible.addAll(delta.enteredCells());
                visible.removeAll(delta.exitedCells());
                lastPath = List.copyOf(path);
            }
            return path;
        });
    }

    /**
     * 预览从实体当前位置到目标的路径，不修改任何游戏状态。
     * 由于 {@link GameMap} 构造后不再修改，此方法可安全地在 HTTP 线程中调用。
     *
     * @param entityId 实体 id。
     * @param to 目标坐标。
     * @return 路径坐标列表；不可达时返回空列表。
     */
    public List<SquareCoordinate> previewPath(String entityId, SquareCoordinate to) {
        SquareCoordinate from;
        MoveType moveType;
        synchronized (this) {
            MapEntity entity = entities.get(entityId);
            if (entity == null) {
                return List.of();
            }
            from = positions.get(entityId);
            moveType = entity.getMoveType();
        }
        return AStarPathFinder.findPath(gameMap, from, to, moveType);
    }

    /**
     * 返回当前游戏状态的全量快照，供序列化为 JSON 后返回给客户端。
     *
     * @return 状态快照（线程安全）。
     */
    public synchronized GameStateSnapshot snapshot() {
        List<EntitySnapshot> entityList = new ArrayList<>();
        for (Map.Entry<String, MapEntity> entry : entities.entrySet()) {
            String id = entry.getKey();
            MapEntity entity = entry.getValue();
            SquareCoordinate pos = positions.get(id);
            List<List<Integer>> visibleList = currentVisibleCells
                .getOrDefault(id, Set.of())
                .stream()
                .map(c -> List.of(c.hexX(), c.hexY()))
                .toList();
            entityList.add(new EntitySnapshot(
                id, pos.hexX(), pos.hexY(),
                entity.getMoveType().name(), entity.getVisionRange(), visibleList
            ));
        }
        List<List<Integer>> pathList = lastPath.stream()
            .map(c -> List.of(c.hexX(), c.hexY()))
            .toList();
        return new GameStateSnapshot(entityList, pathList, gameMap.getWidth(), gameMap.getHeight());
    }

    /**
     * 返回当前绑定的地图对象（只读）。
     *
     * @return 游戏地图。
     */
    public GameMap getGameMap() {
        return gameMap;
    }

    /**
     * 关闭 LogicThread，释放资源。
     */
    public void shutdown() {
        logicThread.shutdown();
    }

    // 仅在构造器中调用，对象未发布前无需同步
    private void placeEntity(String entityId, SquareCoordinate pos, MoveType moveType, int visionRange) {
        MapEntity entity = new MapEntity(entityId, new MoveComponent(moveType), new VisionComponent(visionRange));
        entities.put(entityId, entity);
        positions.put(entityId, pos);
        // 首次调用时 previousVisibleCells 为空，enteredCells 即当前全量可见格子
        VisionDelta delta = visionService.updateVision(gameMap, entity, pos);
        currentVisibleCells.put(entityId, new HashSet<>(delta.enteredCells()));
    }

    /**
     * 实体状态快照（用于 JSON 序列化）。
     *
     * @param entityId 实体 id。
     * @param hexX 当前 X 坐标。
     * @param hexY 当前 Y 坐标。
     * @param moveType 移动类型名称。
     * @param visionRange 视野半径。
     * @param visibleCells 当前可见格子坐标列表，每项为 [hexX, hexY]。
     */
    public record EntitySnapshot(
        String entityId,
        int hexX,
        int hexY,
        String moveType,
        int visionRange,
        List<List<Integer>> visibleCells
    ) {}

    /**
     * 游戏状态全量快照（用于 JSON 序列化）。
     *
     * @param entities 所有实体状态列表。
     * @param lastPath 最近一次移动路径，每项为 [hexX, hexY]。
     * @param mapWidth 地图宽度。
     * @param mapHeight 地图高度。
     */
    public record GameStateSnapshot(
        List<EntitySnapshot> entities,
        List<List<Integer>> lastPath,
        int mapWidth,
        int mapHeight
    ) {}
}

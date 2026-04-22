package com.hui.mapsystem.model;

import com.hui.mapsystem.config.BuildingConfig;
import com.hui.mapsystem.config.ConfigManager;
import com.hui.mapsystem.config.MapTemplateConfig;
import com.hui.mapsystem.config.PlacedBuildingConfig;
import com.hui.mapsystem.config.TerrainConfig;
import com.hui.mapsystem.config.TerrainPlacementConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 游戏地图对象，负责保存格子、占地与基础查询逻辑。
 */
public final class GameMap {
    private static final int[][] CARDINAL_DIRECTIONS = {
        {0, -1},
        {1, 0},
        {0, 1},
        {-1, 0}
    };

    private final String templateId;
    private final int width;
    private final int height;
    private final MapCell[][] cells;
    private final Map<SquareCoordinate, String> occupiedByBuildingId;
    private final Map<String, MapBuilding> buildings;
    private final Set<SquareCoordinate> resourceCoveredCells;

    private GameMap(String templateId,
                    int width,
                    int height,
                    MapCell[][] cells,
                    Map<SquareCoordinate, String> occupiedByBuildingId,
                    Map<String, MapBuilding> buildings,
                    Set<SquareCoordinate> resourceCoveredCells) {
        this.templateId = templateId;
        this.width = width;
        this.height = height;
        this.cells = cells;
        this.occupiedByBuildingId = occupiedByBuildingId;
        this.buildings = buildings;
        this.resourceCoveredCells = resourceCoveredCells;
    }

    /**
     * 根据模板配置构建一张地图。
     *
     * @param templateId 地图模板 id。
     * @return 根据模板实例化后的地图对象。
     * @throws IllegalArgumentException 当模板尺寸、地形编码或建筑占地非法时抛出。
     */
    public static GameMap fromTemplate(String templateId) {
        ConfigManager configManager = ConfigManager.getInstance();
        MapTemplateConfig templateConfig = configManager.getMapTemplate(templateId);
        MapCell[][] cells = new MapCell[templateConfig.getHeight()][templateConfig.getWidth()];
        if (templateConfig.getTerrainRows() != null && !templateConfig.getTerrainRows().isEmpty()) {
            if (templateConfig.getTerrainRows().size() != templateConfig.getHeight()) {
                throw new IllegalArgumentException("Terrain row count does not match map height for " + templateId);
            }
            for (int hexY = 0; hexY < templateConfig.getHeight(); hexY++) {
                List<Integer> terrainRow = templateConfig.getTerrainRows().get(hexY);
                if (terrainRow.size() != templateConfig.getWidth()) {
                    throw new IllegalArgumentException("Terrain column count does not match map width for " + templateId);
                }
                for (int hexX = 0; hexX < templateConfig.getWidth(); hexX++) {
                    TerrainConfig terrainConfig = configManager.getTerrainByCode(terrainRow.get(hexX));
                    cells[hexY][hexX] = MapGridPool.obtain(hexX, hexY, terrainConfig);
                }
            }
        } else {
            TerrainConfig defaultTerrain = configManager.getTerrain(templateConfig.getDefaultTerrainId());
            for (int hexY = 0; hexY < templateConfig.getHeight(); hexY++) {
                for (int hexX = 0; hexX < templateConfig.getWidth(); hexX++) {
                    cells[hexY][hexX] = MapGridPool.obtain(hexX, hexY, defaultTerrain);
                }
            }
            if (templateConfig.getTerrainOverrides() != null) {
                for (TerrainPlacementConfig terrainPlacement : templateConfig.getTerrainOverrides()) {
                    TerrainConfig terrainConfig = configManager.getTerrain(terrainPlacement.getTerrainId());
                    cells[terrainPlacement.getHexY()][terrainPlacement.getHexX()].configure(
                        terrainPlacement.getHexX(),
                        terrainPlacement.getHexY(),
                        terrainConfig
                    );
                }
            }
        }

        GameMap gameMap = new GameMap(
            templateConfig.getId(),
            templateConfig.getWidth(),
            templateConfig.getHeight(),
            cells,
            new HashMap<>(),
            new HashMap<>(),
            new HashSet<>()
        );
        if (templateConfig.getBuildings() != null) {
            for (PlacedBuildingConfig buildingPlacement : templateConfig.getBuildings()) {
                gameMap.placeBuilding(
                    buildingPlacement.getInstanceId(),
                    buildingPlacement.getBuildingId(),
                    new SquareCoordinate(buildingPlacement.getHexX(), buildingPlacement.getHexY())
                );
            }
        }
        return gameMap;
    }

    /**
     * 返回模板唯一标识。
     *
     * @return 模板 id。
     */
    public String getTemplateId() {
        return templateId;
    }

    /**
     * 返回地图宽度。
     *
     * @return 横向格子数。
     */
    public int getWidth() {
        return width;
    }

    /**
     * 返回地图高度。
     *
     * @return 纵向格子数。
     */
    public int getHeight() {
        return height;
    }

    /**
     * 按坐标获取地图格子。
     *
     * @param hexX 地图 X 坐标。
     * @param hexY 地图 Y 坐标。
     * @return 命中的地图格子；越界时返回 null。
     */
    public MapCell getCell(int hexX, int hexY) {
        if (!isWithinBounds(hexX, hexY)) {
            return null;
        }
        return cells[hexY][hexX];
    }

    /**
     * 按坐标对象获取地图格子。
     *
     * @param coordinate 地图坐标。
     * @return 命中的地图格子；越界时返回 null。
     */
    public MapCell getCell(SquareCoordinate coordinate) {
        return getCell(coordinate.hexX(), coordinate.hexY());
    }

    /**
     * 判断数值坐标是否位于地图内。
     *
     * @param hexX 地图 X 坐标。
     * @param hexY 地图 Y 坐标。
     * @return 坐标在地图边界内时返回 true。
     */
    public boolean isWithinBounds(int hexX, int hexY) {
        return hexX >= 0 && hexX < width && hexY >= 0 && hexY < height;
    }

    /**
     * 判断坐标对象是否位于地图内。
     *
     * @param coordinate 地图坐标。
     * @return 坐标在地图边界内时返回 true。
     */
    public boolean isWithinBounds(SquareCoordinate coordinate) {
        return isWithinBounds(coordinate.hexX(), coordinate.hexY());
    }

    /**
     * 判断指定移动类型能否进入某个坐标（无归属上下文）。
     *
     * @param coordinate 目标坐标。
     * @param moveType 移动类型。
     * @return 可进入时返回 true。
     */
    public boolean isWalkable(SquareCoordinate coordinate, MoveType moveType) {
        return isWalkable(coordinate, moveType, null);
    }

    /**
     * 判断指定移动类型和归属方能否进入某个坐标。
     * 当 actorOwnerId 与建筑归属一致时，地面单位可穿越该建筑格子。
     *
     * @param coordinate 目标坐标。
     * @param moveType 移动类型。
     * @param actorOwnerId 行动方归属 id；传 null 视为中立（任何建筑均阻挡）。
     * @return 可进入时返回 true。
     */
    public boolean isWalkable(SquareCoordinate coordinate, MoveType moveType, String actorOwnerId) {
        if (!isWithinBounds(coordinate)) {
            return false;
        }
        MapCell mapCell = getCell(coordinate);
        if (mapCell == null || !mapCell.isWalkable(moveType)) {
            return false;
        }
        if (moveType == MoveType.FLYING) {
            return true;
        }
        if (!occupiedByBuildingId.containsKey(coordinate)) {
            return true;
        }
        if (actorOwnerId != null) {
            String bId = occupiedByBuildingId.get(coordinate);
            MapBuilding building = buildings.get(bId);
            return actorOwnerId.equals(building.getOwnerId());
        }
        return false;
    }

    /**
     * 计算指定移动类型进入某个坐标的代价。
     *
     * @param coordinate 目标坐标。
     * @param moveType 移动类型。
     * @return 遍历代价；越界时返回 {@link Integer#MAX_VALUE}。
     */
    public int getTraversalCost(SquareCoordinate coordinate, MoveType moveType) {
        MapCell mapCell = getCell(coordinate);
        if (mapCell == null) {
            return Integer.MAX_VALUE;
        }
        return moveType == MoveType.FLYING ? 1 : mapCell.getMovementCost();
    }

    /**
     * 获取某个坐标的可走邻接点（无归属上下文）。
     *
     * @param coordinate 当前坐标。
     * @param moveType 移动类型。
     * @return 可通行邻接坐标列表。
     */
    public List<SquareCoordinate> getNeighbors(SquareCoordinate coordinate, MoveType moveType) {
        return getNeighbors(coordinate, moveType, null);
    }

    /**
     * 获取某个坐标的可走邻接点（含归属上下文）。
     *
     * @param coordinate 当前坐标。
     * @param moveType 移动类型。
     * @param actorOwnerId 行动方归属 id；传 null 视为中立。
     * @return 可通行邻接坐标列表。
     */
    public List<SquareCoordinate> getNeighbors(SquareCoordinate coordinate, MoveType moveType, String actorOwnerId) {
        List<SquareCoordinate> neighbors = new ArrayList<>(CARDINAL_DIRECTIONS.length);
        for (int[] direction : CARDINAL_DIRECTIONS) {
            SquareCoordinate neighbor = coordinate.offset(direction[0], direction[1]);
            if (isWalkable(neighbor, moveType, actorOwnerId)) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    /**
     * 在地图上放置一个中立建筑实例。
     *
     * @param instanceId 建筑实例 id。
     * @param buildingId 建筑原型 id。
     * @param origin 建筑原点坐标。
     * @return 创建后的建筑实例。
     */
    public MapBuilding placeBuilding(String instanceId, String buildingId, SquareCoordinate origin) {
        return placeBuilding(instanceId, buildingId, origin, null);
    }

    /**
     * 在地图上放置一个建筑实例并设置归属方。
     * 建筑覆盖的资源格子将被标记为不可采集，直到建筑被移除。
     *
     * @param instanceId 建筑实例 id。
     * @param buildingId 建筑原型 id。
     * @param origin 建筑原点坐标。
     * @param ownerId 归属玩家/阵营 id；中立建筑传 null。
     * @return 创建后的建筑实例。
     * @throws IllegalArgumentException 当占地越界或重叠时抛出。
     */
    public MapBuilding placeBuilding(String instanceId, String buildingId, SquareCoordinate origin, String ownerId) {
        BuildingConfig buildingConfig = ConfigManager.getInstance().getBuilding(buildingId);
        MapBuilding mapBuilding = new MapBuilding(instanceId, buildingConfig, origin, ownerId);
        for (SquareCoordinate occupiedCell : mapBuilding.getOccupiedCells()) {
            if (!isWithinBounds(occupiedCell)) {
                throw new IllegalArgumentException("Building footprint is out of bounds for " + instanceId);
            }
            if (occupiedByBuildingId.containsKey(occupiedCell)) {
                throw new IllegalArgumentException("Building footprint overlaps at " + occupiedCell);
            }
        }
        buildings.put(instanceId, mapBuilding);
        for (SquareCoordinate occupiedCell : mapBuilding.getOccupiedCells()) {
            if (buildingConfig.isBlocksMovement()) {
                occupiedByBuildingId.put(occupiedCell, instanceId);
            }
            MapCell cell = getCell(occupiedCell);
            if (cell != null && cell.getResourceType() != null && !"NONE".equals(cell.getResourceType())) {
                resourceCoveredCells.add(occupiedCell);
            }
        }
        return mapBuilding;
    }

    /**
     * 从地图上移除一个建筑实例，恢复其占格的寻路连通性和资源暴露状态。
     *
     * @param instanceId 建筑实例 id。
     * @return 被移除的建筑实例；不存在时返回 null。
     */
    public MapBuilding removeBuilding(String instanceId) {
        MapBuilding building = buildings.remove(instanceId);
        if (building == null) {
            return null;
        }
        for (SquareCoordinate occupiedCell : building.getOccupiedCells()) {
            occupiedByBuildingId.remove(occupiedCell);
            resourceCoveredCells.remove(occupiedCell);
        }
        return building;
    }

    /**
     * 判断指定格子的地形资源当前是否被建筑覆盖（无法采集）。
     *
     * @param coordinate 地图坐标。
     * @return 资源被覆盖时返回 true。
     */
    public boolean isResourceCovered(SquareCoordinate coordinate) {
        return resourceCoveredCells.contains(coordinate);
    }

    /**
     * 根据实例 id 获取建筑。
     *
     * @param instanceId 建筑实例 id。
     * @return 对应建筑；不存在时返回 null。
     */
    public MapBuilding getBuilding(String instanceId) {
        return buildings.get(instanceId);
    }

    /**
     * 返回地图上的全部建筑。
     *
     * @return 建筑集合视图。
     */
    public Collection<MapBuilding> getBuildings() {
        return buildings.values();
    }
}
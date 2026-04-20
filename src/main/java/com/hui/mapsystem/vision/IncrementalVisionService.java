package com.hui.mapsystem.vision;

import com.hui.mapsystem.model.GameMap;
import com.hui.mapsystem.model.MapEntity;
import com.hui.mapsystem.model.SquareCoordinate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 增量视野服务，仅返回本次视野进入和离开的格子集合。
 */
public final class IncrementalVisionService {
    private final Map<String, Set<SquareCoordinate>> visibleCellsByEntityId = new HashMap<>();

    /**
     * 根据实体当前位置刷新视野并返回增量结果。
     *
     * @param gameMap 地图对象。
     * @param entity 实体对象。
     * @param currentPosition 实体当前位置。
     * @return 视野增量对象。
     */
    public VisionDelta updateVision(GameMap gameMap, MapEntity entity, SquareCoordinate currentPosition) {
        Set<SquareCoordinate> previousVisibleCells = visibleCellsByEntityId.getOrDefault(entity.getEntityId(), Set.of());
        Set<SquareCoordinate> currentVisibleCells = collectVisibleCells(gameMap, currentPosition, entity.getVisionRange());
        Set<SquareCoordinate> enteredCells = new HashSet<>(currentVisibleCells);
        enteredCells.removeAll(previousVisibleCells);
        Set<SquareCoordinate> exitedCells = new HashSet<>(previousVisibleCells);
        exitedCells.removeAll(currentVisibleCells);
        visibleCellsByEntityId.put(entity.getEntityId(), currentVisibleCells);
        return new VisionDelta(Set.copyOf(enteredCells), Set.copyOf(exitedCells), currentVisibleCells.size());
    }

    /**
     * 清理指定实体缓存的视野结果。
     *
     * @param entityId 实体 id。
     */
    public void clear(String entityId) {
        visibleCellsByEntityId.remove(entityId);
    }

    private Set<SquareCoordinate> collectVisibleCells(GameMap gameMap, SquareCoordinate center, int range) {
        Set<SquareCoordinate> visibleCells = new HashSet<>();
        for (int deltaHexY = -range; deltaHexY <= range; deltaHexY++) {
            for (int deltaHexX = -range; deltaHexX <= range; deltaHexX++) {
                if (Math.abs(deltaHexX) + Math.abs(deltaHexY) > range) {
                    continue;
                }
                SquareCoordinate coordinate = center.offset(deltaHexX, deltaHexY);
                if (gameMap.isWithinBounds(coordinate)) {
                    visibleCells.add(coordinate);
                }
            }
        }
        return visibleCells;
    }
}
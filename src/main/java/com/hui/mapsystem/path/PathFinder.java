package com.hui.mapsystem.path;

import com.hui.mapsystem.model.GameMap;
import com.hui.mapsystem.model.MoveType;
import com.hui.mapsystem.model.SquareCoordinate;

import java.util.List;

/**
 * 对外统一的寻路入口。
 */
public final class PathFinder {
    private static GameMap boundMap;

    private PathFinder() {
    }

    /**
     * 绑定当前寻路所使用的地图。
     *
     * @param gameMap 当前有效地图。
     */
    public static void bind(GameMap gameMap) {
        boundMap = gameMap;
    }

    /**
     * 在已绑定地图上执行寻路。
     *
     * @param start 起点坐标。
     * @param end 终点坐标。
     * @param moveType 移动类型。
     * @return 路径结果；不存在路径时返回空列表。
     * @throws IllegalStateException 当调用前尚未绑定地图时抛出。
     */
    public static List<SquareCoordinate> findPath(SquareCoordinate start, SquareCoordinate end, MoveType moveType) {
        if (boundMap == null) {
            throw new IllegalStateException("PathFinder has not been bound to a GameMap");
        }
        return AStarPathFinder.findPath(boundMap, start, end, moveType);
    }
}
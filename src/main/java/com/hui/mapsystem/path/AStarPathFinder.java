package com.hui.mapsystem.path;

import com.hui.mapsystem.model.GameMap;
import com.hui.mapsystem.model.MoveType;
import com.hui.mapsystem.model.SquareCoordinate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A* 寻路工具类。
 */
public final class AStarPathFinder {
    private AStarPathFinder() {
    }

    /**
     * 在指定地图上执行 A* 寻路。
     *
     * @param gameMap 地图对象。
     * @param start 起点坐标。
     * @param end 终点坐标。
     * @param moveType 移动类型。
     * @return 从起点到终点的路径；不存在路径时返回空列表。
     */
    public static List<SquareCoordinate> findPath(GameMap gameMap,
                                                  SquareCoordinate start,
                                                  SquareCoordinate end,
                                                  MoveType moveType) {
        if (!gameMap.isWalkable(start, moveType) || !gameMap.isWalkable(end, moveType)) {
            return List.of();
        }
        if (start.equals(end)) {
            return List.of(start);
        }

        PriorityQueue<PathNode> openSet = new PriorityQueue<>(Comparator.comparingInt(PathNode::fScore));
        Map<SquareCoordinate, SquareCoordinate> cameFrom = new HashMap<>();
        Map<SquareCoordinate, Integer> gScore = new HashMap<>();
        gScore.put(start, 0);
        openSet.offer(new PathNode(start, heuristic(start, end)));

        while (!openSet.isEmpty()) {
            PathNode currentNode = openSet.poll();
            SquareCoordinate current = currentNode.coordinate();
            if (current.equals(end)) {
                return buildPath(cameFrom, current);
            }

            int currentCost = gScore.getOrDefault(current, Integer.MAX_VALUE);
            for (SquareCoordinate neighbor : gameMap.getNeighbors(current, moveType)) {
                int tentativeCost = currentCost + gameMap.getTraversalCost(neighbor, moveType);
                if (tentativeCost >= gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    continue;
                }
                cameFrom.put(neighbor, current);
                gScore.put(neighbor, tentativeCost);
                openSet.offer(new PathNode(neighbor, tentativeCost + heuristic(neighbor, end)));
            }
        }

        return List.of();
    }

    private static List<SquareCoordinate> buildPath(Map<SquareCoordinate, SquareCoordinate> cameFrom,
                                                    SquareCoordinate end) {
        List<SquareCoordinate> path = new ArrayList<>();
        SquareCoordinate cursor = end;
        path.add(cursor);
        while (cameFrom.containsKey(cursor)) {
            cursor = cameFrom.get(cursor);
            path.add(0, cursor);
        }
        return path;
    }

    private static int heuristic(SquareCoordinate start, SquareCoordinate end) {
        return Math.abs(start.hexX() - end.hexX()) + Math.abs(start.hexY() - end.hexY());
    }

    private record PathNode(SquareCoordinate coordinate, int fScore) {
    }
}
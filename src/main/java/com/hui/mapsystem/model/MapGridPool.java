package com.hui.mapsystem.model;

import com.hui.mapsystem.config.TerrainConfig;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 地图格子对象池，避免在地图加载和循环中重复创建格子对象。
 */
public final class MapGridPool {
    private static final Deque<MapCell> POOL = new ArrayDeque<>();

    private MapGridPool() {
    }

    /**
     * 从对象池中获取一个地图格子并完成初始化。
     *
     * @param hexX 格子 X 坐标。
     * @param hexY 格子 Y 坐标。
     * @param terrainConfig 地形配置。
     * @return 已初始化的地图格子。
     */
    public static MapCell obtain(int hexX, int hexY, TerrainConfig terrainConfig) {
        MapCell mapCell = POOL.pollFirst();
        if (mapCell == null) {
            mapCell = new MapCell();
        }
        mapCell.configure(hexX, hexY, terrainConfig);
        return mapCell;
    }

    /**
     * 把地图格子归还到对象池。
     *
     * @param mapCell 待回收格子。
     */
    public static void release(MapCell mapCell) {
        mapCell.reset();
        POOL.offerFirst(mapCell);
    }
}
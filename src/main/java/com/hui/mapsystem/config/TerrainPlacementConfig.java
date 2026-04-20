package com.hui.mapsystem.config;

/**
 * 地形覆写配置，用于旧版模板逻辑中的单点地形替换。
 */
public class TerrainPlacementConfig {
    private int hexX;
    private int hexY;
    private String terrainId;

    /**
     * 返回覆写点 X 坐标。
     *
     * @return 地图 X 坐标。
     */
    public int getHexX() {
        return hexX;
    }

    /**
     * 返回覆写点 Y 坐标。
     *
     * @return 地图 Y 坐标。
     */
    public int getHexY() {
        return hexY;
    }

    /**
     * 返回目标地形 id。
     *
     * @return 地形配置 id。
     */
    public String getTerrainId() {
        return terrainId;
    }
}
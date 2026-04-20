package com.hui.mapsystem.model;

import com.hui.mapsystem.config.TerrainConfig;

/**
 * 地图格子对象，保存地形、移动和资源信息。
 */
public final class MapCell {
    private int hexX;
    private int hexY;
    private String terrainId;
    private int movementCost;
    private boolean walkable;
    private String resourceType;

    void configure(int hexX, int hexY, TerrainConfig terrainConfig) {
        this.hexX = hexX;
        this.hexY = hexY;
        terrainId = terrainConfig.getId();
        movementCost = terrainConfig.getMovementCost();
        walkable = terrainConfig.isWalkable();
        resourceType = terrainConfig.getResourceId();
    }

    void reset() {
        hexX = 0;
        hexY = 0;
        terrainId = null;
        movementCost = 0;
        walkable = false;
        resourceType = null;
    }

    /**
     * 返回格子 X 坐标。
     *
     * @return 地图 X。
     */
    public int getHexX() {
        return hexX;
    }

    /**
     * 返回格子 Y 坐标。
     *
     * @return 地图 Y。
     */
    public int getHexY() {
        return hexY;
    }

    /**
     * 返回当前格子的地形 id。
     *
     * @return 地形配置 id。
     */
    public String getTerrainId() {
        return terrainId;
    }

    /**
     * 返回默认移动代价。
     *
     * @return 当前格子的移动代价。
     */
    public int getMovementCost() {
        return movementCost;
    }

    /**
     * 判断某种移动类型是否能通过当前格子。
     *
     * @param moveType 移动类型。
     * @return 可通行时返回 true。
     */
    public boolean isWalkable(MoveType moveType) {
        return moveType == MoveType.FLYING || walkable;
    }

    /**
     * 返回格子关联资源类型。
     *
     * @return 资源类型字符串。
     */
    public String getResourceType() {
        return resourceType;
    }
}
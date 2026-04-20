package com.hui.mapsystem.config;

/**
 * 建筑放置配置，描述建筑原型在模板中的实例化位置。
 */
public class PlacedBuildingConfig {
    private String instanceId;
    private String buildingId;
    private int hexX;
    private int hexY;

    /**
     * 返回建筑实例 id。
     *
     * @return 模板内唯一实例标识。
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * 返回建筑原型 id。
     *
     * @return 建筑配置 id。
     */
    public String getBuildingId() {
        return buildingId;
    }

    /**
     * 返回建筑原点 X 坐标。
     *
     * @return 建筑放置 X。
     */
    public int getHexX() {
        return hexX;
    }

    /**
     * 返回建筑原点 Y 坐标。
     *
     * @return 建筑放置 Y。
     */
    public int getHexY() {
        return hexY;
    }
}
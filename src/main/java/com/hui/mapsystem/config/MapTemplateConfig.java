package com.hui.mapsystem.config;

import java.util.List;

/**
 * 地图模板配置对象，描述地图尺寸、铺图数据和预放置建筑。
 */
public class MapTemplateConfig {
    private String id;
    private String name;
    private int width;
    private int height;
    private String defaultTerrainId;
    private List<List<Integer>> terrainRows;
    private List<TerrainPlacementConfig> terrainOverrides;
    private List<PlacedBuildingConfig> buildings;

    /**
     * 返回模板唯一标识。
     *
     * @return 模板 id。
     */
    public String getId() {
        return id;
    }

    /**
     * 返回模板显示名称。
     *
     * @return 模板名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 返回模板宽度。
     *
     * @return 每行格子数。
     */
    public int getWidth() {
        return width;
    }

    /**
     * 返回模板高度。
     *
     * @return 地图总行数。
     */
    public int getHeight() {
        return height;
    }

    /**
     * 返回默认地形 id。
     *
     * @return 默认地形配置 id。
     */
    public String getDefaultTerrainId() {
        return defaultTerrainId;
    }

    /**
     * 返回完整铺图矩阵。
     *
     * @return 二维地形 code 列表。
     */
    public List<List<Integer>> getTerrainRows() {
        return terrainRows;
    }

    /**
     * 返回旧版地形覆写列表。
     *
     * @return 按坐标覆写的地形集合。
     */
    public List<TerrainPlacementConfig> getTerrainOverrides() {
        return terrainOverrides;
    }

    /**
     * 返回模板中的建筑放置列表。
     *
     * @return 建筑实例配置集合。
     */
    public List<PlacedBuildingConfig> getBuildings() {
        return buildings;
    }
}
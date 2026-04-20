package com.hui.mapsystem.config;

import java.util.List;

/**
 * 建筑配置对象，描述建筑类型、占地和基础数值。
 */
public class BuildingConfig {
    private String id;
    private int code;
    private String name;
    private String type;
    private String category;
    private boolean blocksMovement;
    private int durability;
    private int visionBonus;
    private List<GridOffsetConfig> footprint;

    /**
     * 返回建筑唯一标识。
     *
     * @return 建筑配置 id。
     */
    public String getId() {
        return id;
    }

    /**
     * 返回建筑数值编码。
     *
     * @return 建筑 code。
     */
    public int getCode() {
        return code;
    }

    /**
     * 返回建筑显示名称。
     *
     * @return 建筑名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 返回建筑行为类型。
     *
     * @return 建筑类型。
     */
    public String getType() {
        return type;
    }

    /**
     * 返回建筑内容分类。
     *
     * @return 建筑分类。
     */
    public String getCategory() {
        return category;
    }

    /**
     * 判断建筑是否阻挡移动。
     *
     * @return 阻挡移动时返回 true。
     */
    public boolean isBlocksMovement() {
        return blocksMovement;
    }

    /**
     * 返回建筑耐久值。
     *
     * @return 建筑耐久。
     */
    public int getDurability() {
        return durability;
    }

    /**
     * 返回建筑附加视野值。
     *
     * @return 视野加成。
     */
    public int getVisionBonus() {
        return visionBonus;
    }

    /**
     * 返回建筑 footprint 定义。
     *
     * @return 相对原点的占地格列表。
     */
    public List<GridOffsetConfig> getFootprint() {
        return footprint;
    }
}
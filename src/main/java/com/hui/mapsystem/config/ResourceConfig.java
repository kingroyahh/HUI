package com.hui.mapsystem.config;

/**
 * 资源配置对象，描述资源节点的基础采集参数。
 */
public class ResourceConfig {
    private String id;
    private String name;
    private String category;
    private int gatherPerHour;
    private int capacity;

    /**
     * 返回资源唯一标识。
     *
     * @return 资源 id。
     */
    public String getId() {
        return id;
    }

    /**
     * 返回资源显示名称。
     *
     * @return 资源名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 返回资源分类。
     *
     * @return 资源分类值。
     */
    public String getCategory() {
        return category;
    }

    /**
     * 返回每小时采集速率。
     *
     * @return 每小时产出或采集量。
     */
    public int getGatherPerHour() {
        return gatherPerHour;
    }

    /**
     * 返回资源容量。
     *
     * @return 资源上限。
     */
    public int getCapacity() {
        return capacity;
    }
}
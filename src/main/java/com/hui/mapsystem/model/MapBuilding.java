package com.hui.mapsystem.model;

import com.hui.mapsystem.config.BuildingConfig;
import com.hui.mapsystem.config.GridOffsetConfig;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 地图建筑实例，描述建筑原型及其落点信息。
 */
public final class MapBuilding {
    private final String instanceId;
    private final BuildingConfig buildingConfig;
    private final SquareCoordinate origin;
    private final String ownerId;

    /**
     * 创建一个地图建筑实例（无归属方）。
     *
     * @param instanceId 建筑实例 id。
     * @param buildingConfig 建筑原型配置。
     * @param origin 建筑原点坐标。
     */
    public MapBuilding(String instanceId, BuildingConfig buildingConfig, SquareCoordinate origin) {
        this(instanceId, buildingConfig, origin, null);
    }

    /**
     * 创建一个地图建筑实例。
     *
     * @param instanceId 建筑实例 id。
     * @param buildingConfig 建筑原型配置。
     * @param origin 建筑原点坐标。
     * @param ownerId 归属玩家/阵营 id；中立建筑传 null。
     */
    public MapBuilding(String instanceId, BuildingConfig buildingConfig, SquareCoordinate origin, String ownerId) {
        this.instanceId = instanceId;
        this.buildingConfig = buildingConfig;
        this.origin = origin;
        this.ownerId = ownerId;
    }

    /**
     * 返回建筑实例 id。
     *
     * @return 实例唯一标识。
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * 返回建筑原型配置。
     *
     * @return 建筑配置对象。
     */
    public BuildingConfig getBuildingConfig() {
        return buildingConfig;
    }

    /**
     * 返回建筑原点。
     *
     * @return 建筑放置原点坐标。
     */
    public SquareCoordinate getOrigin() {
        return origin;
    }

    /**
     * 返回建筑归属方 id。
     *
     * @return 归属玩家/阵营 id；中立建筑返回 null。
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * 计算建筑在地图上的实际占格。
     *
     * @return 占格坐标集合。
     */
    public Set<SquareCoordinate> getOccupiedCells() {
        Set<SquareCoordinate> occupiedCells = new LinkedHashSet<>();
        for (GridOffsetConfig footprintCell : buildingConfig.getFootprint()) {
            occupiedCells.add(origin.offset(footprintCell.getHexX(), footprintCell.getHexY()));
        }
        return occupiedCells;
    }
}
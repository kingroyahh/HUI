package com.hui.mapsystem.config;

import com.hui.mapsystem.model.MoveType;

import java.util.Map;

/**
 * 地形配置对象，描述地形编码、移动代价与关联资源。
 */
public class TerrainConfig {
    private String id;
    private int code;
    private String name;
    private Map<String, Integer> movementCosts;
    private int sightCost;
    private String resourceId;

    /**
     * 返回地形唯一标识。
     *
     * @return 地形 id。
     */
    public String getId() {
        return id;
    }

    /**
     * 返回地形数值编码。
     *
     * @return 地形 code。
     */
    public int getCode() {
        return code;
    }

    /**
     * 返回地形显示名称。
     *
     * @return 地形名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 返回按移动类型配置的代价表。
     *
     * @return 移动类型到代价的映射。
     */
    public Map<String, Integer> getMovementCosts() {
        return movementCosts;
    }

    /**
     * 返回地面单位通过该地形的代价。
     *
     * @return 地面单位移动代价；不可通行时返回极大值。
     */
    public int getMovementCost() {
        return getMovementCost(MoveType.GROUND);
    }

    /**
     * 返回指定移动类型通过该地形的代价。
     *
     * @param moveType 移动类型。
     * @return 对应移动代价；未配置时返回 {@link Integer#MAX_VALUE}。
     */
    public int getMovementCost(MoveType moveType) {
        if (movementCosts == null) {
            return Integer.MAX_VALUE;
        }
        return movementCosts.getOrDefault(moveType.name(), Integer.MAX_VALUE);
    }

    /**
     * 判断地面单位是否可通过该地形。
     *
     * @return 地面单位可通行时返回 true。
     */
    public boolean isWalkable() {
        return isWalkable(MoveType.GROUND);
    }

    /**
     * 判断指定移动类型是否可通过该地形。
     *
     * @param moveType 移动类型。
     * @return 当前移动类型存在配置时返回 true。
     */
    public boolean isWalkable(MoveType moveType) {
        return movementCosts != null && movementCosts.containsKey(moveType.name());
    }

    /**
     * 返回地形视野代价。
     *
     * @return 视野消耗值。
     */
    public int getSightCost() {
        return sightCost;
    }

    /**
     * 返回关联资源 id。
     *
     * @return 资源配置 id。
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * 返回当前地形的资源类型标识。
     *
     * @return 资源类型字符串。
     */
    public String getResourceType() {
        return resourceId;
    }
}
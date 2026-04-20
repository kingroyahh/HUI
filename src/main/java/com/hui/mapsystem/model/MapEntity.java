package com.hui.mapsystem.model;

/**
 * 地图实体对象，组合移动和视野组件。
 */
public final class MapEntity {
    private final String entityId;
    private final MoveComponent moveComponent;
    private final VisionComponent visionComponent;

    /**
     * 创建一个地图实体。
     *
     * @param entityId 实体 id。
     * @param moveComponent 移动组件。
     * @param visionComponent 视野组件。
     */
    public MapEntity(String entityId, MoveComponent moveComponent, VisionComponent visionComponent) {
        this.entityId = entityId;
        this.moveComponent = moveComponent;
        this.visionComponent = visionComponent;
    }

    /**
     * 返回实体 id。
     *
     * @return 实体唯一标识。
     */
    public String getEntityId() {
        return entityId;
    }

    /**
     * 返回实体移动类型。
     *
     * @return 当前移动类型。
     */
    public MoveType getMoveType() {
        return moveComponent.getMoveType();
    }

    /**
     * 返回实体视野范围。
     *
     * @return 视野半径。
     */
    public int getVisionRange() {
        return visionComponent.getRange();
    }
}
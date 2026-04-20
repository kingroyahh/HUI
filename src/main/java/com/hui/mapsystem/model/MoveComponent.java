package com.hui.mapsystem.model;

/**
 * 移动组件，负责保存实体移动类型。
 */
public final class MoveComponent {
    private final MoveType moveType;

    /**
     * 创建一个移动组件。
     *
     * @param moveType 移动类型。
     */
    public MoveComponent(MoveType moveType) {
        this.moveType = moveType;
    }

    /**
     * 返回当前移动类型。
     *
     * @return 移动类型枚举值。
     */
    public MoveType getMoveType() {
        return moveType;
    }
}
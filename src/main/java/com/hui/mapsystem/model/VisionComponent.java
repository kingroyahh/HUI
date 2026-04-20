package com.hui.mapsystem.model;

/**
 * 视野组件，保存实体视野半径。
 */
public final class VisionComponent {
    private final int range;

    /**
     * 创建一个视野组件。
     *
     * @param range 视野半径。
     */
    public VisionComponent(int range) {
        this.range = range;
    }

    /**
     * 返回视野范围。
     *
     * @return 视野半径。
     */
    public int getRange() {
        return range;
    }
}
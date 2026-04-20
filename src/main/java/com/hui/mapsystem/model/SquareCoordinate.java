package com.hui.mapsystem.model;

import java.util.Objects;

/**
 * 不可变地图坐标对象。
 */
public final class SquareCoordinate {
    private final int hexX;
    private final int hexY;

    /**
     * 创建一个地图坐标。
     *
     * @param hexX 地图 X 坐标。
     * @param hexY 地图 Y 坐标。
     */
    public SquareCoordinate(int hexX, int hexY) {
        this.hexX = hexX;
        this.hexY = hexY;
    }

    /**
     * 返回 X 坐标。
     *
     * @return 地图 X。
     */
    public int hexX() {
        return hexX;
    }

    /**
     * 返回 Y 坐标。
     *
     * @return 地图 Y。
     */
    public int hexY() {
        return hexY;
    }

    /**
     * 基于当前坐标生成一个偏移后的新坐标。
     *
     * @param deltaHexX X 方向偏移量。
     * @param deltaHexY Y 方向偏移量。
     * @return 偏移后的新坐标对象。
     */
    public SquareCoordinate offset(int deltaHexX, int deltaHexY) {
        return new SquareCoordinate(hexX + deltaHexX, hexY + deltaHexY);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SquareCoordinate that)) {
            return false;
        }
        return hexX == that.hexX && hexY == that.hexY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hexX, hexY);
    }

    @Override
    public String toString() {
        return "SquareCoordinate{" +
            "hexX=" + hexX +
            ", hexY=" + hexY +
            '}';
    }
}
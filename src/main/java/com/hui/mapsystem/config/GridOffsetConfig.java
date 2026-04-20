package com.hui.mapsystem.config;

/**
 * 相对坐标偏移配置，用于描述 footprint 中的单个格子。
 */
public class GridOffsetConfig {
    private int hexX;
    private int hexY;

    /**
     * 返回相对 X 偏移。
     *
     * @return 相对 X。
     */
    public int getHexX() {
        return hexX;
    }

    /**
     * 返回相对 Y 偏移。
     *
     * @return 相对 Y。
     */
    public int getHexY() {
        return hexY;
    }
}
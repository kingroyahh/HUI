package com.hui.mapsystem.vision;

import com.hui.mapsystem.model.SquareCoordinate;

import java.util.Objects;
import java.util.Set;

/**
 * 视野增量结果对象。
 */
public final class VisionDelta {
    private final Set<SquareCoordinate> enteredCells;
    private final Set<SquareCoordinate> exitedCells;
    private final int visibleCellCount;

    /**
     * 创建一个视野增量结果。
     *
     * @param enteredCells 本次新进入视野的格子集合。
     * @param exitedCells 本次离开视野的格子集合。
     * @param visibleCellCount 当前总可见格子数。
     */
    public VisionDelta(Set<SquareCoordinate> enteredCells,
                       Set<SquareCoordinate> exitedCells,
                       int visibleCellCount) {
        this.enteredCells = enteredCells;
        this.exitedCells = exitedCells;
        this.visibleCellCount = visibleCellCount;
    }

    /**
     * 返回本次新进入视野的格子集合。
     *
     * @return 进入视野的格子集合。
     */
    public Set<SquareCoordinate> enteredCells() {
        return enteredCells;
    }

    /**
     * 返回本次离开视野的格子集合。
     *
     * @return 离开视野的格子集合。
     */
    public Set<SquareCoordinate> exitedCells() {
        return exitedCells;
    }

    /**
     * 返回当前总可见格子数量。
     *
     * @return 当前可见格子数。
     */
    public int visibleCellCount() {
        return visibleCellCount;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VisionDelta that)) {
            return false;
        }
        return visibleCellCount == that.visibleCellCount
            && Objects.equals(enteredCells, that.enteredCells)
            && Objects.equals(exitedCells, that.exitedCells);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enteredCells, exitedCells, visibleCellCount);
    }

    @Override
    public String toString() {
        return "VisionDelta{" +
            "enteredCells=" + enteredCells +
            ", exitedCells=" + exitedCells +
            ", visibleCellCount=" + visibleCellCount +
            '}';
    }
}
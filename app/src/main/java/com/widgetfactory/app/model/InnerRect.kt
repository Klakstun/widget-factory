package com.widgetfactory.app.model

import kotlinx.serialization.Serializable

/**
 * 内部 10×10 编辑网格中元素的坐标与尺寸（以内部小格为单位）。
 * 行/列为左上角位置，宽高为占用小格数，均为整数。
 * 元素最小 2×2。
 */
@Serializable
data class InnerRect(
    val row: Int,
    val col: Int,
    val width: Int,
    val height: Int
) {
    init {
        require(width >= MIN_SIZE) { "width must be >= $MIN_SIZE, got $width" }
        require(height >= MIN_SIZE) { "height must be >= $MIN_SIZE, got $height" }
        require(row >= 0 && col >= 0) { "row/col must be >= 0" }
    }

    val right: Int get() = col + width
    val bottom: Int get() = row + height

    /** 是否与另一个 rect 重叠（格子独占，边接触不算重叠）。 */
    fun overlaps(other: InnerRect): Boolean {
        return col < other.right && right > other.col &&
            row < other.bottom && bottom > other.row
    }

    /** 是否越出 10×10 网格边界。 */
    fun isOutOfBounds(gridSize: Int = GRID_SIZE): Boolean {
        return row < 0 || col < 0 || right > gridSize || bottom > gridSize
    }

    companion object {
        const val GRID_SIZE = 10
        const val MIN_SIZE = 2
    }
}

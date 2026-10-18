package com.widgetfactory.app.model

import kotlinx.serialization.Serializable

/**
 * 外部 5×5 占位网格的 span。
 * 宽高均为 1~5 的整数，允许任意矩形组合（含 1×5、4×2）。
 */
@Serializable
data class ExternalSpan(
    val widthSpan: Int = 2,
    val heightSpan: Int = 2
) {
    init {
        require(widthSpan in 1..5) { "widthSpan must be in [1,5], got $widthSpan" }
        require(heightSpan in 1..5) { "heightSpan must be in [1,5], got $heightSpan" }
    }

    companion object {
        const val MIN = 1
        const val MAX = 5
    }
}

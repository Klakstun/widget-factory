package com.widgetfactory.app.model

import kotlinx.serialization.Serializable

/**
 * 元素类型。每种类型有默认内部尺寸（10×10 网格小格），最小 2×2。
 * 默认尺寸：按钮 4×2（宽×高）、滑杆 2×2、图标/文本框/封面 2×2。
 */
@Serializable
enum class ElementType(val defaultWidth: Int, val defaultHeight: Int, val label: String) {
    BUTTON(defaultWidth = 4, defaultHeight = 2, label = "按钮"),
    SLIDER(defaultWidth = 2, defaultHeight = 2, label = "滑杆"),
    ICON(defaultWidth = 2, defaultHeight = 2, label = "图标"),
    TEXTBOX(defaultWidth = 2, defaultHeight = 2, label = "文本框"),
    COVER(defaultWidth = 2, defaultHeight = 2, label = "封面");

    val defaultRect: InnerRect
        get() = InnerRect(row = 0, col = 0, width = defaultWidth, height = defaultHeight)
}

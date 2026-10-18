package com.widgetfactory.app.model

import kotlinx.serialization.Serializable

/**
 * 元素样式配置（仅引用 Material 3 语义角色，不硬编码颜色）。
 */
@Serializable
data class ElementStyle(
    /** 对应 Material 3 语义角色名（如 primary、surface、secondaryContainer）。 */
    val colorRole: String = "surface",
    val cornerRadiusDp: Float = 8f
)

/**
 * 元素内容配置，按元素类型使用不同字段。
 */
@Serializable
data class ContentConfig(
    /** 按钮文本 / 文本框文本 / 通用文本。 */
    val text: String = "",
    /** 文本框 HTML 内容（内联）或 iframe 源码。 */
    val html: String = "",
    /** 文本框引用的外部 HTML/文本文件。 */
    val fileRef: FileRef? = null,
    /** 图标资源名（drawable 名或 material icon 名）。 */
    val iconResource: String = "",
    /** 封面图片引用。 */
    val imageRef: FileRef? = null,
    /** 封面纯色占位（Material 3 语义角色名）。 */
    val coverColorRole: String = "surfaceVariant",
    /** 滑杆最小值。 */
    val sliderMin: Float = 0f,
    /** 滑杆最大值。 */
    val sliderMax: Float = 100f,
    /** 滑杆步长。 */
    val sliderStep: Float = 1f,
    /** 滑杆当前值。 */
    val sliderValue: Float = 0f
)

/**
 * 组件内的单个元素。
 * 包含类型、内部坐标、样式、绑定动作、内容配置。
 */
@Serializable
data class Element(
    val id: String,
    val type: ElementType,
    val rect: InnerRect,
    val style: ElementStyle = ElementStyle(),
    val action: ActionParam? = null,
    val content: ContentConfig = ContentConfig()
)

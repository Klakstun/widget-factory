package com.widgetfactory.app.model

import kotlinx.serialization.Serializable

/**
 * 组件降级配置（出厂时可设，运行时可由设置页手动切换）。
 */
@Serializable
data class DegradeConfig(
    /** 是否启用精细渲染（内部 10×10）；false 表示直接走系统原生渲染。 */
    val fineRenderingEnabled: Boolean = true,
    /** HTML 渲染是否启用外部 CSS。 */
    val htmlExternalCssEnabled: Boolean = true,
    /** HTML 渲染是否启用外部 JS。 */
    val htmlExternalJsEnabled: Boolean = true,
    /** HTML 渲染是否启用内联 JS。 */
    val htmlInlineJsEnabled: Boolean = true,
    /** 当前是否处于降级渲染状态（运行时异常自动设置）。 */
    val degraded: Boolean = false
)

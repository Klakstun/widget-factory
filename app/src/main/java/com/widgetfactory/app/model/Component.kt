package com.widgetfactory.app.model

import kotlinx.serialization.Serializable

/**
 * 组件模型：可序列化为 JSON 写入配置文件，供桌面 Widget 读取。
 *
 * 包含：组件 ID、名称、创建/更新时间、外部 span、元素列表、降级配置、版本号。
 */
@Serializable
data class Component(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val span: ExternalSpan = ExternalSpan(),
    val elements: List<Element> = emptyList(),
    val degradeConfig: DegradeConfig = DegradeConfig(),
    val version: Int = 1
) {
    companion object {
        /** 单组件最多元素数。 */
        const val MAX_ELEMENTS = 12
        const val CURRENT_VERSION = 1
    }
}

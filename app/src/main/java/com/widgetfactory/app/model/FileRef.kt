package com.widgetfactory.app.model

import kotlinx.serialization.Serializable

/**
 * 统一文件引用模型，供播放、打开、编辑、选文件共用。
 * URI + MIME + 显示名 + 授权时效。
 */
@Serializable
data class FileRef(
    /** 内容 URI（SAF 产出或自定义），序列化时以字符串形式存储。 */
    val uri: String,
    /** MIME 类型，例如 audio/mpeg、text/html、image/png。 */
    val mime: String = "*/*",
    /** 显示用文件名。 */
    val displayName: String = "",
    /** URI 授权时效（毫秒时间戳），0 表示长期或未设置。 */
    val grantTimeout: Long = 0L
) {
    val isGranted: Boolean
        get() = grantTimeout == 0L || grantTimeout > System.currentTimeMillis()
}

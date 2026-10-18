package com.widgetfactory.app.model

import kotlinx.serialization.Serializable

/**
 * 固定动作列表（不可扩展）。
 */
@Serializable
enum class ActionId(val label: String) {
    PLAY("播放"),
    OPEN_FILE("打开文件"),
    EDIT_FILE("编辑文件"),
    PICK_FILE("选文件"),
    OPEN_APP("打开应用"),
    SYSTEM_OP("执行系统操作")
}

/**
 * 系统操作固定子项（由 Agent 定义的合理默认集，每项声明权限）。
 */
@Serializable
enum class SystemOpId(val label: String, val requiredPermission: String? = null) {
    VOLUME_UP("音量+", android.Manifest.permission.MODIFY_AUDIO_SETTINGS),
    VOLUME_DOWN("音量-", android.Manifest.permission.MODIFY_AUDIO_SETTINGS),
    TOGGLE_BLUETOOTH("切换蓝牙", android.Manifest.permission.BLUETOOTH_CONNECT),
    TOGGLE_WIFI("切换 Wi-Fi", android.Manifest.permission.CHANGE_WIFI_STATE),
    EXPAND_NOTIFICATIONS("展开通知栏"),
    LOCK_SCREEN("锁屏")
}

/**
 * 动作绑定参数。不同动作使用不同字段，未使用字段为 null。
 */
@Serializable
data class ActionParam(
    val actionId: ActionId,
    /** 播放/打开/编辑文件时使用。 */
    val fileRef: FileRef? = null,
    /** 播放网络源时使用（URL 字符串）。 */
    val sourceUrl: String? = null,
    /** 选文件时的 MIME 过滤（如 "audio/*"）。 */
    val mimeFilter: String = "*/*",
    /** 打开应用时的包名。 */
    val packageName: String? = null,
    /** 执行系统操作时的子项。 */
    val systemOpId: SystemOpId? = null,
    /** 通用扩展参数。 */
    val extras: Map<String, String> = emptyMap()
)

/**
 * 动作能力声明：支持的源类型、所需权限、是否可回读状态，用于配置时校验。
 */
data class ActionCapability(
    val actionId: ActionId,
    val supportedSources: Set<String>,
    val requiredPermissions: List<String>,
    val canReadState: Boolean
)

object ActionCatalog {
    val capabilities: Map<ActionId, ActionCapability> = mapOf(
        ActionId.PLAY to ActionCapability(
            actionId = ActionId.PLAY,
            supportedSources = setOf("file_uri", "url"),
            requiredPermissions = listOf(android.Manifest.permission.INTERNET),
            canReadState = true
        ),
        ActionId.OPEN_FILE to ActionCapability(
            actionId = ActionId.OPEN_FILE,
            supportedSources = setOf("file_uri"),
            requiredPermissions = emptyList(),
            canReadState = false
        ),
        ActionId.EDIT_FILE to ActionCapability(
            actionId = ActionId.EDIT_FILE,
            supportedSources = setOf("file_uri"),
            requiredPermissions = emptyList(),
            canReadState = false
        ),
        ActionId.PICK_FILE to ActionCapability(
            actionId = ActionId.PICK_FILE,
            supportedSources = emptySet(),
            requiredPermissions = emptyList(),
            canReadState = false
        ),
        ActionId.OPEN_APP to ActionCapability(
            actionId = ActionId.OPEN_APP,
            supportedSources = setOf("package_name"),
            requiredPermissions = emptyList(),
            canReadState = false
        ),
        ActionId.SYSTEM_OP to ActionCapability(
            actionId = ActionId.SYSTEM_OP,
            supportedSources = setOf("system_op"),
            requiredPermissions = listOf(
                android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.CHANGE_WIFI_STATE
            ),
            canReadState = false
        )
    )
}

package com.widgetfactory.app.playback

import com.widgetfactory.app.model.ActionParam
import com.widgetfactory.app.model.FileRef

/**
 * 播放状态。
 */
enum class PlayState {
    IDLE,        // 无输入/空闲
    LOADING,     // 加载中
    PLAYING,     // 播放中
    PAUSED,      // 暂停
    ERROR        // 源不可用
}

/**
 * 循环方式。
 */
enum class LoopMode {
    NONE,     // 不循环
    ONE,      // 单曲循环
    ALL       // 列表循环（本应用无播放列表，等价单曲）
}

/**
 * 播放控制状态机。
 * - 有输入才显示播放控制；无输入不主动抓取源。
 * - 上一首/下一首切换播放源，仅播放时有效。
 * - 源不可用：标题"无播放内容"、封面纯色占位、播放键保留但点击无效。
 */
data class PlaybackState(
    val playState: PlayState = PlayState.IDLE,
    val loopMode: LoopMode = LoopMode.NONE,
    val title: String = "无播放内容",
    val sourceUri: String? = null,
    val currentIndex: Int = 0,
    val sources: List<String> = emptyList()
) {
    val hasInput: Boolean get() = sourceUri != null && sourceUri.isNotBlank()
    val showControls: Boolean get() = hasInput
    val isPlayEnabled: Boolean get() = hasInput && playState != PlayState.ERROR
}

/**
 * 从动作参数构建播放源列表。
 */
fun buildSources(param: ActionParam): List<String> {
    val sources = mutableListOf<String>()
    param.sourceUrl?.takeIf { it.isNotBlank() }?.let { sources.add(it) }
    param.fileRef?.let { sources.add(it.uri) }
    return sources
}

/**
 * 从 FileRef 构建播放源。
 */
fun sourceFromFileRef(file: FileRef?): String? = file?.uri

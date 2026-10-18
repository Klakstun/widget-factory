package com.widgetfactory.app.playback

/**
 * 播放控制器：管理播放状态机。
 *
 * 规则：
 * - 有输入才显示播放控制；无输入不主动抓取源。
 * - 上一首/下一首切换播放源，仅播放时有效。
 * - 源不可用：标题"无播放内容"、封面纯色占位、播放键保留但点击无效。
 * - 不检测源是否被后台刷掉；后台被杀则播放自然停止。
 */
class PlaybackController {

    var state: PlaybackState = PlaybackState()
        private set

    /** 设置播放源（来自配置）。无输入则保持 IDLE。 */
    fun setSources(sources: List<String>) {
        state = if (sources.isEmpty()) {
            state.copy(
                playState = PlayState.ERROR,
                title = "无播放内容",
                sourceUri = null,
                sources = emptyList(),
                currentIndex = 0
            )
        } else {
            state.copy(
                playState = PlayState.IDLE,
                sources = sources,
                currentIndex = 0,
                sourceUri = sources.first(),
                title = sources.first().substringAfterLast('/').ifEmpty { "播放" }
            )
        }
    }

    /** 播放。无输入或源不可用时无效。 */
    fun play() {
        if (!state.isPlayEnabled) return
        state = state.copy(playState = PlayState.PLAYING)
    }

    /** 暂停。 */
    fun pause() {
        if (state.playState == PlayState.PLAYING) {
            state = state.copy(playState = PlayState.PAUSED)
        }
    }

    /** 切换播放/暂停。 */
    fun togglePlay() {
        when (state.playState) {
            PlayState.PLAYING -> pause()
            PlayState.PAUSED, PlayState.IDLE -> play()
            else -> {}
        }
    }

    /** 上一首：仅播放时有效，切换到上一个源。 */
    fun previous() {
        if (state.playState != PlayState.PLAYING) return
        if (state.sources.isEmpty()) return
        val newIndex = (state.currentIndex - 1 + state.sources.size) % state.sources.size
        state = state.copy(
            currentIndex = newIndex,
            sourceUri = state.sources[newIndex],
            title = state.sources[newIndex].substringAfterLast('/').ifEmpty { "播放" }
        )
    }

    /** 下一首：仅播放时有效，切换到下一个源。 */
    fun next() {
        if (state.playState != PlayState.PLAYING) return
        if (state.sources.isEmpty()) return
        val newIndex = (state.currentIndex + 1) % state.sources.size
        state = state.copy(
            currentIndex = newIndex,
            sourceUri = state.sources[newIndex],
            title = state.sources[newIndex].substringAfterLast('/').ifEmpty { "播放" }
        )
    }

    /** 切换循环方式：NONE → ONE → ALL → NONE。 */
    fun toggleLoopMode() {
        val next = when (state.loopMode) {
            LoopMode.NONE -> LoopMode.ONE
            LoopMode.ONE -> LoopMode.ALL
            LoopMode.ALL -> LoopMode.NONE
        }
        state = state.copy(loopMode = next)
    }

    /**
     * 源不可用状态：标题"无播放内容"、封面纯色占位、播放键保留但点击无效。
     * 下次播放时用原配置恢复。
     */
    fun markSourceUnavailable() {
        state = state.copy(
            playState = PlayState.ERROR,
            title = "无播放内容"
        )
    }

    /** 恢复播放：用原配置的 sources 重新设置。 */
    fun restore(sources: List<String>) {
        setSources(sources)
    }
}

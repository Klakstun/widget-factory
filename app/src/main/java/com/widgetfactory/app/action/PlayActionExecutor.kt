package com.widgetfactory.app.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.widgetfactory.app.model.ActionId
import com.widgetfactory.app.model.ActionParam
import com.widgetfactory.app.playback.PlaybackService
import com.widgetfactory.app.ui.SnackbarManager

/**
 * 播放动作：Media3 ExoPlayer + MediaSessionService。
 * 支持本地 URI 与网络 URL，渐进式协议。
 */
class PlayActionExecutor : ActionExecutor {
    override val actionId = ActionId.PLAY

    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun execute(
        context: Context,
        param: ActionParam,
        snackbarManager: SnackbarManager?
    ): Boolean {
        val sourceUri = resolveSource(param)
        if (sourceUri == null) {
            snackbarManager?.show("无播放内容")
            return false
        }

        // 启动播放服务
        val serviceIntent = Intent(context, PlaybackService::class.java)
        context.startForegroundService(serviceIntent)

        // 通过 MediaController 设置播放源
        val sessionToken = SessionToken(context, android.content.ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get()
                controller?.setMediaItem(MediaItem.fromUri(sourceUri))
                controller?.prepare()
                controller?.play()
            } catch (e: Exception) {
                snackbarManager?.show("播放失败")
            }
        }, context.mainExecutor)

        return true
    }

    private fun resolveSource(param: ActionParam): Uri? {
        param.sourceUrl?.takeIf { it.isNotBlank() }?.let { return Uri.parse(it) }
        param.fileRef?.let { return Uri.parse(it.uri) }
        return null
    }
}

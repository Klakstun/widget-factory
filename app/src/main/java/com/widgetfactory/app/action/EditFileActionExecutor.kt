package com.widgetfactory.app.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.widgetfactory.app.model.ActionId
import com.widgetfactory.app.model.ActionParam
import com.widgetfactory.app.ui.SnackbarManager

/**
 * 编辑文件：Intent.ACTION_EDIT，失败降级 ACTION_VIEW。
 * 文件变更同步靠回前台校验 + 监听 + 手动重新导入兜底（此处仅发起编辑）。
 */
class EditFileActionExecutor : ActionExecutor {
    override val actionId = ActionId.EDIT_FILE

    override fun execute(
        context: Context,
        param: ActionParam,
        snackbarManager: SnackbarManager?
    ): Boolean {
        val fileRef = param.fileRef ?: run {
            snackbarManager?.show("未指定文件")
            return false
        }
        val uri = Uri.parse(fileRef.uri)
        val editIntent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(uri, fileRef.mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(editIntent)
            true
        } catch (e: Exception) {
            // 降级为 ACTION_VIEW
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, fileRef.mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(viewIntent)
                snackbarManager?.show("无编辑应用，已降级为打开")
                true
            } catch (e2: Exception) {
                snackbarManager?.show("无法打开该文件")
                false
            }
        }
    }
}

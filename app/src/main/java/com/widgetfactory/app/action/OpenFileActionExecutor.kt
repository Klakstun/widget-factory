package com.widgetfactory.app.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.widgetfactory.app.model.ActionId
import com.widgetfactory.app.model.ActionParam
import com.widgetfactory.app.ui.SnackbarManager

/**
 * 打开文件：Intent.ACTION_VIEW + MIME + FLAG_GRANT_READ_URI_PERMISSION。
 * 无匹配应用则系统提示。
 */
class OpenFileActionExecutor : ActionExecutor {
    override val actionId = ActionId.OPEN_FILE

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
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, fileRef.mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            snackbarManager?.show("没有可打开该文件的应用")
            false
        }
    }
}

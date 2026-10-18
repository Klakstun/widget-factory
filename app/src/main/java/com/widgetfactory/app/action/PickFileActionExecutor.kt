package com.widgetfactory.app.action

import android.content.Context
import android.content.Intent
import com.widgetfactory.app.model.ActionId
import com.widgetfactory.app.model.ActionParam
import com.widgetfactory.app.ui.SnackbarManager

/**
 * 选文件：SAF（ACTION_OPEN_DOCUMENT），产出统一文件引用 FileRef。
 * 注：在应用编辑器内由 ElementConfigPanel 直接处理结果；
 * 此执行器负责发起 SAF 选择器。
 */
class PickFileActionExecutor : ActionExecutor {
    override val actionId = ActionId.PICK_FILE

    override fun execute(
        context: Context,
        param: ActionParam,
        snackbarManager: SnackbarManager?
    ): Boolean {
        // SAF 选择器需要 Activity 上下文处理结果；此处仅发起 Intent。
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = param.mimeFilter
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            snackbarManager?.show("无法打开文件选择器")
            false
        }
    }
}

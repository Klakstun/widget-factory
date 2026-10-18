package com.widgetfactory.app.action

import android.content.Context
import android.content.Intent
import com.widgetfactory.app.model.ActionId
import com.widgetfactory.app.model.ActionParam
import com.widgetfactory.app.ui.SnackbarManager

/**
 * 打开应用：指定包名 Intent 启动。需权限声明（在配置项前显示警告）。
 */
class OpenAppActionExecutor : ActionExecutor {
    override val actionId = ActionId.OPEN_APP

    override fun execute(
        context: Context,
        param: ActionParam,
        snackbarManager: SnackbarManager?
    ): Boolean {
        val pkg = param.packageName ?: run {
            snackbarManager?.show("未指定应用包名")
            return false
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            snackbarManager?.show("未找到应用：$pkg")
            false
        }
    }
}

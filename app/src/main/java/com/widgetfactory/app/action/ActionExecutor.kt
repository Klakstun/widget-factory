package com.widgetfactory.app.action

import android.content.Context
import com.widgetfactory.app.model.ActionParam
import com.widgetfactory.app.ui.SnackbarManager

/**
 * 动作执行器接口。每个固定动作有一个实现。
 * 动作在应用内绑定并写入配置，运行时由 Widget/系统执行。
 */
interface ActionExecutor {
    val actionId: com.widgetfactory.app.model.ActionId

    /**
     * 执行动作。
     * @return true 表示执行成功（或已发起），false 表示失败。
     */
    fun execute(
        context: Context,
        param: ActionParam,
        snackbarManager: SnackbarManager? = null
    ): Boolean
}

package com.widgetfactory.app.action

import com.widgetfactory.app.model.ActionId

/**
 * 动作执行器注册表：将固定的 6 个 ActionId 映射到对应执行器。
 * 动作列表固定不可扩展。
 */
object ActionExecutorRegistry {
    private val executors: Map<ActionId, ActionExecutor> = mapOf(
        ActionId.PLAY to PlayActionExecutor(),
        ActionId.OPEN_FILE to OpenFileActionExecutor(),
        ActionId.EDIT_FILE to EditFileActionExecutor(),
        ActionId.PICK_FILE to PickFileActionExecutor(),
        ActionId.OPEN_APP to OpenAppActionExecutor(),
        ActionId.SYSTEM_OP to SystemOpActionExecutor()
    )

    fun get(actionId: ActionId): ActionExecutor? = executors[actionId]

    fun all(): Map<ActionId, ActionExecutor> = executors
}

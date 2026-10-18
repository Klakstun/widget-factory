package com.widgetfactory.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 应用级提示管理器：所有错误与轻提示统一走 SnackbarHost。
 * 组件内不画错误 UI，通过此 manager 发送提示。
 */
class SnackbarManager(
    private val hostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    fun show(message: String) {
        scope.launch {
            hostState.showSnackbar(message)
        }
    }

    fun show(message: String, actionLabel: String, onAction: () -> Unit) {
        scope.launch {
            val result = hostState.showSnackbar(message, actionLabel)
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }
}

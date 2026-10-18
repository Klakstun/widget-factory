package com.widgetfactory.app.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 接收 Widget 固定成功的回调，将 Widget ID 绑定到组件 ID。
 */
class WidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val componentId = intent.getStringExtra(EXTRA_COMPONENT_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID || componentId == null) return

        // 异步工作：使用 goAsync 保持广播存活，使用本地作用域避免 GlobalScope 泄漏
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                WidgetMappingStore.bind(context, widgetId, componentId)
                WidgetRefreshHelper.refreshAll(context, null)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_COMPONENT_ID = "component_id"
    }
}

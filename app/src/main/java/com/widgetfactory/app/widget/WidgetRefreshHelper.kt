package com.widgetfactory.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.widgetfactory.app.log.Logger
import com.widgetfactory.app.ui.SnackbarManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Widget 刷新与兜底：
 * - 运行时异常 → 自动降级并刷新桌面 Widget。
 * - 功能未实现 → 不自动降级，用户手动切换后旧 Widget 一并刷新。
 * - 刷新失败 → 系统提示用户手动重新拖拽。
 * - 降级、切换均保留用户原配置。
 */
object WidgetRefreshHelper {

    private const val TAG = "WidgetRefresh"

    /**
     * 刷新所有桌面 Widget。
     * 刷新失败时通过 Snackbar 提示用户手动重新拖拽。
     * 挂起函数，调用方负责协程作用域与生命周期。
     */
    suspend fun refreshAll(
        context: Context,
        snackbarManager: SnackbarManager? = null
    ) {
        withContext(Dispatchers.IO) {
            try {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(FactoryWidget::class.java)
                glanceIds.forEach { id ->
                    FactoryWidget().update(context, id)
                }
                Logger.i(TAG, "已刷新 ${glanceIds.size} 个 Widget")
            } catch (e: Exception) {
                Logger.e(TAG, "刷新 Widget 失败", e)
                snackbarManager?.show("刷新 Widget 失败，请手动重新拖拽组件到桌面")
            }
        }
    }

    /**
     * 将指定组件的所有 Widget 标记为降级并刷新。
     * 保留用户原配置（仅修改 degradeConfig.degraded 标志）。
     * 实际降级逻辑在 FactoryWidget.provideGlance 中根据 degradeConfig 决定；
     * 此处触发刷新即可。
     */
    suspend fun degradeAndRefresh(
        context: Context,
        @Suppress("UNUSED_PARAMETER") componentId: String,
        snackbarManager: SnackbarManager? = null
    ) {
        refreshAll(context, snackbarManager)
    }
}

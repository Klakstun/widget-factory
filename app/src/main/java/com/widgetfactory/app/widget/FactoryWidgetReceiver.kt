package com.widgetfactory.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * 桌面 Widget 接收器。
 * - onUpdate 由 Glance 自动处理（调用 FactoryWidget.provideGlance）。
 * - onDeleted 时解除 Widget 与组件的映射绑定。
 */
class FactoryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FactoryWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { id ->
            WidgetMappingStore.unbind(context, id)
        }
    }
}

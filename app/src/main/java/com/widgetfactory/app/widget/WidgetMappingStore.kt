package com.widgetfactory.app.widget

import android.content.Context
import com.widgetfactory.app.data.ComponentStore
import com.widgetfactory.app.model.Component

/**
 * 桌面 Widget ID 与组件 ID 的映射存储。
 * 桌面 Widget 放置后，记录其对应的组件配置 ID。
 */
object WidgetMappingStore {
    private const val PREFS = "widget_mapping"
    private const val KEY_PREFIX = "widget_"

    fun bind(context: Context, widgetId: Int, componentId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("$KEY_PREFIX$widgetId", componentId)
            .apply()
    }

    fun getComponentId(context: Context, widgetId: Int): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("$KEY_PREFIX$widgetId", null)
    }

    fun unbind(context: Context, widgetId: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove("$KEY_PREFIX$widgetId")
            .apply()
    }

    /** 获取 Widget 对应的组件配置。 */
    fun getComponent(context: Context, widgetId: Int): Component? {
        val componentId = getComponentId(context, widgetId) ?: return null
        return ComponentStore.load(context, componentId)
    }
}

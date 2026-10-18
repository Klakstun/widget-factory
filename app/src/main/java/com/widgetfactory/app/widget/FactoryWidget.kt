package com.widgetfactory.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceId
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.widgetfactory.app.log.Logger
import com.widgetfactory.app.model.Component
import com.widgetfactory.app.model.InnerRect

/**
 * 桌面 Widget 主体。
 *
 * - 正常：按内部 10×10 自由网格渲染。
 * - 渲染不了（异常或 degradeConfig 禁用精细渲染）：降级到系统原生渲染，
 *   元素按原相对位置就近落格到 5×5，仍可交互。
 * - 原始 10×10 配置保留，切回精细模式可恢复。
 */
class FactoryWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val component = try {
            val widgetId = extractWidgetId(id)
            WidgetMappingStore.getComponent(context, widgetId)
        } catch (e: Exception) {
            Logger.e("FactoryWidget", "读取组件配置失败", e)
            null
        }

        // 渲染模式在 composable 之外决定，避免在 composable 调用处直接 try-catch
        val useFineRendering = component != null &&
            component.degradeConfig.fineRenderingEnabled &&
            !component.degradeConfig.degraded

        provideContent {
            GlanceTheme {
                when {
                    component == null -> EmptyWidget()
                    useFineRendering -> FineRenderedWidget(component)
                    else -> DegradedWidget(component)
                }
            }
        }
    }

    @Composable
    private fun EmptyWidget() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Widget Factory（未配置）")
        }
    }

    /** 精细渲染：按内部 10×10 网格布局元素。 */
    @Composable
    private fun FineRenderedWidget(component: Component) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(4.dp)
        ) {
            // 用 10 行 × 10 列网格渲染，元素按 rect 占位
            Column(modifier = GlanceModifier.fillMaxSize()) {
                for (row in 0 until InnerRect.GRID_SIZE) {
                    Row(modifier = GlanceModifier.fillMaxSize()) {
                        for (col in 0 until InnerRect.GRID_SIZE) {
                            val el = component.elements.firstOrNull { e ->
                                col in e.rect.col until e.rect.right &&
                                    row in e.rect.row until e.rect.bottom
                            }
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .padding(1.dp)
                            ) {
                                if (el != null && el.rect.col == col && el.rect.row == row) {
                                    // 仅在元素左上角格子渲染元素内容
                                    WidgetElement(el)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** 降级渲染：元素按相对位置就近落格到 5×5，保留交互。 */
    @Composable
    private fun DegradedWidget(component: Component) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(4.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                for (row in 0 until 5) {
                    Row(modifier = GlanceModifier.fillMaxSize()) {
                        for (col in 0 until 5) {
                            val el = component.elements.firstOrNull { e ->
                                mapToOuterGrid(e.rect) == (row to col)
                            }
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                            ) {
                                if (el != null) WidgetElement(el)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun extractWidgetId(id: GlanceId): Int {
        // GlanceId 内部包含 appWidgetId；通过反射或 toString 解析
        return try {
            val field = id.javaClass.getDeclaredField("appWidgetId")
            field.isAccessible = true
            field.getInt(id)
        } catch (_: Exception) {
            // 降级：从 toString 中提取数字
            id.toString().filter { it.isDigit() }.toIntOrNull() ?: -1
        }
    }
}

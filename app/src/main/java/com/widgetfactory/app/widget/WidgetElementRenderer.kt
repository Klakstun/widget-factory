package com.widgetfactory.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.widgetfactory.app.model.Element
import com.widgetfactory.app.model.ElementType
import com.widgetfactory.app.model.InnerRect

/**
 * 在 Glance 中渲染单个元素（按内部 10×10 网格定位）。
 */
@Composable
fun WidgetElement(element: Element) {
    val modifier = GlanceModifier
        .padding(2.dp)
        .background(GlanceTheme.colors.surfaceVariant)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (element.type) {
            ElementType.BUTTON -> {
                Text(
                    text = element.content.text.ifEmpty { "按钮" },
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                )
            }
            ElementType.SLIDER -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("滑杆", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                    Text(
                        element.content.sliderValue.toString(),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                    )
                }
            }
            ElementType.ICON -> {
                Text("图", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
            }
            ElementType.TEXTBOX -> {
                Text(
                    text = element.content.text.ifEmpty { element.content.html.take(30) },
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                )
            }
            ElementType.COVER -> {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("封面", style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer))
                }
            }
        }
    }
}

/**
 * 将内部 10×10 坐标映射到外部 5×5 就近落格（用于降级渲染）。
 * 返回 (row, col) in [0,5)。
 */
fun mapToOuterGrid(rect: InnerRect): Pair<Int, Int> {
    // 10 内部 / 5 外部 = 2 内部格 per 外部格
    val col = rect.col / 2
    val row = rect.row / 2
    return row.coerceIn(0, 4) to col.coerceIn(0, 4)
}

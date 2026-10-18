package com.widgetfactory.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.widgetfactory.app.model.Element
import com.widgetfactory.app.model.InnerRect
import kotlin.math.roundToInt

/**
 * 内部 10×10 编辑网格。
 * - 元素坐标以内部小格为单位
 * - 拖动松手后判定重叠，重叠则回弹原位置
 * - 元素最小 2×2
 */
@Composable
fun InnerGrid(
    elements: List<Element>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onMove: (String, InnerRect) -> Unit,
    modifier: Modifier = Modifier
) {
    var gridPx by remember { mutableStateOf(0) }
    val cellPx = if (gridPx > 0) gridPx / InnerRect.GRID_SIZE else 0

    Column(modifier = modifier) {
        Text(
            "内部编辑 ${InnerRect.GRID_SIZE}×${InnerRect.GRID_SIZE}",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onSizeChanged { gridPx = it.width }
        ) {
            // 10×10 网格线背景
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until InnerRect.GRID_SIZE) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (c in 0 until InnerRect.GRID_SIZE) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }

            // 元素
            elements.forEach { element ->
                val isSelected = element.id == selectedId
                ElementCell(
                    element = element,
                    cellPx = cellPx,
                    isSelected = isSelected,
                    onClick = { onSelect(element.id) },
                    onMove = { newRect -> onMove(element.id, newRect) }
                )
            }
        }
    }
}

@Composable
private fun ElementCell(
    element: Element,
    cellPx: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onMove: (InnerRect) -> Unit
) {
    var dragOffset by remember(element.id) { mutableStateOf(Offset.Zero) }
    var startRect by remember(element.id) { mutableStateOf(element.rect) }

    val left = (element.rect.col * cellPx + dragOffset.x).roundToInt()
    val top = (element.rect.row * cellPx + dragOffset.y).roundToInt()
    val width = element.rect.width * cellPx
    val height = element.rect.height * cellPx

    Box(
        modifier = Modifier
            .offset { IntOffset(left, top) }
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondaryContainer
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(6.dp)
            )
            .pointerInput(element.id) {
                detectDragGestures(
                    onDragStart = {
                        startRect = element.rect
                        onClick()
                    },
                    onDrag = { _, dragAmount ->
                        dragOffset += dragAmount
                    },
                    onDragEnd = {
                        // 松手后按小格对齐并判定
                        if (cellPx > 0) {
                            val newCol = (startRect.col + (dragOffset.x / cellPx)).roundToInt()
                            val newRow = (startRect.row + (dragOffset.y / cellPx)).roundToInt()
                            val newRect = startRect.copy(row = newRow, col = newCol)
                            onMove(newRect)
                        }
                        dragOffset = Offset.Zero
                    },
                    onDragCancel = {
                        dragOffset = Offset.Zero
                    }
                )
            }
    ) {
        Text(
            text = element.type.label,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(2.dp)
        )
    }
}

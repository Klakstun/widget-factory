package com.widgetfactory.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.widgetfactory.app.model.ElementType

/**
 * 元素面板：5 种元素，点击添加到内部网格。
 */
@Composable
fun ElementPalette(
    onAdd: (ElementType) -> Unit,
    canAdd: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ElementType.entries.forEach { type ->
            FilterChip(
                selected = false,
                onClick = { if (canAdd) onAdd(type) },
                label = { Text(type.label) },
                enabled = canAdd,
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
    if (!canAdd) {
        Text(
            "已达最大元素数（12）",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

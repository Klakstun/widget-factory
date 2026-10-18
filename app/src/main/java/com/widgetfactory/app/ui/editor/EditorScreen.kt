package com.widgetfactory.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.widgetfactory.app.data.ComponentStore
import com.widgetfactory.app.grid.GridLogic
import com.widgetfactory.app.model.Component
import com.widgetfactory.app.ui.SnackbarManager
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    componentId: String,
    snackbarManager: SnackbarManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val existing = remember(componentId) {
        if (componentId == "new") null else ComponentStore.load(context, componentId)
    }
    val createdAt = existing?.createdAt ?: System.currentTimeMillis()
    val stableId = existing?.id ?: UUID.randomUUID().toString()
    val state = remember(componentId) {
        EditorState(
            existing ?: Component(
                id = stableId,
                name = "新组件",
                createdAt = createdAt,
                updatedAt = createdAt
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("组件编辑器") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val comp = state.toComponent(
                            id = stableId,
                            createdAt = createdAt,
                            version = Component.CURRENT_VERSION
                        )
                        ComponentStore.save(context, comp)
                        snackbarManager.show("已保存：${comp.name}")
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { state.name = it },
                label = { Text("组件名称") },
                modifier = Modifier.fillMaxWidth()
            )

            // 外部 span 调整
            SpanAdjuster(state)

            ExternalSpanGrid(span = state.span)

            ElementPalette(
                onAdd = { state.addElement(it) },
                canAdd = GridLogic.canAddElement(state.elements)
            )

            InnerGrid(
                elements = state.elements,
                selectedId = state.selectedElementId,
                onSelect = { state.selectedElementId = it },
                onMove = { id, rect -> state.moveElement(id, rect) }
            )

            // 选中元素属性面板
            state.selectedElement?.let { el ->
                ElementConfigPanel(
                    element = el,
                    onContentChange = { content -> state.updateContent(el.id, content) },
                    onActionChange = { action -> state.updateAction(el.id, action) }
                )
                Button(
                    onClick = { state.deleteElement(el.id) }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("删除元素")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SpanAdjuster(state: EditorState) {
    Column {
        Text("外部尺寸 (${state.span.widthSpan} × ${state.span.heightSpan})",
            style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    state.updateSpan(
                        (state.span.widthSpan - 1).coerceAtLeast(1),
                        state.span.heightSpan
                    )
                }
            ) { Text("宽 -") }
            Text(
                "${state.span.widthSpan}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 10.dp)
            )
            Button(
                onClick = {
                    state.updateSpan(
                        (state.span.widthSpan + 1).coerceAtMost(5),
                        state.span.heightSpan
                    )
                }
            ) { Text("宽 +") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    state.updateSpan(
                        state.span.widthSpan,
                        (state.span.heightSpan - 1).coerceAtLeast(1)
                    )
                }
            ) { Text("高 -") }
            Text(
                "${state.span.heightSpan}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 10.dp)
            )
            Button(
                onClick = {
                    state.updateSpan(
                        state.span.widthSpan,
                        (state.span.heightSpan + 1).coerceAtMost(5)
                    )
                }
            ) { Text("高 +") }
        }
    }
}

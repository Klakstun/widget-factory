package com.widgetfactory.app.ui.library

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.widgetfactory.app.data.ComponentStore
import com.widgetfactory.app.model.Component
import com.widgetfactory.app.ui.SnackbarManager
import com.widgetfactory.app.widget.FactoryWidgetReceiver
import com.widgetfactory.app.widget.WidgetPinReceiver
import com.widgetfactory.app.widget.WidgetRefreshHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    snackbarManager: SnackbarManager,
    onNewComponent: () -> Unit,
    onEditComponent: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var components by remember { mutableStateOf<List<Component>>(emptyList()) }

    LaunchedEffect(Unit) {
        components = withContext(Dispatchers.IO) {
            ComponentStore.loadAll(context)
        }
    }

    fun reload() {
        components = ComponentStore.loadAll(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("组件库") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewComponent) {
                Icon(Icons.Default.Add, contentDescription = "新建组件")
            }
        }
    ) { padding ->
        if (components.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("组件库为空", style = MaterialTheme.typography.titleMedium)
                Text("点击 + 新建组件", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(components, key = { it.id }) { comp ->
                    ComponentItem(
                        component = comp,
                        onEdit = { onEditComponent(comp.id) },
                        onDelete = {
                            // 删除库内条目不影响已放到桌面的 Widget
                            ComponentStore.delete(context, comp.id)
                            snackbarManager.show("已删除：${comp.name}")
                            reload()
                        },
                        onAddToHome = {
                            requestPinWidget(context, comp.id, scope, snackbarManager)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ComponentItem(
    component: Component,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddToHome: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(component.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "尺寸 ${component.span.widthSpan}×${component.span.heightSpan} | " +
                    "${component.elements.size} 个元素",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onAddToHome) {
                    Icon(Icons.Default.Add, contentDescription = "添加到桌面")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }
    }
}

/**
 * 请求将组件以桌面 Widget 形式放置到主屏幕。
 * 从组件库选组件 → 配置 → 完成 → 自动生成桌面 Widget。
 */
private fun requestPinWidget(
    context: android.content.Context,
    componentId: String,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarManager: SnackbarManager
) {
    val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
    val provider = ComponentName(context, FactoryWidgetReceiver::class.java)

    if (appWidgetManager?.isRequestPinAppWidgetSupported == true) {
        // 固定成功回调：将 widgetId 绑定到 componentId
        val successIntent = Intent(context, WidgetPinReceiver::class.java).apply {
            putExtra(WidgetPinReceiver.EXTRA_COMPONENT_ID, componentId)
        }
        val pinnedWidgetCallback = android.app.PendingIntent.getBroadcast(
            context,
            componentId.hashCode(),
            successIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val success = appWidgetManager.requestPinAppWidget(provider, null, pinnedWidgetCallback)
        if (success) {
            snackbarManager.show("请在桌面确认添加 Widget")
        } else {
            snackbarManager.show("当前桌面不支持固定 Widget")
        }
    } else {
        scope.launch {
            WidgetRefreshHelper.refreshAll(context, snackbarManager)
        }
        snackbarManager.show("请从桌面 Widget 列表中选择 Widget Factory")
    }
}

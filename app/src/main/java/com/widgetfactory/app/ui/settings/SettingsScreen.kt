package com.widgetfactory.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.widgetfactory.app.log.Logger
import com.widgetfactory.app.settings.SettingsStore
import com.widgetfactory.app.ui.SnackbarManager
import com.widgetfactory.app.widget.WidgetRefreshHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    snackbarManager: SnackbarManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var fineRendering by remember {
        mutableStateOf(SettingsStore.isFineRenderingEnabled(context))
    }
    var htmlCss by remember {
        mutableStateOf(SettingsStore.isHtmlExternalCssEnabled(context))
    }
    var htmlExtJs by remember {
        mutableStateOf(SettingsStore.isHtmlExternalJsEnabled(context))
    }
    var htmlInlineJs by remember {
        mutableStateOf(SettingsStore.isHtmlInlineJsEnabled(context))
    }
    var dynamicColor by remember {
        mutableStateOf(SettingsStore.isDynamicColorEnabled(context))
    }
    var themeMode by remember {
        mutableStateOf(SettingsStore.getThemeMode(context))
    }
    var themeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            // 降级与渲染
            Text("渲染与降级", style = MaterialTheme.typography.titleMedium)

            SettingsSwitch(
                title = "精细渲染（10×10 网格）",
                subtitle = "关闭后 Widget 使用系统原生 5×5 渲染",
                checked = fineRendering,
                onCheckedChange = {
                    fineRendering = it
                    SettingsStore.setFineRenderingEnabled(context, it)
                    scope.launch {
                        WidgetRefreshHelper.refreshAll(context, snackbarManager)
                    }
                }
            )

            SettingsSwitch(
                title = "HTML 外部 CSS",
                subtitle = "允许加载外部样式表",
                checked = htmlCss,
                onCheckedChange = {
                    htmlCss = it
                    SettingsStore.setHtmlExternalCssEnabled(context, it)
                }
            )

            SettingsSwitch(
                title = "HTML 外部 JS",
                subtitle = "允许加载外部脚本",
                checked = htmlExtJs,
                onCheckedChange = {
                    htmlExtJs = it
                    SettingsStore.setHtmlExternalJsEnabled(context, it)
                }
            )

            SettingsSwitch(
                title = "HTML 内联 JS",
                subtitle = "允许执行内联脚本",
                checked = htmlInlineJs,
                onCheckedChange = {
                    htmlInlineJs = it
                    SettingsStore.setHtmlInlineJsEnabled(context, it)
                }
            )

            Spacer(Modifier.height(8.dp))

            // 主题
            Text("主题", style = MaterialTheme.typography.titleMedium)
            SettingsSwitch(
                title = "动态取色（Dynamic Color）",
                subtitle = "仅 Android 12+ 生效，低版本回退预定义主题",
                checked = dynamicColor,
                onCheckedChange = {
                    dynamicColor = it
                    SettingsStore.setDynamicColorEnabled(context, it)
                }
            )

            // 主题模式选择
            ExposedDropdownMenuBox(
                expanded = themeMenuExpanded,
                onExpandedChange = { themeMenuExpanded = !themeMenuExpanded }
            ) {
                val modeLabel = when (themeMode) {
                    "light" -> "浅色"
                    "dark" -> "深色"
                    else -> "跟随系统"
                }
                OutlinedTextField(
                    value = modeLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("主题模式") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = themeMenuExpanded,
                    onDismissRequest = { themeMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("跟随系统") },
                        onClick = {
                            themeMode = "system"
                            SettingsStore.setThemeMode(context, "system")
                            themeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("浅色") },
                        onClick = {
                            themeMode = "light"
                            SettingsStore.setThemeMode(context, "light")
                            themeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("深色") },
                        onClick = {
                            themeMode = "dark"
                            SettingsStore.setThemeMode(context, "dark")
                            themeMenuExpanded = false
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 日志
            Text("日志", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    Logger.exportLogs(context)
                    snackbarManager.show("正在导出日志…")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("导出日志")
            }
            val logCount = Logger.listLogFiles().size
            Text("当前日志文件数：$logCount", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

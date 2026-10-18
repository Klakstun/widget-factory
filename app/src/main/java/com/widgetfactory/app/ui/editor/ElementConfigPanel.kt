package com.widgetfactory.app.ui.editor

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.widgetfactory.app.model.ActionId
import com.widgetfactory.app.model.ActionParam
import com.widgetfactory.app.model.ContentConfig
import com.widgetfactory.app.model.Element
import com.widgetfactory.app.model.FileRef
import com.widgetfactory.app.model.SystemOpId

/**
 * 选中元素的属性配置面板。
 * 按元素类型显示不同字段：文本/HTML/文件引用/动作绑定等。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElementConfigPanel(
    element: Element,
    onContentChange: (ContentConfig) -> Unit,
    onActionChange: (ActionParam?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // 持久化 URI 授权
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {
                // 忽略
            }
            val mime = context.contentResolver.getType(uri) ?: "*/*"
            val displayName = queryDisplayName(context, uri)
            val fileRef = FileRef(
                uri = uri.toString(),
                mime = mime,
                displayName = displayName,
                grantTimeout = 0L
            )
            onContentChange(element.content.copy(fileRef = fileRef))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("元素属性：${element.type.label}", style = MaterialTheme.typography.titleMedium)

        // 按类型显示内容字段
        when (element.type) {
            com.widgetfactory.app.model.ElementType.BUTTON -> {
                OutlinedTextField(
                    value = element.content.text,
                    onValueChange = { onContentChange(element.content.copy(text = it)) },
                    label = { Text("按钮文本") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            com.widgetfactory.app.model.ElementType.SLIDER -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = element.content.sliderMin.toString(),
                        onValueChange = {
                            onContentChange(
                                element.content.copy(sliderMin = it.toFloatOrNull() ?: 0f)
                            )
                        },
                        label = { Text("最小值") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = element.content.sliderMax.toString(),
                        onValueChange = {
                            onContentChange(
                                element.content.copy(sliderMax = it.toFloatOrNull() ?: 100f)
                            )
                        },
                        label = { Text("最大值") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = element.content.sliderStep.toString(),
                    onValueChange = {
                        onContentChange(
                            element.content.copy(sliderStep = it.toFloatOrNull() ?: 1f)
                        )
                    },
                    label = { Text("步长") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            com.widgetfactory.app.model.ElementType.ICON -> {
                OutlinedTextField(
                    value = element.content.iconResource,
                    onValueChange = { onContentChange(element.content.copy(iconResource = it)) },
                    label = { Text("图标资源名") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            com.widgetfactory.app.model.ElementType.TEXTBOX -> {
                OutlinedTextField(
                    value = element.content.text,
                    onValueChange = { onContentChange(element.content.copy(text = it)) },
                    label = { Text("文本内容") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = element.content.html,
                    onValueChange = { onContentChange(element.content.copy(html = it)) },
                    label = { Text("HTML 内容（内联或 iframe）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Text("或引用外部 HTML/文本文件：", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { pickFileLauncher.launch(arrayOf("text/*", "text/html")) }) {
                    Text("选择文件")
                }
                element.content.fileRef?.let {
                    Text("已选：${it.displayName}", style = MaterialTheme.typography.bodySmall)
                }
            }
            com.widgetfactory.app.model.ElementType.COVER -> {
                Text("封面图片：", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { pickFileLauncher.launch(arrayOf("image/*")) }) {
                    Text("选择图片")
                }
                element.content.imageRef?.let {
                    Text("已选：${it.displayName}", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = element.content.coverColorRole,
                    onValueChange = { onContentChange(element.content.copy(coverColorRole = it)) },
                    label = { Text("纯色占位（Material 3 角色名）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 动作绑定
        Text("绑定动作", style = MaterialTheme.typography.titleSmall)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            val currentAction = element.action?.actionId
            OutlinedTextField(
                value = currentAction?.label ?: "无",
                onValueChange = {},
                readOnly = true,
                label = { Text("动作") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("无") },
                    onClick = {
                        onActionChange(null)
                        expanded = false
                    }
                )
                ActionId.entries.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.label) },
                        onClick = {
                            onActionChange(ActionParam(actionId = action))
                            expanded = false
                        }
                    )
                }
            }
        }

        // 动作参数配置
        element.action?.let { action ->
            ActionParamFields(
                action = action,
                onActionChange = onActionChange,
                onPickFile = { mime -> pickFileLauncher.launch(arrayOf(mime)) }
            )
        }
    }
}

@Composable
private fun ActionParamFields(
    action: ActionParam,
    onActionChange: (ActionParam) -> Unit,
    onPickFile: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (action.actionId) {
            ActionId.PLAY -> {
                OutlinedTextField(
                    value = action.sourceUrl ?: "",
                    onValueChange = { onActionChange(action.copy(sourceUrl = it)) },
                    label = { Text("播放源 URL（网络）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("或选择本地音频文件：", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { onPickFile("audio/*") }) { Text("选文件") }
            }
            ActionId.OPEN_FILE, ActionId.EDIT_FILE -> {
                Text("选择文件：", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { onPickFile("*/*") }) { Text("选文件") }
            }
            ActionId.PICK_FILE -> {
                OutlinedTextField(
                    value = action.mimeFilter,
                    onValueChange = { onActionChange(action.copy(mimeFilter = it)) },
                    label = { Text("MIME 过滤（如 audio/*）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ActionId.OPEN_APP -> {
                OutlinedTextField(
                    value = action.packageName ?: "",
                    onValueChange = { onActionChange(action.copy(packageName = it)) },
                    label = { Text("应用包名") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ActionId.SYSTEM_OP -> {
                var opExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = opExpanded,
                    onExpandedChange = { opExpanded = !opExpanded }
                ) {
                    OutlinedTextField(
                        value = action.systemOpId?.label ?: "选择系统操作",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = opExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = opExpanded,
                        onDismissRequest = { opExpanded = false }
                    ) {
                        SystemOpId.entries.forEach { op ->
                            DropdownMenuItem(
                                text = { Text(op.label) },
                                onClick = {
                                    onActionChange(action.copy(systemOpId = op))
                                    opExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 权限警告
        val requiredPerms = com.widgetfactory.app.model.ActionCatalog
            .capabilities[action.actionId]?.requiredPermissions.orEmpty()
        if (requiredPerms.isNotEmpty()) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "需要权限：${requiredPerms.joinToString()}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else uri.toString()
        } ?: uri.toString()
    } catch (_: Exception) {
        uri.toString()
    }
}

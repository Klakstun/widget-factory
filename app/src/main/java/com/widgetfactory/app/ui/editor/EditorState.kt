package com.widgetfactory.app.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.widgetfactory.app.grid.GridLogic
import com.widgetfactory.app.model.Component
import com.widgetfactory.app.model.Element
import com.widgetfactory.app.model.ElementType
import com.widgetfactory.app.model.ExternalSpan
import com.widgetfactory.app.model.InnerRect
import java.util.UUID

/**
 * 编辑器状态：持有当前编辑的组件、选中元素、拖动预览等。
 */
class EditorState(initial: Component) {
    var name by mutableStateOf(initial.name)
    var span by mutableStateOf(initial.span)
    val elements = mutableStateListOf<Element>().apply { addAll(initial.elements) }
    var selectedElementId by mutableStateOf<String?>(null)
    var degradeConfig = initial.degradeConfig

    val selectedElement: Element?
        get() = elements.find { it.id == selectedElementId }

    fun addElement(type: ElementType) {
        if (!GridLogic.canAddElement(elements)) return
        val id = UUID.randomUUID().toString()
        // 寻找一个不重叠的起始位置
        var placed = false
        for (row in 0 until InnerRect.GRID_SIZE step 1) {
            for (col in 0 until InnerRect.GRID_SIZE step 1) {
                val rect = type.defaultRect.copy(row = row, col = col)
                if (GridLogic.isValidPlacement(rect, elements)) {
                    elements.add(Element(id = id, type = type, rect = rect))
                    selectedElementId = id
                    placed = true
                    break
                }
            }
            if (placed) break
        }
        if (placed) reboundSpan()
    }

    fun moveElement(id: String, newRect: InnerRect) {
        val idx = elements.indexOfFirst { it.id == id }
        if (idx < 0) return
        val clamped = GridLogic.clampRect(newRect)
        if (GridLogic.isValidPlacement(clamped, elements, excludeId = id)) {
            elements[idx] = elements[idx].copy(rect = clamped)
            reboundSpan()
        }
        // 重叠则回弹原位置（不更新）
    }

    fun resizeElement(id: String, newWidth: Int, newHeight: Int) {
        val el = elements.firstOrNull { it.id == id } ?: return
        val newRect = el.rect.copy(width = newWidth, height = newHeight)
        val clamped = GridLogic.clampRect(newRect)
        if (GridLogic.isValidPlacement(clamped, elements, excludeId = id)) {
            val idx = elements.indexOfFirst { it.id == id }
            elements[idx] = el.copy(rect = clamped)
            reboundSpan()
        }
    }

    fun updateElement(id: String, updater: (Element) -> Element) {
        val idx = elements.indexOfFirst { it.id == id }
        if (idx >= 0) {
            elements[idx] = updater(elements[idx])
        }
    }

    fun updateContent(id: String, content: com.widgetfactory.app.model.ContentConfig) {
        updateElement(id) { it.copy(content = content) }
    }

    fun updateAction(id: String, action: com.widgetfactory.app.model.ActionParam?) {
        updateElement(id) { it.copy(action = action) }
    }

    fun deleteElement(id: String) {
        elements.removeAll { it.id == id }
        if (selectedElementId == id) selectedElementId = null
        reboundSpan()
    }

    fun updateSpan(widthSpan: Int, heightSpan: Int) {
        span = ExternalSpan(
            widthSpan.coerceIn(ExternalSpan.MIN, ExternalSpan.MAX),
            heightSpan.coerceIn(ExternalSpan.MIN, ExternalSpan.MAX)
        )
    }

    /** 内部布局变化时回弹外部 span 到最小容量尺寸。 */
    private fun reboundSpan() {
        val min = GridLogic.calculateMinSpan(elements)
        if (span.widthSpan < min.widthSpan || span.heightSpan < min.heightSpan) {
            span = min
        }
    }

    fun toComponent(id: String, createdAt: Long, version: Int): Component {
        val now = System.currentTimeMillis()
        return Component(
            id = id,
            name = name,
            createdAt = createdAt,
            updatedAt = now,
            span = span,
            elements = elements.toList(),
            degradeConfig = degradeConfig,
            version = version
        )
    }
}

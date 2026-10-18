package com.widgetfactory.app.grid

import com.widgetfactory.app.model.Component
import com.widgetfactory.app.model.Element
import com.widgetfactory.app.model.ExternalSpan
import com.widgetfactory.app.model.InnerRect

/**
 * 网格系统逻辑：
 * - 外部 span 动态下限计算（由内部元素布局决定）
 * - 内部 10×10 碰撞检测
 * - 元素数量上限校验
 */
object GridLogic {

    /** 内部 10×10 与外部 5×5 的比例：1 个外部格 = 2 个内部小格。 */
    private const val INNER_PER_OUTER = InnerRect.GRID_SIZE / ExternalSpan.MAX

    /**
     * 根据内部元素布局计算最小容纳外部 span。
     * 取所有元素右边缘最大值 / 2 向上取整为宽，下边缘最大值 / 2 向上取整为高，
     * 限制在 [1, 5]。
     */
    fun calculateMinSpan(elements: List<Element>): ExternalSpan {
        if (elements.isEmpty()) return ExternalSpan(1, 1)
        val maxRight = elements.maxOf { it.rect.right }
        val maxBottom = elements.maxOf { it.rect.bottom }
        val width = (maxRight + INNER_PER_OUTER - 1) / INNER_PER_OUTER
        val height = (maxBottom + INNER_PER_OUTER - 1) / INNER_PER_OUTER
        return ExternalSpan(
            widthSpan = width.coerceIn(ExternalSpan.MIN, ExternalSpan.MAX),
            heightSpan = height.coerceIn(ExternalSpan.MIN, ExternalSpan.MAX)
        )
    }

    /**
     * 回弹外部 span 到最小容量尺寸。
     * 仅修改外部 span，内部元素不动。
     */
    fun reboundSpan(component: Component): Component {
        val minSpan = calculateMinSpan(component.elements)
        return if (component.span.widthSpan < minSpan.widthSpan ||
            component.span.heightSpan < minSpan.heightSpan
        ) {
            component.copy(span = minSpan)
        } else {
            component
        }
    }

    /**
     * 查找与 [candidate] 重叠的元素（排除 [excludeId]）。
     * 格子独占，边接触不算重叠。
     */
    fun findCollision(
        candidate: InnerRect,
        elements: List<Element>,
        excludeId: String? = null
    ): Element? {
        return elements.firstOrNull { el ->
            el.id != excludeId && el.rect.overlaps(candidate)
        }
    }

    /** 判断候选位置是否合法：不越界、不重叠。 */
    fun isValidPlacement(
        candidate: InnerRect,
        elements: List<Element>,
        excludeId: String? = null
    ): Boolean {
        if (candidate.isOutOfBounds()) return false
        return findCollision(candidate, elements, excludeId) == null
    }

    /** 是否还能添加元素（单组件最多 12 个）。 */
    fun canAddElement(elements: List<Element>): Boolean =
        elements.size < Component.MAX_ELEMENTS

    /**
     * 将元素按内部小格对齐并夹紧到合法范围。
     * 返回新的 InnerRect（不保证不重叠，需再调 isValidPlacement）。
     */
    fun clampRect(rect: InnerRect): InnerRect {
        val col = rect.col.coerceIn(0, InnerRect.GRID_SIZE - rect.width)
        val row = rect.row.coerceIn(0, InnerRect.GRID_SIZE - rect.height)
        val width = rect.width.coerceIn(InnerRect.MIN_SIZE, InnerRect.GRID_SIZE)
        val height = rect.height.coerceIn(InnerRect.MIN_SIZE, InnerRect.GRID_SIZE)
        return InnerRect(row = row, col = col, width = width, height = height)
    }
}

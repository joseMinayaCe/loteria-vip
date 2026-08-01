package com.loteriavip.app.presentation.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import kotlin.math.max

class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        var height = 0
        var lineLength = 0
        var lineThickness = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue

            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            val lp = child.layoutParams as MarginLayoutParams
            val childWidth = child.measuredWidth + lp.leftMargin + lp.rightMargin
            val childHeight = child.measuredHeight + lp.topMargin + lp.bottomMargin

            if (lineLength + childWidth > width) {
                height += lineThickness
                lineLength = childWidth
                lineThickness = childHeight
            } else {
                lineLength += childWidth
                lineThickness = max(lineThickness, childHeight)
            }
        }
        height += lineThickness + paddingTop + paddingBottom
        setMeasuredDimension(
            resolveSize(MeasureSpec.getSize(widthMeasureSpec), widthMeasureSpec),
            resolveSize(height, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = width - paddingRight
        val lines = mutableListOf<MutableList<View>>()
        var currentLine = mutableListOf<View>()
        var x = paddingLeft

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue

            val lp = child.layoutParams as MarginLayoutParams
            val childWidth = child.measuredWidth + lp.leftMargin + lp.rightMargin

            if (x + childWidth > width && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = mutableListOf()
                x = paddingLeft
            }
            currentLine.add(child)
            x += childWidth
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        var y = paddingTop
        for (line in lines) {
            var lineThickness = 0
            var lineLineWidth = 0
            for (child in line) {
                val lp = child.layoutParams as MarginLayoutParams
                lineLineWidth += child.measuredWidth + lp.leftMargin + lp.rightMargin
            }

            val totalAvailableWidth = (r - l) - paddingLeft - paddingRight
            val offset = if (totalAvailableWidth > lineLineWidth) (totalAvailableWidth - lineLineWidth) / 2 else 0

            var currentX = paddingLeft + offset
            for (child in line) {
                val lp = child.layoutParams as MarginLayoutParams
                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight

                val childLeft = currentX + lp.leftMargin
                val childTop = y + lp.topMargin
                child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)

                currentX += lp.leftMargin + childWidth + lp.rightMargin
                lineThickness = max(lineThickness, lp.topMargin + childHeight + lp.bottomMargin)
            }
            y += lineThickness
        }
    }

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams {
        return MarginLayoutParams(p)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    override fun checkLayoutParams(p: LayoutParams?): Boolean {
        return p is MarginLayoutParams
    }
}

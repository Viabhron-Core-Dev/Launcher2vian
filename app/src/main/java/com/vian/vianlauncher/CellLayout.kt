package com.vian.vianlauncher

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

class CellLayout(
    context: Context,
    val columnCount: Int,
    val rowCount: Int,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    val occupancyMap = Array(columnCount) { BooleanArray(rowCount) }
    var cellWidth = 0
        private set
    var cellHeight = 0
        private set

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (columnCount > 0 && rowCount > 0) {
            cellWidth = widthSize / columnCount
            cellHeight = heightSize / rowCount
        }

        val childWidthSpec = MeasureSpec.makeMeasureSpec(cellWidth, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(cellHeight, MeasureSpec.EXACTLY)

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                child.measure(childWidthSpec, childHeightSpec)
            }
        }

        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val cellInfo = child.tag as? CellInfo
                if (cellInfo != null) {
                    val left = cellInfo.cellX * cellWidth
                    val top = cellInfo.cellY * cellHeight
                    child.layout(left, top, left + cellWidth, top + cellHeight)
                }
            }
        }
    }

    fun placeView(view: View, cellX: Int, cellY: Int) {
        if (cellX !in 0 until columnCount || cellY !in 0 until rowCount) {
            AppLogger.e("CellLayout", "Rejected out-of-range placement: cellX=$cellX, cellY=$cellY")
            return
        }
        AppLogger.d("CellLayout", "Placed view at cellX=$cellX, cellY=$cellY")
        view.tag = CellInfo(cellX, cellY)
        addView(view)
        occupyCell(cellX, cellY)
    }

    fun isOccupied(cellX: Int, cellY: Int): Boolean {
        if (cellX !in 0 until columnCount || cellY !in 0 until rowCount) return true
        return occupancyMap[cellX][cellY]
    }

    fun occupyCell(cellX: Int, cellY: Int) {
        if (cellX in 0 until columnCount && cellY in 0 until rowCount) {
            occupancyMap[cellX][cellY] = true
        }
    }

    fun vacateCell(cellX: Int, cellY: Int) {
        if (cellX in 0 until columnCount && cellY in 0 until rowCount) {
            occupancyMap[cellX][cellY] = false
        }
    }
}

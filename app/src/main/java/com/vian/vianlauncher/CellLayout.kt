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
                val cellInfo = child.tag as? CellInfo
                val spanX = cellInfo?.spanX ?: 1
                val spanY = cellInfo?.spanY ?: 1
                
                val childWidthSpec = MeasureSpec.makeMeasureSpec(cellWidth * spanX, MeasureSpec.EXACTLY)
                val childHeightSpec = MeasureSpec.makeMeasureSpec(cellHeight * spanY, MeasureSpec.EXACTLY)
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
                    val spanX = cellInfo.spanX
                    val spanY = cellInfo.spanY
                    val left = cellInfo.cellX * cellWidth
                    val top = cellInfo.cellY * cellHeight
                    child.layout(left, top, left + cellWidth * spanX, top + cellHeight * spanY)
                }
            }
        }
    }

    fun placeView(view: View, cellX: Int, cellY: Int, spanX: Int = 1, spanY: Int = 1) {
        if (cellX < 0 || cellX + spanX > columnCount || cellY < 0 || cellY + spanY > rowCount) {
            AppLogger.e("CellLayout", "Rejected out-of-range placement: cellX=$cellX, cellY=$cellY, spanX=$spanX, spanY=$spanY")
            return
        }
        AppLogger.d("CellLayout", "Placed view at cellX=$cellX, cellY=$cellY, spanX=$spanX, spanY=$spanY")
        view.tag = CellInfo(cellX, cellY, spanX, spanY)
        addView(view)
        occupyCell(cellX, cellY, spanX, spanY)
    }

    fun isOccupied(cellX: Int, cellY: Int, spanX: Int = 1, spanY: Int = 1): Boolean {
        if (cellX < 0 || cellX + spanX > columnCount || cellY < 0 || cellY + spanY > rowCount) return true
        for (x in cellX until cellX + spanX) {
            for (y in cellY until cellY + spanY) {
                if (occupancyMap[x][y]) return true
            }
        }
        return false
    }

    fun occupyCell(cellX: Int, cellY: Int, spanX: Int = 1, spanY: Int = 1) {
        if (cellX >= 0 && cellX + spanX <= columnCount && cellY >= 0 && cellY + spanY <= rowCount) {
            for (x in cellX until cellX + spanX) {
                for (y in cellY until cellY + spanY) {
                    occupancyMap[x][y] = true
                }
            }
        }
    }

    fun vacateCell(cellX: Int, cellY: Int, spanX: Int = 1, spanY: Int = 1) {
        if (cellX >= 0 && cellX + spanX <= columnCount && cellY >= 0 && cellY + spanY <= rowCount) {
            for (x in cellX until cellX + spanX) {
                for (y in cellY until cellY + spanY) {
                    occupancyMap[x][y] = false
                }
            }
        }
    }
}

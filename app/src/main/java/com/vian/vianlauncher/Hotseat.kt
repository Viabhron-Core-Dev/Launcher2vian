package com.vian.vianlauncher

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

class Hotseat(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    val cellLayout = CellLayout(context, 5, 1)

    init {
        addView(cellLayout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun placeView(view: View, cellX: Int, cellY: Int) {
        cellLayout.placeView(view, cellX, cellY)
    }

    fun clearItems() {
        cellLayout.removeAllViews()
        for (x in 0 until 5) {
            cellLayout.vacateCell(x, 0)
        }
    }
}

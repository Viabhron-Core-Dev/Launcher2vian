package com.vian.vianlauncher

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout

class Hotseat(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    init {
        orientation = HORIZONTAL
        weightSum = 5f
    }

    fun placeView(view: View, index: Int, cellY: Int) {
        if (index !in 0 until 5) return
        AppLogger.d("Hotseat", "Placed view at index=$index")
        
        val slot = getChildAt(index) as? FrameLayout ?: return
        slot.removeAllViews()
        slot.addView(view)
    }

    fun clearItems() {
        removeAllViews()
        for (i in 0 until 5) {
            val slot = FrameLayout(context).apply {
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            }
            addView(slot)
        }
    }
}

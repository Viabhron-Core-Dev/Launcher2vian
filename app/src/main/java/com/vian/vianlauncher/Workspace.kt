package com.vian.vianlauncher

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout

class Workspace(
    context: Context,
    attrs: AttributeSet? = null
) : HorizontalScrollView(context, attrs) {

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
    }
    
    val pages = mutableListOf<CellLayout>()
    var currentPage = 0
    var onPageChangeListener: ((Int) -> Unit)? = null

    init {
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        addView(container, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
    }

    fun setup(columns: Int, rows: Int) {
        AppLogger.d("Workspace", "Setup with columns=$columns, rows=$rows")
        container.removeAllViews()
        pages.clear()

        for (i in 0..2) {
            val page = CellLayout(context, columns, rows).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            pages.add(page)
            container.addView(page)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        for (page in pages) {
            val lp = page.layoutParams
            lp.width = w
            page.layoutParams = lp
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
            val scrollX = scrollX
            val pageWidth = width
            if (pageWidth > 0) {
                val targetPage = (scrollX + pageWidth / 2) / pageWidth
                val newPage = targetPage.coerceIn(0, pages.size - 1)
                smoothScrollTo(newPage * pageWidth, 0)
                
                if (newPage != currentPage) {
                    currentPage = newPage
                    AppLogger.d("Workspace", "Scrolled to page $currentPage")
                    onPageChangeListener?.invoke(currentPage)
                }
            }
            return true
        }
        return super.onTouchEvent(ev)
    }
}

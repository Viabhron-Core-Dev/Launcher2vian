package com.vian.vianlauncher

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.animation.ObjectAnimator

class Workspace(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    val pages = mutableListOf<CellLayout>()
    var currentPage = 0
    var onPageChangeListener: ((Int) -> Unit)? = null

    var onCellTap: ((page: Int, cellX: Int, cellY: Int) -> Unit)? = null
    var onCellLongPress: ((page: Int, cellX: Int, cellY: Int) -> Unit)? = null

    var isLongPressing = false

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val page = pages.getOrNull(currentPage) ?: return false
            if (page.cellWidth > 0 && page.cellHeight > 0) {
                val absoluteX = e.x + scrollX
                val pageStartX = currentPage * width
                val xOnPage = absoluteX - pageStartX
                val cellX = (xOnPage / page.cellWidth).toInt()
                val cellY = (e.y / page.cellHeight).toInt()
                if (page.isOccupied(cellX, cellY)) {
                    onCellTap?.invoke(currentPage, cellX, cellY)
                }
            }
            return true
        }
        override fun onLongPress(e: MotionEvent) {
            isLongPressing = true
            AppLogger.d("Workspace", "onLongPress at ${e.x}, ${e.y}")
            val page = pages.getOrNull(currentPage) ?: return
            if (page.cellWidth > 0 && page.cellHeight > 0) {
                val absoluteX = e.x + scrollX
                val pageStartX = currentPage * width
                val xOnPage = absoluteX - pageStartX
                val cellX = (xOnPage / page.cellWidth).toInt()
                val cellY = (e.y / page.cellHeight).toInt()
                onCellLongPress?.invoke(currentPage, cellX, cellY)
            }
        }
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1 != null) {
                if (e1.x - e2.x > 50 && Math.abs(velocityX) > 200) {
                    scrollToPage(currentPage + 1)
                    return true
                } else if (e2.x - e1.x > 50 && Math.abs(velocityX) > 200) {
                    scrollToPage(currentPage - 1)
                    return true
                }
            }
            return false
        }
    })

    init {
        clipChildren = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        
        val childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        
        for (i in 0 until childCount) {
            getChildAt(i).measure(childWidthSpec, childHeightSpec)
        }
        
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val left = i * width
            child.layout(left, 0, left + width, height)
        }
        scrollTo(currentPage * width, 0)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            isLongPressing = false
        }
        gestureDetector.onTouchEvent(ev)
        val intercept = isLongPressing
        if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
            isLongPressing = false
        }
        return intercept
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
            isLongPressing = false
        }
        return true
    }

    fun setup(columns: Int, rows: Int, pagesCount: Int = 3) {
        AppLogger.d("Workspace", "Setup with columns=$columns, rows=$rows, pages=$pagesCount")
        removeAllViews()
        pages.clear()

        for (i in 0 until pagesCount) {
            val page = CellLayout(context, columns, rows)
            pages.add(page)
            addView(page)
        }
    }

    fun scrollToPage(pageIndex: Int) {
        if (pages.isEmpty()) return
        val newPage = when {
            pageIndex < 0 -> pages.size - 1
            pageIndex >= pages.size -> 0
            else -> pageIndex
        }
        if (newPage != currentPage) {
            currentPage = newPage
            AppLogger.d("Workspace", "Page changed to: $currentPage")
            onPageChangeListener?.invoke(currentPage)
        }
        ObjectAnimator.ofInt(this, "scrollX", currentPage * width).setDuration(250).start()
    }
}

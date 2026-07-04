package com.vian.vianlauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class DragLayer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var isDragging = false
    private var dragBitmap: Bitmap? = null
    private var dragX = 0f
    private var dragY = 0f
    private var touchOffsetX = 0f
    private var touchOffsetY = 0f
    private val paint = Paint().apply { alpha = 200 }
    
    var dragController: DragController? = null

    fun startDrag(bitmap: Bitmap, x: Float, y: Float, touchX: Float, touchY: Float) {
        dragBitmap = bitmap
        touchOffsetX = touchX - x
        touchOffsetY = touchY - y
        dragX = touchX - touchOffsetX
        dragY = touchY - touchOffsetY
        isDragging = true
        AppLogger.d("DragLayer", "Drag started at $dragX, $dragY")
        invalidate()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isDragging) return true
        return super.onInterceptTouchEvent(ev)
    }

    private var lastLogTime = 0L

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isDragging) return super.onTouchEvent(ev)

        when (ev.action) {
            MotionEvent.ACTION_MOVE -> {
                dragX = ev.x - touchOffsetX
                dragY = ev.y - touchOffsetY
                val now = System.currentTimeMillis()
                if (now - lastLogTime > 100) {
                    AppLogger.d("DragLayer", "Drag move to $dragX, $dragY")
                    lastLogTime = now
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                AppLogger.d("DragLayer", "Drag end at $dragX, $dragY")
                dragController?.resolveDrop(ev.x, ev.y)
                isDragging = false
                dragBitmap = null
                invalidate()
            }
        }
        return true
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (isDragging) {
            dragBitmap?.let {
                canvas.drawBitmap(it, dragX, dragY, paint)
            }
        }
    }
}

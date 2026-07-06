package com.vian.vianlauncher

import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DragController(
    private val activity: HomeActivity,
    private val workspace: Workspace,
    private val hotseat: Hotseat,
    private val dragLayer: DragLayer
) {
    private var draggedView: View? = null
    private var draggedItem: WorkspaceItem? = null
    private var fromPage = -1
    private var fromCellX = -1
    private var fromCellY = -1
    private var isHotseatItem = false

    fun startDrag(view: View, item: WorkspaceItem?, page: Int, cellX: Int, cellY: Int, isHotseat: Boolean) {
        draggedView = view
        draggedItem = item
        fromPage = page
        fromCellX = cellX
        fromCellY = cellY
        isHotseatItem = isHotseat

        view.visibility = View.INVISIBLE

        val location = IntArray(2)
        view.getLocationOnScreen(location)

        view.isDrawingCacheEnabled = true
        view.buildDrawingCache()
        val bitmap = view.drawingCache?.let { android.graphics.Bitmap.createBitmap(it) }
        view.isDrawingCacheEnabled = false

        if (bitmap != null) {
            val touchLocation = IntArray(2)
            dragLayer.getLocationOnScreen(touchLocation)
            
            val viewX = location[0].toFloat() - touchLocation[0].toFloat()
            val viewY = location[1].toFloat() - touchLocation[1].toFloat()

            val touchX = viewX + view.width / 2f
            val touchY = viewY + view.height / 2f

            dragLayer.startDrag(bitmap, viewX, viewY, touchX, touchY)
            AppLogger.d("DragController", "Drag started from ${if (isHotseat) "Hotseat" else "Workspace page $page"} cell($cellX, $cellY)")
        } else {
            view.visibility = View.VISIBLE
        }
    }

    fun resolveDrop(screenX: Float, screenY: Float): Boolean {
        val view = draggedView ?: return false
        val item = draggedItem

        val dragLayerLocation = IntArray(2)
        dragLayer.getLocationOnScreen(dragLayerLocation)
        val absoluteScreenX = screenX + dragLayerLocation[0]
        val absoluteScreenY = screenY + dragLayerLocation[1]

        val workspaceLocation = IntArray(2)
        workspace.getLocationOnScreen(workspaceLocation)

        val xInWorkspace = absoluteScreenX - workspaceLocation[0]
        val yInWorkspace = absoluteScreenY - workspaceLocation[1]

        val currentPage = workspace.currentPage
        val page = workspace.pages.getOrNull(currentPage)
        
        var dropped = false

        if (page != null && page.cellWidth > 0 && page.cellHeight > 0) {
            val absoluteX = xInWorkspace + workspace.scrollX
            val pageStartX = currentPage * workspace.width
            val xOnPage = absoluteX - pageStartX
            val targetCellX = (xOnPage / page.cellWidth).toInt()
            val targetCellY = (yInWorkspace / page.cellHeight).toInt()
            
            val cellInfo = view.tag as? CellInfo
            val spanX = cellInfo?.spanX ?: 1
            val spanY = cellInfo?.spanY ?: 1

            val oldPage = if (!isHotseatItem) workspace.pages.getOrNull(fromPage) else null
            oldPage?.vacateCell(fromCellX, fromCellY, spanX, spanY)

            AppLogger.d("DragController", "resolveDrop attempt at target position: page $currentPage cell($targetCellX, $targetCellY), occupied=${page.isOccupied(targetCellX, targetCellY, spanX, spanY)}")

            if (targetCellX >= 0 && targetCellX + spanX <= page.columnCount && 
                targetCellY >= 0 && targetCellY + spanY <= page.rowCount && 
                !page.isOccupied(targetCellX, targetCellY, spanX, spanY)) {
                
                if (!isHotseatItem) {
                    oldPage?.removeView(view)
                } else {
                    val slot = hotseat.getChildAt(fromCellX) as? android.widget.FrameLayout
                    slot?.removeView(view)
                }

                view.visibility = View.VISIBLE
                page.placeView(view, targetCellX, targetCellY, spanX, spanY)

                if (item != null) {
                    val updatedItem = item.copy(
                        page = currentPage,
                        cellX = targetCellX,
                        cellY = targetCellY,
                        container = 0
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        LauncherDatabase.getDatabase(activity).workspaceDao().update(updatedItem)
                        AppLogger.d("DragController", "Successful move to page $currentPage cell($targetCellX, $targetCellY)")
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            activity.refreshWorkspaceItemsList()
                        }
                    }
                } else if (isHotseatItem) {
                    AppLogger.d("DragController", "Successful move to page $currentPage cell($targetCellX, $targetCellY) from Hotseat")
                }
                dropped = true
            } else {
                oldPage?.occupyCell(fromCellX, fromCellY, spanX, spanY)
                AppLogger.d("DragController", "Rejected drop: cell is occupied or out of bounds")
            }
        } else {
            AppLogger.d("DragController", "Rejected drop: out of bounds")
        }

        if (!dropped) {
            view.visibility = View.VISIBLE
        }

        draggedView = null
        draggedItem = null
        return dropped
    }
}

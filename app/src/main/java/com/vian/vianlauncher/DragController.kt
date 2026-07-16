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

        val cellInfo = view.tag as? CellInfo
        val spanX = cellInfo?.spanX ?: 1
        val spanY = cellInfo?.spanY ?: 1

        val hotseatLocation = IntArray(2)
        hotseat.getLocationOnScreen(hotseatLocation)
        val isOverHotseat = absoluteScreenY >= hotseatLocation[1] && absoluteScreenY <= hotseatLocation[1] + hotseat.height

        var dropped = false

        if (isOverHotseat) {
            val xInHotseat = absoluteScreenX - hotseatLocation[0]
            val slotWidth = if (hotseat.dockCount > 0) hotseat.width / hotseat.dockCount else 1
            val targetSlot = (xInHotseat / slotWidth).toInt().coerceIn(0, hotseat.dockCount - 1)

            if (targetSlot == 2) {
                AppLogger.d("DragController", "Rejected drop: cannot drop on drawer toggle")
            } else if (!isHotseatItem) {
                AppLogger.d("DragController", "Rejected drop: Workspace to Hotseat drag not supported")
            } else {
                val slot = hotseat.getChildAt(targetSlot) as? android.widget.FrameLayout
                if (slot == null || slot.childCount > 0) {
                    AppLogger.d("DragController", "Rejected drop: Hotseat slot $targetSlot is occupied or missing")
                } else {
                    val oldSlot = hotseat.getChildAt(fromCellX) as? android.widget.FrameLayout
                    oldSlot?.removeView(view)
                    view.setOnClickListener(null)
                    view.setOnLongClickListener(null)
                    view.visibility = View.VISIBLE
                    hotseat.placeView(view, targetSlot, 0)

                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        val dao = LauncherDatabase.getDatabase(activity).workspaceDao()
                        val hotseatItems = dao.getAllForContainer(1)
                        val draggedHotseatItem = hotseatItems.find { it.cellX == fromCellX }

                        if (draggedHotseatItem != null) {
                            val updatedItem = draggedHotseatItem.copy(
                                cellX = targetSlot,
                                cellY = 0,
                                page = -1,
                                container = 1
                            )
                            dao.update(updatedItem)
                            AppLogger.d("DragController", "Successful move to Hotseat slot $targetSlot")
                            
                            val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
                            mainIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                            val resolveInfos = activity.packageManager.queryIntentActivities(mainIntent, 0)
                            val appInfo = resolveInfos.find { it.activityInfo.packageName == draggedHotseatItem.packageName && it.activityInfo.name == draggedHotseatItem.activityName }
                            
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                if (appInfo != null) {
                                    view.setOnClickListener { 
                                        AppLogger.e("DragController", "Reattached listener CLICKED for ${draggedHotseatItem.packageName}")
                                        activity.launchApp(appInfo) 
                                    }
                                    view.setOnLongClickListener { 
                                        AppLogger.e("DragController", "Reattached listener LONG-CLICKED for ${draggedHotseatItem.packageName}")
                                        activity.showAppOptions(null, appInfo, null, view)
                                        true 
                                    }
                                }
                                activity.refreshWorkspaceItemsList()
                            }
                        }
                    }
                    dropped = true
                }
            }

            if (!dropped) {
                if (!isHotseatItem) {
                    val oldPage = workspace.pages.getOrNull(fromPage)
                    oldPage?.occupyCell(fromCellX, fromCellY, spanX, spanY)
                }
                AppLogger.d("DragController", "Rejected drop on Hotseat")
            }
        } else {
            val workspaceLocation = IntArray(2)
            workspace.getLocationOnScreen(workspaceLocation)
            val xInWorkspace = absoluteScreenX - workspaceLocation[0]
            val yInWorkspace = absoluteScreenY - workspaceLocation[1]
            val currentPage = workspace.currentPage
            val page = workspace.pages.getOrNull(currentPage)
            
            if (page != null && page.cellWidth > 0 && page.cellHeight > 0) {
                val absoluteX = xInWorkspace + workspace.scrollX
                val pageStartX = currentPage * workspace.width
                val xOnPage = absoluteX - pageStartX
                var targetCellX = (xOnPage / page.cellWidth).toInt()
                var targetCellY = (yInWorkspace / page.cellHeight).toInt()
                
                targetCellX = targetCellX.coerceIn(0, Math.max(0, page.columnCount - spanX))
                targetCellY = targetCellY.coerceIn(0, Math.max(0, page.rowCount - spanY))
                val oldPage = if (!isHotseatItem) workspace.pages.getOrNull(fromPage) else null
                oldPage?.vacateCell(fromCellX, fromCellY, spanX, spanY)
                val isOccupied = page.isOccupied(targetCellX, targetCellY, spanX, spanY)
                val existingItem = activity.currentWorkspaceItems.find { it.page == currentPage && it.cellX == targetCellX && it.cellY == targetCellY && it.container == 0 }
                val existingFolder = activity.currentFolders.find { it.page == currentPage && it.cellX == targetCellX && it.cellY == targetCellY }
                AppLogger.d("DragController", "resolveDrop attempt at target position: page $currentPage cell($targetCellX, $targetCellY), occupied=$isOccupied")

                if (targetCellX >= 0 && targetCellX + spanX <= page.columnCount && 
                    targetCellY >= 0 && targetCellY + spanY <= page.rowCount && 
                    !isOccupied) {
                    
                    if (!isHotseatItem) {
                        oldPage?.removeView(view)
                    } else {
                        val slot = hotseat.getChildAt(fromCellX) as? android.widget.FrameLayout
                        slot?.removeView(view)
                        view.setOnClickListener(null)
                        view.setOnLongClickListener(null)
                    }
                    view.visibility = View.VISIBLE
                    page.placeView(view, targetCellX, targetCellY, spanX, spanY)

                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        val dao = LauncherDatabase.getDatabase(activity).workspaceDao()
                        if (!isHotseatItem && item != null) {
                            val updatedItem = item.copy(
                                page = currentPage,
                                cellX = targetCellX,
                                cellY = targetCellY,
                                container = 0
                            )
                            dao.update(updatedItem)
                        } else if (isHotseatItem) {
                            val hotseatItems = dao.getAllForContainer(1)
                            val draggedHotseatItem = hotseatItems.find { it.cellX == fromCellX }
                            if (draggedHotseatItem != null) {
                                val updatedItem = draggedHotseatItem.copy(
                                    page = currentPage,
                                    cellX = targetCellX,
                                    cellY = targetCellY,
                                    container = 0
                                )
                                dao.update(updatedItem)
                            }
                        }
                        AppLogger.d("DragController", "Successful move to page $currentPage cell($targetCellX, $targetCellY)")
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            activity.refreshWorkspaceItemsList()
                        }
                    }
                    dropped = true

                } else if (existingFolder != null && (item != null || isHotseatItem)) {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        val dao = LauncherDatabase.getDatabase(activity).workspaceDao()
                        if (!isHotseatItem && item != null) {
                            val updatedItem = item.copy(page = -1, cellX = 0, cellY = 0, container = -existingFolder.id.toInt())
                            dao.update(updatedItem)
                        } else if (isHotseatItem) {
                            val hotseatItems = dao.getAllForContainer(1)
                            val draggedHotseatItem = hotseatItems.find { it.cellX == fromCellX }
                            if (draggedHotseatItem != null) {
                                val updatedItem = draggedHotseatItem.copy(page = -1, cellX = 0, cellY = 0, container = -existingFolder.id.toInt())
                                dao.update(updatedItem)
                            }
                        }
                        AppLogger.d("DragController", "Added to folder ${existingFolder.id}")
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            activity.forceRebuild()
                        }
                    }
                    dropped = true

                } else if (existingItem != null && (item != null || isHotseatItem) && existingItem.packageName != "__CLOCK_WIDGET__") {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        val db = LauncherDatabase.getDatabase(activity)
                        val dao = db.workspaceDao()
                        val newFolder = com.vian.vianlauncher.FolderInfo(name = "Folder", color = android.graphics.Color.GRAY, cellX = targetCellX, cellY = targetCellY, page = currentPage)
                        val folderId = db.folderDao().insert(newFolder)
                        
                        val updatedItem1 = existingItem.copy(page = -1, cellX = 0, cellY = 0, container = -folderId.toInt())
                        dao.update(updatedItem1)
                        
                        if (!isHotseatItem && item != null) {
                            val updatedItem2 = item.copy(page = -1, cellX = 0, cellY = 0, container = -folderId.toInt())
                            dao.update(updatedItem2)
                        } else if (isHotseatItem) {
                            val hotseatItems = dao.getAllForContainer(1)
                            val draggedHotseatItem = hotseatItems.find { it.cellX == fromCellX }
                            if (draggedHotseatItem != null) {
                                val updatedItem2 = draggedHotseatItem.copy(page = -1, cellX = 0, cellY = 0, container = -folderId.toInt())
                                dao.update(updatedItem2)
                            }
                        }
                        AppLogger.d("DragController", "Created folder $folderId with two items")
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            activity.forceRebuild()
                        }
                    }
                    dropped = true

                } else {
                    oldPage?.occupyCell(fromCellX, fromCellY, spanX, spanY)
                    AppLogger.d("DragController", "Rejected drop: cell is occupied or out of bounds")
                }
            } else {
                AppLogger.d("DragController", "Rejected drop: out of bounds")
            }
        }

        if (!dropped) {
            view.visibility = View.VISIBLE
        }
        draggedView = null
        draggedItem = null
        return dropped
    }
}

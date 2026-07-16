import re

with open('app/src/main/java/com/vian/vianlauncher/DragController.kt', 'r') as f:
    content = f.read()

# Fix workspace to hotseat delete+insert
pattern1 = re.compile(r'dao\.delete\(draggedHotseatItem\.id\)\s*val newItem = WorkspaceItem\(\s*packageName = draggedHotseatItem\.packageName,\s*activityName = draggedHotseatItem\.activityName,\s*cellX = targetSlot,\s*cellY = 0,\s*spanX = 1,\s*spanY = 1,\s*page = -1,\s*container = 1\s*\)\s*dao\.insert\(newItem\)')

replacement1 = """val updatedItem = draggedHotseatItem.copy(
                                cellX = targetSlot,
                                cellY = 0,
                                page = -1,
                                container = 1
                            )
                            dao.update(updatedItem)"""

if pattern1.search(content):
    content = pattern1.sub(replacement1, content)
    print("Fixed Hotseat drop insert")

# Now rewrite the entire workspace drop section starting from `val isOccupied = ...`
pattern2 = re.compile(r'val isOccupied = page\.isOccupied\(targetCellX, targetCellY, spanX, spanY\).*?else \{\n\s*AppLogger\.d\("DragController", "Rejected drop: out of bounds"\)\n\s*\}\n\s*\}', re.DOTALL)

replacement2 = """val isOccupied = page.isOccupied(targetCellX, targetCellY, spanX, spanY)
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
        }"""

if pattern2.search(content):
    content = pattern2.sub(replacement2, content)
    print("Fixed Workspace drop resolveDrop logic")
else:
    print("Could not find pattern2")

with open('app/src/main/java/com/vian/vianlauncher/DragController.kt', 'w') as f:
    f.write(content)

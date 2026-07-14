import re
with open('app/src/main/java/com/vian/vianlauncher/DragController.kt', 'r') as f:
    content = f.read()

target = """                    }
                    dropped = true
                } else {
                    oldPage?.occupyCell(fromCellX, fromCellY, spanX, spanY)
                    AppLogger.d("DragController", "Rejected drop: cell is occupied or out of bounds")
                }"""

replacement = """                    }
                    dropped = true
                } else if (!isHotseatItem && existingFolder != null && item != null) {
                    val updatedItem = item.copy(page = -1, cellX = 0, cellY = 0, container = -existingFolder.id.toInt())
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        LauncherDatabase.getDatabase(activity).workspaceDao().update(updatedItem)
                        AppLogger.d("DragController", "Added to folder ${existingFolder.id}")
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            activity.forceRebuild()
                        }
                    }
                    dropped = true
                } else if (!isHotseatItem && existingItem != null && item != null && existingItem.packageName != "__CLOCK_WIDGET__") {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        val db = LauncherDatabase.getDatabase(activity)
                        val newFolder = com.vian.vianlauncher.FolderInfo(name = "Folder", color = android.graphics.Color.GRAY, cellX = targetCellX, cellY = targetCellY, page = currentPage)
                        val folderId = db.folderDao().insert(newFolder)
                        val updatedItem1 = existingItem.copy(page = -1, cellX = 0, cellY = 0, container = -folderId.toInt())
                        val updatedItem2 = item.copy(page = -1, cellX = 0, cellY = 0, container = -folderId.toInt())
                        db.workspaceDao().update(updatedItem1)
                        db.workspaceDao().update(updatedItem2)
                        AppLogger.d("DragController", "Created folder $folderId with two items")
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            activity.forceRebuild()
                        }
                    }
                    dropped = true
                } else {
                    oldPage?.occupyCell(fromCellX, fromCellY, spanX, spanY)
                    AppLogger.d("DragController", "Rejected drop: cell is occupied or out of bounds")
                }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/vian/vianlauncher/DragController.kt', 'w') as f:
    f.write(content)
print("Updated DragController drop to folder logic")

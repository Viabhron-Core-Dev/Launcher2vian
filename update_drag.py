import re
with open('app/src/main/java/com/vian/vianlauncher/DragController.kt', 'r') as f:
    content = f.read()

target = """                oldPage?.vacateCell(fromCellX, fromCellY, spanX, spanY)
                AppLogger.d("DragController", "resolveDrop attempt at target position: page $currentPage cell($targetCellX, $targetCellY), occupied=${page.isOccupied(targetCellX, targetCellY, spanX, spanY)}")
                if (targetCellX >= 0 && targetCellX + spanX <= page.columnCount && 
                    targetCellY >= 0 && targetCellY + spanY <= page.rowCount && 
                    !page.isOccupied(targetCellX, targetCellY, spanX, spanY)) {"""

replacement = """                oldPage?.vacateCell(fromCellX, fromCellY, spanX, spanY)
                val isOccupied = page.isOccupied(targetCellX, targetCellY, spanX, spanY)
                val existingItem = activity.currentWorkspaceItems.find { it.page == currentPage && it.cellX == targetCellX && it.cellY == targetCellY && it.container == 0 }
                val existingFolder = activity.currentFolders.find { it.page == currentPage && it.cellX == targetCellX && it.cellY == targetCellY }
                AppLogger.d("DragController", "resolveDrop attempt at target position: page $currentPage cell($targetCellX, $targetCellY), occupied=$isOccupied")
                
                if (targetCellX >= 0 && targetCellX + spanX <= page.columnCount && 
                    targetCellY >= 0 && targetCellY + spanY <= page.rowCount && 
                    !isOccupied) {"""

content = content.replace(target, replacement)

target2 = """                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            LauncherDatabase.getDatabase(activity).workspaceDao().update(updatedItem)
                            AppLogger.d("DragController", "Successful move to page $currentPage cell($targetCellX, $targetCellY)")
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                activity.refreshWorkspaceItemsList()
                            }
                        }
                    } else if (isHotseatItem) {"""

replacement2 = """                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            LauncherDatabase.getDatabase(activity).workspaceDao().update(updatedItem)
                            AppLogger.d("DragController", "Successful move to page $currentPage cell($targetCellX, $targetCellY)")
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                activity.refreshWorkspaceItemsList()
                            }
                        }
                    } else if (isHotseatItem) {"""
# wait, actually I just need to append my else-if logic AFTER this entire if block!
# Let's search for the end of `if (targetCellX >= 0 ... && !isOccupied) { ... }` block

import re
with open('app/src/main/java/com/vian/vianlauncher/DragController.kt', 'r') as f:
    content = f.read()

target = """                        if (draggedHotseatItem != null) {
                            dao.delete(draggedHotseatItem.id)

                            val newItem = WorkspaceItem(
                                packageName = draggedHotseatItem.packageName,
                                activityName = draggedHotseatItem.activityName,
                                cellX = targetSlot,
                                cellY = 0,
                                spanX = 1,
                                spanY = 1,
                                page = -1,
                                container = 1
                            )
                            dao.insert(newItem)
                            AppLogger.d("DragController", "Successful move to Hotseat slot $targetSlot")
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                activity.refreshWorkspaceItemsList()
                            }
                        }"""

replacement = """                        if (draggedHotseatItem != null) {
                            dao.delete(draggedHotseatItem.id)

                            val newItem = WorkspaceItem(
                                packageName = draggedHotseatItem.packageName,
                                activityName = draggedHotseatItem.activityName,
                                cellX = targetSlot,
                                cellY = 0,
                                spanX = 1,
                                spanY = 1,
                                page = -1,
                                container = 1
                            )
                            dao.insert(newItem)
                            AppLogger.d("DragController", "Successful move to Hotseat slot $targetSlot")
                            
                            val intent = android.content.Intent().apply {
                                setClassName(draggedHotseatItem.packageName, draggedHotseatItem.activityName)
                            }
                            val appInfo = activity.packageManager.resolveActivity(intent, 0)
                            
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                if (appInfo != null) {
                                    view.setOnClickListener { activity.launchApp(appInfo) }
                                    view.setOnLongClickListener { 
                                        activity.showAppOptions(null, appInfo, null, view)
                                        true 
                                    }
                                }
                                activity.refreshWorkspaceItemsList()
                            }
                        }"""

if target in content:
    with open('app/src/main/java/com/vian/vianlauncher/DragController.kt', 'w') as f:
        f.write(content.replace(target, replacement))
    print("Updated DragController Hotseat->Hotseat")
else:
    print("Target not found")

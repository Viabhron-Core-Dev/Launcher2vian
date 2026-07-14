import re
with open('app/src/main/java/com/vian/vianlauncher/DragController.kt', 'r') as f:
    content = f.read()

target = """                                if (appInfo != null) {
                                    view.setOnClickListener {
                                         AppLogger.d("DragController", "Reattached listener CLICKED for ${draggedHotseatItem.packageName}")
                                        activity.launchApp(appInfo)
                                     }
                                    view.setOnLongClickListener {
                                         AppLogger.d("DragController", "Reattached listener LONG-CLICKED for ${draggedHotseatItem.packageName}")
                                        activity.showAppOptions(null, appInfo, null, view)
                                        true
                                     }
                                }"""

replacement = """                                if (appInfo != null) {
                                    AppLogger.e("DragController", "Successfully resolved appInfo, attaching listeners to Hotseat item ${draggedHotseatItem.packageName}")
                                    view.setOnClickListener {
                                         AppLogger.e("DragController", "Reattached listener CLICKED for ${draggedHotseatItem.packageName}")
                                        activity.launchApp(appInfo)
                                     }
                                    view.setOnLongClickListener {
                                         AppLogger.e("DragController", "Reattached listener LONG-CLICKED for ${draggedHotseatItem.packageName}")
                                        activity.showAppOptions(null, appInfo, null, view)
                                        true
                                     }
                                }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/vian/vianlauncher/DragController.kt', 'w') as f:
    f.write(content)
print("Updated logging in DragController")

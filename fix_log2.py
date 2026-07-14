import re
with open('app/src/main/java/com/vian/vianlauncher/DragController.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r'                                if \(appInfo != null\) \{\n                                    view\.setOnClickListener \{.*?                                    view\.setOnLongClickListener \{.*?\n                                     \}\n                                \}', re.DOTALL)

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

content = pattern.sub(replacement, content)

with open('app/src/main/java/com/vian/vianlauncher/DragController.kt', 'w') as f:
    f.write(content)
print("Updated DragController logging")

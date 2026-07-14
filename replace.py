import re
with open('app/src/main/java/com/vian/vianlauncher/DragController.kt', 'r') as f:
    content = f.read()

target = """                            val intent = android.content.Intent().apply {
                                setClassName(draggedHotseatItem.packageName, draggedHotseatItem.activityName)
                            }
                            val appInfo = activity.packageManager.resolveActivity(intent, 0)"""

replacement = """                            val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
                            mainIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                            val resolveInfos = activity.packageManager.queryIntentActivities(mainIntent, 0)
                            val appInfo = resolveInfos.find { it.activityInfo.packageName == draggedHotseatItem.packageName && it.activityInfo.name == draggedHotseatItem.activityName }"""

if target in content:
    with open('app/src/main/java/com/vian/vianlauncher/DragController.kt', 'w') as f:
        f.write(content.replace(target, replacement))
    print("Updated DragController with correct ResolveInfo lookup")
else:
    print("Target not found")

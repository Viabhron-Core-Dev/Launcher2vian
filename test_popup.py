import re

with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'r') as f:
    content = f.read()

# Add log line
log_line = '            val items = db.workspaceDao().getAllForContainer(-folder.id.toInt())\n            AppLogger.d("Folder", "Query returned ${items.size} items for folder ${folder.id}")'
content = re.sub(r'val items = db\.workspaceDao\(\)\.getAllForContainer\(-folder\.id\.toInt\(\)\)', log_line, content)

# Fix GridLayout layout params
grid_fix = """                val appView = createAppView(appInfo, true)
                appView.layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    setGravity(android.view.Gravity.CENTER)
                }
                appView.setOnClickListener {"""
content = re.sub(r'val appView = createAppView\(appInfo, true\)\s*appView\.setOnClickListener \{', grid_fix, content)

with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'w') as f:
    f.write(content)

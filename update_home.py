import re
with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'r') as f:
    content = f.read()

# 1. Add currentFolders
content = content.replace("private var currentWorkspaceItems = listOf<WorkspaceItem>()", 
                          "var currentWorkspaceItems = listOf<WorkspaceItem>()\n    var currentFolders = listOf<FolderInfo>()")

# 2. In onResume, make refreshWorkspaceItemsList load folders too
# Wait, refreshWorkspaceItemsList is defined later
refresh_func = """    fun refreshWorkspaceItemsList() {
        scope.launch(Dispatchers.IO) {
            val db = LauncherDatabase.getDatabase(this@HomeActivity)
            val items = db.workspaceDao().getAllForContainer(0)
            val folders = db.folderDao().getAll()
            withContext(Dispatchers.Main) {
                currentWorkspaceItems = items
                currentFolders = folders
                AppLogger.d("HomeActivity", "Refreshed currentWorkspaceItems, count=${items.size}, folders=${folders.size}")
            }
        }
    }"""
# Use regex to replace refreshWorkspaceItemsList
content = re.sub(r'    fun refreshWorkspaceItemsList\(\) \{[\s\S]*?    \}\n', refresh_func + '\n', content)

with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'w') as f:
    f.write(content)
print("Updated HomeActivity properties")

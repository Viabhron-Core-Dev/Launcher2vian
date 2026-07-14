import re
with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'r') as f:
    content = f.read()

target = """    fun refreshWorkspaceItemsList() {
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
    }
        }
    }"""

replacement = """    fun refreshWorkspaceItemsList() {
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

content = content.replace(target, replacement)

with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'w') as f:
    f.write(content)
print("Updated HomeActivity brackets")

import re

with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r'\s*currentWorkspaceItems = items\s*for \(folder in currentFolders\) \{')

replacement = """
            val folders = withContext(Dispatchers.IO) {
                LauncherDatabase.getDatabase(this@HomeActivity).folderDao().getAll()
            }
            currentWorkspaceItems = items
            currentFolders = folders
            
            for (folder in currentFolders) {"""

if pattern.search(content):
    content = pattern.sub(replacement, content)
    with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'w') as f:
        f.write(content)
    print("Fixed Rebuild")
else:
    print("Rebuild pattern not found")

import re
with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'r') as f:
    content = f.read()

target = """            currentWorkspaceItems = items
            
            for (item in items) {"""

replacement = """            currentWorkspaceItems = items
            
            for (folder in currentFolders) {
                if (folder.page in 0 until workspace.pages.size) {
                    val page = workspace.pages[folder.page]
                    val folderView = createFolderView(folder)
                    page.placeView(folderView, folder.cellX, folder.cellY, 1, 1)
                }
            }

            for (item in items) {"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'w') as f:
    f.write(content)
print("Updated HomeActivity render loop")

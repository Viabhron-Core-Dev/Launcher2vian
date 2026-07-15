import re

with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r'                            lastDockCount = dockCount\n                            scope\.launch\(Dispatchers\.IO\) \{\n                                withContext\(Dispatchers\.Main\) \{\n                                    rebuildWorkspaceAndHotseat\(\)\n                                \}\n                            \}')

replacement = """                            lastDockCount = dockCount
                            scope.launch(Dispatchers.IO) {
                                migrateClockPosition(rows, cols)
                                withContext(Dispatchers.Main) {
                                    rebuildWorkspaceAndHotseat()
                                }
                            }"""

if pattern.search(content):
    content = pattern.sub(replacement, content)
    with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'w') as f:
        f.write(content)
    print("Fixed Add Page")
else:
    print("Add Page pattern not found")

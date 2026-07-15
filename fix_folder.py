import re

with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'r') as f:
    content = f.read()

pattern = re.compile(r'"Remove Folder" -> \{\s*AlertDialog\.Builder\(this\)\s*\.setTitle\("Remove Folder"\)\s*\.setMessage\("Are you sure\? Items inside will be removed from the home screen\."\)\s*\.setPositiveButton\("Remove"\) \{ _, _ ->\s*scope\.launch\(Dispatchers\.IO\) \{\s*val db = LauncherDatabase\.getDatabase\(this@HomeActivity\)\s*db\.workspaceDao\(\)\.clearContainer\(-folder\.id\.toInt\(\)\)\s*db\.folderDao\(\)\.delete\(folder\.id\)\s*withContext\(Dispatchers\.Main\) \{ rebuildWorkspaceAndHotseat\(\) \}\s*\}\s*\}\s*\.setNegativeButton\("Cancel", null\)\s*\.show\(\)\s*\}')

replacement = """"Remove Folder" -> {
                        scope.launch(Dispatchers.IO) {
                            val db = LauncherDatabase.getDatabase(this@HomeActivity)
                            val items = db.workspaceDao().getAllForContainer(-folder.id.toInt())
                            
                            withContext(Dispatchers.Main) {
                                val pageLayout = workspace.pages.getOrNull(folder.page) ?: return@withContext
                                val cols = pageLayout.columnCount
                                val rows = pageLayout.rowCount
                                
                                val emptyCells = mutableListOf<Pair<Int, Int>>()
                                for (y in 0 until rows) {
                                    for (x in 0 until cols) {
                                        if ((x == folder.cellX && y == folder.cellY) || !pageLayout.isOccupied(x, y)) {
                                            emptyCells.add(Pair(x, y))
                                        }
                                    }
                                }
                                
                                if (items.size <= emptyCells.size) {
                                    scope.launch(Dispatchers.IO) {
                                        items.forEachIndexed { index, item ->
                                            val cell = emptyCells[index]
                                            db.workspaceDao().update(item.copy(page = folder.page, cellX = cell.first, cellY = cell.second, container = 0))
                                        }
                                        db.folderDao().delete(folder.id)
                                        withContext(Dispatchers.Main) { rebuildWorkspaceAndHotseat() }
                                    }
                                } else {
                                    val unplacedCount = items.size - emptyCells.size
                                    AlertDialog.Builder(this@HomeActivity)
                                        .setTitle("Not enough space")
                                        .setMessage("$unplacedCount items cannot fit on this page and will be deleted. Continue?")
                                        .setPositiveButton("Remove Folder") { _, _ ->
                                            scope.launch(Dispatchers.IO) {
                                                items.forEachIndexed { index, item ->
                                                    if (index < emptyCells.size) {
                                                        val cell = emptyCells[index]
                                                        db.workspaceDao().update(item.copy(page = folder.page, cellX = cell.first, cellY = cell.second, container = 0))
                                                    } else {
                                                        db.workspaceDao().delete(item.id)
                                                    }
                                                }
                                                db.folderDao().delete(folder.id)
                                                withContext(Dispatchers.Main) { rebuildWorkspaceAndHotseat() }
                                            }
                                        }
                                        .setNegativeButton("Cancel", null)
                                        .show()
                                }
                            }
                        }
                    }"""

if pattern.search(content):
    content = pattern.sub(replacement, content)
    with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'w') as f:
        f.write(content)
    print("Fixed Folder")
else:
    print("Folder pattern not found")

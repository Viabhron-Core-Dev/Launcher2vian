import re

with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'r') as f:
    content = f.read()

target1 = """    private var lastGridCols = 4
    private var lastGridRows = 5
    private var lastGridPages = 3"""
replacement1 = """    private var lastGridCols = 4
    private var lastGridRows = 5
    private var lastGridPages = 3
    private var lastDockCount = 5"""

content = content.replace(target1, replacement1)

target2 = """    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("vian_launcher_prefs", Context.MODE_PRIVATE)
        val cols = prefs.getInt("grid_cols", 4)
        val rows = prefs.getInt("grid_rows", 5)
        val pages = prefs.getInt("grid_pages", 3)
        
        if (isFirstResume) {
            isFirstResume = false
            lastGridCols = cols
            lastGridRows = rows
            lastGridPages = pages
            scope.launch(Dispatchers.IO) {
                migrateClockPosition(rows, cols)
                withContext(Dispatchers.Main) {
                    rebuildWorkspaceAndHotseat()
                }
            }
        } else if (cols != lastGridCols || rows != lastGridRows || pages != lastGridPages) {"""

replacement2 = """    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("vian_launcher_prefs", Context.MODE_PRIVATE)
        val cols = prefs.getInt("grid_cols", 4)
        val rows = prefs.getInt("grid_rows", 5)
        val pages = prefs.getInt("grid_pages", 3)
        val dockCount = prefs.getInt("dock_count", 5)
        
        if (isFirstResume) {
            isFirstResume = false
            lastGridCols = cols
            lastGridRows = rows
            lastGridPages = pages
            lastDockCount = dockCount
            hotseat.dockCount = dockCount
            scope.launch(Dispatchers.IO) {
                migrateClockPosition(rows, cols)
                withContext(Dispatchers.Main) {
                    rebuildWorkspaceAndHotseat()
                }
            }
        } else if (cols != lastGridCols || rows != lastGridRows || pages != lastGridPages || dockCount != lastDockCount) {
            hotseat.dockCount = dockCount
            lastDockCount = dockCount
"""

content = content.replace(target2, replacement2)

with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'w') as f:
    f.write(content)
print("Replaced HomeActivity")

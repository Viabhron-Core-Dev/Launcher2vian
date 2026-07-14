import re
with open('app/src/main/java/com/vian/vianlauncher/DragController.kt', 'r') as f:
    content = f.read()

target = """                oldPage?.vacateCell(fromCellX, fromCellY, spanX, spanY)
                AppLogger.d("DragController", "resolveDrop attempt at target position: page $currentPage cell($targetCellX, $targetCellY), occupied=${page.isOccupied(targetCellX, targetCellY, spanX, spanY)}")
                if (targetCellX >= 0 && targetCellX + spanX <= page.columnCount && 
                    targetCellY >= 0 && targetCellY + spanY <= page.rowCount && 
                    !page.isOccupied(targetCellX, targetCellY, spanX, spanY)) {"""

# Let's use a more robust regex to replace it
pattern = re.compile(r'                oldPage\?\.vacateCell\(fromCellX, fromCellY, spanX, spanY\)\n                AppLogger\.d\("DragController", "resolveDrop attempt.*?\)\n                if \(targetCellX >= 0 && targetCellX \+ spanX <= page\.columnCount && \n                    targetCellY >= 0 && targetCellY \+ spanY <= page\.rowCount && \n                    !page\.isOccupied\(targetCellX, targetCellY, spanX, spanY\)\) \{')

replacement = """                oldPage?.vacateCell(fromCellX, fromCellY, spanX, spanY)
                val isOccupied = page.isOccupied(targetCellX, targetCellY, spanX, spanY)
                val existingItem = activity.currentWorkspaceItems.find { it.page == currentPage && it.cellX == targetCellX && it.cellY == targetCellY && it.container == 0 }
                val existingFolder = activity.currentFolders.find { it.page == currentPage && it.cellX == targetCellX && it.cellY == targetCellY }
                AppLogger.d("DragController", "resolveDrop attempt at target position: page $currentPage cell($targetCellX, $targetCellY), occupied=$isOccupied")
                
                if (targetCellX >= 0 && targetCellX + spanX <= page.columnCount && 
                    targetCellY >= 0 && targetCellY + spanY <= page.rowCount && 
                    !isOccupied) {"""

content = pattern.sub(replacement, content)

with open('app/src/main/java/com/vian/vianlauncher/DragController.kt', 'w') as f:
    f.write(content)
print("Updated DragController variables")

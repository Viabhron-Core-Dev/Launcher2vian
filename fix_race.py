import re

with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'r') as f:
    content = f.read()

# Add rebuildJob variable
if 'private var rebuildJob: Job? = null' not in content:
    pattern1 = re.compile(r'private val scope = CoroutineScope\(Dispatchers\.Main\)')
    replacement1 = 'private val scope = CoroutineScope(Dispatchers.Main)\n    private var rebuildJob: kotlinx.coroutines.Job? = null'
    content = pattern1.sub(replacement1, content)
    print("Added rebuildJob")

# Fix rebuildWorkspaceAndHotseat
pattern2 = re.compile(r'    private fun rebuildWorkspaceAndHotseat\(\) \{\n\s*workspace\.setup\(lastGridCols, lastGridRows, lastGridPages\)(.*?)\s*scope\.launch \{', re.DOTALL)

replacement2 = """    private fun rebuildWorkspaceAndHotseat() {
        rebuildJob?.cancel()
        rebuildJob = scope.launch {
            workspace.setup(lastGridCols, lastGridRows, lastGridPages)""" + r'\1'

if pattern2.search(content):
    content = pattern2.sub(replacement2, content)
    print("Fixed rebuild function")
else:
    print("rebuild pattern not found")

with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'w') as f:
    f.write(content)

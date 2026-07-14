import re
with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("private fun rebuildWorkspaceAndHotseat() {", "fun forceRebuild() {\n        rebuildWorkspaceAndHotseat()\n    }\n\n    private fun rebuildWorkspaceAndHotseat() {")

with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'w') as f:
    f.write(content)
print("Updated HomeActivity forceRebuild")

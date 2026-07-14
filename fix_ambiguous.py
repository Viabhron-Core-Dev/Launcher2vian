import re
with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
imports_seen = set()

for line in lines:
    if line.startswith("import "):
        if line not in imports_seen:
            imports_seen.add(line)
            new_lines.append(line)
    else:
        new_lines.append(line)

with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'w') as f:
    f.write("".join(new_lines))
print("Fixed ambiguous imports")

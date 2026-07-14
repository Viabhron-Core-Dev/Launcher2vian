import re
with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'r') as f:
    content = f.read()

imports = """import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.Toast
import android.widget.EditText
import android.graphics.Color
"""

content = content.replace("import android.widget.ImageView", imports + "import android.widget.ImageView")

with open('app/src/main/java/com/vian/vianlauncher/HomeActivity.kt', 'w') as f:
    f.write(content)
print("Updated Imports")

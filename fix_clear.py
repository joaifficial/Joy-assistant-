import re

with open("app/src/main/java/com/example/ui/ZoyaScreen.kt", "r") as f:
    content = f.read()

# Add import
if "import androidx.compose.material.icons.Icons" not in content:
    content = content.replace("import androidx.compose.material3.TextButton", "import androidx.compose.material3.TextButton\nimport androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.filled.Clear\n")

target = """                                        imageVector = androidx.compose.material.icons.Icons.Filled.Clear,"""
replacement = """                                        imageVector = Icons.Filled.Clear,"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/ZoyaScreen.kt", "w") as f:
    f.write(content)

import re

with open("app/src/main/java/com/example/ui/ZoyaScreen.kt", "r") as f:
    content = f.read()

scaffold_start = content.find("androidx.compose.material3.Scaffold(")
scaffold_end = content.find("        } else {", scaffold_start)
if scaffold_end != -1:
    scaffold_end = content.find("                } else {", scaffold_start)
    if scaffold_end != -1:
        # Find the end of the entire Scaffold block to replace the whole thing safely
        pass


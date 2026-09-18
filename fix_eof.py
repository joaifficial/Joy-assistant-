import sys

with open("app/src/main/java/com/example/ui/ZoyaScreen.kt", "r") as f:
    content = f.read()

# Let's count curly braces in the entire file
open_count = content.count('{')
close_count = content.count('}')

print(f"Open: {open_count}, Close: {close_count}")


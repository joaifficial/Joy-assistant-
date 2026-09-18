with open("app/src/main/java/com/example/ui/ZoyaScreen.kt", "r") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if line.strip() == "@Composable" and i < len(lines)-1 and "fun ZoyaOrb" in lines[i+1]:
        # Count braces going upwards to find how many we have vs we need
        # Actually I can just delete the extra "}\n" line before @Composable
        if lines[i-1].strip() == "}":
            lines[i-1] = ""
            print("Removed one brace")
        break

with open("app/src/main/java/com/example/ui/ZoyaScreen.kt", "w") as f:
    f.writelines(lines)

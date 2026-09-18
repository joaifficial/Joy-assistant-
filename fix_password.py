with open("app/src/main/java/com/example/ui/ZoyaScreen.kt", "r") as f:
    content = f.read()

target = """                    TextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        placeholder = { Text("AIza...") }
                    )"""

replacement = """                    TextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        placeholder = { Text("AIza...") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/ZoyaScreen.kt", "w") as f:
    f.write(content)

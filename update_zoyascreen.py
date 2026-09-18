import re

with open("app/src/main/java/com/example/ui/ZoyaScreen.kt", "r") as f:
    content = f.read()

# Read imports and add new ones if not present
new_imports = """
import android.content.Context
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
"""
content = content.replace("import androidx.compose.runtime.collectAsState", "import androidx.compose.runtime.collectAsState\n" + new_imports)

# Replace the start of HomeScreen to include SharedPrefs
homescreen_start = """@Composable
fun HomeScreen(onNavigateToChat: () -> Unit) {
    val context = LocalContext.current"""

homescreen_replacement = """@Composable
fun HomeScreen(onNavigateToChat: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var showApiKeyDialog by remember { mutableStateOf(false) }"""
content = content.replace(homescreen_start, homescreen_replacement)


# In dropdown menu, add the API Key option
dropdown_target = """                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Accessibility Settings (Auto-Click)", color = Color.White) }"""

dropdown_replacement = """                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("API Key Settings", color = Color.White) },
                            onClick = {
                                showMenu = false
                                showApiKeyDialog = true
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Accessibility Settings (Auto-Click)", color = Color.White) }"""
content = content.replace(dropdown_target, dropdown_replacement)

# Update the Initialize Button
button_target = """                if (!serviceStarted) {
                    androidx.compose.material3.Button("""

button_replacement = """                if (!serviceStarted) {
                    androidx.compose.material3.Button(
                        enabled = apiKey.isNotEmpty(),"""
content = content.replace(button_target, button_replacement)


# Add the Dialog code before the final brace of HomeScreen
# We need to inject the dialog into the Scaffold content
# Let's find the closing brace of `androidx.compose.material3.Scaffold(` 's block
dialog_code = """
    if (showApiKeyDialog) {
        var tempKey by remember { mutableStateOf(apiKey) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Gemini API Key") },
            text = {
                Column {
                    Text("Enter your Gemini API key to use Z.O.Y.A.")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        placeholder = { Text("AIza...") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Get your API key here",
                        color = Color(0xFF00B0FF),
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        prefs.edit().putString("api_key", tempKey).apply()
                        apiKey = tempKey
                        showApiKeyDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
"""

# Let's find the end of HomeScreen
homescreen_end_idx = content.find("@Composable\nfun ZoyaOrb")

if homescreen_end_idx != -1:
    # We want to inject it just before the closing brace of HomeScreen
    # Let's count back to the closing brace
    for i in range(homescreen_end_idx - 1, 0, -1):
        if content[i] == '}':
            content = content[:i] + dialog_code + "\n" + content[i:]
            break

with open("app/src/main/java/com/example/ui/ZoyaScreen.kt", "w") as f:
    f.write(content)


import re

with open("app/src/main/java/com/example/live/LiveSessionManager.kt", "r") as f:
    content = f.read()

target = "val apiKey = BuildConfig.GEMINI_API_KEY"
replacement = """val prefs = context.getSharedPreferences("ZoyaPrefs", android.content.Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""
        if (apiKey.isEmpty()) {
            addMessage("Error: API Key is missing. Please set it in Settings.")
            _zoyaState.value = ZoyaState.IDLE
            return
        }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/live/LiveSessionManager.kt", "w") as f:
    f.write(content)


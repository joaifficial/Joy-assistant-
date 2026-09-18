import re

with open("app/src/main/java/com/example/ZoyaForegroundService.kt", "r") as f:
    service_content = f.read()

# Expose liveSessionManager
service_content = service_content.replace("private var liveSessionManager: LiveSessionManager? = null", "var liveSessionManager: LiveSessionManager? = null")

with open("app/src/main/java/com/example/ZoyaForegroundService.kt", "w") as f:
    f.write(service_content)

with open("app/src/main/java/com/example/ui/ZoyaScreen.kt", "r") as f:
    content = f.read()

# Fix ZoyaScreen Syntax error by making sure braces are balanced in HomeScreen
# I will just find HomeScreen { ... } and replace it cleanly.
start_hs = content.find("@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)\n@Composable\nfun HomeScreen")
end_hs = content.find("@Composable\nfun ZoyaOrb")

if start_hs != -1 and end_hs != -1:
    hs_content = content[start_hs:end_hs]
    # Keep removing last '}' until it parses or just do a brace count!
    open_c = hs_content.count('{')
    close_c = hs_content.count('}')
    diff = close_c - open_c
    if diff > 0:
        # replace the last `diff` closing braces with nothing
        print(f"Removing {diff} closing braces")
        for _ in range(diff):
            hs_content = hs_content[::-1].replace("}"[::-1], ""[::-1], 1)[::-1]
        
    content = content[:start_hs] + hs_content + "\n" + content[end_hs:]

# Fix ChatScreen reconnect
content = content.replace("liveSessionManager?.reconnect()", "ZoyaForegroundService.activeService?.reconnectSession()")

with open("app/src/main/java/com/example/ui/ZoyaScreen.kt", "w") as f:
    f.write(content)

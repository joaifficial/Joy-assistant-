import re

with open("app/src/main/java/com/example/tools/ToolExecutionEngine.kt", "r") as f:
    content = f.read()

target = """        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager"""
replacement = """        val ctx = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) context.createAttributionContext("zoya_audio") else context
        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager"""

content = content.replace(target, replacement)

target2 = """            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager"""
replacement2 = """            val ctx = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) context.createAttributionContext("zoya_audio") else context
            val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager"""

content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/tools/ToolExecutionEngine.kt", "w") as f:
    f.write(content)


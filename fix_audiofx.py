import re

with open("app/src/main/java/com/example/ZoyaForegroundService.kt", "r") as f:
    content = f.read()

target = """            // Optional: If you want to explicitly enable AEC (Acoustic Echo Canceler)
            // check if it's available. Usually VOICE_COMMUNICATION enables it implicitly.
            try {
                if (android.media.audiofx.AcousticEchoCanceler.isAvailable()) {
                    val aec = android.media.audiofx.AcousticEchoCanceler.create(audioRecord!!.audioSessionId)
                    aec?.enabled = true
                }
                if (android.media.audiofx.NoiseSuppressor.isAvailable()) {
                    val ns = android.media.audiofx.NoiseSuppressor.create(audioRecord!!.audioSessionId)
                    ns?.enabled = true
                }
            } catch (e: Exception) {
                Log.e("ZoyaDiagnostic", "Failed to enable audio effects", e)
            }"""

content = content.replace(target, "")

with open("app/src/main/java/com/example/ZoyaForegroundService.kt", "w") as f:
    f.write(content)


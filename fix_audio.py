import re

with open("app/src/main/AndroidManifest.xml", "r") as f:
    content = f.read()

# Add attribution tag back
if "<attribution" not in content:
    content = content.replace("<application", '    <attribution android:tag="zoya_audio" android:label="@string/app_name" />\n    <application')

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ZoyaForegroundService.kt", "r") as f:
    service_content = f.read()

target = """            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfig,
                audioFormat,
                finalBuf
            )"""

replacement = """            val ctx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                createAttributionContext("zoya_audio")
            } else {
                this
            }
            audioRecord = AudioRecord.Builder()
                .setContext(ctx)
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(audioFormat)
                        .build()
                )
                .setBufferSizeInBytes(finalBuf)
                .build()"""

service_content = service_content.replace(target, replacement)

with open("app/src/main/java/com/example/ZoyaForegroundService.kt", "w") as f:
    f.write(service_content)


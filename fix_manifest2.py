import re

with open("app/src/main/AndroidManifest.xml", "r") as f:
    content = f.read()

target = """        <service
            android:name=".ZoyaForegroundService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="microphone" />"""

replacement = """        <service
            android:name=".ZoyaForegroundService"
            android:enabled="true"
            android:exported="false"
            android:attributionTags="zoya_audio"
            android:foregroundServiceType="microphone" />"""

content = content.replace(target, replacement)

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(content)


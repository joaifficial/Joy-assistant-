import re

with open("app/src/main/AndroidManifest.xml", "r") as f:
    content = f.read()

content = content.replace('<attribution android:tag="microphone_tag" android:label="@string/app_name" />\n', "")
content = content.replace('            android:attributionTags="microphone_tag"\n', "")

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(content)

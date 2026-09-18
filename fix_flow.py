with open("app/src/main/java/com/example/ui/ZoyaScreen.kt", "r") as f:
    content = f.read()

# Make dialog show up instantly if empty
content = content.replace("var showApiKeyDialog by remember { mutableStateOf(false) }", "var showApiKeyDialog by remember { mutableStateOf(apiKey.isEmpty()) }")

target = """                if (!serviceStarted) {
                    androidx.compose.material3.Button(
                        enabled = apiKey.isNotEmpty(),
                        modifier = Modifier.testTag("start_zoya_button"),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        onClick = {
                            val hasMic = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            val hasContacts = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            val hasPhone = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            
                            if (hasMic && hasContacts && hasPhone) {
                                val intent = Intent(context, ZoyaForegroundService::class.java)
                                ContextCompat.startForegroundService(context, intent)
                                serviceStarted = true
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.RECORD_AUDIO,
                                        android.Manifest.permission.READ_CONTACTS,
                                        android.Manifest.permission.CALL_PHONE
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Initialize Z.O.Y.A.", fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                    }
                }"""

replacement = """                if (!serviceStarted) {
                    if (apiKey.isEmpty()) {
                        androidx.compose.material3.Button(
                            modifier = Modifier.testTag("setup_api_button"),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            onClick = {
                                showApiKeyDialog = true
                            }
                        ) {
                            Text("Setup API Key", fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                        }
                    } else {
                        androidx.compose.material3.Button(
                            modifier = Modifier.testTag("start_zoya_button"),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            onClick = {
                                val hasMic = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                val hasContacts = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                val hasPhone = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                
                                if (hasMic && hasContacts && hasPhone) {
                                    val intent = Intent(context, ZoyaForegroundService::class.java)
                                    ContextCompat.startForegroundService(context, intent)
                                    serviceStarted = true
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.RECORD_AUDIO,
                                            android.Manifest.permission.READ_CONTACTS,
                                            android.Manifest.permission.CALL_PHONE
                                        )
                                    )
                                }
                            }
                        ) {
                            Text("Initialize Z.O.Y.A.", fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                        }
                    }
                }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/ZoyaScreen.kt", "w") as f:
    f.write(content)


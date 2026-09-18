import re

with open("app/src/main/java/com/example/ui/ZoyaScreen.kt", "r") as f:
    content = f.read()

# Replace the Scaffold block
start_idx = content.find("    androidx.compose.material3.Scaffold(")
if start_idx != -1:
    end_idx = content.find("    }\n}\n\n@Composable\nfun ZoyaOrb")
    if end_idx == -1:
        end_idx = content.find("    }\n}\n@Composable\nfun ZoyaOrb")

    new_scaffold = """    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Z.O.Y.A.", color = Color.White, fontWeight = FontWeight.Light, fontSize = 24.sp, letterSpacing = 2.sp) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    androidx.compose.material3.IconButton(
                        onClick = { showMenu = !showMenu },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    ) {
                        Text("⚙", color = Color.White, fontSize = 20.sp)
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(Color(0xFF1E1E2E).copy(alpha = 0.9f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("View Logs", color = Color.White) },
                            onClick = {
                                showMenu = false
                                onNavigateToChat()
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Accessibility Settings (Auto-Click)", color = Color.White) },
                            onClick = {
                                showMenu = false
                                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color(0xFF1A1A2E), Color(0xFF0F0F1A)),
                        radius = 1500f
                    )
                )
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(24.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(32.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                ZoyaOrb(state = zoyaState)
                
                Spacer(modifier = Modifier.height(60.dp))

                if (!serviceStarted) {
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
                } else if (zoyaState == ZoyaState.IDLE) {
                    androidx.compose.material3.Button(
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        onClick = {
                            val service = ZoyaForegroundService.activeService
                            if (service != null) {
                                service.reconnectSession()
                            } else {
                                val intent = Intent(context, ZoyaForegroundService::class.java)
                                ContextCompat.startForegroundService(context, intent)
                            }
                        }
                    ) {
                        Text("Reconnect Uplink", fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    androidx.compose.material3.Button(
                        onClick = {
                            val intent = Intent(context, ZoyaForegroundService::class.java)
                            context.stopService(intent)
                            serviceStarted = false
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935).copy(alpha = 0.2f),
                            contentColor = Color(0xFFEF9A9A)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Terminate Session", fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                    }
                } else {
                    Text(
                        text = when (zoyaState) {
                            ZoyaState.LISTENING -> "Awaiting Input..."
                            ZoyaState.THINKING -> "Processing Data..."
                            ZoyaState.SPEAKING -> "Transmitting..."
                            else -> ""
                        },
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    androidx.compose.material3.Button(
                        onClick = {
                            val intent = Intent(context, ZoyaForegroundService::class.java)
                            context.stopService(intent)
                            serviceStarted = false
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935).copy(alpha = 0.2f),
                            contentColor = Color(0xFFEF9A9A)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Disconnect", fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                    }
                }
            }
        }
    }"""
    
    if end_idx != -1:
        content = content[:start_idx] + new_scaffold + content[end_idx:]
        with open("app/src/main/java/com/example/ui/ZoyaScreen.kt", "w") as f:
            f.write(content)
        print("Updated Scaffold")

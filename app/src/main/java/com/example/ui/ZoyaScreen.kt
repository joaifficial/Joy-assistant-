package com.example.ui

import android.content.Intent
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ZoyaForegroundService
import com.example.live.ZoyaState
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border

import androidx.compose.foundation.BorderStroke
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.collectAsState

import android.content.Context
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear



@Composable
fun ZoyaScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToChat = { navController.navigate("chat") }
            )
        }
        composable("chat") {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToChat: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("JoyPrefs", Context.MODE_PRIVATE) }
    val legacyPrefs = remember { context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "")?.ifEmpty { legacyPrefs.getString("api_key", "") } ?: "") }
    var showApiKeyDialog by remember { mutableStateOf(apiKey.isEmpty()) }
    var zoyaState by remember { mutableStateOf(ZoyaForegroundService.currentState) }
    var serviceStarted by remember { mutableStateOf(ZoyaForegroundService.activeService != null) }
    var showMenu by remember { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.RECORD_AUDIO] == true) {
            val intent = Intent(context, ZoyaForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
            serviceStarted = true
        } else {
            android.widget.Toast.makeText(context, "Microphone permission is required!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        ZoyaForegroundService.onStateChange = { state ->
            zoyaState = state
        }
    }

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Column {
                        Text("J.O.Y.", color = Color.White, fontWeight = FontWeight.Light, fontSize = 24.sp, letterSpacing = 3.sp)
                        Text("AI ASSISTANT", color = Color(0xFF00E5FF).copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                    }
                },
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
                            text = { Text("API Key Settings", color = Color.White) },
                            onClick = {
                                showMenu = false
                                showApiKeyDialog = true
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
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Display Over Other Apps (RGB Border)", color = Color.White) },
                            onClick = {
                                showMenu = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(
                                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}")
                                    )
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                }
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
                Spacer(modifier = Modifier.height(12.dp))
                
                JoyOrb(state = zoyaState)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Status Pill Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    val statusColor = when (zoyaState) {
                        ZoyaState.IDLE -> if (serviceStarted) Color(0xFF00E5FF) else Color(0xFF80D8FF)
                        ZoyaState.LISTENING -> Color(0xFFFF007F)
                        ZoyaState.THINKING -> Color(0xFFFFAB00)
                        ZoyaState.SPEAKING -> Color(0xFF00E676)
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color = statusColor, shape = androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = when (zoyaState) {
                            ZoyaState.IDLE -> if (serviceStarted) "JOY STANDBY" else "JOY READY"
                            ZoyaState.LISTENING -> "JOY LISTENING"
                            ZoyaState.THINKING -> "JOY THINKING"
                            ZoyaState.SPEAKING -> "JOY SPEAKING"
                        },
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(36.dp))

                if (!serviceStarted) {
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
                                
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(context)) {
                                    val overlayIntent = Intent(
                                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}")
                                    )
                                    overlayIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(overlayIntent)
                                }

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
                            Text("Initialize J.O.Y.", fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
                        }
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
    }    
    if (showApiKeyDialog) {
        var tempKey by remember { mutableStateOf(apiKey) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Gemini API Key") },
            text = {
                Column {
                    Text("Enter your Gemini API key to use J.O.Y.")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        placeholder = { Text("AIza...") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            if (tempKey.isNotEmpty()) {
                                androidx.compose.material3.IconButton(onClick = { tempKey = "" }) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear text"
                                    )
                                }
                            }
                        }
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
                        legacyPrefs.edit().putString("api_key", tempKey).apply()
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

}



@Composable
fun ZoyaOrb(state: ZoyaState) {
    JoyOrb(state = state)
}

@Composable
fun JoyOrb(state: ZoyaState) {
    val infiniteTransition = rememberInfiniteTransition(label = "joy_orb_transition")

    // Core continuous rotations
    val rotationCW by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cw_rotation"
    )
    val rotationCCW by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ccw_rotation"
    )

    // Fluid organic wave phase
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Harmonic ripple progress for radiating acoustic sound waves
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_progress"
    )

    // Breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_pulse"
    )

    // 3D Orbital particle progression
    val particleOrbit by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_orbit"
    )

    // Dynamic animated values responding smoothly to state changes
    val stateScale = remember { Animatable(1f) }
    val glowIntensity = remember { Animatable(0.5f) }
    val speedMultiplier = remember { Animatable(1f) }

    LaunchedEffect(state) {
        when (state) {
            ZoyaState.IDLE -> {
                launch { stateScale.animateTo(1.0f, animationSpec = tween(600, easing = FastOutSlowInEasing)) }
                launch { glowIntensity.animateTo(0.45f, animationSpec = tween(600)) }
                launch { speedMultiplier.animateTo(1.0f, animationSpec = tween(500)) }
            }
            ZoyaState.LISTENING -> {
                launch { stateScale.animateTo(1.12f, animationSpec = tween(400, easing = FastOutSlowInEasing)) }
                launch { glowIntensity.animateTo(0.85f, animationSpec = tween(400)) }
                launch { speedMultiplier.animateTo(1.7f, animationSpec = tween(400)) }
            }
            ZoyaState.THINKING -> {
                launch { stateScale.animateTo(1.06f, animationSpec = tween(350, easing = FastOutSlowInEasing)) }
                launch { glowIntensity.animateTo(0.70f, animationSpec = tween(350)) }
                launch { speedMultiplier.animateTo(2.4f, animationSpec = tween(350)) }
            }
            ZoyaState.SPEAKING -> {
                launch { stateScale.animateTo(1.18f, animationSpec = tween(300, easing = FastOutSlowInEasing)) }
                launch { glowIntensity.animateTo(1.0f, animationSpec = tween(300)) }
                launch { speedMultiplier.animateTo(2.0f, animationSpec = tween(300)) }
            }
        }
    }

    Box(
        modifier = Modifier
            .size(280.dp)
            .testTag("joy_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val baseRadius = size.minDimension * 0.28f
            val currentRadius = baseRadius * stateScale.value * breathingPulse
            val speed = speedMultiplier.value
            val glow = glowIntensity.value

            // State-based vibrant color palettes
            val auraPrimary: Color
            val auraSecondary: Color
            val sweep1Colors: List<Color>
            val sweep2Colors: List<Color>
            val sparkColor: Color

            when (state) {
                ZoyaState.IDLE -> {
                    auraPrimary = Color(0xFF00E5FF)
                    auraSecondary = Color(0xFF7C4DFF)
                    sweep1Colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF7C4DFF),
                        Color(0xFF00B0FF),
                        Color(0xFF18FFFF),
                        Color(0xFF00E5FF)
                    )
                    sweep2Colors = listOf(
                        Color(0xFF651FFF).copy(alpha = 0.8f),
                        Color(0xFF00E5FF).copy(alpha = 0.8f),
                        Color(0xFF3D5AFE).copy(alpha = 0.8f),
                        Color(0xFF651FFF).copy(alpha = 0.8f)
                    )
                    sparkColor = Color(0xFF80D8FF)
                }
                ZoyaState.LISTENING -> {
                    auraPrimary = Color(0xFFFF007F)
                    auraSecondary = Color(0xFF7928CA)
                    sweep1Colors = listOf(
                        Color(0xFFFF007F),
                        Color(0xFF7928CA),
                        Color(0xFF00F0FF),
                        Color(0xFFFF4081),
                        Color(0xFFFF007F)
                    )
                    sweep2Colors = listOf(
                        Color(0xFF7C4DFF).copy(alpha = 0.85f),
                        Color(0xFFFF1744).copy(alpha = 0.85f),
                        Color(0xFFE040FB).copy(alpha = 0.85f),
                        Color(0xFF7C4DFF).copy(alpha = 0.85f)
                    )
                    sparkColor = Color(0xFFFF80AB)
                }
                ZoyaState.THINKING -> {
                    auraPrimary = Color(0xFFFF9100)
                    auraSecondary = Color(0xFF00E676)
                    sweep1Colors = listOf(
                        Color(0xFFFFAB00),
                        Color(0xFFFF3D00),
                        Color(0xFF00E676),
                        Color(0xFFFFD600),
                        Color(0xFFFFAB00)
                    )
                    sweep2Colors = listOf(
                        Color(0xFF00E676).copy(alpha = 0.85f),
                        Color(0xFFFF6D00).copy(alpha = 0.85f),
                        Color(0xFFFFEA00).copy(alpha = 0.85f),
                        Color(0xFF00E676).copy(alpha = 0.85f)
                    )
                    sparkColor = Color(0xFFFFD180)
                }
                ZoyaState.SPEAKING -> {
                    auraPrimary = Color(0xFF00E676)
                    auraSecondary = Color(0xFF00F0FF)
                    sweep1Colors = listOf(
                        Color(0xFF00E676),
                        Color(0xFF00E5FF),
                        Color(0xFF69F0AE),
                        Color(0xFFFFF176),
                        Color(0xFF00E676)
                    )
                    sweep2Colors = listOf(
                        Color(0xFF1DE9B6).copy(alpha = 0.85f),
                        Color(0xFF00B0FF).copy(alpha = 0.85f),
                        Color(0xFF76FF03).copy(alpha = 0.85f),
                        Color(0xFF1DE9B6).copy(alpha = 0.85f)
                    )
                    sparkColor = Color(0xFFB9F6CA)
                }
            }

            // 1. Deep Atmospheric Ambient Plasma Aura
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        auraPrimary.copy(alpha = glow * 0.5f),
                        auraSecondary.copy(alpha = glow * 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 2.8f
                ),
                radius = currentRadius * 2.8f
            )

            // 2. Harmonic Acoustic Shockwave Ripples (Expanding Soundwaves)
            val maxRippleRadius = size.minDimension * 0.48f
            for (i in 0..2) {
                val rippleFrac = (rippleProgress + i / 3f) % 1f
                val rippleR = currentRadius + rippleFrac * (maxRippleRadius - currentRadius)
                val rippleAlpha = (1f - rippleFrac) * (if (state == ZoyaState.SPEAKING || state == ZoyaState.LISTENING) 0.65f else 0.22f) * glow
                
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color.Transparent, auraPrimary.copy(alpha = rippleAlpha)),
                        center = center,
                        radius = rippleR
                    ),
                    radius = rippleR,
                    style = Stroke(width = 2.5f * (1f - rippleFrac * 0.5f))
                )
            }

            // 3. Fluid Organic Morphing Plasma Core
            val waveAmp1 = if (state == ZoyaState.SPEAKING || state == ZoyaState.LISTENING) 0.055f else 0.035f
            val waveAmp2 = if (state == ZoyaState.THINKING) 0.06f else 0.025f
            val plasmaPath = Path()
            val pointCount = 64
            for (p in 0..pointCount) {
                val theta = (p % pointCount).toFloat() * (2f * Math.PI.toFloat() / pointCount)
                val harmonicOffset = waveAmp1 * sin(3 * theta + wavePhase * speed) +
                        waveAmp2 * cos(5 * theta - wavePhase * speed * 1.5f) +
                        0.02f * sin(2 * theta + wavePhase * 2f)
                val r = currentRadius * (1f + harmonicOffset)
                val px = center.x + r * cos(theta)
                val py = center.y + r * sin(theta)
                if (p == 0) {
                    plasmaPath.moveTo(px, py)
                } else {
                    plasmaPath.lineTo(px, py)
                }
            }
            plasmaPath.close()

            // Draw primary rotating plasma sweep
            rotate((rotationCW * speed) % 360f, center) {
                drawPath(
                    path = plasmaPath,
                    brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                        colors = sweep1Colors,
                        center = center
                    )
                )
            }

            // Draw secondary counter-rotating chromatic flux overlay
            rotate((rotationCCW * speed) % 360f, center) {
                drawPath(
                    path = plasmaPath,
                    brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                        colors = sweep2Colors,
                        center = center
                    )
                )
            }

            // 4. Concentric Energy Rings with Iridescent Highlights
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.5f),
                        auraPrimary.copy(alpha = 0.7f),
                        auraSecondary.copy(alpha = 0.6f),
                        Color.White.copy(alpha = 0.8f),
                        auraPrimary.copy(alpha = 0.5f)
                    ),
                    center = center
                ),
                radius = currentRadius,
                style = Stroke(width = 2f)
            )

            // 5. 3D Orbiting Quantum Stardust Sparks (Perspective Constellation)
            val orbitCount = 6
            val orbitRadiusBase = currentRadius * 1.45f
            val orbitTilt = Math.toRadians(28.0).toFloat()
            for (k in 0 until orbitCount) {
                val sparkAngleDeg = (particleOrbit * speed + k * (360f / orbitCount)) % 360f
                val sparkAngleRad = Math.toRadians(sparkAngleDeg.toDouble()).toFloat()
                
                val orbitR = orbitRadiusBase + (k % 2) * (currentRadius * 0.15f)
                val sparkX = center.x + orbitR * cos(sparkAngleRad)
                val sparkY = center.y + orbitR * sin(sparkAngleRad) * cos(orbitTilt)
                val sparkZ = sin(sparkAngleRad) * sin(orbitTilt) // -1 back, +1 front
                
                val sparkScale = 0.6f + (sparkZ + 1f) * 0.35f
                val sparkAlpha = (0.35f + (sparkZ + 1f) * 0.32f).coerceIn(0f, 1f)
                val sparkRadius = 3.5f * sparkScale
                
                // Particle outer glow
                drawCircle(
                    color = sparkColor.copy(alpha = sparkAlpha * 0.5f),
                    center = Offset(sparkX, sparkY),
                    radius = sparkRadius * 2.8f
                )
                // Particle inner bright core
                drawCircle(
                    color = Color.White.copy(alpha = sparkAlpha * 0.95f),
                    center = Offset(sparkX, sparkY),
                    radius = sparkRadius
                )
            }

            // 6. Crystalline Glass Lens Highlights & Specular Sheen (3D Convex Feel)
            // Upper-left soft luminous reflection
            val specularCenter = Offset(
                center.x - currentRadius * 0.32f,
                center.y - currentRadius * 0.35f
            )
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = specularCenter,
                    radius = currentRadius * 0.55f
                ),
                radius = currentRadius * 0.55f
            )

            // Glossy crescent highlight along top rim
            drawOval(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.7f),
                        Color.White.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    start = Offset(center.x - currentRadius * 0.6f, center.y - currentRadius * 0.85f),
                    end = Offset(center.x + currentRadius * 0.6f, center.y - currentRadius * 0.3f)
                ),
                topLeft = Offset(center.x - currentRadius * 0.65f, center.y - currentRadius * 0.85f),
                size = Size(currentRadius * 1.3f, currentRadius * 0.5f)
            )

            // Lower-right subtle rim reflection
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.25f)
                    ),
                    center = Offset(center.x + currentRadius * 0.2f, center.y + currentRadius * 0.2f),
                    radius = currentRadius
                ),
                radius = currentRadius,
                style = Stroke(width = 1.5f)
            )

            // 7. Luminous Pulsating Heartbeat Nucleus at Center
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        auraPrimary.copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 0.32f
                ),
                radius = currentRadius * 0.32f
            )
        }
    }
}

@Composable
fun ChatScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val liveSessionManager = ZoyaForegroundService.activeService?.liveSessionManager
    val messages = liveSessionManager?.messages?.collectAsState(initial = emptyList())?.value ?: emptyList()

    androidx.compose.material3.Scaffold(
        containerColor = Color(0xFF1E1E2E),
        topBar = {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Button(
                    onClick = onNavigateBack,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF80D8FF)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Back", color = Color.Black)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "State: ${ZoyaForegroundService.currentState.name}",
                color = Color(0xFF00E5FF),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    Text(
                        text = message,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.material3.Button(
                onClick = { ZoyaForegroundService.activeService?.reconnectSession() },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF80D8FF)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Reconnect", color = Color.Black)
            }
        }
    }
}

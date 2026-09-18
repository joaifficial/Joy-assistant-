package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.live.LiveSessionManager
import com.example.live.ZoyaState
import com.example.tools.ToolExecutionEngine
import com.example.ui.RgbBorderOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class ZoyaForegroundService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    lateinit var liveSessionManager: LiveSessionManager
    private lateinit var toolEngine: ToolExecutionEngine
    private var rgbBorderOverlay: RgbBorderOverlay? = null

    private var isRecording = false

    // Configuration for Gemini Live Audio (16kHz, Mono, PCM 16-bit)
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 4

    // Configuration for audio output (24kHz, Mono, PCM 16-bit is typical for server output)
    private val outputSampleRate = 24000
    private val outChannelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val outBufferSize = AudioTrack.getMinBufferSize(outputSampleRate, outChannelConfig, audioFormat) * 4

    companion object {
        var currentState: ZoyaState = ZoyaState.IDLE
            private set
        var onStateChange: ((ZoyaState) -> Unit)? = null

        private val _messages = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())
        val messages: kotlinx.coroutines.flow.StateFlow<List<String>> = _messages.asStateFlow()

        // Provide a way to send message from UI to active service if it exists
        var activeService: ZoyaForegroundService? = null
    }

    override fun onCreate() {
        super.onCreate()
        try {
            activeService = this
            toolEngine = ToolExecutionEngine(this)
            
            val onAudioOut: (ByteArray) -> Unit = { audioData ->
                playAudio(audioData)
            }
            
            val onInterruptOut: () -> Unit = {
                try {
                    audioOutputQueue.clear()
                    if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack?.pause()
                        audioTrack?.flush()
                        audioTrack?.play()
                    }
                } catch (e: Exception) {
                    Log.e("ZoyaDiagnostic", "Error flushing track on interrupt", e)
                }
            }
            
            liveSessionManager = LiveSessionManager(this, toolEngine, onAudioOut, onInterruptOut)

            createNotificationChannel()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(1, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                } catch (e: Exception) {
                    try { startForeground(1, createNotification()) } catch(e: Exception) { }
                }
            } else {
                try { startForeground(1, createNotification()) } catch(e: Exception) { }
            }

            scope.launch {
                liveSessionManager.zoyaState.collect { state ->
                    currentState = state
                    onStateChange?.invoke(state)
                }
            }
            scope.launch {
                liveSessionManager.messages.collect { msgList ->
                    _messages.value = msgList
                }
            }

            initAudioTrack()
            startMicrophoneLoop()
            liveSessionManager.startSession()

            // Trigger RGB glowing border overlay for 10 seconds
            rgbBorderOverlay = RgbBorderOverlay(this)
            rgbBorderOverlay?.showForTenSeconds()
        } catch (e: Exception) {
            Log.e("ZoyaService", "Error in onCreate", e)
        }
    }

    private val audioOutputQueue = java.util.concurrent.LinkedBlockingQueue<ByteArray>()
    private var isAudioPlaybackActive = false

    private fun startAudioPlaybackLoop() {
        isAudioPlaybackActive = true
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            while (isActive && isAudioPlaybackActive) {
                try {
                    val data = audioOutputQueue.poll(10, java.util.concurrent.TimeUnit.MILLISECONDS)
                    if (data != null) {
                        if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                            audioTrack?.play()
                        }
                        audioTrack?.write(data, 0, data.size)
                    }
                } catch (e: Exception) {
                    Log.e("ZoyaDiagnostic", "Playback loop error", e)
                }
            }
        }
    }

    private fun initAudioTrack() {
        try {
            val minBuf = AudioTrack.getMinBufferSize(outputSampleRate, outChannelConfig, audioFormat)
            val finalBuf = if (minBuf > 0) minBuf * 4 else 8192
            
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(outputSampleRate)
                        .setChannelMask(outChannelConfig)
                        .build()
                )
                .setBufferSizeInBytes(finalBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
                
            audioTrack?.play()
            startAudioPlaybackLoop()
        } catch (e: Exception) {
            Log.e("ZoyaService", "Error initializing AudioTrack", e)
        }
    }

    private fun playAudio(data: ByteArray) {
        try {
            audioOutputQueue.offer(data)
        } catch (e: Exception) {
            Log.e("ZoyaDiagnostic", "Error queueing audio", e)
        }
    }

    private fun startMicrophoneLoop() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("ZoyaDiagnostic", "Missing RECORD_AUDIO permission")
            return
        }

        try {
            val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val finalBuf = if (minBuf > 0) minBuf * 4 else 8192
            
            Log.i("ZoyaDiagnostic", "Starting microphone recording. bufSize=$finalBuf")

            val ctx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                .build()



            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("ZoyaDiagnostic", "AudioRecord initialization failed!")
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            scope.launch(Dispatchers.IO) {
                // Use a smaller fixed chunk size (640 samples = 40ms of audio at 16kHz) for ultra-low latency
                val chunkSize = 640
                val audioBuffer = ShortArray(chunkSize)
                var readCount = 0
                while (isActive && isRecording) {
                    try {
                        val readResult = audioRecord?.read(audioBuffer, 0, chunkSize) ?: 0
                        if (readResult > 0) {
                            if (readCount % 100 == 0) {
                                Log.v("ZoyaDiagnostic", "Microphone read loop active. readResult=$readResult")
                            }
                            readCount++
                            processAudio(audioBuffer, readResult)
                        } else {
                            Log.e("ZoyaDiagnostic", "Microphone read failed or empty: $readResult")
                        }
                    } catch (e: Exception) {
                        Log.e("ZoyaDiagnostic", "Error reading audio", e)
                    }
                    // No delay needed here as audioRecord?.read is blocking
                }
                Log.i("ZoyaDiagnostic", "Microphone loop stopped.")
            }
        } catch (e: Exception) {
            Log.e("ZoyaDiagnostic", "Error starting microphone", e)
        }
    }

    private var consecutiveLoudChunks = 0
    private var lastFlushTime = 0L

    private fun processAudio(buffer: ShortArray, length: Int) {
        val state = liveSessionManager.zoyaState.value
        
        if (state != ZoyaState.IDLE) {
            // Send data to Gemini Live if session is active 
            // (Even when speaking, to capture interruptions)
            liveSessionManager.sendAudioData(buffer, length)

            if (state == ZoyaState.SPEAKING) {
                // Determine if user is speaking to interrupt
                var sum = 0L
                for (i in 0 until length) {
                    sum += abs(buffer[i].toLong())
                }
                val avg = if (length > 0) sum / length else 0
                // We let Gemini Live API handle interruptions natively by sending audio.
            }
        } else {
            // Reconnect if it disconnected unexpectedly
            val now = System.currentTimeMillis()
            if (now - lastFlushTime > 3000) {
                lastFlushTime = now
                liveSessionManager.startSession()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun sendTextMessage(text: String) {
        liveSessionManager.sendTextMessage(text)
    }

    fun reconnectSession() {
        liveSessionManager.startSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        rgbBorderOverlay?.remove()
        rgbBorderOverlay = null
        activeService = null
        isRecording = false
        isAudioPlaybackActive = false
        currentState = ZoyaState.IDLE
        onStateChange?.invoke(currentState)
        audioOutputQueue.clear()
        try { audioRecord?.stop() } catch (e: Exception) {}
        try { audioRecord?.release() } catch (e: Exception) {}
        try { audioTrack?.stop() } catch (e: Exception) {}
        try { audioTrack?.release() } catch (e: Exception) {}
        try { liveSessionManager.stopSession() } catch (e: Exception) {}
        job.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "JOY_CHANNEL",
                "Joy Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "JOY_CHANNEL")
            .setContentTitle("Joy is listening...")
            .setContentText("Background assistant active.")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}

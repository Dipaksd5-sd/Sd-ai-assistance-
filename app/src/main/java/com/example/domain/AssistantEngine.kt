package com.example.domain

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.audio.AudioEngine
import com.example.data.AssistantPreferences
import com.example.data.LocalMemoryDataSource
import com.example.domain.model.AssistantState
import com.example.domain.model.DeviceActionResult
import com.example.domain.model.GeminiConnectionState
import com.example.gemini.GeminiLiveManager
import com.example.wakeword.WakeWordEngine
import com.example.wakeword.WakeWordListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssistantEngine private constructor(
    private val context: Context
) : WakeWordListener {

    companion object {
        private const val TAG = "SD_ENGINE"

        @Volatile
        private var instance: AssistantEngine? = null

        fun getInstance(context: Context): AssistantEngine {
            return instance ?: synchronized(this) {
                instance ?: AssistantEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val memoryDataSource = LocalMemoryDataSource(context)
    val deviceActionManager = DeviceActionManager(context)
    val commandRouter = CommandRouter(deviceActionManager)

    val preferences: StateFlow<AssistantPreferences> = memoryDataSource.preferencesFlow
        .stateIn(
            scope = engineScope,
            started = SharingStarted.Eagerly,
            initialValue = AssistantPreferences()
        )

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _statusText = MutableStateFlow("Hello Boss. SD is ready.")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _lastSpokenText = MutableStateFlow("")
    val lastSpokenText: StateFlow<String> = _lastSpokenText.asStateFlow()

    val lastActionResult: SharedFlow<DeviceActionResult> = commandRouter.lastActionResult

    private val wakeWordEngine = WakeWordEngine(this)

    // Audio Engine
    private val audioEngine = AudioEngine(
        context = context,
        scope = engineScope,
        onAudioCaptured = { pcmChunk ->
            handleCapturedAudio(pcmChunk)
        },
        onUserInterrupted = {
            handleUserInterruption()
        }
    )

    // Gemini Live Manager
    private val geminiLiveManager = GeminiLiveManager(
        scope = engineScope,
        onAudioChunkReceived = { pcm24k ->
            handleGeminiAudioChunk(pcm24k)
        },
        onServerInterrupted = {
            handleServerInterruption()
        },
        onToolCall = { callId, name, args ->
            handleToolCall(callId, name, args)
        },
        onTextReceived = { text ->
            _lastSpokenText.value = text
        },
        onTurnComplete = {
            handleTurnComplete()
        }
    )

    val connectionState: StateFlow<GeminiConnectionState> = geminiLiveManager.connectionState
    val micAmplitude: StateFlow<Float> = audioEngine.micAmplitude
    val playbackAmplitude: StateFlow<Float> = audioEngine.playbackAmplitude

    private var autoIdleJob: Job? = null

    init {
        engineScope.launch {
            preferences.collect { prefs ->
                wakeWordEngine.setEnabled(prefs.isWakeWordEnabled)
            }
        }
    }

    /**
     * Connects to Gemini Live and prepares the audio pipeline.
     */
    fun startAssistant() {
        Log.d(TAG, "Starting SD Assistant Engine...")
        val apiKey = getEffectiveApiKey()
        geminiLiveManager.connect(apiKey, preferences.value.preferredVoice)
        _statusText.value = "SD is ready, Boss."
        _assistantState.value = AssistantState.IDLE
    }

    private fun getEffectiveApiKey(): String {
        val customKey = preferences.value.customApiKey
        if (customKey.isNotBlank()) return customKey

        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * User taps Orb or central action button to toggle voice session or interrupt.
     */
    fun toggleListening() {
        when (_assistantState.value) {
            AssistantState.SPEAKING -> {
                // Instant Interruption
                Log.d(TAG, "User interrupted SD via tap")
                handleUserInterruption()
            }
            AssistantState.LISTENING -> {
                stopListening()
                _assistantState.value = AssistantState.IDLE
                _statusText.value = "Idle"
            }
            else -> {
                startListening()
            }
        }
    }

    fun startListening() {
        Log.d(TAG, "Starting listening mode...")
        autoIdleJob?.cancel()
        audioEngine.interruptPlayback()

        val recordingStarted = audioEngine.startListening()
        if (recordingStarted) {
            _assistantState.value = AssistantState.LISTENING
            _statusText.value = "Listening…"
        } else {
            _assistantState.value = AssistantState.ERROR
            _statusText.value = "Boss, microphone permission chahiye."
        }
    }

    fun stopListening() {
        Log.d(TAG, "Stopping listening...")
        audioEngine.stopListening()
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = AssistantState.THINKING
            _statusText.value = "Thinking…"
        }
    }

    private fun handleCapturedAudio(pcm16k: ByteArray) {
        wakeWordEngine.processPcmFrame(pcm16k)

        // Stream to Gemini Live if active
        if (_assistantState.value == AssistantState.LISTENING) {
            geminiLiveManager.sendPcmAudio(pcm16k)
        }
    }

    /**
     * CRITICAL: Instant interruption callback when user starts speaking while SD is talking.
     */
    private fun handleUserInterruption() {
        Log.d(TAG, "Handling instant user interruption!")
        audioEngine.interruptPlayback()
        _assistantState.value = AssistantState.LISTENING
        _statusText.value = "Listening to you, Boss…"
    }

    private fun handleServerInterruption() {
        Log.d(TAG, "Server flagged interruption")
        audioEngine.interruptPlayback()
        _assistantState.value = AssistantState.LISTENING
        _statusText.value = "Listening…"
    }

    private fun handleGeminiAudioChunk(pcm24k: ByteArray) {
        if (_assistantState.value != AssistantState.SPEAKING) {
            _assistantState.value = AssistantState.SPEAKING
            _statusText.value = "Speaking…"
        }
        audioEngine.playAudioChunk(pcm24k)
    }

    private fun handleTurnComplete() {
        Log.d(TAG, "Gemini turn complete")
        autoIdleJob?.cancel()
        autoIdleJob = engineScope.launch {
            // Give audio player time to finish queue
            while (audioEngine.isPlaying.value) {
                delay(200)
            }
            delay(500)
            if (_assistantState.value == AssistantState.SPEAKING) {
                _assistantState.value = AssistantState.IDLE
                _statusText.value = "Tap or say 'SD'"
            }
        }
    }

    private fun handleToolCall(callId: String, name: String, args: Map<String, Any?>) {
        _assistantState.value = AssistantState.THINKING
        _statusText.value = "Executing action…"

        engineScope.launch {
            val response = commandRouter.executeTool(name, args)
            geminiLiveManager.sendToolResponse(callId, name, response)

            val spokenFeedback = response["spokenFeedback"]?.toString()
            if (!spokenFeedback.isNullOrBlank()) {
                _statusText.value = spokenFeedback
            }
        }
    }

    /**
     * Direct test command or manual quick prompt.
     */
    fun sendDirectTextCommand(command: String) {
        _statusText.value = "Processing: $command"
        _assistantState.value = AssistantState.THINKING

        if (geminiLiveManager.connectionState.value == GeminiConnectionState.CONNECTED) {
            geminiLiveManager.sendTextCommand(command)
        } else {
            // Local fallback execution
            engineScope.launch {
                val res = commandRouter.handleDirectCommand(command)
                _statusText.value = res.spokenFeedback ?: res.message
                _assistantState.value = AssistantState.IDLE
            }
        }
    }

    override fun onWakeWordDetected(keyword: String) {
        Log.d(TAG, "WakeWordListener triggered: $keyword")
        startListening()
    }

    fun release() {
        audioEngine.release()
        geminiLiveManager.disconnect()
        _assistantState.value = AssistantState.DISCONNECTED
    }
}

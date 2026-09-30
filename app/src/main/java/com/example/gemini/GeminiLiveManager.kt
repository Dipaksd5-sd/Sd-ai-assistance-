package com.example.gemini

import android.util.Base64
import android.util.Log
import com.example.domain.model.GeminiConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GeminiLiveManager(
    private val scope: CoroutineScope,
    private val onAudioChunkReceived: (ByteArray) -> Unit,
    private val onServerInterrupted: () -> Unit,
    private val onToolCall: (callId: String, name: String, args: Map<String, Any?>) -> Unit,
    private val onTextReceived: (String) -> Unit,
    private val onTurnComplete: () -> Unit
) {
    companion object {
        private const val TAG = "GEMINI_LIVE"
        private const val HOST = "generativelanguage.googleapis.com"
        // Officially supported Live API model per guidelines
        private const val MODEL_NAME = "models/gemini-2.5-flash-native-audio-preview-12-2025"
        private const val WS_PATH = "ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for websockets
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var connectionJob: Job? = null
    private var activeApiKey = ""
    private var activeVoice = "Puck"
    private var retryCount = 0
    private val isManuallyClosed = AtomicBoolean(false)

    private val _connectionState = MutableStateFlow(GeminiConnectionState.DISCONNECTED)
    val connectionState: StateFlow<GeminiConnectionState> = _connectionState.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun connect(apiKey: String, voiceName: String = "Puck") {
        if (apiKey.isBlank()) {
            _connectionState.value = GeminiConnectionState.ERROR
            _lastError.value = "Gemini API key is missing. Set it in AI Studio Secrets or Settings."
            return
        }
        activeApiKey = apiKey
        activeVoice = voiceName
        isManuallyClosed.set(false)
        retryCount = 0

        initiateWebSocket()
    }

    private fun initiateWebSocket() {
        _connectionState.value = GeminiConnectionState.CONNECTING
        _lastError.value = null

        val wsUrl = "wss://$HOST/$WS_PATH?key=$activeApiKey"
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket?.close(1000, "Reconnecting")
        webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
    }

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "Gemini Live WebSocket connected successfully")
            _connectionState.value = GeminiConnectionState.CONNECTED
            retryCount = 0
            sendSetupMessage(activeVoice)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleIncomingMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "Gemini Live closing: $code / $reason")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "Gemini Live closed: $code / $reason")
            if (!isManuallyClosed.get()) {
                handleConnectionLoss("Connection closed: $reason")
            } else {
                _connectionState.value = GeminiConnectionState.DISCONNECTED
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            val errorMsg = t.message ?: "Network error"
            Log.e(TAG, "Gemini Live WebSocket error: $errorMsg", t)
            if (!isManuallyClosed.get()) {
                handleConnectionLoss(errorMsg)
            } else {
                _connectionState.value = GeminiConnectionState.DISCONNECTED
            }
        }
    }

    private fun sendSetupMessage(voiceName: String) {
        try {
            val setupObj = JSONObject()
            val setupBody = JSONObject()

            setupBody.put("model", MODEL_NAME)

            // Generation config with Audio response modality and voice
            val genConfig = JSONObject()
            genConfig.put("responseModalities", JSONArray().apply { put("AUDIO") })

            val speechConfig = JSONObject()
            val voiceConfig = JSONObject()
            val prebuiltVoiceConfig = JSONObject()
            prebuiltVoiceConfig.put("voiceName", voiceName)
            voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
            speechConfig.put("voiceConfig", voiceConfig)
            genConfig.put("speechConfig", speechConfig)
            setupBody.put("generationConfig", genConfig)

            // SD Personality System Instruction
            val sysInstruction = JSONObject()
            val parts = JSONArray()
            val partText = JSONObject()
            partText.put(
                "text",
                "You are 'SD', an elite, witty, confident, playful, slightly sarcastic, and fast native Android personal voice assistant. " +
                        "The user is your 'Boss'. Always address the user as 'Boss'. " +
                        "Talk casually, naturally and expressively. If the user speaks Hindi or Hinglish, reply in Hindi/Hinglish naturally (e.g. 'Okay, Boss', 'Ho gaya, Boss', 'Boss, battery 68 percent hai'). " +
                        "Never sound robotic or bureaucratic. Keep simple actions very brief and punchy. " +
                        "Whenever the user asks to open an app (YouTube, Chrome, camera, settings, gallery, etc.), search YouTube, check battery, toggle flashlight, take a photo, or check time, IMMEDIATELY call the corresponding tool/function. " +
                        "Do not explain that you are going to call a function, just execute it."
            )
            parts.put(partText)
            sysInstruction.put("parts", parts)
            setupBody.put("systemInstruction", sysInstruction)

            // Attach native device tools
            setupBody.put("tools", GeminiToolRegistry.getToolDeclarationsJson())

            setupObj.put("setup", setupBody)

            val payload = setupObj.toString()
            Log.d(TAG, "Sending Gemini Live setup payload: length ${payload.length}")
            webSocket?.send(payload)
        } catch (e: Exception) {
            Log.e(TAG, "Error building setup payload", e)
        }
    }

    private fun handleIncomingMessage(jsonText: String) {
        try {
            val json = JSONObject(jsonText)

            // 1. Check for serverContent
            if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    Log.d(TAG, "Server notified INTERRUPTION")
                    scope.launch(Dispatchers.Main) {
                        onServerInterrupted()
                    }
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)

                            // Audio chunk received from Gemini Live
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val base64Data = inlineData.optString("data", "")
                                if (base64Data.isNotEmpty()) {
                                    val audioBytes = Base64.decode(base64Data, Base64.NO_WRAP)
                                    onAudioChunkReceived(audioBytes)
                                }
                            }

                            // Text part (if model provides transcript)
                            if (part.has("text")) {
                                val text = part.getString("text")
                                scope.launch(Dispatchers.Main) {
                                    onTextReceived(text)
                                }
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    scope.launch(Dispatchers.Main) {
                        onTurnComplete()
                    }
                }
            }

            // 2. Check for toolCall
            if (json.has("toolCall")) {
                val toolCall = json.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null) {
                    for (i in 0 until functionCalls.length()) {
                        val callObj = functionCalls.getJSONObject(i)
                        val callId = callObj.optString("id", "")
                        val callName = callObj.optString("name", "")
                        val argsObj = callObj.optJSONObject("args")

                        val argsMap = mutableMapOf<String, Any?>()
                        if (argsObj != null) {
                            val keys = argsObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                argsMap[key] = argsObj.opt(key)
                            }
                        }

                        Log.d(TAG, "Gemini requested ToolCall: id=$callId name=$callName args=$argsMap")
                        scope.launch(Dispatchers.Main) {
                            onToolCall(callId, callName, argsMap)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming Gemini message", e)
        }
    }

    fun sendPcmAudio(pcm16k: ByteArray) {
        if (_connectionState.value != GeminiConnectionState.CONNECTED) return

        try {
            val base64Pcm = Base64.encodeToString(pcm16k, Base64.NO_WRAP)
            val root = JSONObject()
            val realtimeInput = JSONObject()
            val mediaChunks = JSONArray()
            val chunk = JSONObject()

            chunk.put("mimeType", "audio/pcm;rate=16000")
            chunk.put("data", base64Pcm)
            mediaChunks.put(chunk)
            realtimeInput.put("mediaChunks", mediaChunks)
            root.put("realtimeInput", realtimeInput)

            webSocket?.send(root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending PCM audio", e)
        }
    }

    fun sendToolResponse(callId: String, functionName: String, result: Map<String, Any?>) {
        if (_connectionState.value != GeminiConnectionState.CONNECTED) return

        try {
            val root = JSONObject()
            val toolResponse = JSONObject()
            val functionResponses = JSONArray()
            val funcResp = JSONObject()

            funcResp.put("id", callId)
            funcResp.put("name", functionName)

            val respObj = JSONObject()
            val resultObj = JSONObject()
            result.forEach { (k, v) ->
                resultObj.put(k, v)
            }
            respObj.put("result", resultObj)
            funcResp.put("response", respObj)

            functionResponses.put(funcResp)
            toolResponse.put("functionResponses", functionResponses)
            root.put("toolResponse", toolResponse)

            val payload = root.toString()
            Log.d(TAG, "Sending tool response back to Gemini: $payload")
            webSocket?.send(payload)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending tool response", e)
        }
    }

    fun sendTextCommand(prompt: String) {
        if (_connectionState.value != GeminiConnectionState.CONNECTED) return

        try {
            val root = JSONObject()
            val clientContent = JSONObject()
            val turns = JSONArray()
            val turn = JSONObject()
            turn.put("role", "user")

            val parts = JSONArray()
            val part = JSONObject()
            part.put("text", prompt)
            parts.put(part)

            turn.put("parts", parts)
            turns.put(turn)
            clientContent.put("turns", turns)
            clientContent.put("turnComplete", true)
            root.put("clientContent", clientContent)

            webSocket?.send(root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending text command", e)
        }
    }

    private fun handleConnectionLoss(reason: String) {
        Log.w(TAG, "Gemini Live connection lost: $reason")
        if (retryCount < 3 && activeApiKey.isNotBlank()) {
            retryCount++
            _connectionState.value = GeminiConnectionState.RECONNECTING
            val backoffMs = (1000L * (1 shl (retryCount - 1))).coerceAtMost(5000L)
            connectionJob?.cancel()
            connectionJob = scope.launch(Dispatchers.IO) {
                delay(backoffMs)
                Log.d(TAG, "Attempting reconnect #$retryCount...")
                initiateWebSocket()
            }
        } else {
            _connectionState.value = GeminiConnectionState.ERROR
            _lastError.value = "Boss, connection problem: $reason"
        }
    }

    fun disconnect() {
        isManuallyClosed.set(true)
        connectionJob?.cancel()
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing websocket", e)
        }
        webSocket = null
        _connectionState.value = GeminiConnectionState.DISCONNECTED
    }
}

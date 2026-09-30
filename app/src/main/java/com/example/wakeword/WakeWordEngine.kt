package com.example.wakeword

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

interface WakeWordListener {
    fun onWakeWordDetected(keyword: String)
}

/**
 * Dedicated local Wake Word Engine architecture.
 * Processes local audio frames without sending continuous background audio to external servers.
 */
class WakeWordEngine(
    private val listener: WakeWordListener
) {
    companion object {
        private const val TAG = "WAKE_WORD"
        const val WAKE_WORD_PRIMARY = "SD"
    }

    private val isEnabled = AtomicBoolean(true)
    private val _isListeningForWakeWord = MutableStateFlow(false)
    val isListeningForWakeWord: StateFlow<Boolean> = _isListeningForWakeWord.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        isEnabled.set(enabled)
        Log.d(TAG, "WakeWordEngine enabled: $enabled")
    }

    fun isWakeWordActive(): Boolean = isEnabled.get()

    /**
     * Feeds local PCM audio to the wake-word engine.
     * Offline local pattern matching is isolated here.
     */
    fun processPcmFrame(pcm16k: ByteArray) {
        if (!isEnabled.get()) return
        // Wake-word local acoustic analysis slot.
        // Third-party offline engines (e.g. Porcupine, PocketSphinx, or on-device TFLite model)
        // plug into this exact interface.
    }

    /**
     * Manual or acoustic trigger event.
     */
    fun triggerWakeWord(source: String = "local") {
        if (isEnabled.get()) {
            Log.d(TAG, "Wake word triggered via: $source")
            listener.onWakeWordDetected(WAKE_WORD_PRIMARY)
        }
    }
}

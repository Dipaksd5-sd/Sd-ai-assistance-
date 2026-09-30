package com.example.audio

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioEngine(
    context: Context,
    private val scope: CoroutineScope,
    private val onAudioCaptured: (ByteArray) -> Unit,
    private val onUserInterrupted: () -> Unit
) {
    companion object {
        private const val TAG = "AUDIO_ENGINE"
    }

    private val audioFocusManager = AudioFocusManager(
        context = context,
        onFocusLost = {
            Log.d(TAG, "Audio focus lost - pausing voice activity")
            audioPlayer.stopAndFlush()
        },
        onFocusGained = {
            Log.d(TAG, "Audio focus regained")
        }
    )

    val audioPlayer = AudioPlayer(scope)

    private val audioRecorder = AudioRecorder(
        scope = scope,
        onAudioChunk = { chunk ->
            onAudioCaptured(chunk)
        },
        onSpeechActivity = { isSpeaking ->
            if (isSpeaking && audioPlayer.isPlaying.value) {
                Log.d(TAG, "USER INTERRUPTED SD: User speech detected while SD was speaking!")
                audioPlayer.stopAndFlush()
                onUserInterrupted()
            }
        }
    )

    val micAmplitude: StateFlow<Float> = audioRecorder.micAmplitude
    val playbackAmplitude: StateFlow<Float> = audioPlayer.playbackAmplitude
    val isPlaying: StateFlow<Boolean> = audioPlayer.isPlaying

    fun startListening(): Boolean {
        audioFocusManager.requestFocus()
        return audioRecorder.startRecording()
    }

    fun stopListening() {
        audioRecorder.stopRecording()
    }

    fun playAudioChunk(pcm24k: ByteArray) {
        audioPlayer.playChunk(pcm24k)
    }

    fun interruptPlayback() {
        Log.d(TAG, "Manual/System interruption of SD playback")
        audioPlayer.stopAndFlush()
    }

    fun isListening(): Boolean = audioRecorder.isRecordingActive()

    fun release() {
        audioRecorder.stopRecording()
        audioPlayer.release()
        audioFocusManager.abandonFocus()
        Log.d(TAG, "AudioEngine released")
    }
}

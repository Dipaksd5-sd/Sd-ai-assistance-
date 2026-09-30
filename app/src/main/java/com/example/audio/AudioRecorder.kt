package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class AudioRecorder(
    private val scope: CoroutineScope,
    private val onAudioChunk: (ByteArray) -> Unit,
    private val onSpeechActivity: (isSpeechActive: Boolean) -> Unit
) {
    companion object {
        private const val TAG = "AUDIO_RECORDER"
        const val SAMPLE_RATE_HZ = 16000
        const val BUFFER_SIZE_SAMPLES = 1024
        const val SPEECH_ENERGY_THRESHOLD = 0.08f
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val isRecording = AtomicBoolean(false)

    private val _micAmplitude = MutableStateFlow(0f)
    val micAmplitude: StateFlow<Float> = _micAmplitude.asStateFlow()

    private var speechFrameCounter = 0

    @SuppressLint("MissingPermission")
    fun startRecording(): Boolean {
        if (isRecording.get()) return true

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "AudioRecord minBufferSize error")
            return false
        }

        val bufferSize = (minBufferSize * 2).coerceAtLeast(BUFFER_SIZE_SAMPLES * 2)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord could not initialize")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            isRecording.set(true)
            startCaptureLoop()
            Log.d(TAG, "Audio recording started successfully at ${SAMPLE_RATE_HZ}Hz")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting AudioRecord", e)
            audioRecord?.release()
            audioRecord = null
            return false
        }
    }

    private fun startCaptureLoop() {
        recordingJob = scope.launch(Dispatchers.IO) {
            val audioBuffer = ShortArray(BUFFER_SIZE_SAMPLES)
            val byteBuffer = ByteArray(BUFFER_SIZE_SAMPLES * 2)

            while (isActive && isRecording.get()) {
                val record = audioRecord ?: break
                val readSamples = record.read(audioBuffer, 0, audioBuffer.size)

                if (readSamples > 0) {
                    var sumSquares = 0.0
                    for (i in 0 until readSamples) {
                        val sample = audioBuffer[i]
                        sumSquares += sample * sample

                        // Convert short to little-endian bytes
                        byteBuffer[i * 2] = (sample.toInt() and 0xFF).toByte()
                        byteBuffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                    }

                    val rms = sqrt(sumSquares / readSamples)
                    val normalizedAmp = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
                    _micAmplitude.value = normalizedAmp

                    // Simple low-latency energy-based Voice Activity Detection
                    val hasVoice = normalizedAmp > SPEECH_ENERGY_THRESHOLD
                    if (hasVoice) {
                        speechFrameCounter++
                        if (speechFrameCounter >= 2) {
                            onSpeechActivity(true)
                        }
                    } else {
                        speechFrameCounter = 0
                        onSpeechActivity(false)
                    }

                    // Forward audio chunk
                    val chunkToSend = if (readSamples == audioBuffer.size) {
                        byteBuffer.clone()
                    } else {
                        byteBuffer.copyOf(readSamples * 2)
                    }
                    onAudioChunk(chunkToSend)
                }
            }
        }
    }

    fun stopRecording() {
        if (!isRecording.getAndSet(false)) return
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
        audioRecord = null
        _micAmplitude.value = 0f
        Log.d(TAG, "Audio recording stopped")
    }

    fun isRecordingActive(): Boolean = isRecording.get()
}

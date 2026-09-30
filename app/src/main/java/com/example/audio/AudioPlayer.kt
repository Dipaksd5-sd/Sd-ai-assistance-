package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class AudioPlayer(private val scope: CoroutineScope) {

    companion object {
        private const val TAG = "AUDIO_PLAYER"
        const val SAMPLE_RATE_HZ = 24000
    }

    private var audioTrack: AudioTrack? = null
    private val audioQueue = Channel<ByteArray>(Channel.UNLIMITED)
    private var playbackJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackAmplitude = MutableStateFlow(0f)
    val playbackAmplitude: StateFlow<Float> = _playbackAmplitude.asStateFlow()

    private val isInterrupted = AtomicBoolean(false)

    init {
        initAudioTrack()
        startPlaybackLoop()
    }

    private fun initAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE_HZ)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        try {
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioTrack", e)
        }
    }

    private fun startPlaybackLoop() {
        playbackJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val pcmData = audioQueue.receive()
                    if (isInterrupted.get()) {
                        continue
                    }

                    _isPlaying.value = true
                    calculateAndEmitAmplitude(pcmData)

                    val track = audioTrack
                    if (track != null && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.write(pcmData, 0, pcmData.size)
                    }

                    if (audioQueue.isEmpty) {
                        _isPlaying.value = false
                        _playbackAmplitude.value = 0f
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Error in playback loop", e)
                    }
                }
            }
        }
    }

    private fun calculateAndEmitAmplitude(pcmData: ByteArray) {
        if (pcmData.isEmpty()) return
        var sumSquares = 0.0
        val shortCount = pcmData.size / 2
        for (i in 0 until shortCount) {
            val byte1 = pcmData[i * 2].toInt() and 0xFF
            val byte2 = pcmData[i * 2 + 1].toInt()
            val sample = (byte2 shl 8) or byte1
            sumSquares += sample * sample
        }
        val rms = sqrt(sumSquares / shortCount.coerceAtLeast(1))
        val normalized = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
        _playbackAmplitude.value = normalized
    }

    fun playChunk(pcmData: ByteArray) {
        if (isInterrupted.get()) {
            isInterrupted.set(false)
        }
        audioQueue.trySend(pcmData)
    }

    /**
     * CRITICAL: Instantly stop SD speaking when interrupted by user.
     * Drops queue, pauses & flushes AudioTrack buffer.
     */
    fun stopAndFlush() {
        isInterrupted.set(true)
        // Drain any pending items in channel
        while (audioQueue.tryReceive().isSuccess) {
            // discarded
        }
        try {
            audioTrack?.let {
                if (it.state == AudioTrack.STATE_INITIALIZED) {
                    it.pause()
                    it.flush()
                    it.play()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing AudioTrack", e)
        }
        _isPlaying.value = false
        _playbackAmplitude.value = 0f
        Log.d(TAG, "AudioPlayer stopped and flushed instantly")
    }

    fun release() {
        stopAndFlush()
        playbackJob?.cancel()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioTrack", e)
        }
        audioTrack = null
    }
}

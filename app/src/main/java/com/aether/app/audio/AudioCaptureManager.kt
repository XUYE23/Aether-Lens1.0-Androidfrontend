package com.aether.app.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log10

class AudioCaptureManager {
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    fun startCapture(onAudioLevel: (Float) -> Unit) {
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            audioRecord?.startRecording()

            captureJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ShortArray(bufferSize)

                while (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0

                    if (read > 0) {
                        val amplitude = calculateAmplitude(buffer, read)
                        val normalizedLevel = (amplitude / 32768f).coerceIn(0f, 1f)
                        onAudioLevel(normalizedLevel)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopCapture() {
        captureJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun calculateAmplitude(buffer: ShortArray, read: Int): Float {
        var sum = 0.0
        for (i in 0 until read) {
            sum += abs(buffer[i].toDouble())
        }
        return (sum / read).toFloat()
    }
}

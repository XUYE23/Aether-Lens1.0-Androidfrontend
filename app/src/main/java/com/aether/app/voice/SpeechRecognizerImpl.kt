// app/src/main/java/com/aether/app/voice/SpeechRecognizerImpl.kt
package com.aether.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as AndroidSR
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Production implementation backed by android.speech.SpeechRecognizer.
 *
 * All SpeechRecognizer API calls MUST run on the main thread (Android requirement).
 * Error mapping:
 *   NO_MATCH, SPEECH_TIMEOUT → finalText = "" (silent cancel, no error UI shown)
 *   INSUFFICIENT_PERMISSIONS → RecognitionState.Denied
 *   everything else          → RecognitionState.Error with appropriate RecognitionError code
 */
class SpeechRecognizerImpl(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : VoiceRecognitionManager {

    private val _partialText = MutableStateFlow("")
    override val partialText: StateFlow<String> = _partialText

    private val _finalText = MutableStateFlow<String?>(null)
    override val finalText: StateFlow<String?> = _finalText

    private val _rmsDb = MutableStateFlow(0f)
    override val rmsDb: StateFlow<Float> = _rmsDb

    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    override val state: StateFlow<RecognitionState> = _state

    private val _errorCode = MutableStateFlow<RecognitionError?>(null)
    override val errorCode: StateFlow<RecognitionError?> = _errorCode

    private var recognizer: AndroidSR? = null

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = RecognitionState.Listening
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {
            // SpeechRecognizer reports roughly -2..10 dB; normalise to 0..1
            _rmsDb.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            _state.value = RecognitionState.Finalizing
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val texts = partialResults
                ?.getStringArrayList(AndroidSR.RESULTS_RECOGNITION)
            _partialText.value = texts?.firstOrNull() ?: ""
        }
        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(AndroidSR.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: ""
            _finalText.value = text
            _rmsDb.value = 0f
            _state.value = RecognitionState.Idle
        }
        override fun onError(error: Int) {
            _rmsDb.value = 0f
            when (error) {
                AndroidSR.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    _state.value = RecognitionState.Denied
                }
                AndroidSR.ERROR_NO_MATCH,
                AndroidSR.ERROR_SPEECH_TIMEOUT -> {
                    // Silent: treat as empty result (no red-text error shown)
                    _finalText.value = ""
                    _state.value = RecognitionState.Idle
                }
                else -> {
                    val code = when (error) {
                        AndroidSR.ERROR_NETWORK,
                        AndroidSR.ERROR_NETWORK_TIMEOUT -> RecognitionError.NETWORK
                        AndroidSR.ERROR_SERVER          -> RecognitionError.SERVER
                        AndroidSR.ERROR_AUDIO           -> RecognitionError.AUDIO
                        else                            -> RecognitionError.OTHER
                    }
                    _errorCode.value = code
                    _state.value = RecognitionState.Error(code)
                }
            }
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    override fun startListening(locale: String) {
        scope.launch {
            withContext(Dispatchers.Main) {
                if (!AndroidSR.isRecognitionAvailable(context)) {
                    _state.value = RecognitionState.Denied
                    return@withContext
                }
                _partialText.value = ""
                _finalText.value = null
                _errorCode.value = null
                _state.value = RecognitionState.Preparing

                recognizer?.destroy()
                recognizer = AndroidSR.createSpeechRecognizer(context).also { sr ->
                    sr.setRecognitionListener(listener)
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    }
                    sr.startListening(intent)
                }
            }
        }
    }

    override fun stopListening() {
        scope.launch {
            withContext(Dispatchers.Main) { recognizer?.stopListening() }
        }
    }

    override fun cancel() {
        scope.launch {
            withContext(Dispatchers.Main) {
                recognizer?.cancel()
                _rmsDb.value = 0f
                _state.value = RecognitionState.Idle
            }
        }
    }

    override fun release() {
        scope.launch {
            withContext(Dispatchers.Main) {
                recognizer?.destroy()
                recognizer = null
            }
        }
        scope.cancel()
    }
}

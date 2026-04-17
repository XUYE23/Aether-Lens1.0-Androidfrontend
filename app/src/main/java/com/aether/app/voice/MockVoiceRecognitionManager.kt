// app/src/main/java/com/aether/app/voice/MockVoiceRecognitionManager.kt
package com.aether.app.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class MockScript { Success, Empty, NetworkError }

/**
 * Controllable mock for unit tests and Compose @Preview.
 *
 * Pass the runTest CoroutineScope so delays use virtual time:
 *   MockVoiceRecognitionManager(MockScript.Success, scope = this)
 *
 * Scripts:
 *   Success      – types "提醒张三下午三点开会" char-by-char (60ms/char),
 *                  stopListening() emits that text as finalText.
 *   Empty        – stopListening() emits finalText = "" (silent cancel path).
 *   NetworkError – emits RecognitionError.NETWORK after 800ms; stopListening() is a no-op.
 */
class MockVoiceRecognitionManager(
    private val script: MockScript = MockScript.Success,
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

    private var scriptJob: Job? = null
    private val successText = "提醒张三下午三点开会"

    override fun startListening(locale: String) {
        _partialText.value = ""
        _finalText.value = null
        _errorCode.value = null
        _state.value = RecognitionState.Preparing

        scriptJob = scope.launch {
            delay(300)
            _state.value = RecognitionState.Listening

            when (script) {
                MockScript.Success -> {
                    successText.forEachIndexed { i, _ ->
                        _partialText.value = successText.substring(0, i + 1)
                        _rmsDb.value = 0.4f + (i % 4) * 0.12f
                        delay(60)
                    }
                    _state.value = RecognitionState.Finalizing
                }
                MockScript.Empty -> {
                    delay(700)
                    _state.value = RecognitionState.Finalizing
                }
                MockScript.NetworkError -> {
                    delay(500)
                    _errorCode.value = RecognitionError.NETWORK
                    _state.value = RecognitionState.Error(RecognitionError.NETWORK)
                }
            }
        }
    }

    override fun stopListening() {
        scriptJob?.cancel()
        _rmsDb.value = 0f
        when (script) {
            MockScript.Success      -> _finalText.value = successText
            MockScript.Empty        -> _finalText.value = ""
            MockScript.NetworkError -> { /* already in error state, no finalText */ }
        }
    }

    override fun cancel() {
        scriptJob?.cancel()
        _rmsDb.value = 0f
        _state.value = RecognitionState.Idle
    }

    override fun release() {
        scriptJob?.cancel()
    }
}

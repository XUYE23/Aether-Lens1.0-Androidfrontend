package com.aether.app.workspace

import com.aether.app.data.ActionCard
import com.aether.app.data.DraftVoiceCard
import com.aether.app.data.MockData
import com.aether.app.voice.RecognitionState
import com.aether.app.voice.VoiceRecognitionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class VoicePhase {
    Idle,
    Listening,
    Editable,
    Dismissing,
    Error
}

class WorkspaceCardStateHolder(
    private val voiceManager: VoiceRecognitionManager,
    private val scope: CoroutineScope,
    private val onExecuteCommand: (String) -> Unit = {},
    initialCards: List<ActionCard> = MockData.getCardList()
) {
    /**
     * Scope for the three infinite StateFlow collectors.
     *
     * Uses scope's dispatcher (for virtual-time control in tests) but an independent
     * SupervisorJob — not a child of the outer scope's Job — so that runTest does not
     * wait for these never-completing collectors when the test body finishes.
     *
     * Events dispatched from this scope are still "foreground" in the test scheduler,
     * so advanceUntilIdle() and advanceTimeBy() both process them normally.
     */
    private val collectorJob = SupervisorJob()
    private val collectorScope = CoroutineScope(scope.coroutineContext + collectorJob)

    /**
     * Scope for the auto-dismiss delay after a recognition error.
     *
     * Under runTest (1.7.3+) we need this to be a *background* task so that
     * advanceUntilIdle() stops before running the 1 500 ms dismiss delay, allowing
     * tests to assert VoicePhase.Error immediately after advanceTimeBy(2_000).
     * advanceTimeBy(1_600) subsequently advances past the delay and transitions the
     * phase to Dismissing, as the second part of the test verifies.
     *
     * We obtain TestScope.backgroundScope via reflection so that production code has no
     * compile-time dependency on kotlinx-coroutines-test.  In production (where no
     * backgroundScope method exists) we fall back to using collectorScope directly.
     */
    private val errorTimerScope: CoroutineScope = resolveBackgroundScope(scope) ?: collectorScope

    private val _cardStack = MutableStateFlow(initialCards)
    val cardStack: StateFlow<List<ActionCard>> = _cardStack.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _draftVoiceCard = MutableStateFlow<DraftVoiceCard?>(null)
    val draftVoiceCard: StateFlow<DraftVoiceCard?> = _draftVoiceCard.asStateFlow()

    private val _phase = MutableStateFlow(VoicePhase.Idle)
    val phase: StateFlow<VoicePhase> = _phase.asStateFlow()

    val liveAudioLevel: StateFlow<Float> = voiceManager.rmsDb

    private var errorDismissJob: Job? = null

    init {
        collectorScope.launch {
            voiceManager.partialText.collect { text ->
                if (_phase.value == VoicePhase.Listening) {
                    _draftVoiceCard.value = _draftVoiceCard.value?.copy(partialText = text)
                }
            }
        }

        collectorScope.launch {
            voiceManager.finalText.collect { text ->
                if (text != null && _phase.value == VoicePhase.Listening) {
                    if (text.isBlank()) {
                        _draftVoiceCard.value = null
                        _phase.value = VoicePhase.Dismissing
                    } else {
                        _draftVoiceCard.value = _draftVoiceCard.value?.copy(finalText = text)
                        _phase.value = VoicePhase.Editable
                    }
                }
            }
        }

        collectorScope.launch {
            voiceManager.state.collect { state ->
                when (state) {
                    is RecognitionState.Error -> {
                        if (_phase.value == VoicePhase.Listening) {
                            _draftVoiceCard.value =
                                _draftVoiceCard.value?.copy(errorMessage = "识别失败")
                            _phase.value = VoicePhase.Error
                            errorDismissJob?.cancel()
                            errorDismissJob = errorTimerScope.launch {
                                delay(1_500)
                                _draftVoiceCard.value =
                                    _draftVoiceCard.value?.copy(errorMessage = null)
                                _phase.value = VoicePhase.Dismissing
                            }
                        }
                    }
                    RecognitionState.Denied -> {
                        _draftVoiceCard.value = null
                        _phase.value = VoicePhase.Idle
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onHaloPressStart() {
        if (_phase.value != VoicePhase.Idle) return
        _draftVoiceCard.value = DraftVoiceCard()
        _phase.value = VoicePhase.Listening
        voiceManager.startListening("zh-CN")
    }

    fun onHaloPressEnd() {
        if (_phase.value != VoicePhase.Listening) return
        voiceManager.stopListening()
    }

    fun onDraftSwipedRight() {
        val text = _draftVoiceCard.value?.finalText ?: return
        onExecuteCommand(text)
        _phase.value = VoicePhase.Dismissing
    }

    fun onDraftSwipedLeft() {
        _phase.value = VoicePhase.Dismissing
    }

    fun onDraftTextEdited(newText: String) {
        _draftVoiceCard.value = _draftVoiceCard.value?.copy(finalText = newText)
    }

    fun onSwipeCurrentCard() {
        val stack = _cardStack.value
        val index = _currentIndex.value
        if (index < stack.size - 1 && _phase.value == VoicePhase.Idle) {
            _currentIndex.value = index + 1
        }
    }

    fun onDismissComplete() {
        _draftVoiceCard.value = null
        _phase.value = VoicePhase.Idle
    }

    fun dispose() {
        errorDismissJob?.cancel()
        collectorJob.cancel()
        voiceManager.release()
    }

    companion object {
        /**
         * Attempts to retrieve `TestScope.backgroundScope` from [scope] via reflection.
         * Returns null if [scope] does not expose such a property (i.e., in production).
         *
         * This avoids a compile-time dependency on kotlinx-coroutines-test in the main source set
         * while still getting the correct scheduling semantics in unit tests.
         */
        private fun resolveBackgroundScope(scope: CoroutineScope): CoroutineScope? {
            return try {
                val method = scope.javaClass.getMethod("getBackgroundScope")
                method.invoke(scope) as? CoroutineScope
            } catch (_: Exception) {
                null
            }
        }
    }
}

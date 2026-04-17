// app/src/main/java/com/aether/app/voice/VoiceRecognitionManager.kt
package com.aether.app.voice

import kotlinx.coroutines.flow.StateFlow

interface VoiceRecognitionManager {
    /** Intermediate recognition text, refreshes continuously while listening. */
    val partialText: StateFlow<String>

    /**
     * Final recognition text emitted once after stopListening().
     * null  = not yet available (reset to null on each startListening call).
     * ""    = empty / no speech detected (treat as silent cancel).
     * "..."  = recognised text → transitions holder to Editable.
     */
    val finalText: StateFlow<String?>

    /** Microphone level, normalised 0f..1f. Drives AuroraWaveHalo amplitude. */
    val rmsDb: StateFlow<Float>

    val state: StateFlow<RecognitionState>
    val errorCode: StateFlow<RecognitionError?>

    fun startListening(locale: String = "zh-CN")
    fun stopListening()   // triggers finalText emission
    fun cancel()          // aborts without emitting finalText
    fun release()         // destroy underlying resources
}

// ── State ──────────────────────────────────────────────────────────────────────

sealed class RecognitionState {
    object Idle       : RecognitionState()
    object Preparing  : RecognitionState()   // recogniser booting, no rms yet
    object Listening  : RecognitionState()   // receiving partial results
    object Finalizing : RecognitionState()   // stopListening() called, awaiting results
    object Denied     : RecognitionState()   // RECORD_AUDIO permission denied
    data class Error(val code: RecognitionError) : RecognitionState()
}

// ── Error classification ────────────────────────────────────────────────────────
//
// Silent  (NO_MATCH, TIMEOUT) → mapped to finalText="" in SpeechRecognizerImpl,
//                               never reaches Error state, treated as empty result.
// Visible (everything else)  → emitted as RecognitionState.Error, shown as red text.
// Denied                     → emitted as RecognitionState.Denied, Toast shown.

enum class RecognitionError { NETWORK, SERVER, AUDIO, OTHER }

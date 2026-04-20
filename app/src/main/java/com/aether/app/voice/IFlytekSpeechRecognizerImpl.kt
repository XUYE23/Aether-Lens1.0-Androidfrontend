package com.aether.app.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.StateFlow

/**
 * Compatibility wrapper for environments where the iFlytek SDK AARs are not
 * present in `app/libs`.
 *
 * The project previously referenced `com.iflytek.cloud.*` directly, which makes
 * the whole app uncompilable unless the proprietary SDK is checked into the repo.
 * To keep the app runnable in Android Studio, we gracefully fall back to the
 * platform speech recognizer until the vendor SDK is added back.
 */
class IFlytekSpeechRecognizerImpl(
    context: Context
) : VoiceRecognitionManager {

    companion object {
        private const val TAG = "IFlytekCompat"
    }

    private val delegate: VoiceRecognitionManager = SpeechRecognizerImpl(context)

    init {
        Log.w(
            TAG,
            "iFlytek SDK not bundled with this project; using Android SpeechRecognizer fallback."
        )
    }

    override val partialText: StateFlow<String> = delegate.partialText
    override val finalText: StateFlow<String?> = delegate.finalText
    override val rmsDb: StateFlow<Float> = delegate.rmsDb
    override val state: StateFlow<RecognitionState> = delegate.state
    override val errorCode: StateFlow<RecognitionError?> = delegate.errorCode

    override fun startListening(locale: String) = delegate.startListening(locale)

    override fun stopListening() = delegate.stopListening()

    override fun cancel() = delegate.cancel()

    override fun release() = delegate.release()
}

package com.opendroid.ai.core.voice

import android.content.Context

/**
 * Local sherpa keyword spotting is the primary wake-word path. Android's
 * SpeechRecognizer remains only a compatibility fallback for devices where
 * the local runtime or model cannot be initialized.
 */
class WakeWordDetector(
    context: Context,
    onStatus: (String) -> Unit = {}
) {
    private val localDetector = SherpaWakeWordDetector(context, onStatus = onStatus)
    private val compatibilityDetector = LegacySpeechWakeWordDetector(context)
    @Volatile private var listening = false
    @Volatile private var callback: (() -> Unit)? = null

    fun startListening(onWakeWordDetected: () -> Unit) {
        if (listening) return
        listening = true
        callback = onWakeWordDetected
        localDetector.startListening(
            onWakeWordDetected = { deliverWakeWord() },
            onUnavailable = {
                if (listening) {
                    compatibilityDetector.startListening { deliverWakeWord() }
                }
            }
        )
    }

    private fun deliverWakeWord() {
        if (!listening) return
        listening = false
        callback?.invoke()
        callback = null
    }

    fun stopListening() {
        listening = false
        callback = null
        localDetector.stopListening()
        compatibilityDetector.stopListening()
    }

    fun destroy() {
        stopListening()
        localDetector.destroy()
        compatibilityDetector.destroy()
    }
}

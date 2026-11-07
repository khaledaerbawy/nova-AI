package com.opendroid.ai.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class WakeWordDetector(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var intent: Intent? = null
    private var onWakeWordDetectedCallback: (() -> Unit)? = null
    private var isListening = false
    private var wakeWordTriggered = false

    private val handler = Handler(Looper.getMainLooper())
    private val restartRunnable = Runnable {
        startSpeechListening()
    }

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
    }

    fun startListening(onWakeWordDetected: () -> Unit) {
        if (isListening) return
        this.onWakeWordDetectedCallback = onWakeWordDetected
        wakeWordTriggered = false
        isListening = true
        startSpeechListening()
    }

    private fun startSpeechListening() {
        if (!isListening || wakeWordTriggered) return

        cleanupRecognizer()

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            } catch (e: Exception) {
                scheduleRestart()
                return
            }
        } else {
            scheduleRestart()
            return
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                scheduleRestart()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches.orEmpty().any(::containsWakePhrase)) {
                    triggerWakeWord()
                } else {
                    scheduleRestart()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches.orEmpty().any(::containsWakePhrase)) {
                    triggerWakeWord()
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            scheduleRestart()
        }
    }

    private fun containsWakePhrase(text: String): Boolean {
        val normalized = text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return normalized.contains("hey nova") ||
            normalized.contains("hey no va") ||
            normalized.contains("hello nova") ||
            normalized.contains("hi nova")
    }

    private fun triggerWakeWord() {
        if (wakeWordTriggered || !isListening) return
        wakeWordTriggered = true
        handler.removeCallbacks(restartRunnable)
        onWakeWordDetectedCallback?.invoke()
    }

    private fun scheduleRestart() {
        handler.removeCallbacks(restartRunnable)
        if (isListening && !wakeWordTriggered) {
            handler.postDelayed(restartRunnable, 1000)
        }
    }

    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {}
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {}
        speechRecognizer = null
    }

    fun stopListening() {
        isListening = false
        wakeWordTriggered = false
        handler.removeCallbacks(restartRunnable)
        cleanupRecognizer()
        onWakeWordDetectedCallback = null
    }

    fun destroy() {
        stopListening()
    }
}

package com.talha.ultron.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.*

/**
 * Free wake-word detection using Android's built-in SpeechRecognizer in
 * continuous-loop mode. Less battery-efficient than Porcupine but requires
 * zero external dependencies or API keys. Works fully offline.
 */
class FreeWakeWordManager(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit,
    private val onSetupFailed: (String) -> Unit
) {

    private var recognizer: SpeechRecognizer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isRunning = false

    private val wakePhrases = listOf("hey ultron", "ultron", "hey android", "android")

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onSetupFailed("Speech recognition not available on this device")
            return
        }
        isRunning = true
        startListeningLoop()
    }

    fun stop() {
        isRunning = false
        scope.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun startListeningLoop() {
        if (!isRunning) return
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val heard = matches?.firstOrNull()?.lowercase() ?: ""
                    if (wakePhrases.any { heard.contains(it) }) {
                        onWakeWordDetected()
                    }
                    if (isRunning) startListeningLoop()
                }
                override fun onError(error: Int) {
                    if (isRunning) {
                        scope.launch { delay(500); startListeningLoop() }
                    }
                }
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val heard = matches?.firstOrNull()?.lowercase() ?: ""
                    if (wakePhrases.any { heard.contains(it) }) {
                        onWakeWordDetected()
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            startListening(intent)
        }
    }
}

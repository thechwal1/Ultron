package com.talha.ultron.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Wraps Android's built-in TTS engine — always works offline.
 */
class VoiceOutputManager(
    context: Context,
    private val deepVoiceEnabled: Boolean = true,
    private val preferredVoiceName: String? = null,
    private val onSpeakingStarted: () -> Unit = {},
    private val onSpeakingFinished: () -> Unit = {}
) {

    private var ready = false
    private val tts: TextToSpeech

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                tts.language = Locale.US
                if (!preferredVoiceName.isNullOrBlank()) {
                    tts.voices?.firstOrNull { it.name == preferredVoiceName }?.let { tts.voice = it }
                }
                if (deepVoiceEnabled) {
                    tts.setPitch(0.72f)
                    tts.setSpeechRate(0.90f)
                }
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { onSpeakingStarted() }
                    override fun onDone(utteranceId: String?) { onSpeakingFinished() }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { onSpeakingFinished() }
                })
            }
        }
    }

    fun speak(text: String) {
        if (!ready) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "ultron_utterance")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

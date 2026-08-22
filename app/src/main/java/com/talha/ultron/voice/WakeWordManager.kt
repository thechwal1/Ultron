package com.talha.ultron.voice

import android.content.Context
import ai.picovoice.porcupine.Porcupine

/**
 * Wake word detection via Picovoice Porcupine. Offline, low battery drain.
 * Requires PORVOICE_ACCESS_KEY in local.properties.
 */
class WakeWordManager(context: Context, accessKey: String, onWake: () -> Unit) {

    private val porcupine: Porcupine = Porcupine.Builder()
        .setAccessKey(accessKey)
        .setKeywordPath("hey-ultron.ppn")
        .setSensitivity(0.7f)
        .build(context)

    private val audioRecorder = android.media.AudioRecord(
        android.media.MediaRecorder.AudioSource.MIC,
        porcupine.sampleRate,
        android.media.AudioFormat.CHANNEL_IN_MONO,
        android.media.AudioFormat.ENCODING_PCM_16BIT,
        porcupine.frameLength * 2
    )

    private var isRunning = false
    private val buffer = ShortArray(porcupine.frameLength)

    fun start() {
        isRunning = true
        audioRecorder.startRecording()
        Thread {
            while (isRunning) {
                val read = audioRecorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val keywordIndex = porcupine.process(buffer)
                    if (keywordIndex >= 0) {
                        onWake()
                    }
                }
            }
        }.start()
    }

    fun stop() {
        isRunning = false
        audioRecorder.stop()
        audioRecorder.release()
        porcupine.delete()
    }
}

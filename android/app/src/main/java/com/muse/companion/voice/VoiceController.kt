package com.muse.companion.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Push-to-talk STT (via the system recognizer intent) + spoken TTS. On-device recognition is
 * preferred when the device supports it. Requires RECORD_AUDIO at runtime for in-process
 * recognition flows. NOTE: not yet verified on a physical device (see docs/09-feature-status.md).
 */
class VoiceController(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    fun init() {
        if (tts == null) tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) tts?.language = Locale.getDefault()
    }

    fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "muse-${text.hashCode()}")
    }

    /** Interrupts any spoken output immediately (part of the stop/cancel contract). */
    fun stopSpeaking() {
        tts?.stop()
    }

    /** Intent that launches the system speech recognizer UI; result handled by the Activity. */
    fun recognitionIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Muse")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

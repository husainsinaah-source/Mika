package com.wangzan101.mika.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Foreground-only "Hey Mika" detector.
 *
 * This is an opt-in experimental implementation based on Android SpeechRecognizer.
 * It is NOT an always-on, screen-off hotword engine: Android's recognizer is not
 * designed for uninterrupted listening. Never start it from a background service.
 */
class MikaWakeListener(context: Context) {
    var onCommand: (String?) -> Unit = {}
    val available: Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    private val handler = Handler(Looper.getMainLooper())
    private val recognizer = if (available) SpeechRecognizer.createSpeechRecognizer(context.applicationContext) else null
    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
    }
    private var active = false
    private var armedUntil = 0L

    init {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onResults(results: Bundle?) {
                if (!active) return
                val alternatives = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val armed = SystemClock.elapsedRealtime() < armedUntil
                val action = alternatives.firstNotNullOfOrNull { MikaWakePhrase.parse(it, armed) }
                if (action != null) {
                    armedUntil = if (action.command == null) SystemClock.elapsedRealtime() + 12_000L else 0L
                    onCommand(action.command)
                }
                retry(500)
            }

            override fun onError(error: Int) {
                if (!active) return
                // Don't keep retrying if the user revoked microphone permission.
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    stop()
                    return
                }
                retry(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_SERVER) 1800 else 600)
            }
        })
    }

    fun start() {
        if (!available || active) return
        active = true
        retry(100)
    }

    fun stop() {
        active = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.cancel()
    }

    private fun retry(delayMillis: Long) {
        handler.removeCallbacksAndMessages(null)
        if (!active) return
        handler.postDelayed({
            if (!active) return@postDelayed
            try {
                recognizer?.startListening(intent)
            } catch (_: RuntimeException) {
                retry(2000)
            }
        }, delayMillis)
    }

    fun release() {
        stop()
        recognizer?.destroy()
    }
}

internal data class MikaWakeAction(val command: String?)

/** A command may be attached to the wake phrase or follow it within twelve seconds. */
internal object MikaWakePhrase {
    private val prefix = Regex("""^\s*(?:hey[\s,]+)?mika\b[\s,.:!?-]*(.*)$""", RegexOption.IGNORE_CASE)

    fun parse(text: String, armed: Boolean): MikaWakeAction? {
        val value = text.trim()
        if (value.isEmpty()) return null
        val match = prefix.matchEntire(value)
        if (match != null) return MikaWakeAction(match.groupValues[1].trim().ifEmpty { null })
        return if (armed) MikaWakeAction(value) else null
    }
}

/** Mika-owned speech output controls that use the phone's installed Android TTS engine. */
class MikaSpokenReplies(context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var ready = false
    var speaking by mutableStateOf(false)
        private set

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            main.post {
                if (status == TextToSpeech.SUCCESS) {
                    engine?.language = Locale.getDefault()
                    engine?.setSpeechRate(1.0f)
                    ready = true
                }
            }
        }
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { main.post { speaking = false } }
            @Deprecated("Legacy TTS failure callback")
            override fun onError(utteranceId: String?) { main.post { speaking = false } }
            override fun onError(utteranceId: String?, errorCode: Int) { main.post { speaking = false } }
        })
    }

    fun say(message: String) {
        if (!ready || message.isBlank()) return
        speaking = true
        val result = engine?.speak(message.take(4000), TextToSpeech.QUEUE_FLUSH, null, "mika-" + System.nanoTime())
        if (result != TextToSpeech.SUCCESS) speaking = false
    }

    fun stop() {
        engine?.stop()
        speaking = false
    }

    fun release() {
        stop()
        ready = false
        engine?.shutdown()
        engine = null
    }
}

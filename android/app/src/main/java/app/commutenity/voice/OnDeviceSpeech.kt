package app.commutenity.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi

/**
 * English-only, on-device speech recognition (works offline). Requires API 31+.
 *
 * All methods MUST be called on the main thread (SpeechRecognizer requirement); callbacks are
 * delivered on the main thread too.
 */
class OnDeviceSpeech(private val context: Context) {

    companion object {
        private const val LANGUAGE = "en-GB"

        // Literal values for constants newer than minSdk 26.
        private const val ERROR_RECOGNIZER_BUSY = 8
        private const val ERROR_INSUFFICIENT_PERMISSIONS = 9
        private const val ERROR_SERVER_DISCONNECTED = 11
        private const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
        private const val ERROR_LANGUAGE_UNAVAILABLE = 13
        private const val ERROR_NO_MATCH = 7
        private const val ERROR_SPEECH_TIMEOUT = 6

        fun isAvailable(context: Context): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && onDeviceAvailable(context)

        @RequiresApi(Build.VERSION_CODES.S)
        private fun onDeviceAvailable(context: Context): Boolean =
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

        private fun messageFor(error: Int): String = when (error) {
            ERROR_NO_MATCH, ERROR_SPEECH_TIMEOUT ->
                "I didn't catch that. Try again or type your question."
            ERROR_INSUFFICIENT_PERMISSIONS ->
                "Microphone permission is off. You can still type your question."
            ERROR_LANGUAGE_NOT_SUPPORTED, ERROR_LANGUAGE_UNAVAILABLE ->
                "English voice isn't installed on this phone. Please type your question."
            ERROR_RECOGNIZER_BUSY -> "The microphone is busy. Try again."
            else -> "Voice didn't work this time. Try again or type your question."
        }
    }

    private var recognizer: SpeechRecognizer? = null

    /** Starts listening. Main thread only. Replaces any session already running. */
    fun start(
        onPartial: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            onError(messageFor(ERROR_LANGUAGE_UNAVAILABLE))
            return
        }
        startListening(onPartial, onResult, onError, retriedDisconnect = false)
    }

    /** Stops listening early; results heard so far are still delivered if the recognizer sends them. */
    fun stop() {
        recognizer?.stopListening()
    }

    /** Abandons the current session: no partial, result, or error callbacks are delivered after this. */
    fun cancel() = destroy()

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startListening(
        partial: (String) -> Unit,
        result: (String) -> Unit,
        fail: (String) -> Unit,
        retriedDisconnect: Boolean,
    ) {
        destroy()
        val created = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer = created

        created.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onPartialResults(partialResults: Bundle?) {
                if (recognizer !== created) return
                val text = firstResult(partialResults)
                if (!text.isNullOrBlank()) partial(text)
            }

            override fun onResults(results: Bundle?) {
                if (recognizer !== created) return
                result(firstResult(results).orEmpty())
            }

            override fun onError(error: Int) {
                if (recognizer !== created) return
                if (error == ERROR_SERVER_DISCONNECTED && !retriedDisconnect) {
                    // The first request after creating a recognizer can fail with 11; the next works.
                    startListening(partial, result, fail, retriedDisconnect = true)
                } else {
                    fail(messageFor(error))
                }
            }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        created.startListening(intent)
    }

    private fun firstResult(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
}

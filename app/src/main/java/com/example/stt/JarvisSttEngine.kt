package com.example.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

interface SttListener {
    fun onListeningStarted()
    fun onRmsAmplitude(amplitude: Float)
    fun onPartialTranscript(partial: String)
    fun onFinalTranscript(result: String)
    fun onListeningEnded()
    fun onSttError(errorCode: Int, message: String)
    fun onWakeWordDetected()
}

class JarvisSttEngine(
    private val context: Context,
    private val listener: SttListener
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var wakeWordDetectionEnabled = false
    private val mainScope = CoroutineScope(Dispatchers.Main)

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@JarvisSttEngine)
            }
        } else {
            Log.e("JarvisStt", "Speech recognition unavailable on this device")
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        wakeWordDetectionEnabled = enabled
    }

    fun startListening() {
        mainScope.launch {
            if (speechRecognizer == null) {
                initRecognizer()
            }

            if (speechRecognizer == null) {
                listener.onSttError(-1, "Speech recognition service not available.")
                return@launch
            }

            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                speechRecognizer?.startListening(intent)
                isListening = true
                listener.onListeningStarted()
            } catch (e: Exception) {
                Log.e("JarvisStt", "Failed to start speech recognition", e)
                isListening = false
                listener.onSttError(-2, e.localizedMessage ?: "Failed to start microphone")
            }
        }
    }

    fun stopListening() {
        mainScope.launch {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e("JarvisStt", "Error stopping listening", e)
            } finally {
                isListening = false
                listener.onListeningEnded()
            }
        }
    }

    fun cancel() {
        mainScope.launch {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                // Ignore
            } finally {
                isListening = false
                listener.onListeningEnded()
            }
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d("JarvisStt", "Microphone ready for speech")
    }

    override fun onBeginningOfSpeech() {
        Log.d("JarvisStt", "User began speaking")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Normalize typical rmsdB (-2 to ~10 dB) to 0f..1f for clean visualizer rendering
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        listener.onRmsAmplitude(normalized)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        Log.d("JarvisStt", "Speech input ended")
        isListening = false
        listener.onListeningEnded()
    }

    override fun onError(error: Int) {
        isListening = false
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient audio permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network operation timed out"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
            else -> "Speech recognition error ($error)"
        }
        Log.w("JarvisStt", "STT Error: $message ($error)")
        listener.onSttError(error, message)
        listener.onListeningEnded()
    }

    override fun onResults(results: Bundle?) {
        isListening = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val bestMatch = matches?.firstOrNull()?.trim() ?: ""

        if (bestMatch.isNotBlank()) {
            if (wakeWordDetectionEnabled && isWakeWord(bestMatch)) {
                listener.onWakeWordDetected()
                val cleanCommand = cleanWakeWord(bestMatch)
                if (cleanCommand.isNotBlank()) {
                    listener.onFinalTranscript(cleanCommand)
                }
            } else {
                listener.onFinalTranscript(bestMatch)
            }
        }
        listener.onListeningEnded()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull()?.trim() ?: ""
        if (partial.isNotBlank()) {
            listener.onPartialTranscript(partial)
            if (wakeWordDetectionEnabled && isWakeWord(partial)) {
                listener.onWakeWordDetected()
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun isWakeWord(phrase: String): Boolean {
        val lower = phrase.lowercase(Locale.ROOT)
        return lower.contains("hey jarvis") || lower.contains("jarvis")
    }

    private fun cleanWakeWord(phrase: String): String {
        return phrase.replace(Regex("(?i)hey\\s+jarvis\\s*"), "")
            .replace(Regex("(?i)jarvis\\s*"), "")
            .trim()
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore
        }
        speechRecognizer = null
    }
}

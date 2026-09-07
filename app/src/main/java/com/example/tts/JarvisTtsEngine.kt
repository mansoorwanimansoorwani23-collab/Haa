package com.example.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

interface TtsListener {
    fun onSpeechStart()
    fun onSpeechDone()
    fun onSpeechError(errorMsg: String)
    fun onSpeechAmplitude(amplitude: Float)
}

class JarvisTtsEngine(
    private val context: Context,
    private val listener: TtsListener
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val availableVoices: StateFlow<List<Voice>> = _availableVoices.asStateFlow()

    private var currentVoiceName: String? = null
    private var speechRate: Float = 1.0f
    private var speechPitch: Float = 0.88f // Calm, sophisticated masculine resonance

    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = tts?.setLanguage(Locale.US)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("JarvisTts", "US English not directly supported, using default locale")
                tts?.language = Locale.getDefault()
            }

            isInitialized = true
            setupVoiceSelection()
            applyVoiceSettings()
            setupProgressListener()
            Log.d("JarvisTts", "Native Android TTS initialized successfully.")
        } else {
            isInitialized = false
            listener.onSpeechError("TTS initialization failed on this device.")
        }
    }

    private fun setupVoiceSelection() {
        tts?.let { engine ->
            try {
                val voices = engine.voices?.filter { v ->
                    !v.isNetworkConnectionRequired && (v.locale.language == "en" || v.locale == Locale.getDefault())
                }?.toList() ?: emptyList()

                _availableVoices.value = voices

                // Prioritize male voice if tagged, or en-gb/en-us masculine sounding
                val bestMaleVoice = voices.firstOrNull { v ->
                    val name = v.name.lowercase(Locale.ROOT)
                    name.contains("male") || name.contains("en-gb") || name.contains("en_gb") || name.contains("#male")
                } ?: voices.firstOrNull { v ->
                    v.locale.language == "en"
                }

                bestMaleVoice?.let {
                    engine.voice = it
                    currentVoiceName = it.name
                }
            } catch (e: Exception) {
                Log.e("JarvisTts", "Error selecting voice", e)
            }
        }
    }

    fun setVoice(voice: Voice) {
        tts?.voice = voice
        currentVoiceName = voice.name
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(speechRate)
    }

    fun setPitch(pitch: Float) {
        speechPitch = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(speechPitch)
    }

    private fun applyVoiceSettings() {
        tts?.setPitch(speechPitch)
        tts?.setSpeechRate(speechRate)
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                scope.launch {
                    listener.onSpeechStart()
                    startAmplitudeSimulation()
                }
            }

            override fun onDone(utteranceId: String?) {
                scope.launch {
                    stopAmplitudeSimulation()
                    listener.onSpeechDone()
                }
            }

            override fun onError(utteranceId: String?) {
                scope.launch {
                    stopAmplitudeSimulation()
                    listener.onSpeechError("TTS utterance failed")
                }
            }
        })
    }

    fun speak(text: String, utteranceId: String = "jarvis_speech_${System.currentTimeMillis()}") {
        if (!isInitialized || tts == null) {
            listener.onSpeechError("TTS engine not ready")
            return
        }

        stop() // Clear any existing playback immediately

        applyVoiceSettings()
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result == TextToSpeech.ERROR) {
            listener.onSpeechError("Could not synthesize speech")
        }
    }

    fun stop() {
        stopAmplitudeSimulation()
        tts?.stop()
    }

    private fun startAmplitudeSimulation() {
        stopAmplitudeSimulation()
        amplitudeJob = scope.launch {
            while (isActive) {
                // Dynamically simulate speech waveform amplitude based on realistic phoneme rhythm
                val base = Random.nextFloat() * 0.7f + 0.3f
                listener.onSpeechAmplitude(base)
                delay(60)
            }
        }
    }

    private fun stopAmplitudeSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        listener.onSpeechAmplitude(0f)
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

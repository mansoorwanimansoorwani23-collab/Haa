package com.example.ui

import android.app.Application
import android.os.Build
import android.speech.tts.Voice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiResponse
import com.example.ai.JarvisAiEngine
import com.example.data.ConversationEntity
import com.example.data.JarvisDatabase
import com.example.data.MemoryEntity
import com.example.service.JarvisVoiceService
import com.example.stt.JarvisSttEngine
import com.example.stt.SttListener
import com.example.tools.JarvisToolExecutor
import com.example.tts.JarvisTtsEngine
import com.example.tts.TtsListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

data class ConfirmationRequest(
    val title: String,
    val description: String,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)

class JarvisViewModel(application: Application) : AndroidViewModel(application), SttListener, TtsListener {

    private val db = JarvisDatabase.getInstance(application)
    val toolExecutor = JarvisToolExecutor(application)
    val aiEngine = JarvisAiEngine(toolExecutor)

    private val ttsEngine = JarvisTtsEngine(application, this)
    private val sttEngine = JarvisSttEngine(application, this)

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _assistantSpeech = MutableStateFlow("Quantum telemetry verified. Ready for instructions, sir.")
    val assistantSpeech: StateFlow<String> = _assistantSpeech.asStateFlow()

    private val _lastActionBadge = MutableStateFlow<String?>(null)
    val lastActionBadge: StateFlow<String?> = _lastActionBadge.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _isContinuousMode = MutableStateFlow(false)
    val isContinuousMode: StateFlow<Boolean> = _isContinuousMode.asStateFlow()

    private val _isWakeWordEnabled = MutableStateFlow(false)
    val isWakeWordEnabled: StateFlow<Boolean> = _isWakeWordEnabled.asStateFlow()

    private val _isBackgroundServiceActive = MutableStateFlow(false)
    val isBackgroundServiceActive: StateFlow<Boolean> = _isBackgroundServiceActive.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(0.88f)
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<ConfirmationRequest?>(null)
    val pendingConfirmation: StateFlow<ConfirmationRequest?> = _pendingConfirmation.asStateFlow()

    val availableVoices: StateFlow<List<Voice>> = ttsEngine.availableVoices

    val conversationHistory: StateFlow<List<ConversationEntity>> = db.conversationDao()
        .getRecentConversations(30)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val memoryList: StateFlow<List<MemoryEntity>> = db.memoryDao()
        .getAllMemories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var activeJob: Job? = null

    init {
        // Initial welcome speech in masculine JARVIS tone
        viewModelScope.launch {
            delay(800)
            speakResponse("All quantum systems operational. At your service, sir.")
        }
    }

    fun toggleListening() {
        // Interruption handling: if JARVIS is currently speaking, stop immediately!
        if (_assistantState.value == AssistantState.SPEAKING) {
            ttsEngine.stop()
            _assistantState.value = AssistantState.IDLE
            sttEngine.startListening()
            return
        }

        if (_assistantState.value == AssistantState.LISTENING) {
            sttEngine.stopListening()
        } else {
            _errorMessage.value = null
            sttEngine.startListening()
        }
    }

    fun stopAllAudio() {
        ttsEngine.stop()
        sttEngine.cancel()
        _assistantState.value = AssistantState.IDLE
        _audioAmplitude.value = 0f
    }

    fun sendTextCommand(text: String) {
        if (text.isBlank()) return
        ttsEngine.stop()
        _currentTranscript.value = text
        processUserPrompt(text)
    }

    private fun processUserPrompt(prompt: String) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _assistantState.value = AssistantState.THINKING
            _errorMessage.value = null

            // Persist user prompt in conversation log
            db.conversationDao().insertMessage(
                ConversationEntity(role = "user", content = prompt)
            )

            // Prepare history for context
            val history = conversationHistory.value.takeLast(6).map { entity ->
                Pair(entity.role, entity.content)
            }

            val response = aiEngine.processUserTurn(
                userInput = prompt,
                conversationHistory = history,
                customApiKey = _customApiKey.value
            )

            when (response) {
                is AiResponse.SpokenText -> {
                    _lastActionBadge.value = response.toolActionDescription
                    _assistantSpeech.value = response.text

                    db.conversationDao().insertMessage(
                        ConversationEntity(
                            role = "jarvis",
                            content = response.text,
                            toolCall = response.toolActionDescription
                        )
                    )

                    speakResponse(response.text)
                }
                is AiResponse.ToolExecution -> {
                    _lastActionBadge.value = response.toolName
                    val spoken = response.immediateSpokenFeedback ?: "Executing ${response.toolName}, sir."
                    _assistantSpeech.value = spoken
                    speakResponse(spoken)
                }
                is AiResponse.Error -> {
                    _assistantState.value = AssistantState.ERROR
                    _errorMessage.value = response.message
                    _assistantSpeech.value = "Warning: ${response.message}"
                    speakResponse("Quantum communication failure. Please check telemetry.")
                }
            }
        }
    }

    private fun speakResponse(text: String) {
        _assistantState.value = AssistantState.SPEAKING
        ttsEngine.speak(text)
    }

    // --- STT Callbacks ---
    override fun onListeningStarted() {
        _assistantState.value = AssistantState.LISTENING
        _currentTranscript.value = "Listening..."
    }

    override fun onRmsAmplitude(amplitude: Float) {
        if (_assistantState.value == AssistantState.LISTENING) {
            _audioAmplitude.value = amplitude
        }
    }

    override fun onPartialTranscript(partial: String) {
        _currentTranscript.value = partial
    }

    override fun onFinalTranscript(result: String) {
        _currentTranscript.value = result
        processUserPrompt(result)
    }

    override fun onListeningEnded() {
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = AssistantState.IDLE
            _audioAmplitude.value = 0f
        }
    }

    override fun onSttError(errorCode: Int, message: String) {
        // Speech timeout or silence is normal; return to IDLE gracefully
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = AssistantState.IDLE
            _audioAmplitude.value = 0f
            if (errorCode != android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT &&
                errorCode != android.speech.SpeechRecognizer.ERROR_NO_MATCH
            ) {
                _errorMessage.value = message
            }
        }
    }

    override fun onWakeWordDetected() {
        // Immediate audio stop and transition to active command capture
        ttsEngine.stop()
        _assistantState.value = AssistantState.LISTENING
        _currentTranscript.value = "JARVIS summoned..."
    }

    // --- TTS Callbacks ---
    override fun onSpeechStart() {
        _assistantState.value = AssistantState.SPEAKING
    }

    override fun onSpeechDone() {
        _assistantState.value = AssistantState.IDLE
        _audioAmplitude.value = 0f

        // Continuous conversation mode: start listening automatically for the next turn
        if (_isContinuousMode.value) {
            viewModelScope.launch {
                delay(600)
                if (_assistantState.value == AssistantState.IDLE) {
                    toggleListening()
                }
            }
        }
    }

    override fun onSpeechError(errorMsg: String) {
        _assistantState.value = AssistantState.IDLE
        _audioAmplitude.value = 0f
        _errorMessage.value = errorMsg
    }

    override fun onSpeechAmplitude(amplitude: Float) {
        if (_assistantState.value == AssistantState.SPEAKING) {
            _audioAmplitude.value = amplitude
        }
    }

    // --- Settings controls ---
    fun setVoice(voice: Voice) {
        ttsEngine.setVoice(voice)
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        ttsEngine.setSpeechRate(rate)
    }

    fun setSpeechPitch(pitch: Float) {
        _speechPitch.value = pitch
        ttsEngine.setPitch(pitch)
    }

    fun toggleContinuousMode() {
        _isContinuousMode.value = !_isContinuousMode.value
    }

    fun toggleWakeWord() {
        val newVal = !_isWakeWordEnabled.value
        _isWakeWordEnabled.value = newVal
        sttEngine.setWakeWordEnabled(newVal)
    }

    fun toggleBackgroundService() {
        val newVal = !_isBackgroundServiceActive.value
        _isBackgroundServiceActive.value = newVal
        val app = getApplication<Application>()
        if (newVal) {
            JarvisVoiceService.start(app)
        } else {
            JarvisVoiceService.stop(app)
        }
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            db.memoryDao().clearAllMemories()
            db.conversationDao().clearHistory()
            _assistantSpeech.value = "Memory banks purged, sir."
            speakResponse("Memory banks purged, sir.")
        }
    }

    fun deleteMemory(key: String) {
        viewModelScope.launch {
            db.memoryDao().deleteMemory(key)
        }
    }

    fun saveMemoryFact(key: String, value: String) {
        viewModelScope.launch {
            db.memoryDao().saveMemory(MemoryEntity(key = key, value = value))
        }
    }

    fun dismissConfirmation() {
        _pendingConfirmation.value = null
    }

    override fun onCleared() {
        super.onCleared()
        ttsEngine.shutdown()
        sttEngine.destroy()
    }
}

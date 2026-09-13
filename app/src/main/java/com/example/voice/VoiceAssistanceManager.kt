package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

enum class VoiceAssistantState {
    INITIALIZING,
    READY,
    SPEAKING,
    DISABLED,
    ERROR
}

/**
 * Intelligent Tactical Voice Assistant for INS/GNSS Dead Reckoning Navigation.
 * Speaks real-time acoustic cues during GNSS signal blackouts, restoration,
 * velocity alerts, drift warnings, and user-initiated telemetry readouts.
 */
class VoiceAssistanceManager(context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "VoiceAssistance"

    private var tts: TextToSpeech? = null
    private val appContext = context.applicationContext

    private val _assistantState = MutableStateFlow(VoiceAssistantState.INITIALIZING)
    val assistantState: StateFlow<VoiceAssistantState> = _assistantState.asStateFlow()

    private val _isVoiceMuted = MutableStateFlow(false)
    val isVoiceMuted: StateFlow<Boolean> = _isVoiceMuted.asStateFlow()

    private val _lastSpokenMessage = MutableStateFlow("Voice Assistance ready.")
    val lastSpokenMessage: StateFlow<String> = _lastSpokenMessage.asStateFlow()

    // Rate limiting to avoid voice spam
    private var lastBlackoutAnnouncementTime = 0L
    private var lastRestorationAnnouncementTime = 0L
    private var lastDriftWarningTime = 0L

    init {
        try {
            tts = TextToSpeech(appContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TTS: ${e.message}")
            _assistantState.value = VoiceAssistantState.ERROR
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Language US not supported, falling back to default")
                tts?.setLanguage(Locale.getDefault())
            }

            tts?.setPitch(1.05f) // Crisp tactical voice pitch
            tts?.setSpeechRate(1.0f) // Clear cadence

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _assistantState.value = VoiceAssistantState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    _assistantState.value = VoiceAssistantState.READY
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _assistantState.value = VoiceAssistantState.READY
                }
            })

            _assistantState.value = VoiceAssistantState.READY
            Log.i(TAG, "Voice Assistant initialized successfully.")
        } else {
            _assistantState.value = VoiceAssistantState.ERROR
            Log.e(TAG, "TTS Initialization failed with status $status")
        }
    }

    fun toggleMute() {
        _isVoiceMuted.value = !_isVoiceMuted.value
        if (_isVoiceMuted.value) {
            stopSpeaking()
        } else {
            speak("Voice assistance enabled.")
        }
    }

    fun speak(text: String, flushQueue: Boolean = true) {
        if (_isVoiceMuted.value) return

        _lastSpokenMessage.value = text
        val queueMode = if (flushQueue) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, queueMode, null, "UTTERANCE_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
            _assistantState.value = VoiceAssistantState.READY
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS: ${e.message}")
        }
    }

    /**
     * Announces entering GNSS blackout zone
     */
    fun announceGnssBlackout(scenarioTitle: String? = null) {
        val now = System.currentTimeMillis()
        if (now - lastBlackoutAnnouncementTime < 4000) return
        lastBlackoutAnnouncementTime = now

        val msg = if (scenarioTitle != null) {
            "Warning. GNSS signal lost in $scenarioTitle. Engaging Dead Reckoning inertial AI engine."
        } else {
            "Warning. Satellite signal lost. Dead reckoning navigation engaged."
        }
        speak(msg, flushQueue = true)
    }

    /**
     * Announces GNSS signal lock restored
     */
    fun announceGnssRestored() {
        val now = System.currentTimeMillis()
        if (now - lastRestorationAnnouncementTime < 4000) return
        lastRestorationAnnouncementTime = now

        speak("GNSS carrier lock restored. Inertial bias recalibrated.", flushQueue = true)
    }

    /**
     * Announces excessive drift warning
     */
    fun announceDriftWarning(driftMeters: Float) {
        val now = System.currentTimeMillis()
        if (now - lastDriftWarningTime < 8000) return
        lastDriftWarningTime = now

        speak("Notice. Cumulative drift ${driftMeters.toInt()} meters. Applying non-holonomic constraint correction.", flushQueue = false)
    }

    /**
     * Reads out full mission status on demand
     */
    fun speakFullTelemetry(
        speedKmph: Float,
        headingDeg: Float,
        isGnssActive: Boolean,
        driftPercent: Float
    ) {
        val mode = if (isGnssActive) "GNSS carrier lock active" else "Dead reckoning active"
        val text = "Current speed is ${speedKmph.toInt()} kilometers per hour. Heading ${headingDeg.toInt()} degrees. Status: $mode. Relative drift is ${"%.1f".format(driftPercent)} percent."
        speak(text, flushQueue = true)
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS: ${e.message}")
        }
    }
}

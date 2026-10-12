package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.LanguageMode
import com.example.data.model.VoiceDiagnosticsInfo
import com.example.data.model.VoiceState
import com.example.tools.IntentClassifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {
    companion object {
        private const val TAG = "VoiceManager"
        private const val UTTERANCE_ID = "jarvis_tts_utterance"

        private val STOP_LISTENING_PHRASES = listOf(
            "stop listening",
            "stop listen",
            "exit live mode",
            "stop live mode",
            "deactivate live mode",
            "turn off live mode",
            "jarvis stop",
            "stop jarvis",
            "quit listening",
            "quiet jarvis",
            "quiet",
            "sleep jarvis",
            "shut down live mode",
            "ఆపు", // Telugu: "Stop"
            "వినడం ఆపు" // Telugu: "Stop listening"
        )

        /**
         * Cleans spoken text thoroughly:
         * - Removes markdown formatting
         * - Removes raw JSON objects & arrays
         * - Removes URLs & web links
         * - Removes source metadata & internal tool logs
         * - Removes system status labels (LISTENING, ANALYZING, SEARCHING WEB, HOME ARRIVAL DETECTED)
         * - Preserves the true meaning and natural punctuation of the response
         */
        fun cleanTextForSpeech(text: String): String {
            if (text.isBlank()) return ""

            var cleaned = text

            // 1. Remove raw markdown code blocks ``` ... ```
            cleaned = cleaned.replace(Regex("```[\\s\\S]*?```"), "Code omitted.")

            // 2. Remove raw JSON objects or arrays: e.g. {"app": "youtube"} or {"query": "..."}
            cleaned = cleaned.replace(Regex("\\{[\\s\\S]*?\\}"), "")

            // 3. Remove URLs & web domains
            cleaned = cleaned.replace(Regex("https?://\\S+"), "")
            cleaned = cleaned.replace(Regex("www\\.\\S+"), "")

            // 4. Remove source citations and lists like "Sources:\n• ..." or "(https://...)" or "[Source](...)"
            cleaned = cleaned.replace(Regex("(?i)\\bSources?:\\s*[\\s\\S]*$"), "")
            cleaned = cleaned.replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)"), "$1")

            // 5. Remove system status labels, protocol headers, and activity markers
            val statusPhrases = listOf(
                "JARVIS // PROTOCOL",
                "JARVIS // HOME ARRIVAL",
                "JARVIS // APPLICATION DETECTION",
                "JARVIS // APP DETECTION",
                "JARVIS // APP DETECTION FALLBACK",
                "HOME ARRIVAL DETECTED [SIMULATED]",
                "HOME ARRIVAL DETECTED",
                "ARRIVAL PROTOCOL INITIATED",
                "SEARCHING WEB",
                "SEARCHING THE WEB",
                "LISTENING",
                "ANALYZING",
                "PROCESSING",
                "RESPONSE READY",
                "LIVE TELEMETRY & MARKET BRIEFING",
                "WEB KNOWLEDGE RETRIEVAL",
                "CHECKING AUTHORIZED SERVICES",
                "AWAITING USER CONFIRMATION",
                "CONTINUOUS LISTENING // Say 'Stop listening' to halt",
                "CONTINUOUS LISTENING"
            )
            for (status in statusPhrases) {
                cleaned = cleaned.replace(status, "", ignoreCase = true)
            }

            // Remove divider banners like "--- JARVIS TELEMETRY ---"
            cleaned = cleaned.replace(Regex("-{3,}[^-]+-{3,}"), "")

            // 6. Remove internal tool call representations
            cleaned = cleaned.replace(Regex("ToolCallInfo\\([^\\)]*\\)"), "")

            // 7. Remove markdown formatting markers
            cleaned = cleaned.replace(Regex("[*_]{1,3}"), "")
            cleaned = cleaned.replace(Regex("(?m)^#{1,6}\\s+"), "")
            cleaned = cleaned.replace("~~", "")
            cleaned = cleaned.replace("`", "")
            cleaned = cleaned.replace(Regex("(?m)^>\\s*"), "")
            cleaned = cleaned.replace(Regex("(?m)^[•\\*\\-\\+]\\s+"), "")
            cleaned = cleaned.replace(Regex("[\\[\\]|]"), " ")

            // 8. Clean up extra punctuation and whitespace
            cleaned = cleaned.replace(Regex("\\s+"), " ")
                .replace(Regex("\\s+([.,!?:;])"), "$1")
                .replace(Regex("([.,!?:;])\\1+"), "$1")
                .trim()

            return cleaned
        }
    }

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isTtsInitialized = MutableStateFlow(false)
    val isTtsInitialized: StateFlow<Boolean> = _isTtsInitialized.asStateFlow()

    private var currentLanguage = LanguageMode.ENGLISH
    val activeLanguageMode = MutableStateFlow(LanguageMode.ENGLISH)

    // Language support warning state
    val languageWarning = MutableStateFlow<String?>(null)
    val isLanguageSupported = MutableStateFlow(true)

    // Available installed voices for the current language
    val availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val selectedVoice = MutableStateFlow<Voice?>(null)
    val activeVoiceName = MutableStateFlow("Default Voice")

    // Audio level for live waveform animation (0.0f to 1.0f)
    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    // Recognition speech text flow
    private val _recognizedSpeech = MutableStateFlow("")
    val recognizedSpeech: StateFlow<String> = _recognizedSpeech.asStateFlow()

    // Single source of truth voice state machine: IDLE -> LISTENING -> PROCESSING -> SPEAKING -> IDLE/LISTENING
    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // Live Mode state flow (runs continuously on background until "stop listening")
    private val _isLiveMode = MutableStateFlow(false)
    val isLiveMode: StateFlow<Boolean> = _isLiveMode.asStateFlow()

    val isSpeechRecognitionSupported = MutableStateFlow(true)
    val voiceDiagnostics = MutableStateFlow(VoiceDiagnosticsInfo())

    var onSpeechResult: ((String) -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null
    var onSpeakingStarted: (() -> Unit)? = null
    var onSpeakingFinished: (() -> Unit)? = null
    var onStopLiveModeRequested: (() -> Unit)? = null

    // Voice Pitch & Speed: default to natural human cadences (1.00)
    var speechRate: Float = 1.0f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    var speechPitch: Float = 1.0f
        set(value) {
            field = value
            tts?.setPitch(value)
        }

    var isTtsEnabled: Boolean = true

    // Pending utterance queue if speak() is invoked before onInit() completes
    private var pendingSpeechText: String? = null
    private var restartListeningRunnable: Runnable? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
        initSpeechRecognizer()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            _isTtsInitialized.value = true
            applyLanguageAndVoice()

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _voiceState.value = VoiceState.SPEAKING
                    _isSpeaking.value = true
                    mainHandler.post { onSpeakingStarted?.invoke() }
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _audioLevel.value = 0f
                    if (_isLiveMode.value) {
                        _voiceState.value = VoiceState.IDLE
                        mainHandler.post {
                            onSpeakingFinished?.invoke()
                            scheduleRestartListening(400)
                        }
                    } else {
                        _voiceState.value = VoiceState.IDLE
                        mainHandler.post { onSpeakingFinished?.invoke() }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _audioLevel.value = 0f
                    _voiceState.value = VoiceState.IDLE
                    mainHandler.post {
                        onSpeakingFinished?.invoke()
                        if (_isLiveMode.value) {
                            scheduleRestartListening(500)
                        }
                    }
                }
            })

            // Execute queued speech if speak() was called before initialization finished
            pendingSpeechText?.let { queuedText ->
                pendingSpeechText = null
                speak(queuedText)
            }
        } else {
            _isTtsInitialized.value = false
            languageWarning.value = "Text-to-Speech engine failed to initialize (status code: $status)."
            Log.e(TAG, "TTS Initialization failed with code: $status")
        }
    }

    fun setLanguage(languageMode: LanguageMode) {
        currentLanguage = languageMode
        activeLanguageMode.value = languageMode
        val reqLocale = when (languageMode) {
            LanguageMode.ENGLISH -> "en-IN"
            LanguageMode.TELUGU, LanguageMode.TANGLISH -> "te-IN"
        }
        voiceDiagnostics.value = voiceDiagnostics.value.copy(requestedLocale = reqLocale)
        applyLanguageAndVoice()
    }

    /**
     * Configures the TTS engine language and compatible voice:
     * - Defaults English to English (India), Locale("en", "IN")
     * - Sets Telugu to Telugu (India), Locale("te", "IN")
     * - Verifies language availability and shows a clear warning if unsupported
     * - Never silently falls back to an unrelated language
     */
    private fun applyLanguageAndVoice() {
        if (!_isTtsInitialized.value) return
        val targetLocale = currentLanguage.getLocale()

        try {
            val availability = tts?.isLanguageAvailable(targetLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            Log.d(TAG, "Checking availability for $targetLocale: $availability")

            if (availability == TextToSpeech.LANG_MISSING_DATA || availability == TextToSpeech.LANG_NOT_SUPPORTED) {
                isLanguageSupported.value = false
                val langLabel = if (currentLanguage == LanguageMode.TELUGU) "Telugu (India)" else "English (India)"
                val warning = if (availability == TextToSpeech.LANG_MISSING_DATA) {
                    "Voice data for $langLabel is missing on this device. Please install voice data in Android Settings > Text-to-Speech."
                } else {
                    "Speech synthesis for $langLabel is not supported by your current TTS engine."
                }
                languageWarning.value = warning
                activeVoiceName.value = "$langLabel (Voice Data Missing)"
                Log.w(TAG, warning)

                // If English (India) is missing, check if general English is available to prevent total silence
                if (currentLanguage == LanguageMode.ENGLISH) {
                    val fallbackLocale = Locale.ENGLISH
                    val fbAvail = tts?.isLanguageAvailable(fallbackLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                    if (fbAvail >= TextToSpeech.LANG_AVAILABLE) {
                        tts?.setLanguage(fallbackLocale)
                        languageWarning.value = "English (India) voice missing; using generic English voice. Please install en-IN in Android Settings."
                    }
                }
            } else {
                isLanguageSupported.value = true
                languageWarning.value = null
                val setLangResult = tts?.setLanguage(targetLocale)
                Log.d(TAG, "tts.setLanguage($targetLocale) result: $setLangResult")

                // Query installed voices for this language
                val allVoices = try {
                    tts?.voices
                } catch (e: Exception) {
                    Log.w(TAG, "Error fetching voices", e)
                    null
                }

                if (!allVoices.isNullOrEmpty()) {
                    val matchingVoices = allVoices.filter { v ->
                        v.locale.language.equals(targetLocale.language, ignoreCase = true)
                    }.sortedBy { it.name }

                    availableVoices.value = matchingVoices

                    // Pick compatible voice
                    val bestVoice = matchingVoices.find { v ->
                        v.locale.country.equals(targetLocale.country, ignoreCase = true) && !v.isNetworkConnectionRequired
                    } ?: matchingVoices.find { v ->
                        v.locale.country.equals(targetLocale.country, ignoreCase = true)
                    } ?: matchingVoices.firstOrNull()

                    if (bestVoice != null) {
                        try {
                            tts?.voice = bestVoice
                            selectedVoice.value = bestVoice
                            activeVoiceName.value = bestVoice.name
                            Log.d(TAG, "Selected compatible voice: ${bestVoice.name}")
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not set voice: ${bestVoice.name}", e)
                        }
                    } else {
                        activeVoiceName.value = "${targetLocale.displayLanguage} (${targetLocale.displayCountry})"
                    }
                } else {
                    availableVoices.value = emptyList()
                    activeVoiceName.value = "${targetLocale.displayLanguage} (${targetLocale.displayCountry})"
                }
            }

            tts?.setPitch(speechPitch)
            tts?.setSpeechRate(speechRate)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying language $targetLocale", e)
            languageWarning.value = "Error configuring speech engine: ${e.message}"
        }
    }

    /**
     * User selection of a specific compatible voice
     */
    fun selectVoice(voice: Voice) {
        try {
            tts?.voice = voice
            selectedVoice.value = voice
            activeVoiceName.value = voice.name
            Log.d(TAG, "User selected voice: ${voice.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to select voice ${voice.name}", e)
        }
    }

    /**
     * Speaks the test sentence:
     * - English: "Hello, sir. JARVIS voice system is working correctly."
     * - Telugu: "నమస్కారం సార్. జార్విస్ వాయిస్ సిస్టమ్ సరిగ్గా పనిచేస్తోంది."
     */
    fun testVoice() {
        val testPhrase = when (currentLanguage) {
            LanguageMode.TELUGU -> "నమస్కారం సార్. జార్విస్ వాయిస్ సిస్టమ్ సరిగ్గా పనిచేస్తోంది."
            LanguageMode.TANGLISH -> "నమస్కారం సార్. జార్విస్ వాయిస్ సిస్టమ్ సరిగ్గా పనిచేస్తోంది."
            LanguageMode.ENGLISH -> "Hello, sir. JARVIS voice system is working correctly."
        }
        speak(testPhrase)
    }

    private fun initSpeechRecognizer() {
        mainHandler.post {
            val available = SpeechRecognizer.isRecognitionAvailable(context)
            isSpeechRecognitionSupported.value = available

            val serviceIntent = Intent("android.speech.RecognitionService")
            val resolveInfo = context.packageManager.resolveService(serviceIntent, 0)
            val enginePkg = resolveInfo?.serviceInfo?.packageName ?: if (available) "com.google.android.googlequicksearchbox" else "Unavailable"
            voiceDiagnostics.value = voiceDiagnostics.value.copy(
                selectedEnginePackage = enginePkg,
                isEngineAvailable = available,
                actualRecognitionStatus = if (available) "Ready" else "Service Unavailable"
            )

            if (!available) {
                Log.w(TAG, "Speech recognition service not preinstalled on device/emulator.")
                return@post
            }

            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _voiceState.value = VoiceState.LISTENING
                            _isListening.value = true
                            voiceDiagnostics.value = voiceDiagnostics.value.copy(
                                actualRecognitionStatus = "Listening..."
                            )
                        }

                        override fun onBeginningOfSpeech() {
                            _voiceState.value = VoiceState.LISTENING
                            _isListening.value = true
                            voiceDiagnostics.value = voiceDiagnostics.value.copy(
                                actualRecognitionStatus = "Speech detected"
                            )
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            if (_voiceState.value == VoiceState.LISTENING) {
                                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
                                _audioLevel.value = normalized
                                voiceDiagnostics.value = voiceDiagnostics.value.copy(audioLevel = normalized)
                            }
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            // Audio input completed; recognizer stopped listening, transitioning to PROCESSING
                            _voiceState.value = VoiceState.PROCESSING
                            _isListening.value = false
                            _audioLevel.value = 0f
                            voiceDiagnostics.value = voiceDiagnostics.value.copy(
                                actualRecognitionStatus = "Processing speech"
                            )
                        }

                        override fun onError(error: Int) {
                            _voiceState.value = VoiceState.IDLE
                            _isListening.value = false
                            _audioLevel.value = 0f

                            // Cancel any hanging recognition session
                            try {
                                speechRecognizer?.cancel()
                            } catch (e: Exception) {
                                Log.w(TAG, "Error cancelling recognizer on error", e)
                            }

                            val isTimeoutOrNoMatch = error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                                    error == SpeechRecognizer.ERROR_NO_MATCH

                            if (_isLiveMode.value && isTimeoutOrNoMatch && _voiceState.value != VoiceState.SPEAKING) {
                                scheduleRestartListening(350)
                                return
                            }

                            val errorMsg = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Microphone audio capture error"
                                SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer client busy. Tap mic to retry."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                                SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech network timeout"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Speak clearly into microphone."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy"
                                SpeechRecognizer.ERROR_SERVER -> "Google speech server error"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Microphone timed out waiting for audio"
                                else -> "Speech recognition error ($error)"
                            }

                            voiceDiagnostics.value = voiceDiagnostics.value.copy(
                                actualRecognitionStatus = "Error: $errorMsg",
                                lastErrorDetails = errorMsg
                            )

                            mainHandler.post { onSpeechError?.invoke(errorMsg) }

                            if (_isLiveMode.value && _voiceState.value != VoiceState.SPEAKING) {
                                scheduleRestartListening(1000)
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            _voiceState.value = VoiceState.PROCESSING
                            _isListening.value = false
                            _audioLevel.value = 0f
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val bestMatch = matches?.firstOrNull()?.trim() ?: ""
                            val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                            val confidence = scores?.firstOrNull() ?: 1.0f
                            val normalized = IntentClassifier.normalizePrompt(bestMatch)

                            voiceDiagnostics.value = voiceDiagnostics.value.copy(
                                actualRecognitionStatus = "Completed",
                                lastRawTranscript = bestMatch,
                                lastNormalizedTranscript = normalized,
                                lastConfidenceScore = confidence
                            )
                            _recognizedSpeech.value = bestMatch

                            if (bestMatch.isNotBlank()) {
                                if (isStopListeningPhrase(bestMatch)) {
                                    setLiveMode(false)
                                    _voiceState.value = VoiceState.IDLE
                                    mainHandler.post { onStopLiveModeRequested?.invoke() }
                                    return
                                }
                                mainHandler.post { onSpeechResult?.invoke(bestMatch) }
                            } else {
                                _voiceState.value = VoiceState.IDLE
                                if (_isLiveMode.value && !_isSpeaking.value) {
                                    scheduleRestartListening(400)
                                }
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull()?.trim() ?: ""
                            if (partial.isNotBlank()) {
                                _recognizedSpeech.value = partial
                                voiceDiagnostics.value = voiceDiagnostics.value.copy(
                                    actualRecognitionStatus = "Hearing...",
                                    lastRawTranscript = partial
                                )
                                if (_isLiveMode.value && isStopListeningPhrase(partial)) {
                                    stopListening()
                                    setLiveMode(false)
                                    mainHandler.post { onStopLiveModeRequested?.invoke() }
                                }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing SpeechRecognizer", e)
            }
        }
    }

    private fun isStopListeningPhrase(text: String): Boolean {
        val clean = text.lowercase().replace(Regex("[.,?!]"), "").trim()
        return STOP_LISTENING_PHRASES.any { phrase ->
            clean == phrase || clean.startsWith(phrase) || clean.endsWith(phrase) || clean.contains("stop listening")
        }
    }

    fun setLiveMode(enabled: Boolean) {
        _isLiveMode.value = enabled
        if (enabled) {
            JarvisLiveService.start(context)
            startListening()
        } else {
            cancelScheduledRestart()
            JarvisLiveService.stop(context)
            stopListening()
        }
    }

    private fun scheduleRestartListening(delayMs: Long) {
        cancelScheduledRestart()
        restartListeningRunnable = Runnable {
            if (_isLiveMode.value && !_isSpeaking.value) {
                startListening()
            }
        }
        mainHandler.postDelayed(restartListeningRunnable!!, delayMs)
    }

    private fun cancelScheduledRestart() {
        restartListeningRunnable?.let { mainHandler.removeCallbacks(it) }
        restartListeningRunnable = null
    }

    fun startListening() {
        // Prevent race condition: if already in LISTENING state, avoid launching a duplicate session
        if (_voiceState.value == VoiceState.LISTENING) {
            Log.d(TAG, "Already in LISTENING state. Ignoring duplicate startListening call.")
            return
        }

        stopSpeaking()
        cancelScheduledRestart()

        // 1. Check microphone permission
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _voiceState.value = VoiceState.IDLE
            onSpeechError?.invoke("Microphone permission required. Tap mic icon to grant permission.")
            return
        }

        // 2. Check if device has speech recognition service
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = VoiceState.IDLE
            onSpeechError?.invoke("Speech recognition engine not detected on emulator. Use Voice Test Prompts or text.")
            return
        }

        mainHandler.post {
            try {
                // Cancel any lingering recognition session to prevent ERROR_RECOGNIZER_BUSY
                try {
                    speechRecognizer?.cancel()
                } catch (e: Exception) {
                    Log.w(TAG, "Error cancelling previous recognizer", e)
                }

                if (speechRecognizer == null) {
                    initSpeechRecognizer()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                    // Set speech-to-text language: en-IN for English (India), te-IN for Telugu (India)
                    when (currentLanguage) {
                        LanguageMode.ENGLISH -> {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                        }
                        LanguageMode.TELUGU, LanguageMode.TANGLISH -> {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "te-IN")
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "te-IN")
                        }
                    }
                }
                _voiceState.value = VoiceState.LISTENING
                _isListening.value = true
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _voiceState.value = VoiceState.IDLE
                _isListening.value = false
                Log.e(TAG, "Error starting speech recognition", e)
                onSpeechError?.invoke("Could not start listening: ${e.message}")
            }
        }
    }

    // Direct voice prompt simulation (crucial for streaming emulators where hardware mic audio isn't piped)
    fun simulateVoiceInput(spokenText: String) {
        _recognizedSpeech.value = spokenText
        val normalized = IntentClassifier.normalizePrompt(spokenText)
        voiceDiagnostics.value = voiceDiagnostics.value.copy(
            actualRecognitionStatus = "Simulated Input",
            lastRawTranscript = spokenText,
            lastNormalizedTranscript = normalized,
            lastConfidenceScore = 1.0f
        )
        if (isStopListeningPhrase(spokenText)) {
            setLiveMode(false)
            _voiceState.value = VoiceState.IDLE
            onStopLiveModeRequested?.invoke()
        } else {
            _voiceState.value = VoiceState.PROCESSING
            onSpeechResult?.invoke(spokenText)
        }
    }

    fun stopListening() {
        cancelScheduledRestart()
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognition", e)
            } finally {
                _isListening.value = false
                _audioLevel.value = 0f
                if (_voiceState.value == VoiceState.LISTENING) {
                    _voiceState.value = VoiceState.IDLE
                }
            }
        }
    }

    /**
     * Speaks the sanitized text:
     * - Queues speech if TTS is initializing
     * - Cleans markdown, JSON, URLs, status labels, and logs
     * - Avoids overlapping speech by flushing previous utterance
     * - Stops microphone recognition before speech begins
     */
    fun speak(text: String) {
        cancelScheduledRestart()
        if (!isTtsEnabled || text.isBlank()) {
            if (_voiceState.value == VoiceState.PROCESSING) {
                _voiceState.value = VoiceState.IDLE
            }
            return
        }

        // If recognizer is active, cancel it before speaking to avoid mic capture of JARVIS's own voice
        if (_voiceState.value == VoiceState.LISTENING) {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelling recognizer before speak", e)
            }
            _isListening.value = false
        }

        val cleanSpeech = cleanTextForSpeech(text)
        if (cleanSpeech.isBlank()) {
            if (_voiceState.value == VoiceState.PROCESSING) {
                _voiceState.value = VoiceState.IDLE
            }
            return
        }

        // If TTS is still initializing, queue the utterance
        if (!_isTtsInitialized.value) {
            Log.d(TAG, "TTS not yet initialized, queuing: '$cleanSpeech'")
            pendingSpeechText = cleanSpeech
            return
        }

        mainHandler.post {
            try {
                _voiceState.value = VoiceState.SPEAKING
                _isSpeaking.value = true
                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID)
                }
                tts?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
            } catch (e: Exception) {
                Log.e(TAG, "Error executing speak()", e)
                _voiceState.value = VoiceState.IDLE
                _isSpeaking.value = false
            }
        }
    }

    fun stopSpeaking() {
        mainHandler.post {
            try {
                tts?.stop()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping TTS", e)
            } finally {
                _isSpeaking.value = false
                _audioLevel.value = 0f
                if (_voiceState.value == VoiceState.SPEAKING) {
                    _voiceState.value = VoiceState.IDLE
                }
            }
        }
    }

    fun release() {
        cancelScheduledRestart()
        setLiveMode(false)
        _voiceState.value = VoiceState.IDLE
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            _isTtsInitialized.value = false
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing VoiceManager", e)
        }
    }
}

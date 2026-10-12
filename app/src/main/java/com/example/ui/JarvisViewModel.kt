package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.api.GeminiResult
import com.example.data.api.RawFunctionCall
import com.example.data.api.TurnMessage
import com.example.data.diagnostics.CoreDiagnosticsRunner
import com.example.data.diagnostics.DiagnosticItem
import com.example.data.diagnostics.DiagnosticStatus
import com.example.data.diagnostics.DiagnosticSuiteReport
import com.example.data.launcher.AndroidAppLauncher
import com.example.data.launcher.AppLaunchResult
import com.example.data.launcher.AppLauncher
import com.example.data.local.ConversationRepository
import com.example.data.local.JarvisDatabase
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteEntity
import com.example.data.messaging.HomeArrivalManager
import com.example.data.model.ChatMessage
import com.example.data.model.HomeArrivalPendingState
import com.example.data.model.JarvisState
import com.example.data.model.LanguageMode
import com.example.data.model.MessageSender
import com.example.data.model.MessagingServiceType
import com.example.data.model.PendingWebsiteFallback
import com.example.data.model.PracticeQuestion
import com.example.data.model.ResponseStyle
import com.example.data.model.SearchItem
import com.example.data.model.ServiceUnreadReport
import com.example.data.model.StructuredSearchResult
import com.example.data.model.ToolCallInfo
import com.example.data.model.ToolExecutionStatus
import com.example.data.model.UserIntent
import com.example.data.model.VoiceDiagnosticsInfo
import com.example.data.model.VoiceLatencyMetrics
import com.example.data.model.VoiceState
import com.example.data.network.NetworkDiagnostics
import com.example.data.network.NetworkManager
import com.example.data.study.StudyManager
import com.example.tools.IntentClassificationResult
import com.example.tools.IntentClassifier
import com.example.tools.JarvisToolsManager
import com.example.ui.haptics.HapticIntensity
import com.example.ui.haptics.HapticManager
import com.example.voice.VoiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProtocolStatus(
    val phase: String, // "SEARCHING WEB", "SOURCE RETRIEVAL", "ANALYZING", "RESPONSE READY"
    val query: String = "",
    val sourcesFound: Int = 0,
    val details: String = ""
)

enum class JarvisTab(val label: String) {
    CORE("Core"),
    MEMORY("Memory"),
    TOOLS("Tools & PC"),
    STUDY("Study"),
    SETTINGS("Settings")
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "JarvisViewModel"
    }

    private val database = JarvisDatabase.getDatabase(application)
    val toolsManager = JarvisToolsManager(application, database)
    val voiceManager = VoiceManager(application)
    val hapticManager = HapticManager(application)
    val networkManager = NetworkManager(application)

    // Live Network State
    val isOnline: StateFlow<Boolean> = networkManager.isOnline
    val connectionType: StateFlow<String> = networkManager.connectionType
    val lastLatencyMs: StateFlow<Long?> = networkManager.lastLatencyMs

    // Voice Diagnostics & Latency Metrics
    val voiceLatencyMetrics = MutableStateFlow(VoiceLatencyMetrics())
    val voiceDiagnostics: StateFlow<VoiceDiagnosticsInfo> = voiceManager.voiceDiagnostics
    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState

    // Persistent Configuration State
    private val prefs = application.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
    val customApiKey = MutableStateFlow(prefs.getString("custom_api_key", "") ?: "")
    val selectedModel = MutableStateFlow(prefs.getString("selected_model", "gemini-3.5-flash") ?: "gemini-3.5-flash")
    val languageMode = MutableStateFlow(
        try {
            LanguageMode.valueOf(prefs.getString("language_mode", LanguageMode.ENGLISH.name) ?: LanguageMode.ENGLISH.name)
        } catch (_: Exception) {
            LanguageMode.ENGLISH
        }
    )
    val responseStyle = MutableStateFlow(ResponseStyle.CONCISE_JARVIS)
    val isMemoryInjectionEnabled = MutableStateFlow(true)
    val isHandsFreeEnabled = MutableStateFlow(false)
    val userTitle = MutableStateFlow("Sir")
    val hapticIntensity = MutableStateFlow(HapticIntensity.NORMAL)

    // BACKGROUND CHECKS SETTINGS
    val bgCheckInstagram = MutableStateFlow(prefs.getBoolean("bg_check_instagram", true))
    val bgCheckTelegram = MutableStateFlow(prefs.getBoolean("bg_check_telegram", true))
    val bgCheckWhatsApp = MutableStateFlow(prefs.getBoolean("bg_check_whatsapp", false))
    val bgCheckGmail = MutableStateFlow(prefs.getBoolean("bg_check_gmail", false))
    val bgCheckDiscord = MutableStateFlow(prefs.getBoolean("bg_check_discord", false))

    // MESSAGE PRIVACY SETTINGS
    val privacyUnreadCounts = MutableStateFlow(prefs.getBoolean("privacy_unread_counts", true))
    val privacyReadContents = MutableStateFlow(prefs.getBoolean("privacy_read_contents", false))
    val privacySenderNames = MutableStateFlow(prefs.getBoolean("privacy_sender_names", false))
    val simulatedTestingMode = MutableStateFlow(prefs.getBoolean("simulated_testing_mode", false))

    val homeArrivalManager = HomeArrivalManager(application)
    val appLauncher: AppLauncher = AndroidAppLauncher(application)
    val pendingHomeArrivalState = MutableStateFlow<HomeArrivalPendingState?>(null)
    val pendingWebsiteFallbackState = MutableStateFlow<PendingWebsiteFallback?>(null)

    fun confirmWebsiteFallback(approved: Boolean) {
        val pending = pendingWebsiteFallbackState.value ?: return
        pendingWebsiteFallbackState.value = null

        if (approved) {
            val (success, msg) = appLauncher.launchWebsiteFallback(pending.appId)
            snackbarMessage.value = msg
            val reply = "Opening official website for ${pending.displayName}, Sir."
            val jarvisMsg = ChatMessage(sender = MessageSender.JARVIS, text = reply)
            appendMessage(jarvisMsg)
            setJarvisState(JarvisState.SPEAKING)
            voiceManager.speak(reply)
        } else {
            val reply = "Understood, Sir. Keeping website closed."
            val jarvisMsg = ChatMessage(sender = MessageSender.JARVIS, text = reply)
            appendMessage(jarvisMsg)
            setJarvisState(JarvisState.STANDBY)
            voiceManager.speak(reply)
        }
    }

    fun setSimulatedTestingMode(enabled: Boolean) {
        simulatedTestingMode.value = enabled
        prefs.edit().putBoolean("simulated_testing_mode", enabled).apply()
        snackbarMessage.value = if (enabled) "Simulated Protocol Testing: ENABLED [DEMO]" else "Simulated Protocol Testing: DISABLED [PRODUCTION]"
    }

    fun setBgCheckInstagram(enabled: Boolean) {
        bgCheckInstagram.value = enabled
        prefs.edit().putBoolean("bg_check_instagram", enabled).apply()
        snackbarMessage.value = "Instagram background check: ${if (enabled) "ON" else "OFF"}"
    }

    fun setBgCheckTelegram(enabled: Boolean) {
        bgCheckTelegram.value = enabled
        prefs.edit().putBoolean("bg_check_telegram", enabled).apply()
        snackbarMessage.value = "Telegram background check: ${if (enabled) "ON" else "OFF"}"
    }

    fun setBgCheckWhatsApp(enabled: Boolean) {
        bgCheckWhatsApp.value = enabled
        prefs.edit().putBoolean("bg_check_whatsapp", enabled).apply()
        snackbarMessage.value = "WhatsApp background check: ${if (enabled) "ON" else "OFF"}"
    }

    fun setBgCheckGmail(enabled: Boolean) {
        bgCheckGmail.value = enabled
        prefs.edit().putBoolean("bg_check_gmail", enabled).apply()
        snackbarMessage.value = "Gmail background check: ${if (enabled) "ON" else "OFF"}"
    }

    fun setBgCheckDiscord(enabled: Boolean) {
        bgCheckDiscord.value = enabled
        prefs.edit().putBoolean("bg_check_discord", enabled).apply()
        snackbarMessage.value = "Discord background check: ${if (enabled) "ON" else "OFF"}"
    }

    fun setPrivacyUnreadCounts(enabled: Boolean) {
        privacyUnreadCounts.value = enabled
        prefs.edit().putBoolean("privacy_unread_counts", enabled).apply()
        snackbarMessage.value = "Unread counts privacy: ${if (enabled) "ON" else "OFF"}"
    }

    fun setPrivacyReadContents(enabled: Boolean) {
        privacyReadContents.value = enabled
        prefs.edit().putBoolean("privacy_read_contents", enabled).apply()
        snackbarMessage.value = "Read message contents: ${if (enabled) "ON" else "OFF"}"
    }

    fun setPrivacySenderNames(enabled: Boolean) {
        privacySenderNames.value = enabled
        prefs.edit().putBoolean("privacy_sender_names", enabled).apply()
        snackbarMessage.value = "Sender names privacy: ${if (enabled) "ON" else "OFF"}"
    }

    fun dismissHomeArrivalPending() {
        pendingHomeArrivalState.value = null
        protocolStatus.value = null
        val msg = "Understood, Sir. Keeping notifications closed."
        appendMessage(ChatMessage(sender = MessageSender.JARVIS, text = msg))
        voiceManager.speak(msg)
    }

    fun openHomeArrivalService(service: MessagingServiceType) {
        val isAuthorized = when (service) {
            MessagingServiceType.INSTAGRAM -> bgCheckInstagram.value
            MessagingServiceType.TELEGRAM -> bgCheckTelegram.value
            MessagingServiceType.WHATSAPP -> bgCheckWhatsApp.value
            MessagingServiceType.GMAIL -> bgCheckGmail.value
            MessagingServiceType.DISCORD -> bgCheckDiscord.value
        }
        if (!isAuthorized) {
            val msg = "${service.displayName} is not authorized for background checks in settings, Sir."
            snackbarMessage.value = msg
            appendMessage(ChatMessage(sender = MessageSender.JARVIS, text = msg))
            setJarvisState(JarvisState.SPEAKING)
            voiceManager.speak(msg)
            pendingHomeArrivalState.value = null
            protocolStatus.value = null
            return
        }

        val launchResult = appLauncher.launchApp(service.id)
        val msg = when (launchResult) {
            is AppLaunchResult.Success -> launchResult.message
            is AppLaunchResult.SecureFolderLocked -> launchResult.message
            is AppLaunchResult.NotInstalled -> {
                pendingWebsiteFallbackState.value = PendingWebsiteFallback(
                    appId = launchResult.appId,
                    displayName = launchResult.displayName,
                    websiteUrl = launchResult.websiteUrl
                )
                launchResult.message
            }
            is AppLaunchResult.NotAllowed -> launchResult.message
            is AppLaunchResult.Failure -> launchResult.errorMessage
        }
        snackbarMessage.value = msg
        val jarvisMsg = ChatMessage(
            sender = MessageSender.JARVIS,
            text = msg
        )
        appendMessage(jarvisMsg)
        setJarvisState(JarvisState.SPEAKING)
        voiceManager.speak(msg)
        pendingHomeArrivalState.value = null
        protocolStatus.value = null
    }

    fun openBothHomeArrivalServices() {
        val isIgAuth = bgCheckInstagram.value
        val isTgAuth = bgCheckTelegram.value
        val openedServices = mutableListOf<String>()
        val statusNotes = mutableListOf<String>()

        if (isIgAuth) {
            when (val res = appLauncher.launchApp("instagram")) {
                is AppLaunchResult.Success -> openedServices.add("Instagram")
                is AppLaunchResult.SecureFolderLocked -> statusNotes.add(res.message)
                is AppLaunchResult.NotInstalled -> statusNotes.add(res.message)
                else -> {}
            }
        }
        if (isTgAuth) {
            when (val res = appLauncher.launchApp("telegram")) {
                is AppLaunchResult.Success -> openedServices.add("Telegram")
                is AppLaunchResult.SecureFolderLocked -> statusNotes.add(res.message)
                is AppLaunchResult.NotInstalled -> statusNotes.add(res.message)
                else -> {}
            }
        }

        val msg = if (openedServices.isNotEmpty()) {
            val openedText = "Opening authorized services: ${openedServices.joinToString(" and ")}, Sir."
            if (statusNotes.isNotEmpty()) "$openedText ${statusNotes.joinToString(" ")}" else openedText
        } else if (statusNotes.isNotEmpty()) {
            statusNotes.joinToString(" ")
        } else {
            "Neither service is authorized in settings, Sir."
        }
        snackbarMessage.value = msg
        val jarvisMsg = ChatMessage(
            sender = MessageSender.JARVIS,
            text = msg
        )
        appendMessage(jarvisMsg)
        setJarvisState(JarvisState.SPEAKING)
        voiceManager.speak(msg)
        pendingHomeArrivalState.value = null
        protocolStatus.value = null
    }

    fun updateCustomApiKey(key: String) {
        val trimmed = key.trim()
        customApiKey.value = trimmed
        prefs.edit().putString("custom_api_key", trimmed).apply()
        snackbarMessage.value = if (trimmed.isNotEmpty()) "Gemini API key saved." else "Custom API key cleared."
    }

    fun updateSelectedModel(model: String) {
        selectedModel.value = model
        prefs.edit().putString("selected_model", model).apply()
        snackbarMessage.value = "Active model: $model"
    }

    fun setHapticIntensity(newIntensity: HapticIntensity) {
        hapticIntensity.value = newIntensity
        hapticManager.intensity = newIntensity
        if (newIntensity != HapticIntensity.OFF) {
            hapticManager.testSample(newIntensity)
        }
        snackbarMessage.value = "Haptic feedback set to: ${newIntensity.label}"
    }

    fun runNetworkDiagnostics() {
        viewModelScope.launch {
            snackbarMessage.value = "Checking network connectivity..."
            val diag = networkManager.runDiagnostics()
            val text = if (diag.isConnected) {
                "Internet probe successful: ${diag.connectionType} (${diag.latencyMs} ms latency)."
            } else {
                diag.statusMessage
            }
            snackbarMessage.value = text
            val jarvisMsg = ChatMessage(
                sender = MessageSender.JARVIS,
                text = "--- NETWORK CONNECTIVITY PROBE ---\n" +
                        "• Status: ${if (diag.isConnected) "ONLINE [Connected]" else "OFFLINE [Disconnected]"}\n" +
                        "• Connection Type: ${diag.connectionType}\n" +
                        "• Latency to Google Servers: ${if (diag.latencyMs >= 0) "${diag.latencyMs} ms" else "Timed out"}\n" +
                        "• Telemetry: ${diag.statusMessage}"
            )
            appendMessage(jarvisMsg)
            setJarvisState(JarvisState.SPEAKING)
            voiceManager.speak(if (diag.isConnected) "Internet connection confirmed online with ${diag.latencyMs} milliseconds latency." else "Internet connection probe failed, Sir.")
        }
    }

    private val geminiClient = GeminiClient(
        customApiKey = { customApiKey.value },
        modelProvider = { selectedModel.value }
    )

    val diagnosticsRunner = CoreDiagnosticsRunner(
        context = application,
        database = database,
        toolsManager = toolsManager,
        voiceManager = voiceManager,
        geminiClient = geminiClient,
        networkManager = networkManager,
        activeModelProvider = { selectedModel.value }
    )

    val diagnosticReport = MutableStateFlow<DiagnosticSuiteReport?>(null)
    val showDiagnosticsDialog = MutableStateFlow(false)
    private var diagnosticsJob: Job? = null

    fun openDiagnosticsDialog() {
        showDiagnosticsDialog.value = true
        if (diagnosticReport.value == null || diagnosticReport.value?.isRunning == false) {
            startCoreDiagnostics()
        }
    }

    fun dismissDiagnosticsDialog() {
        showDiagnosticsDialog.value = false
    }

    fun startCoreDiagnostics() {
        diagnosticsJob?.cancel()
        showDiagnosticsDialog.value = true
        hapticManager.onThinkingTransition()
        snackbarMessage.value = "Executing Core Diagnostics..."
        setJarvisState(JarvisState.THINKING)

        diagnosticsJob = viewModelScope.launch {
            voiceManager.speak("Initiating core diagnostics sequence, Sir.")
            diagnosticsRunner.runDiagnosticsFlow().collect { report ->
                diagnosticReport.value = report
                if (!report.isRunning) {
                    setJarvisState(JarvisState.SPEAKING)
                    hapticManager.onSpeakingTransition()
                    val passedCount = report.items.count { it.status == DiagnosticStatus.PASSED }
                    val summarySpeech = if (passedCount == report.items.size) {
                        "Core diagnostics complete. All ten subsystems are fully nominal."
                    } else {
                        "Core diagnostics complete. $passedCount of ${report.items.size} subsystems passed."
                    }
                    voiceManager.speak(summarySpeech)

                    // Generate a nice ChatMessage entry in chat as well
                    val sb = StringBuilder()
                    sb.append("⚡ CORE DIAGNOSTICS REPORT\n")
                    sb.append("───────────────────────────────\n")
                    report.items.forEach { item ->
                        val icon = when (item.status) {
                            DiagnosticStatus.PASSED -> "✓"
                            DiagnosticStatus.WARNING -> "⚠"
                            DiagnosticStatus.FAILED -> "✗"
                            else -> "○"
                        }
                        val statusTag = item.status.name
                        sb.append("$icon ${item.name}: $statusTag (${item.latencyMs}ms)\n")
                        sb.append("   • ${item.summary}\n")
                    }
                    sb.append("───────────────────────────────\n")
                    sb.append("Overall: ${report.overallStatus} (${report.durationMs}ms)")

                    val reportMsg = ChatMessage(
                        sender = MessageSender.JARVIS,
                        text = sb.toString()
                    )
                    appendMessage(reportMsg)
                }
            }
        }
    }

    // UI State
    val activeTab = MutableStateFlow(JarvisTab.CORE)
    val jarvisState = MutableStateFlow(JarvisState.STANDBY)
    val inputText = MutableStateFlow("")
    val snackbarMessage = MutableStateFlow<String?>(null)
    val protocolStatus = MutableStateFlow<ProtocolStatus?>(null)

    // Audio & Waveform Level (0f..1f)
    val audioLevel: StateFlow<Float> = voiceManager.audioLevel
    val activeVoiceName: StateFlow<String> = voiceManager.activeVoiceName
    val isSpeechRecognitionSupported: StateFlow<Boolean> = voiceManager.isSpeechRecognitionSupported

    // Pending confirmation for tool execution
    val pendingConfirmation = MutableStateFlow<ToolCallInfo?>(null)

    // Room Persistent Conversation History
    val conversationRepository = ConversationRepository(database.conversationDao())

    val messages: StateFlow<List<ChatMessage>> = conversationRepository.messagesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val messageCount: StateFlow<Int> = conversationRepository.messageCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun appendMessage(message: ChatMessage) {
        viewModelScope.launch {
            conversationRepository.saveMessage(message)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            conversationRepository.deleteMessage(messageId)
            snackbarMessage.value = "Message deleted from Room."
        }
    }

    // Room DB Flows for Memories & Notes
    val memories: StateFlow<List<MemoryEntity>> = database.memoryDao()
        .getAllMemoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = database.noteDao()
        .getAllNotesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Study State
    val currentQuizQuestion = MutableStateFlow<PracticeQuestion?>(StudyManager.sampleQuestions.firstOrNull())
    val selectedQuizAnswer = MutableStateFlow<Int?>(null)
    val showQuizExplanation = MutableStateFlow(false)
    val studyInTelugu = MutableStateFlow(false)

    // Live Mode flow
    val isLiveMode: StateFlow<Boolean> = voiceManager.isLiveMode

    private var currentJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun setJarvisState(newState: JarvisState) {
        if (jarvisState.value != newState) {
            jarvisState.value = newState
            when (newState) {
                JarvisState.LISTENING -> hapticManager.onListeningTransition()
                JarvisState.PROCESSING, JarvisState.OPENING_APP, JarvisState.THINKING, JarvisState.SEARCHING, JarvisState.ANALYZING -> hapticManager.onThinkingTransition()
                JarvisState.SPEAKING -> hapticManager.onSpeakingTransition()
                JarvisState.STANDBY -> hapticManager.onStandbyTransition()
            }
        }
    }

    init {
        voiceManager.speechRate = prefs.getFloat("speech_rate", 1.0f)
        voiceManager.speechPitch = prefs.getFloat("speech_pitch", 1.0f)
        voiceManager.setLanguage(languageMode.value)
        setupVoiceCallbacks()
        seedInitialGreetingIfEmpty()
    }

    private fun setupVoiceCallbacks() {
        voiceManager.onSpeechResult = { recognizedText ->
            inputText.value = recognizedText
            sendMessage(recognizedText)
        }

        voiceManager.onSpeechError = { errorMsg ->
            if (!voiceManager.isLiveMode.value) {
                setJarvisState(JarvisState.STANDBY)
                snackbarMessage.value = errorMsg
            }
        }

        voiceManager.onSpeakingStarted = {
            setJarvisState(JarvisState.SPEAKING)
        }

        voiceManager.onSpeakingFinished = {
            setJarvisState(JarvisState.STANDBY)
            if (isHandsFreeEnabled.value && !voiceManager.isLiveMode.value) {
                mainHandler.postDelayed({
                    startListening()
                }, 400)
            }
        }

        voiceManager.onStopLiveModeRequested = {
            val title = userTitle.value
            val stopReply = when (languageMode.value) {
                LanguageMode.ENGLISH -> "Live Mode deactivated as requested, $title. Standing by."
                LanguageMode.TELUGU -> "సరే సార్, లైవ్ మోడ్ ఆపివేయబడింది. స్టాండ్‌బైలో ఉన్నాను."
                LanguageMode.TANGLISH -> "Live Mode stopped as requested, $title. Standing by."
            }
            val userMsg = ChatMessage(sender = MessageSender.USER, text = "Stop listening")
            val jarvisMsg = ChatMessage(sender = MessageSender.JARVIS, text = stopReply)
            appendMessage(userMsg)
            appendMessage(jarvisMsg)
            setJarvisState(JarvisState.SPEAKING)
            voiceManager.speak(stopReply)
            snackbarMessage.value = "Live Mode deactivated"
        }
    }

    private fun seedInitialGreetingIfEmpty() {
        viewModelScope.launch {
            val existing = conversationRepository.getAllMessages()
            if (existing.isEmpty()) {
                val greeting = when (languageMode.value) {
                    LanguageMode.ENGLISH -> "Systems online, Sir. Neural matrix initialized and ready for your command."
                    LanguageMode.TELUGU -> "నమస్కారం సార్! జార్విస్ సిస్టమ్స్ ఆన్‌లైన్‌లో ఉన్నాయి. మీకు ఎలా సహాయపడగలను?"
                    LanguageMode.TANGLISH -> "Systems ready, Sir! Jarvis fully online ga unnadu. Today em commands execute cheyyali?"
                }
                appendMessage(
                    ChatMessage(
                        sender = MessageSender.JARVIS,
                        text = greeting
                    )
                )
            }
        }
    }

    fun setLanguage(mode: LanguageMode) {
        languageMode.value = mode
        prefs.edit().putString("language_mode", mode.name).apply()
        voiceManager.setLanguage(mode)
        studyInTelugu.value = (mode == LanguageMode.TELUGU || mode == LanguageMode.TANGLISH)
    }

    fun setSpeechRate(rate: Float) {
        voiceManager.speechRate = rate
        prefs.edit().putFloat("speech_rate", rate).apply()
    }

    fun setSpeechPitch(pitch: Float) {
        voiceManager.speechPitch = pitch
        prefs.edit().putFloat("speech_pitch", pitch).apply()
    }

    fun selectVoice(voice: android.speech.tts.Voice) {
        voiceManager.selectVoice(voice)
        prefs.edit().putString("selected_voice_name", voice.name).apply()
    }

    fun testVoice() {
        voiceManager.testVoice()
    }

    fun startListening() {
        interruptSpeech()
        setJarvisState(JarvisState.LISTENING)
        voiceManager.startListening()
    }

    fun stopListening() {
        voiceManager.stopListening()
        setJarvisState(JarvisState.STANDBY)
    }

    fun toggleLiveMode(forceState: Boolean? = null) {
        val newState = forceState ?: !voiceManager.isLiveMode.value
        voiceManager.setLiveMode(newState)
        val title = userTitle.value
        if (newState) {
            val announcement = when (languageMode.value) {
                LanguageMode.ENGLISH -> "Live Mode engaged, $title. I am listening continuously on background. Say 'Stop listening' at any time to halt."
                LanguageMode.TELUGU -> "లైవ్ మోడ్ ప్రారంభించబడింది, సార్. నేను నిరంతరం వింటున్నాను. ఆపడానికి 'Stop listening' అని చెప్పండి."
                LanguageMode.TANGLISH -> "Live Mode active ga undi, $title. Jarvis continuous ga listen chesthadu. Say 'Stop listening' to halt."
            }
            val msg = ChatMessage(sender = MessageSender.JARVIS, text = announcement)
            appendMessage(msg)
            setJarvisState(JarvisState.SPEAKING)
            voiceManager.speak(announcement)
            snackbarMessage.value = "Live Mode Active // Continuous background listening"
        } else {
            interruptSpeech()
            val stopMsg = "Live Mode deactivated. Standing by, $title."
            val msg = ChatMessage(sender = MessageSender.JARVIS, text = stopMsg)
            appendMessage(msg)
            setJarvisState(JarvisState.STANDBY)
            snackbarMessage.value = "Live Mode Deactivated"
        }
    }

    fun simulateVoiceCommand(prompt: String) {
        inputText.value = prompt
        voiceManager.simulateVoiceInput(prompt)
    }

    fun interruptSpeech() {
        voiceManager.stopSpeaking()
        currentJob?.cancel()
        if (jarvisState.value == JarvisState.SPEAKING || jarvisState.value == JarvisState.THINKING) {
            setJarvisState(JarvisState.STANDBY)
        }
    }

    fun sendMessage(userText: String = inputText.value) {
        val prompt = userText.trim()
        if (prompt.isEmpty()) return

        interruptSpeech()
        inputText.value = ""

        // Append user message to Room database
        val userMsg = ChatMessage(sender = MessageSender.USER, text = prompt)
        appendMessage(userMsg)
        setJarvisState(JarvisState.PROCESSING)

        // Check if answering pending Home Arrival confirmation
        val pendingHomeArrival = pendingHomeArrivalState.value
        if (pendingHomeArrival != null) {
            val lower = prompt.lowercase().trim()
            if (handlePendingHomeArrivalResponse(lower, pendingHomeArrival)) {
                return
            }
        }

        // Check if answering pending Website Fallback confirmation ("Would you like me to open the official website?")
        val pendingWebsite = pendingWebsiteFallbackState.value
        if (pendingWebsite != null) {
            val lower = prompt.lowercase().trim()
            if (lower == "yes" || lower == "yeah" || lower == "sure" || lower == "yep" || lower == "please" ||
                lower == "okay" || lower == "ok" || lower.contains("open website") || lower.contains("open the website") ||
                lower.contains("go ahead") || lower.contains("open it") || lower.startsWith("yes ")) {
                confirmWebsiteFallback(true)
                return
            } else if (lower == "no" || lower == "nope" || lower == "nah" || lower == "cancel" || lower == "dont" ||
                lower == "don't" || lower == "nevermind" || lower == "dismiss" || lower.startsWith("no ") || lower.startsWith("don't ")) {
                confirmWebsiteFallback(false)
                return
            }
        }

        // Classify intent before executing any computer-control or search function
        val lastConfidence = voiceManager.voiceDiagnostics.value.lastConfidenceScore
        val classification = IntentClassifier.classify(prompt, lastConfidence)
        Log.d(TAG, "Prompt: '$prompt' -> Classified Intent: ${classification.intent} (ambiguous=${classification.isAmbiguous})")

        currentJob = viewModelScope.launch {
            when (classification.intent) {
                UserIntent.STOP_SPEECH -> {
                    handleStopSpeechIntent()
                }
                UserIntent.QUICK_DEVICE_ACTION -> {
                    handleQuickDeviceActionIntent(prompt, classification)
                }
                UserIntent.HOME_ARRIVAL -> {
                    handleHomeArrivalIntent(prompt, classification)
                }
                UserIntent.STUDY -> {
                    handleStudyIntent(prompt, classification)
                }
                UserIntent.COMPUTER_SEARCH -> {
                    handleComputerSearchIntent(prompt, classification)
                }
                UserIntent.OPEN_APPLICATION -> {
                    handleOpenApplicationIntent(prompt, classification)
                }
                UserIntent.INFORMATION_REQUEST -> {
                    handleInformationRequestIntent(prompt, classification)
                }
                UserIntent.CONVERSATION -> {
                    handleConversationIntent(prompt, classification)
                }
            }
        }
    }

    private fun handleStopSpeechIntent() {
        val t0 = System.currentTimeMillis()
        interruptSpeech()
        val latencyMs = System.currentTimeMillis() - t0
        voiceLatencyMetrics.value = voiceLatencyMetrics.value.copy(
            intentClassificationMs = 1L,
            executionMs = latencyMs,
            totalLatencyMs = latencyMs,
            lastActionDescription = "Stop Speech"
        )
        setJarvisState(JarvisState.STANDBY)
    }

    private suspend fun handleQuickDeviceActionIntent(prompt: String, classification: IntentClassificationResult) {
        val t0 = System.currentTimeMillis()
        val action = classification.targetApp.ifEmpty { "time" }
        setJarvisState(JarvisState.PROCESSING)

        val (replyText, spokenText, toolCard) = when (action) {
            "time" -> {
                val timeStr = toolsManager.getCurrentTime()
                val spoken = "The time is " + (timeStr.lines().firstOrNull { it.startsWith("Current Time:") }?.replace("Current Time:", "")?.trim() ?: timeStr)
                Triple(
                    "Current temporal coordinates, Sir:\n$timeStr",
                    spoken,
                    ToolCallInfo(
                        toolName = "getCurrentTime",
                        argumentsJson = "{}",
                        summary = "Clock Telemetry",
                        resultMessage = timeStr,
                        isSimulated = false
                    )
                )
            }
            "system", "diagnostics" -> {
                val sysInfo = toolsManager.getSystemInfo()
                Triple(
                    "JARVIS System Telemetry, Sir:\n$sysInfo",
                    "All core systems operational, Sir. Diagnostics nominal.",
                    ToolCallInfo(
                        toolName = "getSystemInfo",
                        argumentsJson = "{}",
                        summary = "System Telemetry",
                        resultMessage = sysInfo,
                        isSimulated = false
                    )
                )
            }
            else -> {
                val timeStr = toolsManager.getCurrentTime()
                Triple(
                    timeStr,
                    "Temporal coordinates retrieved, Sir.",
                    ToolCallInfo(
                        toolName = "getCurrentTime",
                        argumentsJson = "{}",
                        summary = "Clock Telemetry",
                        resultMessage = timeStr,
                        isSimulated = false
                    )
                )
            }
        }

        val latencyMs = (System.currentTimeMillis() - t0).coerceAtLeast(1L)
        voiceLatencyMetrics.value = voiceLatencyMetrics.value.copy(
            intentClassificationMs = 1L,
            executionMs = latencyMs,
            totalLatencyMs = latencyMs,
            lastActionDescription = "Quick Device Action: $action"
        )

        val jarvisMsg = ChatMessage(
            sender = MessageSender.JARVIS,
            text = replyText,
            toolCall = toolCard
        )
        appendMessage(jarvisMsg)
        setJarvisState(JarvisState.SPEAKING)
        voiceManager.speak(spokenText)
    }

    private fun handlePendingHomeArrivalResponse(lower: String, pending: HomeArrivalPendingState): Boolean {
        // If user says "no", "cancel", "don't", "nevermind", "dismiss"
        if (lower == "no" || lower == "nope" || lower == "cancel" || lower == "dont" || lower == "don't" || lower == "nevermind" || lower == "dismiss" || lower.startsWith("no ") || lower.startsWith("don't ")) {
            val msg = "Understood, Sir. Keeping notifications closed."
            appendMessage(ChatMessage(sender = MessageSender.JARVIS, text = msg))
            setJarvisState(JarvisState.SPEAKING)
            voiceManager.speak(msg)
            pendingHomeArrivalState.value = null
            protocolStatus.value = null
            return true
        }

        // If user says "both" or "open both"
        if (lower.contains("both") || lower == "open both" || lower == "all" || lower == "open all") {
            openBothHomeArrivalServices()
            return true
        }

        // If user says "instagram"
        if (lower.contains("instagram") || lower == "insta") {
            openHomeArrivalService(MessagingServiceType.INSTAGRAM)
            return true
        }

        // If user says "telegram"
        if (lower.contains("telegram") || lower == "tg") {
            openHomeArrivalService(MessagingServiceType.TELEGRAM)
            return true
        }

        // If user says "whatsapp"
        if (lower.contains("whatsapp")) {
            openHomeArrivalService(MessagingServiceType.WHATSAPP)
            return true
        }

        // If user says "gmail"
        if (lower.contains("gmail")) {
            openHomeArrivalService(MessagingServiceType.GMAIL)
            return true
        }

        // If user says "yes" or "open them" or "sure" or "yeah"
        val isAffirmative = lower == "yes" || lower == "sure" || lower == "yeah" || lower == "yep" ||
                lower == "open them" || lower == "open" || lower.startsWith("yes ") || lower == "please" ||
                lower == "please open" || lower == "ok" || lower == "okay" || lower == "yup"

        if (isAffirmative) {
            val unreadServices = pending.unreadReports.filter { it.isAuthorized && it.isAvailable && it.unreadCount > 0 }

            // If the user's previous statement clearly identified the service, open only that service
            if (pending.previouslyIdentifiedService != null) {
                openHomeArrivalService(pending.previouslyIdentifiedService)
                return true
            }

            if (unreadServices.size > 1) {
                // Ask which service if multiple services have unread messages
                val serviceNames = unreadServices.joinToString(", ") { it.service.displayName }
                val msg = "Which service would you like me to open, Sir? $serviceNames, or both?"
                appendMessage(ChatMessage(sender = MessageSender.JARVIS, text = msg))
                setJarvisState(JarvisState.SPEAKING)
                voiceManager.speak(msg)
                pendingHomeArrivalState.value = pending.copy(awaitingServiceSelection = true)
                return true
            } else if (unreadServices.size == 1) {
                openHomeArrivalService(unreadServices.first().service)
                return true
            } else {
                val msg = "No unread notifications to open, Sir."
                appendMessage(ChatMessage(sender = MessageSender.JARVIS, text = msg))
                setJarvisState(JarvisState.SPEAKING)
                voiceManager.speak(msg)
                pendingHomeArrivalState.value = null
                protocolStatus.value = null
                return true
            }
        }

        return false
    }

    fun handleDirectMessagingCheck(prompt: String): Boolean {
        val lower = prompt.lowercase().trim()

        val mentionsInstagram = lower.contains("instagram") || lower.contains("insta")
        val mentionsTelegram = lower.contains("telegram") || lower.contains("tg")
        val mentionsCheckOrQuery = lower.contains("check") || lower.contains("message") ||
                lower.contains("unread") || lower.contains("notification") ||
                lower.contains("status") || lower.contains("any new") || lower.contains("do i have")

        // 1. Direct Instagram check
        if (mentionsInstagram && !mentionsTelegram && mentionsCheckOrQuery) {
            val isSim = simulatedTestingMode.value
            val isAuth = bgCheckInstagram.value
            val report = homeArrivalManager.checkSingleService(MessagingServiceType.INSTAGRAM, isAuth, isSim)
            val response = if (!report.isAuthorized) {
                "Instagram is not connected, sir."
            } else if (report.isSimulated) {
                "[SIMULATED] Sir, you have 2 unread messages from Instagram. Should I open them?"
            } else {
                "I couldn't check Instagram right now, sir."
            }
            appendMessage(ChatMessage(sender = MessageSender.JARVIS, text = response))
            setJarvisState(JarvisState.SPEAKING)
            voiceManager.speak(response)
            return true
        }

        // 2. Direct Telegram check
        if (mentionsTelegram && !mentionsInstagram && mentionsCheckOrQuery) {
            val isSim = simulatedTestingMode.value
            val isAuth = bgCheckTelegram.value
            val report = homeArrivalManager.checkSingleService(MessagingServiceType.TELEGRAM, isAuth, isSim)
            val response = if (!report.isAuthorized) {
                "Telegram is not connected, sir."
            } else if (report.isSimulated) {
                "[SIMULATED] Sir, you have 3 unread messages from Telegram. Should I open them?"
            } else {
                "I couldn't check Telegram right now, sir."
            }
            appendMessage(ChatMessage(sender = MessageSender.JARVIS, text = response))
            setJarvisState(JarvisState.SPEAKING)
            voiceManager.speak(response)
            return true
        }

        // 3. Both or General message check
        val isGeneralCheck = (lower == "check messages" || lower == "check my messages" ||
                lower == "check notifications" || lower == "any messages" || lower == "any unread messages" ||
                lower == "check unread messages" || lower == "do i have any messages" ||
                lower == "do i have any messages?" || lower == "check my unread messages" ||
                lower.contains("check both") || lower.contains("check all messages"))

        if (isGeneralCheck || (mentionsInstagram && mentionsTelegram && mentionsCheckOrQuery)) {
            val isSim = simulatedTestingMode.value
            val isIgAuth = bgCheckInstagram.value
            val isTgAuth = bgCheckTelegram.value

            val response = if (isSim) {
                "[SIMULATED] Sir, you have 2 unread messages from Instagram and 3 from Telegram. Should I open them?"
            } else {
                val parts = mutableListOf<String>()
                if (!isIgAuth) {
                    parts.add("Instagram is not connected, sir.")
                } else {
                    parts.add("I couldn't check Instagram right now, sir.")
                }

                if (!isTgAuth) {
                    parts.add("Telegram is not connected, sir.")
                } else {
                    parts.add("I couldn't check Telegram right now, sir.")
                }

                parts.joinToString(" ")
            }

            appendMessage(ChatMessage(sender = MessageSender.JARVIS, text = response))
            setJarvisState(JarvisState.SPEAKING)
            voiceManager.speak(response)
            return true
        }

        return false
    }

    private suspend fun handleHomeArrivalIntent(prompt: String, classification: IntentClassificationResult) {
        setJarvisState(JarvisState.THINKING)

        val isIgAuth = bgCheckInstagram.value
        val isTgAuth = bgCheckTelegram.value
        val isWaAuth = bgCheckWhatsApp.value
        val isGmAuth = bgCheckGmail.value
        val isDcAuth = bgCheckDiscord.value
        val unreadCounts = privacyUnreadCounts.value
        val readContents = privacyReadContents.value
        val senderNames = privacySenderNames.value
        val isSim = simulatedTestingMode.value

        val reports = homeArrivalManager.checkAuthorizedServices(
            isInstagramAuthorized = isIgAuth,
            isTelegramAuthorized = isTgAuth,
            isWhatsAppAuthorized = isWaAuth,
            isGmailAuthorized = isGmAuth,
            isDiscordAuthorized = isDcAuth,
            unreadCountsEnabled = unreadCounts,
            readContentsEnabled = readContents,
            senderNamesEnabled = senderNames,
            isSimulatedMode = isSim
        )

        val lowerPrompt = prompt.lowercase()
        val clearlyInstagram = lowerPrompt.contains("instagram") || lowerPrompt.contains("insta")
        val clearlyTelegram = lowerPrompt.contains("telegram") || lowerPrompt.contains("tg")

        // 1. Explicit SIMULATION mode (for demo/testing only):
        if (isSim) {
            val activityEntryText = buildString {
                appendLine("JARVIS // PROTOCOL")
                appendLine("HOME ARRIVAL DETECTED [SIMULATED]")
                appendLine("CHECKING AUTHORIZED SERVICES [SIMULATED]")
                if (isIgAuth) appendLine("INSTAGRAM: 2 UNREAD [SIMULATED]")
                if (isTgAuth) appendLine("TELEGRAM: 3 UNREAD [SIMULATED]")
                append("AWAITING USER CONFIRMATION")
            }
            appendMessage(ChatMessage(sender = MessageSender.JARVIS, text = activityEntryText))

            protocolStatus.value = ProtocolStatus(
                phase = "HOME ARRIVAL DETECTED [SIMULATED]",
                query = "CHECKING AUTHORIZED SERVICES [SIMULATED]",
                sourcesFound = 5,
                details = "INSTAGRAM: 2 UNREAD [SIMULATED]\nTELEGRAM: 3 UNREAD [SIMULATED]\nAWAITING USER CONFIRMATION"
            )

            val previouslyIdentified: MessagingServiceType? = when {
                clearlyInstagram && !clearlyTelegram -> MessagingServiceType.INSTAGRAM
                clearlyTelegram && !clearlyInstagram -> MessagingServiceType.TELEGRAM
                else -> null
            }

            val speechText = if (clearlyInstagram && !clearlyTelegram) {
                "[SIMULATED] Sir, you have 2 unread messages from Instagram. Should I open them?"
            } else if (clearlyTelegram && !clearlyInstagram) {
                "[SIMULATED] Sir, you have 3 unread messages from Telegram. Should I open them?"
            } else {
                "[SIMULATED] Sir, you have 2 unread messages from Instagram and 3 from Telegram. Should I open them?"
            }

            val jarvisMsg = ChatMessage(sender = MessageSender.JARVIS, text = speechText)
            appendMessage(jarvisMsg)
            setJarvisState(JarvisState.SPEAKING)
            voiceManager.speak(speechText)

            pendingHomeArrivalState.value = HomeArrivalPendingState(
                unreadReports = reports,
                previouslyIdentifiedService = previouslyIdentified
            )
            return
        }

        // 2. PRODUCTION MODE: NEVER FABRICATE MESSAGE COUNTS
        // In production, JARVIS must never claim to have unread messages without real verified integration retrieval.
        // If Instagram is not connected: "Instagram is not connected, sir."
        // If Telegram is not connected: "Telegram is not connected, sir."
        // If an integration fails: "I couldn't check Instagram right now, sir."
        val activityEntryText = buildString {
            appendLine("JARVIS // PROTOCOL")
            appendLine("HOME ARRIVAL DETECTED")
            appendLine("CHECKING AUTHORIZED SERVICES")
            appendLine("INSTAGRAM: ${if (isIgAuth) "COULD NOT BE CHECKED" else "NOT CONNECTED"}")
            appendLine("TELEGRAM: ${if (isTgAuth) "COULD NOT BE CHECKED" else "NOT CONNECTED"}")
            append("INTEGRATION STATUS LOGGED")
        }
        appendMessage(ChatMessage(sender = MessageSender.JARVIS, text = activityEntryText))

        protocolStatus.value = ProtocolStatus(
            phase = "HOME ARRIVAL DETECTED",
            query = "CHECKING AUTHORIZED SERVICES",
            sourcesFound = 0,
            details = "INSTAGRAM: ${if (isIgAuth) "COULD NOT BE CHECKED" else "NOT CONNECTED"}\nTELEGRAM: ${if (isTgAuth) "COULD NOT BE CHECKED" else "NOT CONNECTED"}"
        )

        val speechText = if (clearlyInstagram && !clearlyTelegram) {
            if (!isIgAuth) "Instagram is not connected, sir."
            else "I couldn't check Instagram right now, sir."
        } else if (clearlyTelegram && !clearlyInstagram) {
            if (!isTgAuth) "Telegram is not connected, sir."
            else "I couldn't check Telegram right now, sir."
        } else {
            val igPart = if (!isIgAuth) "Instagram is not connected, sir." else "I couldn't check Instagram right now, sir."
            val tgPart = if (!isTgAuth) "Telegram is not connected, sir." else "I couldn't check Telegram right now, sir."
            "Welcome home, Sir. $igPart $tgPart"
        }

        val jarvisMsg = ChatMessage(sender = MessageSender.JARVIS, text = speechText)
        appendMessage(jarvisMsg)
        setJarvisState(JarvisState.SPEAKING)
        voiceManager.speak(speechText)

        // Clear pending state since no unread messages are claimed
        pendingHomeArrivalState.value = null
    }

    private suspend fun handleStudyIntent(prompt: String, classification: IntentClassificationResult) {
        activeTab.value = JarvisTab.STUDY
        setJarvisState(JarvisState.THINKING)
        protocolStatus.value = ProtocolStatus(phase = "ANALYZING", query = prompt, details = "STUDY MODE")

        val studyPrompt = "You are in JARVIS Academic Study Mode. The user requested: \"$prompt\". Provide a pedagogical, step-by-step intuitive breakdown with fundamental principles, a worked example, and a comprehension check. Address the user as '${userTitle.value}'."
        when (val result = geminiClient.generateResponse(
            systemInstruction = buildSystemInstruction(),
            history = emptyList(),
            newPrompt = studyPrompt,
            toolsEnabled = false
        )) {
            is GeminiResult.Success -> {
                protocolStatus.value = null
                val reply = result.text.ifEmpty { "Academic breakdown prepared for \"$prompt\", Sir." }
                val jarvisMsg = ChatMessage(sender = MessageSender.JARVIS, text = reply)
                appendMessage(jarvisMsg)
                setJarvisState(JarvisState.SPEAKING)
                voiceManager.speak(cleanSpokenText(reply))
            }
            is GeminiResult.Error -> {
                protocolStatus.value = null
                val fallback = "JARVIS Study Matrix Engaged, ${userTitle.value}.\n\nPedagogical analysis for \"$prompt\":\n1. Concept: Structured breakdown of fundamental principles.\n2. Methodology: Step-by-step logical derivation and constraint checking.\n3. Interactive practice problem and formulas are now displayed in the Study Tab."
                val jarvisMsg = ChatMessage(sender = MessageSender.JARVIS, text = fallback)
                appendMessage(jarvisMsg)
                setJarvisState(JarvisState.SPEAKING)
                voiceManager.speak("Study Mode engaged, Sir. Review the pedagogical breakdown on screen.")
            }
        }
    }

    private suspend fun handleComputerSearchIntent(prompt: String, classification: IntentClassificationResult) {
        setJarvisState(JarvisState.THINKING)
        val query = classification.extractedQuery.ifEmpty { prompt }
        protocolStatus.value = ProtocolStatus(phase = "RESPONSE READY", query = query, details = "LAUNCHING BROWSER")

        val (success, message) = if (classification.searchEngine == "youtube") {
            toolsManager.openBrowserYouTubeSearch(query)
        } else {
            toolsManager.openBrowserGoogleSearch(query)
        }

        val toolName = if (classification.searchEngine == "youtube") "searchYouTube" else "searchGoogle"
        val toolCard = ToolCallInfo(
            toolName = toolName,
            argumentsJson = "{\"query\":\"$query\"}",
            summary = "Browser Search (${classification.searchEngine.uppercase()})",
            status = if (success) ToolExecutionStatus.COMPLETED else ToolExecutionStatus.FAILED,
            resultMessage = message,
            isSimulated = false
        )

        val reply = if (classification.searchEngine == "youtube") {
            "Opening YouTube in browser to search for '$query', Sir."
        } else {
            "Opening Google search in Chrome browser for '$query', Sir."
        }

        val jarvisMsg = ChatMessage(
            sender = MessageSender.JARVIS,
            text = reply,
            toolCall = toolCard
        )
        appendMessage(jarvisMsg)
        protocolStatus.value = null
        setJarvisState(JarvisState.SPEAKING)
        voiceManager.speak(reply)
    }

    private suspend fun handleOpenApplicationIntent(prompt: String, classification: IntentClassificationResult) {
        setJarvisState(JarvisState.THINKING)
        val app = classification.targetApp.ifEmpty { "chrome" }
        protocolStatus.value = ProtocolStatus(phase = "RESPONSE READY", query = app, details = "LAUNCHING APP")

        val launchResult = appLauncher.launchApp(app)
        val (status, reply) = when (launchResult) {
            is AppLaunchResult.Success -> {
                Pair(ToolExecutionStatus.COMPLETED, launchResult.message)
            }
            is AppLaunchResult.SecureFolderLocked -> {
                Pair(ToolExecutionStatus.DENIED, launchResult.message)
            }
            is AppLaunchResult.NotInstalled -> {
                pendingWebsiteFallbackState.value = PendingWebsiteFallback(
                    appId = launchResult.appId,
                    displayName = launchResult.displayName,
                    websiteUrl = launchResult.websiteUrl
                )
                Pair(ToolExecutionStatus.PENDING_APPROVAL, launchResult.message)
            }
            is AppLaunchResult.NotAllowed -> {
                Pair(ToolExecutionStatus.DENIED, launchResult.message)
            }
            is AppLaunchResult.Failure -> {
                Pair(ToolExecutionStatus.FAILED, launchResult.errorMessage)
            }
        }

        val toolCard = ToolCallInfo(
            toolName = "launchApp",
            argumentsJson = "{\"appId\":\"$app\"}",
            summary = when (launchResult) {
                is AppLaunchResult.Success -> "Launch: ${launchResult.displayName}"
                is AppLaunchResult.SecureFolderLocked -> "Secure Folder: ${launchResult.displayName}"
                is AppLaunchResult.NotInstalled -> "Not Installed: ${launchResult.displayName}"
                is AppLaunchResult.NotAllowed -> "Not Allowed: $app"
                is AppLaunchResult.Failure -> "Launch Failed: ${launchResult.displayName}"
            },
            status = status,
            resultMessage = reply,
            isSimulated = false
        )

        val jarvisMsg = ChatMessage(
            sender = MessageSender.JARVIS,
            text = reply,
            toolCall = toolCard
        )
        appendMessage(jarvisMsg)
        protocolStatus.value = null
        setJarvisState(JarvisState.SPEAKING)
        voiceManager.speak(reply)
    }

    private suspend fun handleInformationRequestIntent(prompt: String, classification: IntentClassificationResult) {
        val lower = prompt.lowercase()

        // 1. LISTENING -> THINKING -> SEARCHING transition
        setJarvisState(JarvisState.SEARCHING)
        protocolStatus.value = ProtocolStatus(
            phase = "SEARCHING WEB",
            query = prompt
        )

        // 2. Real information retrieval based on query semantics (never open Chrome)
        val structuredResult: StructuredSearchResult = when {
            lower.contains("news") || lower.contains("headline") -> {
                toolsManager.getNews(prompt)
            }
            lower.contains("price") || lower.contains("bitcoin") || lower.contains("crypto") ||
                    lower.contains("weather") || lower.contains("temperature") -> {
                toolsManager.getCurrentInformation(prompt)
            }
            else -> {
                toolsManager.searchWeb(prompt)
            }
        }

        // 3. SOURCE RETRIEVAL & ANALYZING transition
        val sourceCount = structuredResult.results.size
        protocolStatus.value = ProtocolStatus(
            phase = "SOURCE RETRIEVAL",
            query = prompt,
            sourcesFound = sourceCount,
            details = "$sourceCount sources found"
        )

        setJarvisState(JarvisState.ANALYZING)
        kotlinx.coroutines.delay(350) // Realistic pulse visual feedback

        protocolStatus.value = ProtocolStatus(
            phase = "ANALYZING",
            query = prompt,
            sourcesFound = sourceCount
        )

        if (structuredResult.results.isEmpty()) {
            protocolStatus.value = null
            // Strict Fallback Requirement (Section 6 & 7):
            // "Live web search is currently unavailable. I can still answer using my existing knowledge, or you can ask me to open Google."
            val fallbackMsg = "Live web search is currently unavailable. I can still answer using my existing knowledge, or you can ask me to open Google."
            val jarvisMsg = ChatMessage(
                sender = MessageSender.JARVIS,
                text = fallbackMsg
            )
            appendMessage(jarvisMsg)
            setJarvisState(JarvisState.SPEAKING)
            voiceManager.speak(fallbackMsg)
            return
        }

        // 4. Synthesize with Gemini or deterministic real synthesis
        val searchDataString = structuredResult.results.joinToString("\n\n") {
            "Title: ${it.title}\nSource: ${it.source}\nURL: ${it.url}\nSnippet: ${it.snippet}"
        }

        val geminiResult = if (geminiClient.hasValidApiKey()) {
            geminiClient.synthesizeFromWebResults(
                systemInstruction = buildSystemInstruction(),
                userPrompt = prompt,
                searchResultsJson = searchDataString
            )
        } else {
            null
        }

        val finalAnswer = if (geminiResult is GeminiResult.Success && geminiResult.text.isNotBlank()) {
            geminiResult.text
        } else {
            buildDirectSynthesis(prompt, structuredResult)
        }

        protocolStatus.value = ProtocolStatus(
            phase = "RESPONSE READY",
            query = prompt,
            sourcesFound = sourceCount
        )

        val toolCard = ToolCallInfo(
            toolName = structuredResult.tool,
            argumentsJson = "{\"query\":\"${structuredResult.query}\"}",
            summary = "Live Information Retrieval ($sourceCount verified sources)",
            status = ToolExecutionStatus.COMPLETED,
            resultMessage = "Retrieved $sourceCount real sources.",
            isSimulated = false
        )

        val jarvisMsg = ChatMessage(
            sender = MessageSender.JARVIS,
            text = finalAnswer,
            toolCall = toolCard
        )
        appendMessage(jarvisMsg)
        setJarvisState(JarvisState.SPEAKING)
        voiceManager.speak(cleanSpokenText(finalAnswer))
    }

    private suspend fun handleConversationIntent(prompt: String, classification: IntentClassificationResult) {
        if (classification.isAmbiguous && classification.clarificationPrompt != null) {
            val jarvisMsg = ChatMessage(
                sender = MessageSender.JARVIS,
                text = classification.clarificationPrompt
            )
            appendMessage(jarvisMsg)
            setJarvisState(JarvisState.SPEAKING)
            voiceManager.speak(classification.clarificationPrompt)
            return
        }

        if (handleDirectMessagingCheck(prompt)) {
            return
        }
        val lower = prompt.lowercase()

        // Time check
        if (lower.contains("time") && (lower.contains("what") || lower.contains("tell") || lower.contains("current"))) {
            val timeText = toolsManager.getCurrentTime()
            val jarvisMsg = ChatMessage(
                sender = MessageSender.JARVIS,
                text = "Current temporal coordinates, Sir:\n$timeText",
                toolCall = ToolCallInfo(
                    toolName = "getCurrentTime",
                    argumentsJson = "{}",
                    summary = "Clock Telemetry",
                    resultMessage = timeText,
                    isSimulated = false
                )
            )
            appendMessage(jarvisMsg)
            setJarvisState(JarvisState.SPEAKING)
            val firstLine = timeText.lines().firstOrNull()?.replace("Current Time: ", "") ?: "displayed on screen"
            voiceManager.speak("The time is $firstLine, Sir.")
            return
        }

        // Core Diagnostics command (automatic testing of all 10 subsystems)
        if (lower.contains("core diagnostics") || lower.contains("run diagnostics") || lower.contains("test subsystems")) {
            startCoreDiagnostics()
            return
        }

        // Diagnostics check
        if (lower.contains("system diagnostics") || lower.contains("battery metrics") || lower.contains("telemetry")) {
            val sysText = toolsManager.getSystemInfo()
            val jarvisMsg = ChatMessage(
                sender = MessageSender.JARVIS,
                text = sysText,
                toolCall = ToolCallInfo(
                    toolName = "getSystemInfo",
                    argumentsJson = "{}",
                    summary = "System Telemetry",
                    resultMessage = sysText,
                    isSimulated = false
                )
            )
            appendMessage(jarvisMsg)
            setJarvisState(JarvisState.SPEAKING)
            voiceManager.speak("System diagnostics nominal, Sir. Telemetry posted to console.")
            return
        }

        // Generative conversation
        setJarvisState(JarvisState.THINKING)
        processPrompt(prompt)
    }

    private fun buildDirectSynthesis(prompt: String, result: StructuredSearchResult): String {
        val sb = StringBuilder()
        val titleUpper = when (result.tool) {
            "getNews" -> "CURRENT NEWS BRIEFING"
            "getCurrentInformation" -> "LIVE TELEMETRY & MARKET BRIEFING"
            else -> "WEB KNOWLEDGE RETRIEVAL"
        }

        sb.append("$titleUpper\n\n")
        sb.append("Here is the latest verified information regarding \"${result.query}\", Sir:\n\n")

        result.results.take(4).forEachIndexed { index, item ->
            sb.append("${index + 1}. **${item.title}** (${item.source})\n")
            sb.append("   ${item.snippet}\n\n")
        }

        sb.append("Sources:\n")
        result.results.take(4).forEach { item ->
            sb.append("• ${item.source} (${item.url})\n")
        }

        return sb.toString().trim()
    }

    private fun cleanSpokenText(text: String): String {
        return VoiceManager.cleanTextForSpeech(text)
    }

    private suspend fun processPrompt(prompt: String) {
        val systemPrompt = buildSystemInstruction()
        val historyTurns = messages.value.takeLast(10).dropLast(1).map {
            TurnMessage(
                role = if (it.sender == MessageSender.USER) "user" else "model",
                text = it.text
            )
        }

        // Call Gemini
        when (val result = geminiClient.generateResponse(
            systemInstruction = systemPrompt,
            history = historyTurns,
            newPrompt = prompt,
            toolsEnabled = true
        )) {
            is GeminiResult.Success -> {
                handleGeminiSuccess(result, prompt)
            }
            is GeminiResult.Error -> {
                handleGeminiFallback(prompt, result.message)
            }
        }
    }

    private suspend fun handleGeminiSuccess(result: GeminiResult.Success, originalPrompt: String) {
        if (result.functionCalls.isNotEmpty()) {
            for (fn in result.functionCalls) {
                executeFunctionCall(fn, result.text, originalPrompt)
            }
        } else {
            val responseText = result.text.ifEmpty { "Command executed as directed, Sir." }
            val jarvisMsg = ChatMessage(
                sender = MessageSender.JARVIS,
                text = responseText
            )
            appendMessage(jarvisMsg)
            setJarvisState(JarvisState.SPEAKING)
            voiceManager.speak(cleanSpokenText(responseText))
        }
    }

    private suspend fun executeFunctionCall(call: RawFunctionCall, accompanyingText: String, originalPrompt: String) {
        val toolName = call.name
        val args = call.arguments
        Log.d(TAG, "Executing tool: $toolName with args: $args")

        // SAFETY ENFORCEMENT (Section 10):
        // Keep computer control separate from information retrieval.
        // Gemini must NEVER use computer control just because a user asks a question.
        if (toolName == "openWebsite" || toolName == "computerControl") {
            val intent = IntentClassifier.classify(originalPrompt).intent
            if (intent != UserIntent.COMPUTER_SEARCH && intent != UserIntent.OPEN_APPLICATION) {
                Log.w(TAG, "Blocked computer control attempt for conversation question '$originalPrompt'")
                // Redirect to information retrieval
                handleInformationRequestIntent(originalPrompt, IntentClassificationResult(UserIntent.INFORMATION_REQUEST, originalPrompt))
                return
            }
        }

        if (toolName == "searchWeb" || toolName == "getNews" || toolName == "getCurrentInformation") {
            val query = args["query"]?.toString() ?: originalPrompt
            handleInformationRequestIntent(query, IntentClassificationResult(UserIntent.INFORMATION_REQUEST, query))
            return
        }

        val isDangerous = (toolName == "computerControl" && toolsManager.isDangerousAction(args["action"]?.toString() ?: ""))

        if (isDangerous && toolsManager.requireConfirmationForDangerousActions) {
            val pending = ToolCallInfo(
                toolName = toolName,
                argumentsJson = args.toString(),
                summary = "Requesting approval to execute '$toolName' with parameters: $args",
                status = ToolExecutionStatus.PENDING_APPROVAL,
                requiresConfirmation = true
            )
            pendingConfirmation.value = pending
            setJarvisState(JarvisState.STANDBY)
            return
        }

        // Execute tool directly
        val executionResult = runTool(toolName, args)

        val toolCard = ToolCallInfo(
            toolName = toolName,
            argumentsJson = args.toString(),
            summary = "Action: $toolName",
            status = ToolExecutionStatus.COMPLETED,
            resultMessage = executionResult,
            isSimulated = false
        )

        val replyText = if (accompanyingText.isNotBlank()) {
            "$accompanyingText\n\n$executionResult"
        } else {
            executionResult
        }

        val jarvisMsg = ChatMessage(
            sender = MessageSender.JARVIS,
            text = replyText,
            toolCall = toolCard
        )
        appendMessage(jarvisMsg)
        setJarvisState(JarvisState.SPEAKING)
        voiceManager.speak(cleanSpokenText(replyText))
    }

    private suspend fun runTool(toolName: String, args: Map<String, Any?>): String {
        return when (toolName) {
            "getCurrentTime" -> toolsManager.getCurrentTime()
            "getSystemInfo" -> toolsManager.getSystemInfo()
            "launchApp", "openApp" -> {
                val appId = (args["appId"] ?: args["app"] ?: args["target"])?.toString() ?: ""
                val meta = appLauncher.getAppMetadata(appId)
                if (meta == null) {
                    "Security Alert: Application '$appId' is not permitted by JARVIS security protocol. Arbitrary launches are prohibited."
                } else {
                    when (val res = appLauncher.launchApp(appId)) {
                        is AppLaunchResult.Success -> res.message
                        is AppLaunchResult.SecureFolderLocked -> res.message
                        is AppLaunchResult.NotInstalled -> res.message
                        is AppLaunchResult.NotAllowed -> res.message
                        is AppLaunchResult.Failure -> res.errorMessage
                    }
                }
            }
            "openWebsite" -> {
                val url = args["url"]?.toString() ?: "https://google.com"
                val lowerUrl = url.lowercase()
                val matchedApp = when {
                    lowerUrl.contains("instagram.com") -> "instagram"
                    lowerUrl.contains("telegram.org") || lowerUrl.contains("t.me") -> "telegram"
                    lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") -> "youtube"
                    lowerUrl.contains("spotify.com") -> "spotify"
                    lowerUrl.contains("whatsapp.com") -> "whatsapp"
                    lowerUrl.contains("discord.com") -> "discord"
                    else -> null
                }
                if (matchedApp != null && appLauncher.isAppInstalled(matchedApp)) {
                    when (val res = appLauncher.launchApp(matchedApp)) {
                        is AppLaunchResult.Success -> res.message
                        else -> {
                            val (_, msg) = toolsManager.openWebsite(url)
                            msg
                        }
                    }
                } else {
                    val (_, msg) = toolsManager.openWebsite(url)
                    msg
                }
            }
            "createNote" -> {
                val title = args["title"]?.toString() ?: "Note"
                val content = args["content"]?.toString() ?: ""
                toolsManager.createNote(title, content)
            }
            "saveMemory" -> {
                val key = args["key"]?.toString() ?: "info"
                val content = args["content"]?.toString() ?: ""
                val cat = args["category"]?.toString() ?: "general"
                toolsManager.saveMemory(key, content, cat)
            }
            "getMemory" -> {
                val query = args["query"]?.toString() ?: ""
                toolsManager.getMemory(query)
            }
            "computerControl" -> {
                val action = args["action"]?.toString() ?: "open_app"
                val target = args["target"]?.toString() ?: ""
                if (action == "shell" || action == "cmd" || action == "exec" || action == "run_command" || action == "intent") {
                    "Security Alert: Execution of arbitrary shell commands or raw intents is strictly prohibited."
                } else if (action == "open_app") {
                    val meta = appLauncher.getAppMetadata(target)
                    if (meta == null) {
                        "Security Alert: Application '$target' is not permitted in the JARVIS allowlist."
                    } else {
                        when (val res = appLauncher.launchApp(target)) {
                            is AppLaunchResult.Success -> res.message
                            is AppLaunchResult.SecureFolderLocked -> res.message
                            is AppLaunchResult.NotInstalled -> res.message
                            is AppLaunchResult.NotAllowed -> res.message
                            is AppLaunchResult.Failure -> res.errorMessage
                        }
                    }
                } else {
                    val (_, msg) = toolsManager.executeComputerAction(action, target)
                    msg
                }
            }
            else -> "Tool '$toolName' executed successfully."
        }
    }

    fun confirmPendingTool(approved: Boolean) {
        val pending = pendingConfirmation.value ?: return
        pendingConfirmation.value = null

        viewModelScope.launch {
            if (approved) {
                val result = runTool(pending.toolName, emptyMap())
                val jarvisMsg = ChatMessage(
                    sender = MessageSender.JARVIS,
                    text = "Action confirmed and executed, Sir.\n$result",
                    toolCall = pending.copy(status = ToolExecutionStatus.COMPLETED, resultMessage = result)
                )
                appendMessage(jarvisMsg)
                voiceManager.speak("Action confirmed and executed, Sir.")
            } else {
                val jarvisMsg = ChatMessage(
                    sender = MessageSender.JARVIS,
                    text = "Action cancelled as instructed, Sir.",
                    toolCall = pending.copy(status = ToolExecutionStatus.DENIED, resultMessage = "User declined execution.")
                )
                appendMessage(jarvisMsg)
                voiceManager.speak("Understood, Sir. Action aborted.")
            }
            setJarvisState(JarvisState.STANDBY)
        }
    }

    private suspend fun handleGeminiFallback(prompt: String, errorMessage: String) {
        Log.w(TAG, "Gemini fallback triggered: $errorMessage")
        val lower = prompt.lowercase()
        val classification = IntentClassifier.classify(prompt)

        // Strict fallback logic: NEVER open Chrome for informational questions!
        when (classification.intent) {
            UserIntent.STOP_SPEECH -> {
                handleStopSpeechIntent()
            }
            UserIntent.QUICK_DEVICE_ACTION -> {
                handleQuickDeviceActionIntent(prompt, classification)
            }
            UserIntent.HOME_ARRIVAL -> {
                handleHomeArrivalIntent(prompt, classification)
            }
            UserIntent.INFORMATION_REQUEST -> {
                handleInformationRequestIntent(prompt, classification)
            }
            UserIntent.COMPUTER_SEARCH -> {
                handleComputerSearchIntent(prompt, classification)
            }
            UserIntent.OPEN_APPLICATION -> {
                handleOpenApplicationIntent(prompt, classification)
            }
            UserIntent.STUDY -> {
                handleStudyIntent(prompt, classification)
            }
            UserIntent.CONVERSATION -> {
                val (fallbackText, toolInfo) = when {
                    lower.contains("time") || lower.contains("date") || lower.contains("clock") -> {
                        val time = toolsManager.getCurrentTime()
                        Pair(
                            "According to local atomic telemetry, Sir:\n$time",
                            ToolCallInfo(toolName = "getCurrentTime", argumentsJson = "{}", summary = "Clock Telemetry", resultMessage = time, isSimulated = false)
                        )
                    }
                    lower.contains("system") || lower.contains("battery") || lower.contains("diagnostic") || lower.contains("ram") -> {
                        val info = toolsManager.getSystemInfo()
                        Pair(
                            "Subsystem diagnostic report, Sir:\n$info",
                            ToolCallInfo(toolName = "getSystemInfo", argumentsJson = "{}", summary = "System Metrics", resultMessage = info, isSimulated = false)
                        )
                    }
                    lower.contains("note") || lower.contains("remember to") -> {
                        val noteTitle = "Task from Jarvis"
                        toolsManager.createNote(noteTitle, prompt)
                        Pair("I have stored this in your persistent memory notes, Sir: '$prompt'", null)
                    }
                    lower.startsWith("switch to telugu") || lower.startsWith("change language to telugu") || lower.startsWith("speak in telugu") || lower == "telugu" -> {
                        setLanguage(LanguageMode.TELUGU)
                        Pair("సరే సార్! నేను ఇకపై తెలుగులో సంభాషిస్తాను. మీకు ఎలా సహాయపడగలను?", null)
                    }
                    lower.startsWith("switch to english") || lower.startsWith("change language to english") || lower.startsWith("speak in english") || lower == "english" -> {
                        setLanguage(LanguageMode.ENGLISH)
                        Pair("Understood, Sir. Spoken language switched to English (India).", null)
                    }
                    else -> {
                        val title = userTitle.value
                        if (networkManager.isOnline.value) {
                            Pair("Live web search is currently unavailable. I can still answer using my existing knowledge, or you can ask me to open Google.", null)
                        } else {
                            Pair("Alert, $title: No internet connectivity detected on device. Please check your Wi-Fi, Ethernet, or mobile data connection.", null)
                        }
                    }
                }

                val jarvisMsg = ChatMessage(
                    sender = MessageSender.JARVIS,
                    text = fallbackText,
                    toolCall = toolInfo
                )
                appendMessage(jarvisMsg)
                setJarvisState(JarvisState.SPEAKING)
                voiceManager.speak(cleanSpokenText(fallbackText))
            }
        }
    }

    private suspend fun buildSystemInstruction(): String = withContext(Dispatchers.IO) {
        val title = userTitle.value
        val memoriesStr = if (isMemoryInjectionEnabled.value) {
            val list = database.memoryDao().getAllMemories()
            val memText = if (list.isNotEmpty()) {
                "\n[SAVED USER MEMORIES & PREFERENCES]:\n" + list.joinToString("\n") {
                    "- ${it.key}: ${it.content} (${it.category})"
                }
            } else ""

            val historyContext = conversationRepository.getConversationContextForMemory(8)
            val historyText = if (historyContext.isNotBlank()) {
                "\n[PERSISTENT CONVERSATIONAL MEMORY]:\n$historyContext"
            } else ""

            memText + historyText
        } else ""

        val languageGuide = when (languageMode.value) {
            LanguageMode.ENGLISH -> "Respond strictly in crisp, refined, and intelligent English."
            LanguageMode.TELUGU -> "Respond strictly in polite, fluent, natural Telugu (తెలుగు లిపి). Call the user సార్ (Sir)."
            LanguageMode.TANGLISH -> "Respond in natural Tanglish (Telugu + English mixed, using English alphabet/Telugu phrases, e.g., 'Sir, system status is completely normal. Mee kosam em cheyyali?')."
        }

        val styleGuide = when (responseStyle.value) {
            ResponseStyle.CONCISE_JARVIS -> "Keep spoken responses brief, razor-sharp, aristocratic, witty, and immediately actionable."
            ResponseStyle.DETAILED_ENGINEER -> "Provide comprehensive, deeply technical, structured breakdowns with bullet points."
            ResponseStyle.ACADEMIC_TUTOR -> "Provide intuitive, pedagogical explanations with step-by-step logic."
        }

        """
You are JARVIS (Just A Rather Very Intelligent System), the premier personal AI assistant built with Gemini intelligence.
Address the user respectfully as '$title'.
$languageGuide
$styleGuide

You have access to native Android and computer companion tools:
- getCurrentTime()
- getSystemInfo()
- openWebsite(url)
- searchWeb(query)
- createNote(title, content)
- saveMemory(key, content, category)
- getMemory(query)
- computerControl(action, target)

Always invoke the appropriate tool when user queries require real-time information, actions, or memory persistence.
$memoriesStr
        """.trimIndent()
    }

    // Memory operations
    fun addMemory(key: String, content: String, category: String = "general") {
        viewModelScope.launch(Dispatchers.IO) {
            database.memoryDao().insertMemory(
                MemoryEntity(key = key.trim(), content = content.trim(), category = category)
            )
            snackbarMessage.value = "Memory '$key' saved to matrix."
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            database.memoryDao().deleteMemoryById(id)
            snackbarMessage.value = "Memory record deleted."
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch(Dispatchers.IO) {
            database.memoryDao().clearAll()
            snackbarMessage.value = "All memories purged."
        }
    }

    // Notes operations
    fun addNote(title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.noteDao().insertNote(
                NoteEntity(title = title.trim(), content = content.trim())
            )
            snackbarMessage.value = "Note created."
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            database.noteDao().deleteNoteById(id)
            snackbarMessage.value = "Note deleted."
        }
    }

    fun clearConversation() {
        viewModelScope.launch {
            conversationRepository.clearHistory()
            val greeting = when (languageMode.value) {
                LanguageMode.ENGLISH -> "Systems online, Sir. Neural matrix initialized and ready for your command."
                LanguageMode.TELUGU -> "నమస్కారం సార్! జార్విస్ సిస్టమ్స్ ఆన్‌లైన్‌లో ఉన్నాయి. మీకు ఎలా సహాయపడగలను?"
                LanguageMode.TANGLISH -> "Systems ready, Sir! Jarvis fully online ga unnadu. Today em commands execute cheyyali?"
            }
            appendMessage(ChatMessage(sender = MessageSender.JARVIS, text = greeting))
            snackbarMessage.value = "Conversation history cleared from Room database."
        }
    }

    // Study Mode operations
    fun answerQuiz(optionIndex: Int) {
        selectedQuizAnswer.value = optionIndex
        showQuizExplanation.value = true
    }

    fun nextQuizQuestion() {
        selectedQuizAnswer.value = null
        showQuizExplanation.value = false
        val questions = StudyManager.sampleQuestions
        val currentIndex = questions.indexOf(currentQuizQuestion.value)
        val nextIndex = (currentIndex + 1) % questions.size
        currentQuizQuestion.value = questions[nextIndex]
    }

    fun askStudyConcept(subject: String, topic: String) {
        activeTab.value = JarvisTab.CORE
        val prompt = StudyManager.createStudyPrompt(subject, topic, studyInTelugu.value)
        sendMessage(prompt)
    }

    fun solveMathProblem(problem: String) {
        activeTab.value = JarvisTab.CORE
        val prompt = StudyManager.createMathSolverPrompt(problem, studyInTelugu.value)
        sendMessage(prompt)
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}

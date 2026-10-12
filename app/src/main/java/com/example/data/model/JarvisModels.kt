package com.example.data.model

import java.util.UUID

enum class JarvisState {
    STANDBY,
    LISTENING,
    PROCESSING,
    THINKING,
    SEARCHING,
    ANALYZING,
    OPENING_APP,
    SPEAKING
}

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

enum class UserIntent {
    STOP_SPEECH,
    QUICK_DEVICE_ACTION,
    INFORMATION_REQUEST,
    COMPUTER_SEARCH,
    OPEN_APPLICATION,
    CONVERSATION,
    STUDY,
    HOME_ARRIVAL
}

data class VoiceDiagnosticsInfo(
    val selectedEnginePackage: String = "Default Android Recognizer",
    val requestedLocale: String = "en-IN",
    val isEngineAvailable: Boolean = true,
    val actualRecognitionStatus: String = "Ready",
    val lastRawTranscript: String = "",
    val lastNormalizedTranscript: String = "",
    val lastConfidenceScore: Float = 1.0f,
    val lastErrorDetails: String? = null,
    val isLanguageSupportedOnDevice: Boolean = true,
    val isLiveModeActive: Boolean = false,
    val audioLevel: Float = 0f
)

data class VoiceLatencyMetrics(
    val speechRecognitionMs: Long = 0L,
    val intentClassificationMs: Long = 0L,
    val executionMs: Long = 0L,
    val ttsStartupMs: Long = 0L,
    val totalLatencyMs: Long = 0L,
    val lastActionDescription: String = "None"
)

enum class MessagingServiceType(
    val id: String,
    val displayName: String,
    val packageName: String,
    val webFallbackUrl: String,
    val isFuture: Boolean = false
) {
    INSTAGRAM("instagram", "Instagram", "com.instagram.android", "https://instagram.com/direct/inbox/"),
    TELEGRAM("telegram", "Telegram", "org.telegram.messenger", "https://web.telegram.org/"),
    WHATSAPP("whatsapp", "WhatsApp", "com.whatsapp", "https://web.whatsapp.com/", isFuture = true),
    GMAIL("gmail", "Gmail", "com.google.android.gm", "https://mail.google.com/", isFuture = true),
    DISCORD("discord", "Discord", "com.discord", "https://discord.com/app", isFuture = true)
}

data class ServiceUnreadReport(
    val service: MessagingServiceType,
    val isAuthorized: Boolean,
    val isAvailable: Boolean,
    val unreadCount: Int = 0,
    val unavailableReason: String? = null,
    val isSimulated: Boolean = false
)

data class HomeArrivalPendingState(
    val unreadReports: List<ServiceUnreadReport>,
    val awaitingServiceSelection: Boolean = false,
    val previouslyIdentifiedService: MessagingServiceType? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class PendingWebsiteFallback(
    val appId: String,
    val displayName: String,
    val websiteUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SearchItem(
    val title: String,
    val url: String,
    val snippet: String,
    val source: String
)

data class StructuredSearchResult(
    val query: String,
    val tool: String = "searchWeb",
    val results: List<SearchItem> = emptyList(),
    val error: String? = null
)

enum class MessageSender {
    USER,
    JARVIS,
    SYSTEM,
    TOOL
}

enum class ToolExecutionStatus {
    PENDING_APPROVAL,
    EXECUTING,
    COMPLETED,
    DENIED,
    FAILED
}

data class ToolCallInfo(
    val callId: String = UUID.randomUUID().toString(),
    val toolName: String,
    val argumentsJson: String,
    val summary: String,
    val status: ToolExecutionStatus = ToolExecutionStatus.COMPLETED,
    val resultMessage: String = "",
    val isSimulated: Boolean = false,
    val requiresConfirmation: Boolean = false
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCall: ToolCallInfo? = null,
    val isStreaming: Boolean = false,
    val language: String = "English"
)

enum class LanguageMode(val code: String, val displayName: String, val ttsLocale: String) {
    ENGLISH("en", "English (India)", "en-IN"),
    TELUGU("te", "Telugu (India)", "te-IN"),
    TANGLISH("te-en", "Tanglish (Telugu + English)", "te-IN");

    fun getLocale(): java.util.Locale = when (this) {
        ENGLISH -> java.util.Locale("en", "IN")
        TELUGU -> java.util.Locale("te", "IN")
        TANGLISH -> java.util.Locale("te", "IN")
    }
}

enum class ResponseStyle(val displayName: String, val description: String) {
    CONCISE_JARVIS("Crisp Jarvis", "Concise, aristocratic, witty, high-tech assistant tone"),
    DETAILED_ENGINEER("Technical Analyst", "In-depth, structured, architectural answers"),
    ACADEMIC_TUTOR("Academic Tutor", "Pedagogical, step-by-step explanations with analogies")
}

data class PracticeQuestion(
    val id: String = UUID.randomUUID().toString(),
    val subject: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanationEn: String,
    val explanationTe: String
)

data class ComputerToolAction(
    val id: String = UUID.randomUUID().toString(),
    val actionType: String,
    val target: String,
    val title: String,
    val description: String,
    val isConsequential: Boolean = false,
    val simulatedOutput: String = ""
)

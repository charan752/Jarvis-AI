package com.example.tools

import com.example.data.model.UserIntent

data class IntentClassificationResult(
    val intent: UserIntent,
    val extractedQuery: String = "",
    val targetApp: String = "",
    val searchEngine: String = "google", // "google" or "youtube"
    val isAmbiguous: Boolean = false,
    val clarificationPrompt: String? = null
)

object IntentClassifier {

    private val COMPUTER_APPS = listOf(
        "instagram", "insta", "telegram", "tg", "whatsapp", "gmail", "email", "mail", "discord",
        "chrome", "google chrome", "browser",
        "youtube", "yt", "notepad", "notes", "keep", "vscode", "visual studio code",
        "spotify", "terminal", "calculator", "calc", "maps", "explorer"
    )

    // Fast command-alias dictionary for O(1) in-memory resolution of common commands
    private val STOP_ALIASES = setOf(
        "stop", "stop speaking", "be quiet", "quiet", "cancel", "shut up", "hush",
        "stop listening", "halt", "pause", "quiet jarvis", "stop jarvis", "jarvis stop",
        "silence", "stop it", "nevermind", "dismiss"
    )

    private val APP_ALIASES = mapOf(
        // Instagram
        "open instagram" to "instagram", "launch instagram" to "instagram", "start instagram" to "instagram",
        "open insta" to "instagram", "launch insta" to "instagram", "start insta" to "instagram",
        "instagram" to "instagram",

        // YouTube
        "open youtube" to "youtube", "launch youtube" to "youtube", "start youtube" to "youtube",
        "open yt" to "youtube", "launch yt" to "youtube", "start yt" to "youtube",
        "youtube" to "youtube",

        // Chrome
        "open chrome" to "chrome", "launch chrome" to "chrome", "start chrome" to "chrome",
        "open google chrome" to "chrome", "launch google chrome" to "chrome",
        "open browser" to "chrome", "launch browser" to "chrome",

        // Telegram
        "open telegram" to "telegram", "launch telegram" to "telegram", "start telegram" to "telegram",
        "open tg" to "telegram", "launch tg" to "telegram",

        // Spotify
        "open spotify" to "spotify", "launch spotify" to "spotify", "start spotify" to "spotify",
        "play music" to "spotify", "open music" to "spotify",

        // WhatsApp
        "open whatsapp" to "whatsapp", "launch whatsapp" to "whatsapp", "start whatsapp" to "whatsapp",
        "open wa" to "whatsapp",

        // Gmail
        "open gmail" to "gmail", "launch gmail" to "gmail", "start gmail" to "gmail",
        "open email" to "gmail", "open mail" to "gmail",

        // Discord
        "open discord" to "discord", "launch discord" to "discord", "start discord" to "discord",

        // Calculator
        "open calculator" to "calculator", "launch calculator" to "calculator", "start calculator" to "calculator",
        "open calc" to "calculator", "launch calc" to "calculator",

        // Notepad
        "open notepad" to "notepad", "launch notepad" to "notepad", "open notes" to "notepad",
        "open keep" to "notepad", "launch keep" to "notepad",

        // Maps
        "open maps" to "maps", "launch maps" to "maps", "open google maps" to "maps", "open navigation" to "maps"
    )

    private val QUICK_UTILITY_ALIASES = mapOf(
        "system status" to "system",
        "system info" to "system",
        "battery status" to "system",
        "battery info" to "system",
        "diagnostics" to "diagnostics"
    )

    /**
     * Normalizes raw spoken or typed prompt for command matching:
     * - Strips conversational prefixes ("jarvis", "please", "can you")
     * - Strips speech disfluencies and fillers ("uh", "um", "ah")
     * - Cleans punctuation, trailing polite suffixes, and extra whitespace
     */
    fun normalizePrompt(rawPrompt: String): String {
        return rawPrompt.lowercase()
            .replace(Regex("[.,!?;:\"'’]"), "")
            .replace(Regex("^(?:uh\\s+|um\\s+|ah\\s+|er\\s+|hmm\\s+|hey\\s+jarvis\\s+|ok\\s+jarvis\\s+|jarvis\\s+|please\\s+|can\\s+you\\s+|could\\s+you\\s+|would\\s+you\\s+)+"), "")
            .replace(Regex("\\s+(?:please|thanks|thank you)$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun classify(rawPrompt: String, confidence: Float = 1.0f): IntentClassificationResult {
        val trimmed = rawPrompt.trim()
        val normalized = normalizePrompt(trimmed)
        val lower = trimmed.lowercase()

        // -1. Confidence check: If transcript is uncertain (< 0.38f), ask clarification instead of guessing
        if (confidence in 0.001f..0.38f && trimmed.isNotEmpty()) {
            return IntentClassificationResult(
                intent = UserIntent.CONVERSATION,
                extractedQuery = trimmed,
                isAmbiguous = true,
                clarificationPrompt = "I didn't catch that clearly, Sir. Could you please repeat?"
            )
        }

        // -0. Incomplete command check: e.g. "open" or "launch" without target app
        if (normalized in setOf("open", "launch", "start", "run", "open app", "open the")) {
            return IntentClassificationResult(
                intent = UserIntent.CONVERSATION,
                extractedQuery = trimmed,
                isAmbiguous = true,
                clarificationPrompt = "Which application would you like me to open, Sir?"
            )
        }

        // 0. Instant STOP check (< 0.1ms)
        if (STOP_ALIASES.contains(normalized) || isStopPhrase(normalized)) {
            return IntentClassificationResult(
                intent = UserIntent.STOP_SPEECH,
                extractedQuery = trimmed
            )
        }

        // 1. Fast Command-Alias Dictionary Match (< 0.2ms)
        val directApp = APP_ALIASES[normalized]
        if (directApp != null) {
            return IntentClassificationResult(
                intent = UserIntent.OPEN_APPLICATION,
                targetApp = directApp,
                extractedQuery = trimmed
            )
        }

        val directUtility = QUICK_UTILITY_ALIASES[normalized]
        if (directUtility != null) {
            return IntentClassificationResult(
                intent = UserIntent.QUICK_DEVICE_ACTION,
                targetApp = directUtility,
                extractedQuery = trimmed
            )
        }

        // 2. Check for HOME_ARRIVAL intent
        if (isHomeArrivalRequest(lower) || isHomeArrivalRequest(normalized)) {
            return IntentClassificationResult(
                intent = UserIntent.HOME_ARRIVAL,
                extractedQuery = trimmed
            )
        }

        // 3. Check for STUDY intent
        // E.g. "Explain photosynthesis", "Teach me calculus", "Solve quadratic equation"
        if (isStudyRequest(lower) || isStudyRequest(normalized)) {
            return IntentClassificationResult(
                intent = UserIntent.STUDY,
                extractedQuery = trimmed
            )
        }

        // 4. Check for explicit COMPUTER_SEARCH
        // E.g. "Search Google for ...", "Open Google and search ...", "Search YouTube for ..."
        val youtubeSearchMatch = Regex("(?i)^(?:search\\s+youtube\\s+(?:for\\s+)?|open\\s+youtube\\s+and\\s+search\\s+(?:for\\s+)?)(.+)$").find(trimmed)
        if (youtubeSearchMatch != null) {
            val q = youtubeSearchMatch.groupValues[1].trim()
            return IntentClassificationResult(
                intent = UserIntent.COMPUTER_SEARCH,
                extractedQuery = q,
                searchEngine = "youtube",
                targetApp = "youtube"
            )
        }

        val googleSearchMatch = Regex("(?i)^(?:search\\s+google\\s+(?:for\\s+)?|open\\s+google\\s+and\\s+search\\s+(?:for\\s+)?)(.+)$").find(trimmed)
        if (googleSearchMatch != null) {
            val q = googleSearchMatch.groupValues[1].trim()
            return IntentClassificationResult(
                intent = UserIntent.COMPUTER_SEARCH,
                extractedQuery = q,
                searchEngine = "google",
                targetApp = "chrome"
            )
        }

        if (lower.startsWith("search google ") || lower.startsWith("search on google ") || lower.startsWith("google ")) {
            val q = trimmed.replace(Regex("(?i)^(?:search\\s+(?:on\\s+)?google\\s+(?:for\\s+)?|google\\s+)"), "").trim()
            return IntentClassificationResult(
                intent = UserIntent.COMPUTER_SEARCH,
                extractedQuery = q,
                searchEngine = "google",
                targetApp = "chrome"
            )
        }

        if (lower.startsWith("search youtube ") || lower.startsWith("search on youtube ")) {
            val q = trimmed.replace(Regex("(?i)^search\\s+(?:on\\s+)?youtube\\s+(?:for\\s+)?"), "").trim()
            return IntentClassificationResult(
                intent = UserIntent.COMPUTER_SEARCH,
                extractedQuery = q,
                searchEngine = "youtube",
                targetApp = "youtube"
            )
        }

        // 5. Check for regex OPEN_APPLICATION variations
        // E.g. "Open Chrome.", "Open YouTube.", "Open Notepad.", "Launch Spotify", "Start Calculator"
        val openAppMatch = Regex("(?i)^(?:jarvis[,\\s]+|please[,\\s]+|can you\\s+|could you\\s+)?(?:open|launch|start|run)\\s+(?:the\\s+)?([a-zA-Z0-9_\\s]+?)(?:\\s+app|\\s+application)?(?:\\.|\\!)?$").find(trimmed)
        if (openAppMatch != null) {
            val candidate = openAppMatch.groupValues[1].trim().lowercase()
            if (!candidate.contains("search") && !candidate.contains("question") && !candidate.contains("topic")) {
                val matchedApp = COMPUTER_APPS.find { candidate == it || candidate.startsWith(it) }
                if (matchedApp != null) {
                    val canonicalName = when {
                        matchedApp.contains("instagram") || matchedApp == "insta" -> "instagram"
                        matchedApp.contains("telegram") || matchedApp == "tg" -> "telegram"
                        matchedApp.contains("youtube") || matchedApp == "yt" -> "youtube"
                        matchedApp.contains("chrome") || matchedApp.contains("browser") -> "chrome"
                        matchedApp.contains("spotify") -> "spotify"
                        matchedApp.contains("whatsapp") -> "whatsapp"
                        matchedApp.contains("gmail") || matchedApp.contains("email") || matchedApp == "mail" -> "gmail"
                        matchedApp.contains("discord") -> "discord"
                        matchedApp.contains("calculator") || matchedApp == "calc" -> "calculator"
                        matchedApp.contains("notepad") || matchedApp.contains("note") || matchedApp.contains("keep") -> "notepad"
                        matchedApp.contains("maps") -> "maps"
                        matchedApp.contains("vscode") -> "vscode"
                        matchedApp.contains("terminal") -> "terminal"
                        matchedApp.contains("explorer") -> "explorer"
                        else -> matchedApp
                    }
                    return IntentClassificationResult(
                        intent = UserIntent.OPEN_APPLICATION,
                        targetApp = canonicalName,
                        extractedQuery = trimmed
                    )
                }
            }
        }

        // 6. Check for INFORMATION_REQUEST
        // Rule: Never open Chrome for ordinary information questions.
        // Instead: INFORMATION_REQUEST -> Real web information service -> Analyze & synthesize.
        if (isInformationRequest(lower) || isInformationRequest(normalized)) {
            return IntentClassificationResult(
                intent = UserIntent.INFORMATION_REQUEST,
                extractedQuery = trimmed
            )
        }

        // 7. Default to CONVERSATION (Reasoning / Dialogue with Gemini)
        return IntentClassificationResult(
            intent = UserIntent.CONVERSATION,
            extractedQuery = trimmed
        )
    }

    private fun isStopPhrase(normalized: String): Boolean {
        return normalized == "stop" ||
                normalized.startsWith("stop ") ||
                normalized.endsWith(" stop") ||
                normalized.contains("stop speaking") ||
                normalized.contains("be quiet") ||
                normalized == "cancel"
    }

    private fun isStudyRequest(lower: String): Boolean {
        if (lower.startsWith("explain ") ||
            lower.startsWith("teach me ") ||
            lower.startsWith("solve ") ||
            lower.contains("quadratic equation") ||
            lower.contains("maths problem") ||
            lower.contains("math problem") ||
            lower.contains("study mode") ||
            lower.contains("physics question") ||
            lower.contains("chemistry concept")
        ) {
            return true
        }
        return false
    }

    private fun isInformationRequest(lower: String): Boolean {
        // Explicit informational signals
        val newsKeywords = listOf("news", "headline", "breaking news", "ai news", "tech news")
        val currentKeywords = listOf("current", "latest", "today", "recent", "what's the latest", "what is the latest")
        val liveDataKeywords = listOf("price of", "bitcoin", "weather", "temperature", "who won", "score of", "stock price", "market")

        val hasNews = newsKeywords.any { lower.contains(it) }
        val hasCurrent = currentKeywords.any { lower.contains(it) }
        val hasLiveData = liveDataKeywords.any { lower.contains(it) }

        // Question starters about real world information
        val isInfoQuestion = lower.startsWith("what is the current") ||
                lower.startsWith("what's the current") ||
                lower.startsWith("what is the latest") ||
                lower.startsWith("what's the latest") ||
                lower.startsWith("who won") ||
                lower.startsWith("what happened in") ||
                lower.startsWith("what happened today") ||
                lower.startsWith("what's the weather") ||
                lower.startsWith("what is the weather") ||
                lower.startsWith("what is the price") ||
                lower.startsWith("what's the price") ||
                lower.startsWith("search the web for") ||
                lower.startsWith("search web for") ||
                lower.startsWith("get news on") ||
                lower.startsWith("get the news")

        return hasNews || (hasCurrent && (lower.contains("what") || lower.contains("who") || lower.contains("update") || lower.contains("happened"))) || hasLiveData || isInfoQuestion
    }

    private fun isHomeArrivalRequest(lower: String): Boolean {
        val clean = lower.replace("'", "")
            .replace("’", "")
            .replace(".", "")
            .replace("!", "")
            .replace(",", "")
            .replace("?", "")
            .trim()

        val exactMatches = setOf(
            "im back home", "i am back home",
            "im home", "i am home",
            "i reached home", "reached home",
            "ive reached home", "i have reached home",
            "im back", "i am back",
            "arrived home", "i arrived home",
            "just arrived home", "i just arrived home",
            "just reached home", "i just reached home",
            "just got home", "i just got home",
            "back home", "finally home", "im finally home"
        )
        if (exactMatches.contains(clean)) return true

        val arrivalPrefixes = listOf(
            "im back home", "i am back home",
            "im home", "i am home",
            "i reached home", "reached home",
            "ive reached home", "i have reached home",
            "im back", "i am back",
            "arrived home", "i arrived home"
        )
        return arrivalPrefixes.any { clean.startsWith(it) }
    }
}

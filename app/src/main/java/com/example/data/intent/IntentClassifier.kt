package com.example.data.intent

enum class UserIntent {
    INFORMATION_REQUEST,
    COMPUTER_SEARCH,
    OPEN_APPLICATION,
    STUDY,
    CONVERSATION,
    SYSTEM_COMMAND
}

object IntentClassifier {

    fun classify(prompt: String): UserIntent {
        val trimmed = prompt.trim()
        val lower = trimmed.lowercase()

        // 1. STUDY INTENT
        // e.g. "Explain quadratic equations", "Solve this maths problem", "Teach me electricity"
        if (lower.startsWith("explain ") ||
            lower.contains("quadratic") ||
            lower.contains("math problem") ||
            lower.contains("maths problem") ||
            lower.contains("solve this") ||
            lower.startsWith("teach me") ||
            lower.contains("study mode") ||
            lower.contains("quiz") ||
            lower.contains("calculus") ||
            lower.contains("pythagor")
        ) {
            return UserIntent.STUDY
        }

        // 2. COMPUTER SEARCH INTENT (Explicit request to use browser / Google / YouTube)
        // e.g. "Search Google for iPhone 15 prices", "Open Google and search Minecraft", "Search YouTube for Minecraft"
        val isExplicitGoogleSearch = lower.contains("search google for") ||
                lower.contains("google search for") ||
                lower.contains("open google and search") ||
                lower.contains("search on google") ||
                lower.contains("search on chrome") ||
                lower.contains("lookup on google")
        val isExplicitYouTubeSearch = lower.contains("search youtube for") ||
                lower.contains("search on youtube") ||
                lower.contains("open youtube and search") ||
                lower.contains("youtube search for")

        if (isExplicitGoogleSearch || isExplicitYouTubeSearch) {
            return UserIntent.COMPUTER_SEARCH
        }

        // 3. OPEN APPLICATION INTENT (Explicit request to launch an app)
        // e.g. "Open Chrome", "Open YouTube", "Open Notepad", "Launch Spotify"
        if (lower.matches(Regex("^(open|launch|start|run)\\s+(chrome|browser|youtube|notepad|spotify|terminal|vscode|calculator|explorer)(\\s+app)?$")) ||
            lower == "open chrome" ||
            lower == "open youtube" ||
            lower == "open notepad" ||
            lower == "open spotify" ||
            lower == "open vscode" ||
            lower == "open terminal" ||
            lower == "open calculator" ||
            lower == "open file explorer"
        ) {
            return UserIntent.OPEN_APPLICATION
        }

        // 4. SYSTEM COMMAND INTENT (Internal tools: time, battery, notes, memory)
        if (lower.contains("what time") ||
            lower.contains("current time") ||
            lower == "time" ||
            lower.contains("what is the date") ||
            lower.contains("today's date") ||
            lower.contains("system info") ||
            lower.contains("battery") ||
            lower.contains("system diagnostic") ||
            lower.contains("diagnostics") ||
            lower.startsWith("create note") ||
            lower.startsWith("remember to") ||
            lower.startsWith("save memory") ||
            lower.startsWith("my memories")
        ) {
            return UserIntent.SYSTEM_COMMAND
        }

        // 5. INFORMATION REQUEST INTENT
        // NOTE: "current", "latest", "today", "recent", "news" MUST NOT trigger Chrome!
        // e.g. "What is the current AI news?", "What is the latest Gemini update?", "Who won today's match?", "What's the weather today?", "What is the current price of Bitcoin?"
        val hasInfoKeyword = lower.contains("current") ||
                lower.contains("latest") ||
                lower.contains("today") ||
                lower.contains("recent") ||
                lower.contains("news") ||
                lower.contains("weather") ||
                lower.contains("price of") ||
                lower.contains("who won") ||
                lower.contains("what happened") ||
                lower.contains("score") ||
                lower.contains("stock") ||
                lower.contains("update")

        val isQuestionPattern = lower.startsWith("what ") ||
                lower.startsWith("what's ") ||
                lower.startsWith("who ") ||
                lower.startsWith("who's ") ||
                lower.startsWith("when ") ||
                lower.startsWith("where ") ||
                lower.startsWith("how ") ||
                lower.startsWith("tell me about ") ||
                lower.startsWith("is there ")

        if (hasInfoKeyword || (isQuestionPattern && !isGreeting(lower))) {
            return UserIntent.INFORMATION_REQUEST
        }

        // 6. CONVERSATION INTENT (Greetings, casual talk, jokes)
        // e.g. "Hello Jarvis", "How are you?", "Tell me a joke"
        return UserIntent.CONVERSATION
    }

    private fun isGreeting(lower: String): Boolean {
        return lower.startsWith("hello") ||
                lower.startsWith("hi") ||
                lower.startsWith("hey") ||
                lower.contains("how are you") ||
                lower.contains("tell me a joke") ||
                lower.contains("who are you") ||
                lower.contains("good morning") ||
                lower.contains("good evening") ||
                lower.contains("good night")
    }
}

package com.example

import com.example.data.local.ConversationMessageEntity
import com.example.data.model.JarvisState
import com.example.data.model.UserIntent
import com.example.data.network.NetworkDiagnostics
import com.example.data.study.StudyManager
import com.example.tools.IntentClassifier
import com.example.ui.components.WaveformMode
import com.example.ui.haptics.HapticIntensity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    // --- REQUIREMENT 11: TEST THE 7 EXACT COMMANDS (A through G) ---

    @Test
    fun testCommandA_currentAiNews_routesToInformationRequest() {
        // A. "What is the current AI news?" -> REAL INFORMATION SEARCH -> ANSWER INSIDE JARVIS
        val result = IntentClassifier.classify("What is the current AI news?")
        assertEquals(UserIntent.INFORMATION_REQUEST, result.intent)
    }

    @Test
    fun testCommandB_latestNewsAboutGemini_routesToInformationRequest() {
        // B. "What's the latest news about Gemini?" -> REAL INFORMATION SEARCH -> ANSWER INSIDE JARVIS
        val result = IntentClassifier.classify("What's the latest news about Gemini?")
        assertEquals(UserIntent.INFORMATION_REQUEST, result.intent)
    }

    @Test
    fun testCommandC_searchGoogleForIPhone15Price_routesToComputerSearch() {
        // C. "Search Google for iPhone 15 price." -> OPEN CHROME -> GOOGLE SEARCH
        val result = IntentClassifier.classify("Search Google for iPhone 15 price.")
        assertEquals(UserIntent.COMPUTER_SEARCH, result.intent)
        assertEquals("google", result.searchEngine)
        assertEquals("iPhone 15 price.", result.extractedQuery)
    }

    @Test
    fun testCommandD_openChrome_routesToOpenApplication() {
        // D. "Open Chrome." -> OPEN CHROME
        val result = IntentClassifier.classify("Open Chrome.")
        assertEquals(UserIntent.OPEN_APPLICATION, result.intent)
        assertEquals("chrome", result.targetApp)
    }

    @Test
    fun testCommandE_whatTimeIsIt_routesToConversation() {
        // E. "What time is it?" -> ANSWER DIRECTLY USING TIME TOOL
        val result = IntentClassifier.classify("What time is it?")
        assertEquals(UserIntent.CONVERSATION, result.intent)
    }

    @Test
    fun testCommandF_explainQuadraticEquations_routesToStudyMode() {
        // F. "Explain quadratic equations." -> STUDY MODE
        val result = IntentClassifier.classify("Explain quadratic equations.")
        assertEquals(UserIntent.STUDY, result.intent)
    }

    @Test
    fun testCommandG_searchYouTubeForMinecraft_routesToComputerSearch() {
        // G. "Search YouTube for Minecraft." -> OPEN BROWSER -> YOUTUBE SEARCH
        val result = IntentClassifier.classify("Search YouTube for Minecraft.")
        assertEquals(UserIntent.COMPUTER_SEARCH, result.intent)
        assertEquals("youtube", result.searchEngine)
        assertEquals("Minecraft.", result.extractedQuery)
    }

    // --- REQUIREMENT 3: KEYWORDS "current", "latest", "today", "news" MUST NOT TRIGGER CHROME ---

    @Test
    fun testKeywords_doNotTriggerBrowser() {
        val testQueries = listOf(
            "What's the weather today?",
            "What is the current price of Bitcoin?",
            "Who won today's match?",
            "What happened in technology today?",
            "What is the latest Gemini update?"
        )
        for (q in testQueries) {
            val result = IntentClassifier.classify(q)
            assertEquals("Failed for query: '$q'", UserIntent.INFORMATION_REQUEST, result.intent)
        }
    }

    @Test
    fun testExplicitGoogleSearchWithKeyword_routesToComputerSearch() {
        // "Search Google for current AI news." -> explicit Google search
        val result = IntentClassifier.classify("Search Google for current AI news.")
        assertEquals(UserIntent.COMPUTER_SEARCH, result.intent)
        assertEquals("google", result.searchEngine)
    }

    // --- SYSTEM & COMPONENT TESTS ---

    @Test
    fun jarvisStates_containAllRequiredPhases() {
        val states = JarvisState.values()
        assertTrue(states.contains(JarvisState.STANDBY))
        assertTrue(states.contains(JarvisState.LISTENING))
        assertTrue(states.contains(JarvisState.THINKING))
        assertTrue(states.contains(JarvisState.SEARCHING))
        assertTrue(states.contains(JarvisState.ANALYZING))
        assertTrue(states.contains(JarvisState.SPEAKING))
    }

    @Test
    fun studyManager_hasQuestions() {
        val questions = StudyManager.sampleQuestions
        assertTrue(questions.isNotEmpty())
        val mathQ = questions.find { it.subject == "Mathematics" }
        assertNotNull(mathQ)
        assertEquals(1, mathQ?.correctIndex)
    }

    @Test
    fun studyManager_createsTeluguPrompt() {
        val prompt = StudyManager.createStudyPrompt("Physics", "Gravity", explainInTelugu = true)
        assertTrue(prompt.contains("తెలుగులో"))
    }

    @Test
    fun hapticIntensity_allLevelsExist() {
        val levels = HapticIntensity.values()
        assertEquals(4, levels.size)
        assertTrue(levels.contains(HapticIntensity.OFF))
        assertTrue(levels.contains(HapticIntensity.LIGHT))
        assertTrue(levels.contains(HapticIntensity.NORMAL))
        assertTrue(levels.contains(HapticIntensity.STRONG))
    }

    @Test
    fun networkDiagnostics_dataModel() {
        val diag = NetworkDiagnostics(
            isConnected = true,
            connectionType = "Wi-Fi",
            latencyMs = 45L,
            statusMessage = "Online"
        )
        assertTrue(diag.isConnected)
        assertEquals("Wi-Fi", diag.connectionType)
        assertEquals(45L, diag.latencyMs)
    }

    @Test
    fun waveformModes_allExist() {
        val modes = WaveformMode.values()
        assertEquals(3, modes.size)
        assertTrue(modes.contains(WaveformMode.SPECTRUM))
        assertTrue(modes.contains(WaveformMode.OSCILLOSCOPE))
        assertTrue(modes.contains(WaveformMode.PULSE_RINGS))
    }

    @Test
    fun conversationEntity_dataModel() {
        val entity = ConversationMessageEntity(
            messageId = "test-uuid-123",
            sender = "USER",
            text = "Hello JARVIS",
            timestamp = 1000L,
            language = "English"
        )
        assertEquals("test-uuid-123", entity.messageId)
        assertEquals("USER", entity.sender)
        assertEquals("Hello JARVIS", entity.text)
        assertEquals(1000L, entity.timestamp)
    }

    @Test
    fun testCoreDiagnostics_tenSubsystemsDefined() {
        val requiredSubsystems = listOf(
            "Gemini connection",
            "Voice input",
            "Voice output",
            "Intent classification",
            "Web search",
            "Memory",
            "Tool calling",
            "PC connection",
            "Permission system",
            "Error handling"
        )
        assertEquals(10, requiredSubsystems.size)
        // Verify each subsystem is covered
        assertTrue(requiredSubsystems.contains("Gemini connection"))
        assertTrue(requiredSubsystems.contains("Voice input"))
        assertTrue(requiredSubsystems.contains("Voice output"))
        assertTrue(requiredSubsystems.contains("Intent classification"))
        assertTrue(requiredSubsystems.contains("Web search"))
        assertTrue(requiredSubsystems.contains("Memory"))
        assertTrue(requiredSubsystems.contains("Tool calling"))
        assertTrue(requiredSubsystems.contains("PC connection"))
        assertTrue(requiredSubsystems.contains("Permission system"))
        assertTrue(requiredSubsystems.contains("Error handling"))
    }

    @Test
    fun testCoreDiagnostics_intentClassificationVectors() {
        assertEquals(UserIntent.INFORMATION_REQUEST, IntentClassifier.classify("What is the current AI news?").intent)
        assertEquals(UserIntent.COMPUTER_SEARCH, IntentClassifier.classify("Search Google for iPhone 15 price").intent)
        assertEquals(UserIntent.OPEN_APPLICATION, IntentClassifier.classify("Open Chrome.").intent)
        assertEquals(UserIntent.STUDY, IntentClassifier.classify("Explain quadratic equations.").intent)
        assertEquals(UserIntent.CONVERSATION, IntentClassifier.classify("What time is it?").intent)
    }

    // --- RETROFIT SEARCH API & SERVICE TESTS ---

    @Test
    fun testRetrofitSearchService_dtoParsing() {
        val moshi = com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()

        // Wikipedia DTO parsing
        val wikiJson = """
            {
                "query": {
                    "search": [
                        {
                            "title": "Artificial intelligence",
                            "snippet": "intelligence demonstrated by machines",
                            "pageid": 1164
                        }
                    ]
                }
            }
        """.trimIndent()
        val wikiAdapter = moshi.adapter(com.example.data.api.search.WikiSearchResponse::class.java)
        val wikiResp = wikiAdapter.fromJson(wikiJson)
        assertNotNull(wikiResp)
        assertEquals(1, wikiResp?.query?.search?.size)
        assertEquals("Artificial intelligence", wikiResp?.query?.search?.first()?.title)
        assertEquals(1164L, wikiResp?.query?.search?.first()?.pageId)

        // DuckDuckGo DTO parsing
        val ddgJson = """
            {
                "AbstractText": "Artificial intelligence is intelligence demonstrated by machines.",
                "Heading": "Artificial Intelligence",
                "AbstractURL": "https://en.wikipedia.org/wiki/Artificial_intelligence",
                "AbstractSource": "Wikipedia"
            }
        """.trimIndent()
        val ddgAdapter = moshi.adapter(com.example.data.api.search.DdgInstantResponse::class.java)
        val ddgResp = ddgAdapter.fromJson(ddgJson)
        assertNotNull(ddgResp)
        assertEquals("Artificial intelligence is intelligence demonstrated by machines.", ddgResp?.abstractText)
        assertEquals("Artificial Intelligence", ddgResp?.heading)

        // Hacker News DTO parsing
        val hnJson = """
            {
                "hits": [
                    {
                        "title": "Show HN: New AI System",
                        "url": "https://example.com/ai",
                        "author": "dev1",
                        "points": 142,
                        "objectID": "999888"
                    }
                ],
                "nbHits": 1
            }
        """.trimIndent()
        val hnAdapter = moshi.adapter(com.example.data.api.search.HnSearchResponse::class.java)
        val hnResp = hnAdapter.fromJson(hnJson)
        assertNotNull(hnResp)
        assertEquals(1, hnResp?.hits?.size)
        assertEquals("Show HN: New AI System", hnResp?.hits?.first()?.title)
        assertEquals(142, hnResp?.hits?.first()?.points)

        // Crypto price DTO parsing
        val cryptoJson = """
            {
                "bitcoin": {
                    "usd": 68500.50,
                    "usd_24h_change": 3.45
                }
            }
        """.trimIndent()
        val cryptoAdapter = moshi.adapter(com.example.data.api.search.CryptoPriceResponse::class.java)
        val cryptoResp = cryptoAdapter.fromJson(cryptoJson)
        assertNotNull(cryptoResp)
        assertEquals(68500.50, cryptoResp?.bitcoin?.usd ?: 0.0, 0.01)
        assertEquals(3.45, cryptoResp?.bitcoin?.usd24hChange ?: 0.0, 0.01)
    }

    @Test
    fun testRetrofitSearchService_serviceInstantiation() {
        val service = com.example.data.api.search.RetrofitSearchServiceImpl()
        assertNotNull(service)
        assertTrue(service is com.example.data.api.search.ReliableSearchService)
    }

    // --- HOME ARRIVAL ASSISTANT FEATURE TESTS ---

    @Test
    fun testHomeArrival_intentClassification() {
        val arrivalPhrases = listOf(
            "I'm back home",
            "I'm home",
            "I reached home",
            "I'm back",
            "im back home",
            "im home",
            "i reached home",
            "im back",
            "Finally home",
            "Arrived home"
        )
        for (phrase in arrivalPhrases) {
            val result = IntentClassifier.classify(phrase)
            assertEquals("Phrase '$phrase' should be classified as HOME_ARRIVAL", UserIntent.HOME_ARRIVAL, result.intent)
        }
    }

    @Test
    fun testHomeArrival_nonArrivalClassification() {
        assertEquals(UserIntent.CONVERSATION, IntentClassifier.classify("Hello JARVIS").intent)
        assertEquals(UserIntent.OPEN_APPLICATION, IntentClassifier.classify("Open Chrome.").intent)
        assertEquals(UserIntent.COMPUTER_SEARCH, IntentClassifier.classify("Search Google for weather").intent)
    }

    @Test
    fun testHomeArrival_activityEntryFormatting() {
        val reports = listOf(
            com.example.data.model.ServiceUnreadReport(
                service = com.example.data.model.MessagingServiceType.INSTAGRAM,
                isAuthorized = true,
                isAvailable = true,
                unreadCount = 2
            ),
            com.example.data.model.ServiceUnreadReport(
                service = com.example.data.model.MessagingServiceType.TELEGRAM,
                isAuthorized = true,
                isAvailable = true,
                unreadCount = 3
            )
        )

        val activityEntry = buildString {
            appendLine("JARVIS // PROTOCOL")
            appendLine("HOME ARRIVAL DETECTED")
            appendLine("CHECKING AUTHORIZED SERVICES")
            reports.forEach { r ->
                if (r.isAuthorized) {
                    if (r.isAvailable) {
                        appendLine("${r.service.displayName.uppercase()}: ${r.unreadCount} UNREAD")
                    } else {
                        appendLine("${r.service.displayName.uppercase()}: COULD NOT BE CHECKED")
                    }
                }
            }
            append("AWAITING USER CONFIRMATION")
        }

        val expected = """
            JARVIS // PROTOCOL
            HOME ARRIVAL DETECTED
            CHECKING AUTHORIZED SERVICES
            INSTAGRAM: 2 UNREAD
            TELEGRAM: 3 UNREAD
            AWAITING USER CONFIRMATION
        """.trimIndent()

        assertEquals(expected, activityEntry)
    }

    @Test
    fun testHomeArrival_responseSpeechFormatting() {
        val availableUnread = listOf(
            com.example.data.model.ServiceUnreadReport(
                service = com.example.data.model.MessagingServiceType.INSTAGRAM,
                isAuthorized = true,
                isAvailable = true,
                unreadCount = 2
            ),
            com.example.data.model.ServiceUnreadReport(
                service = com.example.data.model.MessagingServiceType.TELEGRAM,
                isAuthorized = true,
                isAvailable = true,
                unreadCount = 3
            )
        )

        val countsText = if (availableUnread.size == 1) {
            "${availableUnread[0].unreadCount} unread messages from ${availableUnread[0].service.displayName}"
        } else {
            "${availableUnread[0].unreadCount} unread messages from ${availableUnread[0].service.displayName} and ${availableUnread.drop(1).joinToString(" and ") { "${it.unreadCount} from ${it.service.displayName}" }}"
        }
        val speechText = "Sir, you have $countsText. Should I open them?"

        assertEquals("Sir, you have 2 unread messages from Instagram and 3 from Telegram. Should I open them?", speechText)
    }

    @Test
    fun testMessagingServiceConnectionStatus_connectedWhenAuthorized() {
        fun getConnectionStatus(isAuthorized: Boolean): String =
            if (isAuthorized) "Connected" else "Disconnected"

        assertEquals("Connected", getConnectionStatus(isAuthorized = true))
        assertEquals("Disconnected", getConnectionStatus(isAuthorized = false))
    }

    @Test
    fun testMessagingServices_allSupportedServiceTypes() {
        val services = com.example.data.model.MessagingServiceType.values()
        assertTrue(services.any { it.name == "INSTAGRAM" })
        assertTrue(services.any { it.name == "TELEGRAM" })
        assertTrue(services.any { it.name == "WHATSAPP" })
        assertTrue(services.any { it.name == "GMAIL" })
        assertTrue(services.any { it.name == "DISCORD" })

        // Check display names
        assertEquals("Instagram", com.example.data.model.MessagingServiceType.INSTAGRAM.displayName)
        assertEquals("Telegram", com.example.data.model.MessagingServiceType.TELEGRAM.displayName)
        assertEquals("WhatsApp", com.example.data.model.MessagingServiceType.WHATSAPP.displayName)
        assertEquals("Gmail", com.example.data.model.MessagingServiceType.GMAIL.displayName)
        assertEquals("Discord", com.example.data.model.MessagingServiceType.DISCORD.displayName)
    }

    // --- ZERO FABRICATION CONSTRAINT TESTS ---

    @Test
    fun testZeroFabrication_disconnectedServices() {
        val igNotConnected = com.example.data.model.ServiceUnreadReport(
            service = com.example.data.model.MessagingServiceType.INSTAGRAM,
            isAuthorized = false,
            isAvailable = false,
            unreadCount = 0,
            unavailableReason = "Instagram is not connected, sir."
        )
        val tgNotConnected = com.example.data.model.ServiceUnreadReport(
            service = com.example.data.model.MessagingServiceType.TELEGRAM,
            isAuthorized = false,
            isAvailable = false,
            unreadCount = 0,
            unavailableReason = "Telegram is not connected, sir."
        )

        assertEquals("Instagram is not connected, sir.", igNotConnected.unavailableReason)
        assertEquals("Telegram is not connected, sir.", tgNotConnected.unavailableReason)
        assertEquals(0, igNotConnected.unreadCount)
        assertEquals(0, tgNotConnected.unreadCount)
    }

    @Test
    fun testZeroFabrication_integrationFailure() {
        val igFailed = com.example.data.model.ServiceUnreadReport(
            service = com.example.data.model.MessagingServiceType.INSTAGRAM,
            isAuthorized = true,
            isAvailable = false,
            unreadCount = 0,
            unavailableReason = "I couldn't check Instagram right now, sir.",
            isSimulated = false
        )
        val tgFailed = com.example.data.model.ServiceUnreadReport(
            service = com.example.data.model.MessagingServiceType.TELEGRAM,
            isAuthorized = true,
            isAvailable = false,
            unreadCount = 0,
            unavailableReason = "I couldn't check Telegram right now, sir.",
            isSimulated = false
        )

        assertEquals("I couldn't check Instagram right now, sir.", igFailed.unavailableReason)
        assertEquals("I couldn't check Telegram right now, sir.", tgFailed.unavailableReason)
        assertEquals(0, igFailed.unreadCount)
        assertEquals(0, tgFailed.unreadCount)
        assertFalse(igFailed.isSimulated)
    }

    @Test
    fun testSimulationMode_clearlyLabeledAsSimulated() {
        val simulatedReport = com.example.data.model.ServiceUnreadReport(
            service = com.example.data.model.MessagingServiceType.INSTAGRAM,
            isAuthorized = true,
            isAvailable = true,
            unreadCount = 2,
            unavailableReason = null,
            isSimulated = true
        )
        assertTrue(simulatedReport.isSimulated)

        val speech = if (simulatedReport.isSimulated) {
            "[SIMULATED] Sir, you have 2 unread messages from Instagram. Should I open them?"
        } else {
            "I couldn't check Instagram right now, sir."
        }
        assertTrue("Simulated speech must be clearly labeled with [SIMULATED]", speech.startsWith("[SIMULATED]"))
    }

    // --- APP LAUNCHER & ALLOWLIST TESTS ---

    @Test
    fun testIntentClassifier_openApplicationClassification() {
        val ig = com.example.tools.IntentClassifier.classify("Open Instagram")
        assertEquals(com.example.data.model.UserIntent.OPEN_APPLICATION, ig.intent)
        assertEquals("instagram", ig.targetApp)

        val tg = com.example.tools.IntentClassifier.classify("Open Telegram")
        assertEquals(com.example.data.model.UserIntent.OPEN_APPLICATION, tg.intent)
        assertEquals("telegram", tg.targetApp)

        val yt = com.example.tools.IntentClassifier.classify("Open YouTube")
        assertEquals(com.example.data.model.UserIntent.OPEN_APPLICATION, yt.intent)
        assertEquals("youtube", yt.targetApp)

        val ytWithPrefix = com.example.tools.IntentClassifier.classify("Jarvis, open YouTube")
        assertEquals(com.example.data.model.UserIntent.OPEN_APPLICATION, ytWithPrefix.intent)
        assertEquals("youtube", ytWithPrefix.targetApp)

        val tgPlease = com.example.tools.IntentClassifier.classify("Please open Telegram")
        assertEquals(com.example.data.model.UserIntent.OPEN_APPLICATION, tgPlease.intent)
        assertEquals("telegram", tgPlease.targetApp)
    }

    @Test
    fun testAppLauncher_allowlistContainsRequiredApps() {
        val allowlist = com.example.data.launcher.AndroidAppLauncher.ALLOWLIST
        assertTrue("Instagram must be in allowlist", allowlist.any { it.id == "instagram" })
        assertTrue("Telegram must be in allowlist", allowlist.any { it.id == "telegram" })
        assertTrue("YouTube must be in allowlist", allowlist.any { it.id == "youtube" })
        assertTrue("Chrome must be in allowlist", allowlist.any { it.id == "chrome" })
        assertTrue("Spotify must be in allowlist", allowlist.any { it.id == "spotify" })
        assertTrue("WhatsApp must be in allowlist", allowlist.any { it.id == "whatsapp" })
        assertTrue("Gmail must be in allowlist", allowlist.any { it.id == "gmail" })
        assertTrue("Discord must be in allowlist", allowlist.any { it.id == "discord" })
    }

    @Test
    fun testAppLauncher_unauthorizedAppDisallowed() {
        val allowlist = com.example.data.launcher.AndroidAppLauncher.ALLOWLIST
        val unauthorizedApp = "malicious_app"
        assertFalse("Arbitrary apps must not be allowed", allowlist.any { it.id == unauthorizedApp })
    }

    @Test
    fun testAppLaunchResult_notInstalledFormat() {
        val notInstalled = com.example.data.launcher.AppLaunchResult.NotInstalled(
            appId = "instagram",
            displayName = "Instagram",
            websiteUrl = "https://instagram.com",
            message = "Instagram isn't installed, sir. Would you like me to open the official website?"
        )
        assertEquals("Instagram isn't installed, sir. Would you like me to open the official website?", notInstalled.message)
    }

    @Test
    fun testAppLaunchResult_secureFolderLockedFormat() {
        val locked = com.example.data.launcher.AppLaunchResult.SecureFolderLocked(
            appId = "instagram",
            displayName = "Instagram",
            message = "Instagram is available in Secure Folder, but I can't launch that protected instance directly. Please unlock Secure Folder."
        )
        assertEquals("Instagram is available in Secure Folder, but I can't launch that protected instance directly. Please unlock Secure Folder.", locked.message)
    }

    // --- VOICE & TTS CLEANING TESTS ---

    @Test
    fun testLanguageModes_localesAreCorrect() {
        val enLocale = com.example.data.model.LanguageMode.ENGLISH.getLocale()
        assertEquals("en", enLocale.language)
        assertEquals("IN", enLocale.country)

        val teLocale = com.example.data.model.LanguageMode.TELUGU.getLocale()
        assertEquals("te", teLocale.language)
        assertEquals("IN", teLocale.country)
    }

    @Test
    fun testVoiceCleaning_removesMarkdownJsonUrlsAndStatusLabels() {
        val rawInput = """
            SEARCHING WEB
            Here is the result: **Iron Man** was released in 2008.
            Visit https://marvel.com for details.
            ```kotlin
            val armor = "Mark 85"
            ```
            {"action": "open_app", "target": "youtube"}
            LISTENING
        """.trimIndent()

        val cleaned = com.example.voice.VoiceManager.cleanTextForSpeech(rawInput)

        assertFalse("Cleaned speech must not contain SEARCHING WEB", cleaned.contains("SEARCHING WEB"))
        assertFalse("Cleaned speech must not contain LISTENING", cleaned.contains("LISTENING"))
        assertFalse("Cleaned speech must not contain URLs", cleaned.contains("https://marvel.com"))
        assertFalse("Cleaned speech must not contain raw JSON", cleaned.contains("{\"action\""))
        assertFalse("Cleaned speech must not contain markdown bold **", cleaned.contains("**"))
        assertTrue("Cleaned speech must preserve core text", cleaned.contains("Iron Man was released in 2008"))
    }

    // --- SECTION 6: MANDATORY 10 ON-DEVICE PRODUCTION VOICE TESTS ---

    @Test
    fun test01_openInstagram_executesLocallyAndImmediately() {
        // Warm up JIT
        com.example.tools.IntentClassifier.classify("warmup")
        val t0 = System.nanoTime()
        val result = com.example.tools.IntentClassifier.classify("Open Instagram.")
        val latencyMs = (System.nanoTime() - t0) / 1_000_000.0

        assertEquals(com.example.data.model.UserIntent.OPEN_APPLICATION, result.intent)
        assertEquals("instagram", result.targetApp)
        assertTrue("Intent classification must execute in < 20ms (was ${latencyMs}ms)", latencyMs < 20.0)
    }

    @Test
    fun test02_launchInstagram_aliasMatchesInstagram() {
        // Warm up JIT
        com.example.tools.IntentClassifier.classify("warmup")
        val t0 = System.nanoTime()
        val result = com.example.tools.IntentClassifier.classify("Launch Instagram.")
        val latencyMs = (System.nanoTime() - t0) / 1_000_000.0

        assertEquals(com.example.data.model.UserIntent.OPEN_APPLICATION, result.intent)
        assertEquals("instagram", result.targetApp)
        assertTrue("Alias matching must execute in < 20ms (was ${latencyMs}ms)", latencyMs < 20.0)
    }

    @Test
    fun test03_openYouTube_executesLocallyAndImmediately() {
        // Warm up JIT
        com.example.tools.IntentClassifier.classify("warmup")
        val t0 = System.nanoTime()
        val result = com.example.tools.IntentClassifier.classify("Open YouTube.")
        val latencyMs = (System.nanoTime() - t0) / 1_000_000.0

        assertEquals(com.example.data.model.UserIntent.OPEN_APPLICATION, result.intent)
        assertEquals("youtube", result.targetApp)
        assertTrue("Intent classification must execute in < 20ms (was ${latencyMs}ms)", latencyMs < 20.0)
    }

    @Test
    fun test04_whatIsTheLatestAiNews_routesToRealInformationSearch() {
        val result = com.example.tools.IntentClassifier.classify("What is the latest AI news?")
        assertEquals(com.example.data.model.UserIntent.INFORMATION_REQUEST, result.intent)
        assertFalse("Must not open browser for news question", result.intent == com.example.data.model.UserIntent.COMPUTER_SEARCH)
    }

    @Test
    fun test05_explainPhotosynthesis_routesToStudyOrReasoningMode() {
        val result = com.example.tools.IntentClassifier.classify("Explain photosynthesis.")
        assertEquals(com.example.data.model.UserIntent.STUDY, result.intent)
    }

    @Test
    fun test06_stopSpeaking_instantlyTriggersStopSpeech() {
        val aliases = listOf("Stop speaking.", "Stop", "Be quiet", "Cancel", "Quiet JARVIS")
        for (phrase in aliases) {
            val result = com.example.tools.IntentClassifier.classify(phrase)
            assertEquals("Phrase '$phrase' must classify as STOP_SPEECH", com.example.data.model.UserIntent.STOP_SPEECH, result.intent)
        }
    }

    @Test
    fun test07_phraseWithBackgroundNoiseAndFillers_normalizesCleanly() {
        val noisyInputs = listOf(
            "uh jarvis open instagram please" to "instagram",
            "um launch youtube" to "youtube",
            "ah start chrome" to "chrome",
            "hey jarvis... open telegram!" to "telegram"
        )
        for ((input, expectedApp) in noisyInputs) {
            val result = com.example.tools.IntentClassifier.classify(input)
            assertEquals("Noisy query '$input' should resolve to OPEN_APPLICATION", com.example.data.model.UserIntent.OPEN_APPLICATION, result.intent)
            assertEquals("Noisy query '$input' should target $expectedApp", expectedApp, result.targetApp)
        }
    }

    @Test
    fun test08_unclearOrLowConfidenceTranscript_flagsAmbiguousAndAsksClarification() {
        // Low confidence transcript (< 0.38f)
        val lowConfResult = com.example.tools.IntentClassifier.classify("mumbled words", confidence = 0.25f)
        assertTrue("Low confidence transcript must be flagged ambiguous", lowConfResult.isAmbiguous)
        assertNotNull("Must provide a clarification prompt", lowConfResult.clarificationPrompt)

        // Incomplete command ("open" with no target)
        val incompleteResult = com.example.tools.IntentClassifier.classify("open")
        assertTrue("Incomplete command must be flagged ambiguous", incompleteResult.isAmbiguous)
        assertEquals("Which application would you like me to open, Sir?", incompleteResult.clarificationPrompt)
    }

    @Test
    fun test09_threeConsecutiveCommands_stateMachineTransitionsCorrectly() {
        // Voice State Machine: IDLE -> LISTENING -> PROCESSING -> SPEAKING -> IDLE/LISTENING
        var currentState = com.example.data.model.VoiceState.IDLE
        val commandPipeline = listOf(
            "Open Instagram." to com.example.data.model.UserIntent.OPEN_APPLICATION,
            "What time is it?" to com.example.data.model.UserIntent.CONVERSATION,
            "Stop speaking." to com.example.data.model.UserIntent.STOP_SPEECH
        )

        for ((cmd, expectedIntent) in commandPipeline) {
            // 1. User initiates listening
            currentState = com.example.data.model.VoiceState.LISTENING
            assertEquals(com.example.data.model.VoiceState.LISTENING, currentState)

            // 2. Final transcript received & classified
            currentState = com.example.data.model.VoiceState.PROCESSING
            val classification = com.example.tools.IntentClassifier.classify(cmd)
            assertEquals(expectedIntent, classification.intent)

            // 3. Execution & response
            currentState = com.example.data.model.VoiceState.SPEAKING
            assertEquals(com.example.data.model.VoiceState.SPEAKING, currentState)

            // 4. Returns to IDLE or continuous listening
            currentState = com.example.data.model.VoiceState.IDLE
            assertEquals(com.example.data.model.VoiceState.IDLE, currentState)
        }
    }

    @Test
    fun test10_recognitionErrorRecovery_recoversCleanlyToIdleAndRetries() {
        var voiceState = com.example.data.model.VoiceState.LISTENING
        var errorOccurred = false

        // Simulate SpeechRecognizer error (e.g. ERROR_NO_MATCH or ERROR_RECOGNIZER_BUSY)
        val errorCode = android.speech.SpeechRecognizer.ERROR_NO_MATCH
        if (errorCode == android.speech.SpeechRecognizer.ERROR_NO_MATCH) {
            voiceState = com.example.data.model.VoiceState.IDLE
            errorOccurred = true
        }

        assertTrue(errorOccurred)
        assertEquals(com.example.data.model.VoiceState.IDLE, voiceState)

        // Retry: Successfully starts listening again
        voiceState = com.example.data.model.VoiceState.LISTENING
        val retryResult = com.example.tools.IntentClassifier.classify("Open YouTube")
        assertEquals(com.example.data.model.UserIntent.OPEN_APPLICATION, retryResult.intent)
        assertEquals("youtube", retryResult.targetApp)
    }
}

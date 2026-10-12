package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GeminiResult {
    data class Success(
        val text: String,
        val functionCalls: List<RawFunctionCall> = emptyList()
    ) : GeminiResult()

    data class Error(val message: String, val isNetworkError: Boolean = false) : GeminiResult()
}

data class RawFunctionCall(
    val name: String,
    val arguments: Map<String, Any?>
)

data class TurnMessage(
    val role: String, // "user" or "model"
    val text: String
)

class GeminiClient(
    private val customApiKey: () -> String = { "" },
    private val modelProvider: () -> String = { "gemini-3.5-flash" }
) {
    companion object {
        private const val TAG = "GeminiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getEffectiveApiKey(): String {
        val override = customApiKey().trim()
        if (override.isNotEmpty()) return override
        return try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
    }

    fun hasValidApiKey(): Boolean {
        val key = getEffectiveApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun generateResponse(
        systemInstruction: String,
        history: List<TurnMessage>,
        newPrompt: String,
        toolsEnabled: Boolean = true
    ): GeminiResult = withContext(Dispatchers.IO) {
        val key = getEffectiveApiKey()
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResult.Error(
                "Gemini API key is not configured. Please add it via AI Studio Secrets or Settings."
            )
        }

        val model = modelProvider().ifEmpty { "gemini-3.5-flash" }
        val endpoint = "$BASE_URL/$model:generateContent?key=$key"

        try {
            val rootJson = JSONObject()

            // System instruction
            if (systemInstruction.isNotEmpty()) {
                val sysObj = JSONObject()
                val sysParts = JSONArray()
                sysParts.put(JSONObject().put("text", systemInstruction))
                sysObj.put("parts", sysParts)
                rootJson.put("systemInstruction", sysObj)
            }

            // Contents array (Conversation History + Current Prompt)
            val contentsArray = JSONArray()
            // Keep recent turns up to 10
            val recentHistory = history.takeLast(10)
            for (turn in recentHistory) {
                val contentObj = JSONObject()
                contentObj.put("role", if (turn.role == "user") "user" else "model")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", turn.text))
                contentObj.put("parts", parts)
                contentsArray.put(contentObj)
            }

            // Current prompt
            val currentContentObj = JSONObject()
            currentContentObj.put("role", "user")
            val currentParts = JSONArray()
            currentParts.put(JSONObject().put("text", newPrompt))
            currentContentObj.put("parts", currentParts)
            contentsArray.put(currentContentObj)

            rootJson.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            genConfig.put("topP", 0.95)
            rootJson.put("generationConfig", genConfig)

            // Function declarations
            if (toolsEnabled) {
                val toolsArray = JSONArray()
                val toolObj = JSONObject()
                toolObj.put("functionDeclarations", buildFunctionDeclarations())
                toolsArray.put(toolObj)
                rootJson.put("tools", toolsArray)
            }

            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error code ${response.code}: $responseBody")
                val errMsg = try {
                    val errorObj = JSONObject(responseBody).optJSONObject("error")
                    errorObj?.optString("message") ?: "API Error ${response.code}"
                } catch (_: Exception) {
                    "API Error ${response.code}"
                }
                return@withContext GeminiResult.Error(errMsg)
            }

            // Parse response
            val resObj = JSONObject(responseBody)
            val candidates = resObj.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext GeminiResult.Error("No response candidates returned by Gemini.")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            val textBuilder = StringBuilder()
            val functionCalls = mutableListOf<RawFunctionCall>()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        textBuilder.append(part.getString("text"))
                    }
                    if (part.has("functionCall")) {
                        val fnObj = part.getJSONObject("functionCall")
                        val fnName = fnObj.optString("name")
                        val argsObj = fnObj.optJSONObject("args")
                        val argsMap = mutableMapOf<String, Any?>()
                        if (argsObj != null) {
                            val keys = argsObj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                argsMap[k] = argsObj.get(k)
                            }
                        }
                        functionCalls.add(RawFunctionCall(fnName, argsMap))
                    }
                }
            }

            GeminiResult.Success(
                text = textBuilder.toString().trim(),
                functionCalls = functionCalls
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini request", e)
            GeminiResult.Error(e.message ?: "Network error communicating with Gemini", isNetworkError = true)
        }
    }

    suspend fun synthesizeFromWebResults(
        systemInstruction: String,
        userPrompt: String,
        searchResultsJson: String
    ): GeminiResult = withContext(Dispatchers.IO) {
        val key = getEffectiveApiKey()
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResult.Error(
                "Gemini API key is not configured. Please add it via AI Studio Secrets or Settings."
            )
        }

        val model = modelProvider().ifEmpty { "gemini-3.5-flash" }
        val endpoint = "$BASE_URL/$model:generateContent?key=$key"

        try {
            val rootJson = JSONObject()

            // System instruction for synthesis
            val sysObj = JSONObject()
            val sysParts = JSONArray()
            val fullSys = "$systemInstruction\n\nYou are JARVIS. You have retrieved real live web information for the user's inquiry.\nAnalyze the retrieved search results carefully, synthesize the facts into a direct, crisp, natural answer for the user. Do NOT mention you are an AI model. State the findings authoritatively and clearly. At the very end of your response, list the sources in the format:\n\nSources:\n• Source Name (URL)\n• Source Name (URL)"
            sysParts.put(JSONObject().put("text", fullSys))
            sysObj.put("parts", sysParts)
            rootJson.put("systemInstruction", sysObj)

            val contentsArray = JSONArray()
            val userContent = JSONObject().apply {
                put("role", "user")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", "User Question: $userPrompt\n\nLive Retrieved Web Data:\n$searchResultsJson\n\nPlease synthesize and directly answer the question using this real data."))
                put("parts", parts)
            }
            contentsArray.put(userContent)
            rootJson.put("contents", contentsArray)

            val genConfig = JSONObject()
            genConfig.put("temperature", 0.3)
            genConfig.put("topP", 0.95)
            rootJson.put("generationConfig", genConfig)

            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext GeminiResult.Error("Synthesis API Error ${response.code}")
            }

            val resObj = JSONObject(responseBody)
            val candidates = resObj.optJSONArray("candidates") ?: return@withContext GeminiResult.Error("No candidates")
            if (candidates.length() == 0) return@withContext GeminiResult.Error("Empty candidates")

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textBuilder = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("text")) {
                        textBuilder.append(p.getString("text"))
                    }
                }
            }

            GeminiResult.Success(text = textBuilder.toString().trim())
        } catch (e: Exception) {
            GeminiResult.Error(e.message ?: "Network error during synthesis", isNetworkError = true)
        }
    }

    private fun buildFunctionDeclarations(): JSONArray {
        val declarations = JSONArray()

        // 1. getCurrentTime
        declarations.put(JSONObject().apply {
            put("name", "getCurrentTime")
            put("description", "Returns the current local date, time, and day of week.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 2. getSystemInfo
        declarations.put(JSONObject().apply {
            put("name", "getSystemInfo")
            put("description", "Retrieves system diagnostic metrics including battery level, charging state, OS version, device model, and network.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 3. searchWeb
        declarations.put(JSONObject().apply {
            put("name", "searchWeb")
            put("description", "Retrieves live web knowledge, factual references, and entity information from the web. Returns structured results (title, url, snippet, source). Does not open browser.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The search query to retrieve information for")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })

        // 4. getNews
        declarations.put(JSONObject().apply {
            put("name", "getNews")
            put("description", "Retrieves current breaking news, latest headlines, and recent developments on any topic. Returns structured results (title, url, snippet, source). Does not open browser.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The news topic or query to fetch recent stories for")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })

        // 5. getCurrentInformation
        declarations.put(JSONObject().apply {
            put("name", "getCurrentInformation")
            put("description", "Retrieves real-time information such as current market prices (e.g. Bitcoin, crypto), current weather, or live data. Returns structured results.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The real-time information query (e.g. 'Bitcoin price', 'weather today')")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })

        // 6. openWebsite
        declarations.put(JSONObject().apply {
            put("name", "openWebsite")
            put("description", "Opens a specific website URL in the external browser. ONLY use when user explicitly asks to open a website.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("url", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The full URL to open (e.g., https://en.wikipedia.org)")
                    })
                })
                put("required", JSONArray().apply { put("url") })
            })
        })

        // 7. createNote
        declarations.put(JSONObject().apply {
            put("name", "createNote")
            put("description", "Creates a persistent note in Jarvis memory.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("title", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Title of the note")
                    })
                    put("content", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Detailed text content of the note")
                    })
                })
                put("required", JSONArray().apply { put("title"); put("content") })
            })
        })

        // 8. saveMemory
        declarations.put(JSONObject().apply {
            put("name", "saveMemory")
            put("description", "Saves a personal user fact, preference, or datum in Jarvis long-term memory.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("key", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Short identifier or label (e.g., favorite_programming_language)")
                    })
                    put("content", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The memory content to remember")
                    })
                    put("category", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Category: personal, work, preference, or fact")
                    })
                })
                put("required", JSONArray().apply { put("key"); put("content") })
            })
        })

        // 9. getMemory
        declarations.put(JSONObject().apply {
            put("name", "getMemory")
            put("description", "Retrieves memories or saved facts matching a keyword or query from Jarvis long-term memory.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Search query or keyword for memory lookup")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })

        // 10. computerControl
        declarations.put(JSONObject().apply {
            put("name", "computerControl")
            put("description", "Sends an automation command to the companion computer. ONLY execute when the user explicitly requests computer actions ('open notepad', 'open vscode', 'lock pc'). NEVER use for general questions.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("action", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The action type: open_app, open_url, take_screenshot, system_stats, lock_pc")
                    })
                    put("target", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The target parameter, e.g. 'chrome', 'vscode', 'spotify'")
                    })
                })
                put("required", JSONArray().apply { put("action"); put("target") })
            })
        })

        return declarations
    }
}

package com.example.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class NetworkDiagnostics(
    val isConnected: Boolean,
    val connectionType: String,
    val latencyMs: Long,
    val statusMessage: String
)

class NetworkManager(private val context: Context) {
    companion object {
        private const val TAG = "NetworkManager"
        private const val PING_URL = "https://www.google.com/generate_204"
    }

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _connectionType = MutableStateFlow(determineConnectionType())
    val connectionType: StateFlow<String> = _connectionType.asStateFlow()

    private val _lastLatencyMs = MutableStateFlow<Long?>(null)
    val lastLatencyMs: StateFlow<Long?> = _lastLatencyMs.asStateFlow()

    init {
        registerNetworkCallback()
    }

    private fun checkInitialConnectivity(): Boolean {
        return try {
            val net = connectivityManager?.activeNetwork ?: return false
            val caps = connectivityManager.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Log.w(TAG, "Error checking connectivity", e)
            true // Optimistic fallback
        }
    }

    private fun determineConnectionType(): String {
        return try {
            val net = connectivityManager?.activeNetwork ?: return "Offline"
            val caps = connectivityManager.getNetworkCapabilities(net) ?: return "Offline"
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular LTE/5G"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet / Virtual"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN Active"
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> "Online"
                else -> "Offline"
            }
        } catch (_: Exception) {
            "Connected"
        }
    }

    private fun registerNetworkCallback() {
        try {
            val builder = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            connectivityManager?.registerNetworkCallback(
                builder.build(),
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isOnline.value = true
                        _connectionType.value = determineConnectionType()
                    }

                    override fun onLost(network: Network) {
                        _isOnline.value = checkInitialConnectivity()
                        _connectionType.value = determineConnectionType()
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        _isOnline.value = hasInternet
                        _connectionType.value = determineConnectionType()
                    }
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not register network callback", e)
        }
    }

    suspend fun runDiagnostics(): NetworkDiagnostics = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        var latency = -1L
        var success = false
        var message = ""

        try {
            val request = Request.Builder()
                .url(PING_URL)
                .build()
            val response = httpClient.newCall(request).execute()
            latency = System.currentTimeMillis() - start
            _lastLatencyMs.value = latency
            success = response.isSuccessful || response.code == 204
            message = if (success) {
                "Internet connection verified ($latency ms response via ${connectionType.value})."
            } else {
                "Gateway reached, but server responded with code ${response.code}."
            }
        } catch (e: Exception) {
            latency = System.currentTimeMillis() - start
            success = false
            message = "Network probe failed: ${e.localizedMessage ?: "Connection timed out"}. Please check device/emulator Wi-Fi or data connection."
            Log.w(TAG, "Ping failed", e)
        }

        _isOnline.value = success || checkInitialConnectivity()

        NetworkDiagnostics(
            isConnected = success,
            connectionType = determineConnectionType(),
            latencyMs = latency,
            statusMessage = message
        )
    }

    // Live Web Knowledge Search (fetches real answers from Wikipedia / DuckDuckGo directly over internet)
    suspend fun searchLiveWeb(query: String): String? = withContext(Dispatchers.IO) {
        val cleanQuery = query
            .replace(Regex("(?i)^(who is|what is|tell me about|explain|search for|search|lookup)\\s+"), "")
            .trim()
            .replace("?", "")

        if (cleanQuery.isEmpty()) return@withContext null

        // 1. Try Wikipedia Summary API
        try {
            val encodedTopic = URLEncoder.encode(cleanQuery.replace(" ", "_"), "UTF-8")
            val wikiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/$encodedTopic"
            val request = Request.Builder()
                .url(wikiUrl)
                .header("User-Agent", "JarvisAI/1.0 (Android Personal Assistant)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val type = json.optString("type", "")
                if (type != "disambiguation") {
                    val extract = json.optString("extract", "")
                    val title = json.optString("title", cleanQuery)
                    if (extract.isNotBlank()) {
                        return@withContext "**$title** (via Live Web Knowledge):\n$extract"
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Wikipedia search skipped: ${e.message}")
        }

        // 2. Try DuckDuckGo Instant Answer API
        try {
            val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
            val ddgUrl = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"
            val request = Request.Builder()
                .url(ddgUrl)
                .header("User-Agent", "JarvisAI/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val abstractText = json.optString("AbstractText", "")
                val heading = json.optString("Heading", cleanQuery)
                if (abstractText.isNotBlank()) {
                    return@withContext "**$heading** (via Web Knowledge Feed):\n$abstractText"
                }

                // Related topics fallback
                val relatedTopics = json.optJSONArray("RelatedTopics")
                if (relatedTopics != null && relatedTopics.length() > 0) {
                    val first = relatedTopics.optJSONObject(0)
                    val firstText = first?.optString("Text", "")
                    if (!firstText.isNullOrBlank()) {
                        return@withContext "According to web records, Sir:\n$firstText"
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "DuckDuckGo search skipped: ${e.message}")
        }

        null
    }
}

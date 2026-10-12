package com.example.data.network

import android.net.Uri
import android.util.Log
import com.example.data.api.search.ReliableSearchService
import com.example.data.api.search.RetrofitSearchServiceImpl
import com.example.data.model.SearchItem
import com.example.data.model.StructuredSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WebInformationService(
    val reliableSearchService: ReliableSearchService = RetrofitSearchServiceImpl()
) {
    companion object {
        private const val TAG = "WebInformationService"
        private const val USER_AGENT = "JarvisAI/2.0 (Android Live Information Retrieval)"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // 1. searchWeb(query) using Retrofit Service
    suspend fun searchWeb(query: String): StructuredSearchResult = withContext(Dispatchers.IO) {
        val cleanQuery = sanitizeQuery(query)
        if (cleanQuery.isBlank()) {
            return@withContext StructuredSearchResult(query = query, tool = "searchWeb", error = "Empty search query")
        }

        // Primary: Real-time search via Retrofit Service
        try {
            val retrofitResult = reliableSearchService.searchWeb(cleanQuery)
            if (retrofitResult.results.isNotEmpty()) {
                return@withContext retrofitResult
            }
        } catch (e: Exception) {
            Log.w(TAG, "Primary Retrofit search failed, falling back: ${e.message}")
        }

        val items = mutableListOf<SearchItem>()

        // A. Wikipedia Search API
        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val wikiUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&format=json&utf8=1"
            val request = Request.Builder()
                .url(wikiUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val queryObj = json.optJSONObject("query")
                val searchArr = queryObj?.optJSONArray("search")
                if (searchArr != null) {
                    val count = minOf(searchArr.length(), 4)
                    for (i in 0 until count) {
                        val item = searchArr.getJSONObject(i)
                        val title = item.optString("title", "")
                        val rawSnippet = item.optString("snippet", "")
                        val cleanSnippet = cleanHtml(rawSnippet)
                        val encodedTitle = URLEncoder.encode(title.replace(" ", "_"), "UTF-8")
                        val pageUrl = "https://en.wikipedia.org/wiki/$encodedTitle"

                        if (title.isNotBlank()) {
                            items.add(
                                SearchItem(
                                    title = title,
                                    url = pageUrl,
                                    snippet = cleanSnippet,
                                    source = "Wikipedia"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wikipedia search query failed: ${e.message}")
        }

        // B. DuckDuckGo Instant Answer API
        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val ddgUrl = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
            val request = Request.Builder()
                .url(ddgUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val abstractText = json.optString("AbstractText", "")
                val heading = json.optString("Heading", cleanQuery)
                val abstractUrl = json.optString("AbstractURL", "")
                val abstractSource = json.optString("AbstractSource", "DuckDuckGo")

                if (abstractText.isNotBlank()) {
                    items.add(
                        0,
                        SearchItem(
                            title = heading,
                            url = if (abstractUrl.isNotBlank()) abstractUrl else "https://duckduckgo.com/?q=$encoded",
                            snippet = abstractText,
                            source = abstractSource
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "DuckDuckGo query failed: ${e.message}")
        }

        StructuredSearchResult(
            query = query,
            tool = "searchWeb",
            results = items,
            error = if (items.isEmpty()) "No web records retrieved for '$query'" else null
        )
    }

    // 2. getNews(query)
    suspend fun getNews(query: String): StructuredSearchResult = withContext(Dispatchers.IO) {
        val cleanQuery = sanitizeQuery(query)

        // Primary: Real-time news search via Retrofit Service
        try {
            val retrofitResult = reliableSearchService.searchNews(cleanQuery)
            if (retrofitResult.results.isNotEmpty()) {
                return@withContext retrofitResult
            }
        } catch (e: Exception) {
            Log.w(TAG, "Primary Retrofit news search failed, falling back: ${e.message}")
        }

        val items = mutableListOf<SearchItem>()

        // A. Hacker News Algolia Live News Search
        try {
            val searchQuery = if (cleanQuery.isBlank() || cleanQuery.equals("news", ignoreCase = true)) "AI news" else cleanQuery
            val encoded = URLEncoder.encode(searchQuery, "UTF-8")
            val hnUrl = "https://hn.algolia.com/api/v1/search_by_date?query=$encoded&tags=story&hitsPerPage=5"
            val request = Request.Builder()
                .url(hnUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val hits = json.optJSONArray("hits")
                if (hits != null) {
                    for (i in 0 until hits.length()) {
                        val hit = hits.getJSONObject(i)
                        val title = hit.optString("title", "")
                        val storyUrl = hit.optString("url", "")
                        val objectId = hit.optString("objectID", "")
                        val finalUrl = if (storyUrl.isNotBlank()) storyUrl else "https://news.ycombinator.com/item?id=$objectId"
                        val domain = extractDomain(finalUrl)

                        if (title.isNotBlank()) {
                            items.add(
                                SearchItem(
                                    title = title,
                                    url = finalUrl,
                                    snippet = "Recent reporting: $title",
                                    source = domain
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "News query failed: ${e.message}")
        }

        // B. Fallback / supplementary Wikipedia Current Events / Topics
        if (items.size < 3) {
            val wikiFallback = searchWeb(cleanQuery)
            for (res in wikiFallback.results) {
                if (items.none { it.title.equals(res.title, ignoreCase = true) }) {
                    items.add(res)
                }
            }
        }

        StructuredSearchResult(
            query = query,
            tool = "getNews",
            results = items,
            error = if (items.isEmpty()) "No news stories retrieved for '$query'" else null
        )
    }

    // 3. getCurrentInformation(query)
    suspend fun getCurrentInformation(query: String): StructuredSearchResult = withContext(Dispatchers.IO) {
        // Primary: Real-time current information via Retrofit Service
        try {
            val retrofitResult = reliableSearchService.getCurrentInformation(query)
            if (retrofitResult.results.isNotEmpty()) {
                return@withContext retrofitResult
            }
        } catch (e: Exception) {
            Log.w(TAG, "Primary Retrofit current info call failed: ${e.message}")
        }

        val lower = query.lowercase().trim()
        val items = mutableListOf<SearchItem>()

        // Check if query is about crypto/bitcoin price
        if (lower.contains("bitcoin") || lower.contains("btc") || lower.contains("crypto") || lower.contains("price")) {
            try {
                val cryptoUrl = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana&vs_currencies=usd&include_24hr_change=true"
                val request = Request.Builder()
                    .url(cryptoUrl)
                    .header("User-Agent", USER_AGENT)
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val btc = json.optJSONObject("bitcoin")
                    val eth = json.optJSONObject("ethereum")
                    val sol = json.optJSONObject("solana")

                    val btcPrice = btc?.optDouble("usd", -1.0) ?: -1.0
                    val btcChange = btc?.optDouble("usd_24h_change", 0.0) ?: 0.0
                    val ethPrice = eth?.optDouble("usd", -1.0) ?: -1.0
                    val solPrice = sol?.optDouble("usd", -1.0) ?: -1.0

                    val snippet = StringBuilder()
                    if (btcPrice > 0) {
                        snippet.append("Bitcoin (BTC) current price is $${String.format("%,.2f", btcPrice)} USD (24h: ${String.format("%+.2f", btcChange)}%). ")
                    }
                    if (ethPrice > 0) {
                        snippet.append("Ethereum (ETH) is $${String.format("%,.2f", ethPrice)} USD. ")
                    }
                    if (solPrice > 0) {
                        snippet.append("Solana (SOL) is $${String.format("%,.2f", solPrice)} USD.")
                    }

                    items.add(
                        SearchItem(
                            title = "Real-Time Cryptocurrency Market Data",
                            url = "https://www.coingecko.com/en/coins/bitcoin",
                            snippet = snippet.toString().trim(),
                            source = "CoinGecko Market API"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Crypto price fetch failed: ${e.message}")
            }
        }

        // Check if query is about weather
        if (lower.contains("weather") || lower.contains("temperature") || lower.contains("forecast") || lower.contains("rain")) {
            try {
                val weatherUrl = "https://wttr.in/?format=j1"
                val request = Request.Builder()
                    .url(weatherUrl)
                    .header("User-Agent", USER_AGENT)
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val currentConditionArr = json.optJSONArray("current_condition")
                    val firstCondition = currentConditionArr?.optJSONObject(0)
                    val tempC = firstCondition?.optString("temp_C", "")
                    val tempF = firstCondition?.optString("temp_F", "")
                    val descArr = firstCondition?.optJSONArray("weatherDesc")
                    val desc = descArr?.optJSONObject(0)?.optString("value", "")
                    val humidity = firstCondition?.optString("humidity", "")
                    val windSpeed = firstCondition?.optString("windspeedKmph", "")

                    val nearestArea = json.optJSONArray("nearest_area")?.optJSONObject(0)
                    val areaName = nearestArea?.optJSONArray("areaName")?.optJSONObject(0)?.optString("value", "Current Location")
                    val country = nearestArea?.optJSONArray("country")?.optJSONObject(0)?.optString("value", "")

                    if (!tempC.isNullOrBlank()) {
                        val snippet = "Weather for $areaName, $country: $desc, Temperature: $tempC°C ($tempF°F), Humidity: $humidity%, Wind: $windSpeed km/h."
                        items.add(
                            SearchItem(
                                title = "Current Live Weather Observation",
                                url = "https://wttr.in",
                                snippet = snippet,
                                source = "Live Meteorological Feed"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Weather fetch failed: ${e.message}")
            }
        }

        // For general questions or if specific metrics didn't yield results, query general web search
        if (items.isEmpty()) {
            val general = searchWeb(query)
            items.addAll(general.results)
        }

        StructuredSearchResult(
            query = query,
            tool = "getCurrentInformation",
            results = items,
            error = if (items.isEmpty()) "Live information could not be retrieved at this time" else null
        )
    }

    private fun sanitizeQuery(query: String): String {
        return query
            .replace(Regex("(?i)^(who is|what is|tell me about|explain|search for|search|lookup|what's the|what happened to|what is the)\\s+"), "")
            .trim()
            .replace("?", "")
    }

    private fun cleanHtml(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#039;", "'")
            .trim()
    }

    private fun extractDomain(rawUrl: String): String {
        return try {
            val host = URL(rawUrl).host.lowercase()
            when {
                host.contains("reuters") -> "Reuters"
                host.contains("techcrunch") -> "TechCrunch"
                host.contains("theverge") -> "The Verge"
                host.contains("atlantic") -> "The Atlantic"
                host.contains("nytimes") -> "The New York Times"
                host.contains("bloomberg") -> "Bloomberg"
                host.contains("wired") -> "Wired"
                host.contains("bbc") -> "BBC News"
                host.contains("google") -> "Google AI"
                host.contains("ycombinator") -> "Hacker News"
                host.contains("wikipedia") -> "Wikipedia"
                host.startsWith("www.") -> host.removePrefix("www.").capitalizeDomain()
                else -> host.capitalizeDomain()
            }
        } catch (_: Exception) {
            "Web Source"
        }
    }

    private fun String.capitalizeDomain(): String {
        return this.split(".").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: this
    }
}

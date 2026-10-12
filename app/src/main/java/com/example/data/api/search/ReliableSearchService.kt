package com.example.data.api.search

import android.net.Uri
import android.util.Log
import com.example.data.model.SearchItem
import com.example.data.model.StructuredSearchResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Service contract for reliable, real-time web search and live data retrieval.
 * Replaces simulated search results with actual network data.
 */
interface ReliableSearchService {
    suspend fun searchWeb(query: String, limit: Int = 6): StructuredSearchResult
    suspend fun searchNews(query: String, limit: Int = 6): StructuredSearchResult
    suspend fun getCurrentInformation(query: String): StructuredSearchResult
    suspend fun getInstantAnswer(query: String): SearchItem?
}

/**
 * Production implementation of [ReliableSearchService] utilizing Retrofit,
 * Moshi, and OkHttp to query reliable live knowledge and search APIs.
 */
class RetrofitSearchServiceImpl(
    customClient: OkHttpClient? = null
) : ReliableSearchService {

    companion object {
        private const val TAG = "RetrofitSearchService"
        private const val USER_AGENT = "JARVIS-Android/2.0 (Live AI Assistant; Real-Time Search)"
        private const val WIKIPEDIA_BASE_URL = "https://en.wikipedia.org/"
        private const val DUCKDUCKGO_BASE_URL = "https://api.duckduckgo.com/"
        private const val HACKERNEWS_BASE_URL = "https://hn.algolia.com/"
        private const val COINGECKO_BASE_URL = "https://api.coingecko.com/"
    }

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val httpClient: OkHttpClient = customClient ?: run {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }

    private val wikiApi: WikipediaSearchApi by lazy {
        Retrofit.Builder()
            .baseUrl(WIKIPEDIA_BASE_URL)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WikipediaSearchApi::class.java)
    }

    private val duckDuckGoApi: DuckDuckGoSearchApi by lazy {
        Retrofit.Builder()
            .baseUrl(DUCKDUCKGO_BASE_URL)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DuckDuckGoSearchApi::class.java)
    }

    private val hackerNewsApi: HackerNewsSearchApi by lazy {
        Retrofit.Builder()
            .baseUrl(HACKERNEWS_BASE_URL)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(HackerNewsSearchApi::class.java)
    }

    private val cryptoApi: CryptoMarketApi by lazy {
        Retrofit.Builder()
            .baseUrl(COINGECKO_BASE_URL)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CryptoMarketApi::class.java)
    }

    override suspend fun searchWeb(query: String, limit: Int): StructuredSearchResult = withContext(Dispatchers.IO) {
        val cleanQuery = sanitizeQuery(query)
        if (cleanQuery.isBlank()) {
            return@withContext StructuredSearchResult(
                query = query,
                tool = "searchWeb",
                results = emptyList(),
                error = "Search query cannot be empty"
            )
        }

        val items = mutableListOf<SearchItem>()

        // 1. Query Wikipedia Search API via Retrofit
        try {
            val response = wikiApi.search(
                action = "query",
                list = "search",
                format = "json",
                utf8 = 1,
                limit = limit,
                query = cleanQuery
            )

            if (response.isSuccessful) {
                val searchList = response.body()?.query?.search ?: emptyList()
                for (item in searchList.take(limit)) {
                    val title = item.title.orEmpty().trim()
                    val rawSnippet = item.snippet.orEmpty()
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
            } else {
                Log.w(TAG, "Wikipedia Retrofit query returned code: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wikipedia Retrofit call failed: ${e.message}")
        }

        // 2. Query DuckDuckGo Instant Answer API via Retrofit
        try {
            val ddgResponse = duckDuckGoApi.getInstantAnswer(query = cleanQuery)
            if (ddgResponse.isSuccessful) {
                val body = ddgResponse.body()
                val abstractText = body?.abstractText.orEmpty().trim()
                val heading = body?.heading.orEmpty().ifBlank { cleanQuery }
                val abstractUrl = body?.abstractUrl.orEmpty()
                val abstractSource = body?.abstractSource.orEmpty().ifBlank { "DuckDuckGo" }

                if (abstractText.isNotBlank()) {
                    val finalUrl = if (abstractUrl.isNotBlank()) {
                        abstractUrl
                    } else {
                        "https://duckduckgo.com/?q=" + Uri.encode(cleanQuery)
                    }
                    items.add(
                        0,
                        SearchItem(
                            title = heading,
                            url = finalUrl,
                            snippet = abstractText,
                            source = abstractSource
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "DuckDuckGo Retrofit call failed: ${e.message}")
        }

        StructuredSearchResult(
            query = query,
            tool = "searchWeb",
            results = items,
            error = if (items.isEmpty()) "No verified live records found for '$query'" else null
        )
    }

    override suspend fun searchNews(query: String, limit: Int): StructuredSearchResult = withContext(Dispatchers.IO) {
        val cleanQuery = sanitizeQuery(query)
        val items = mutableListOf<SearchItem>()
        val searchQuery = if (cleanQuery.isBlank() || cleanQuery.equals("news", ignoreCase = true)) {
            "AI news"
        } else {
            cleanQuery
        }

        // 1. Query Hacker News Live News Search via Retrofit
        try {
            val response = hackerNewsApi.searchNewsByDate(
                query = searchQuery,
                tags = "story",
                hitsPerPage = limit
            )

            if (response.isSuccessful) {
                val hits = response.body()?.hits ?: emptyList()
                for (hit in hits) {
                    val title = hit.title.orEmpty().trim()
                    val rawUrl = hit.url.orEmpty()
                    val objectId = hit.objectId.orEmpty()
                    val finalUrl = if (rawUrl.isNotBlank()) {
                        rawUrl
                    } else if (objectId.isNotBlank()) {
                        "https://news.ycombinator.com/item?id=$objectId"
                    } else {
                        ""
                    }
                    val domain = extractDomain(finalUrl)

                    if (title.isNotBlank()) {
                        val authorTag = if (!hit.author.isNullOrBlank()) "By ${hit.author}" else ""
                        val pointsTag = if (hit.points != null) "${hit.points} points" else ""
                        val meta = listOf(authorTag, pointsTag).filter { it.isNotBlank() }.joinToString(" • ")
                        val snippet = if (meta.isNotBlank()) "Recent story: $title ($meta)" else "Recent story: $title"

                        items.add(
                            SearchItem(
                                title = title,
                                url = if (finalUrl.isNotBlank()) finalUrl else "https://news.google.com/search?q=" + Uri.encode(title),
                                snippet = snippet,
                                source = domain
                            )
                        )
                    }
                }
            } else {
                Log.w(TAG, "Hacker News Retrofit call returned code: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Hacker News Retrofit call failed: ${e.message}")
        }

        // 2. Fallback / supplement with Wikipedia topic search if fewer than 3 stories
        if (items.size < 3) {
            val supplementary = searchWeb(searchQuery, limit = 3)
            for (res in supplementary.results) {
                if (items.none { it.title.equals(res.title, ignoreCase = true) }) {
                    items.add(res)
                }
            }
        }

        StructuredSearchResult(
            query = query,
            tool = "getNews",
            results = items,
            error = if (items.isEmpty()) "No current news articles retrieved for '$query'" else null
        )
    }

    override suspend fun getCurrentInformation(query: String): StructuredSearchResult = withContext(Dispatchers.IO) {
        val lower = query.lowercase().trim()
        val items = mutableListOf<SearchItem>()

        // Cryptocurrency telemetry check
        if (lower.contains("bitcoin") || lower.contains("btc") || lower.contains("crypto") || lower.contains("price") || lower.contains("ethereum") || lower.contains("solana")) {
            try {
                val response = cryptoApi.getPrices()
                if (response.isSuccessful) {
                    val body = response.body()
                    val btc = body?.bitcoin
                    val eth = body?.ethereum
                    val sol = body?.solana

                    val sb = StringBuilder()
                    if (btc?.usd != null && btc.usd > 0) {
                        val change = btc.usd24hChange ?: 0.0
                        sb.append("Bitcoin (BTC): $${String.format("%,.2f", btc.usd)} USD (${String.format("%+.2f", change)}% 24h). ")
                    }
                    if (eth?.usd != null && eth.usd > 0) {
                        val change = eth.usd24hChange ?: 0.0
                        sb.append("Ethereum (ETH): $${String.format("%,.2f", eth.usd)} USD (${String.format("%+.2f", change)}% 24h). ")
                    }
                    if (sol?.usd != null && sol.usd > 0) {
                        val change = sol.usd24hChange ?: 0.0
                        sb.append("Solana (SOL): $${String.format("%,.2f", sol.usd)} USD (${String.format("%+.2f", change)}% 24h).")
                    }

                    if (sb.isNotBlank()) {
                        items.add(
                            SearchItem(
                                title = "Live Market Telemetry (Cryptocurrency)",
                                url = "https://www.coingecko.com/en/coins/bitcoin",
                                snippet = sb.toString().trim(),
                                source = "CoinGecko API"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Crypto Retrofit call failed: ${e.message}")
            }
        }

        // Instant answer knowledge check
        val instant = getInstantAnswer(query)
        if (instant != null && items.none { it.title == instant.title }) {
            items.add(instant)
        }

        // If still empty, perform web search
        if (items.isEmpty()) {
            return@withContext searchWeb(query)
        }

        StructuredSearchResult(
            query = query,
            tool = "getCurrentInformation",
            results = items,
            error = null
        )
    }

    override suspend fun getInstantAnswer(query: String): SearchItem? = withContext(Dispatchers.IO) {
        val cleanQuery = sanitizeQuery(query)
        if (cleanQuery.isBlank()) return@withContext null

        try {
            val response = duckDuckGoApi.getInstantAnswer(query = cleanQuery)
            if (response.isSuccessful) {
                val body = response.body()
                val text = body?.abstractText.orEmpty().trim()
                if (text.isNotBlank()) {
                    val heading = body?.heading.orEmpty().ifBlank { cleanQuery }
                    val url = body?.abstractUrl.orEmpty().ifBlank { "https://duckduckgo.com/?q=" + Uri.encode(cleanQuery) }
                    return@withContext SearchItem(
                        title = heading,
                        url = url,
                        snippet = text,
                        source = body?.abstractSource.orEmpty().ifBlank { "DuckDuckGo" }
                    )
                }
            }
        } catch (_: Exception) {}

        // Fallback to Wikipedia summary
        try {
            val summaryResp = wikiApi.getPageSummary(title = cleanQuery.replace(" ", "_"))
            if (summaryResp.isSuccessful) {
                val summary = summaryResp.body()
                val extract = summary?.extract.orEmpty().trim()
                if (extract.isNotBlank()) {
                    val pageUrl = summary?.contentUrls?.desktop?.page ?: ("https://en.wikipedia.org/wiki/" + URLEncoder.encode(cleanQuery.replace(" ", "_"), "UTF-8"))
                    return@withContext SearchItem(
                        title = summary?.title ?: cleanQuery,
                        url = pageUrl,
                        snippet = extract,
                        source = "Wikipedia Summary"
                    )
                }
            }
        } catch (_: Exception) {}

        null
    }

    private fun sanitizeQuery(raw: String): String {
        return raw.trim()
            .removePrefix("\"").removeSuffix("\"")
            .replace(Regex("(?i)^(?:search(?:\\s+for)?|what(?:'s|\\s+is)|tell\\s+me\\s+about|find|lookup)\\s+"), "")
            .trim()
    }

    private fun cleanHtml(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .trim()
    }

    private fun extractDomain(urlString: String): String {
        return try {
            if (urlString.isBlank()) return "Web Source"
            val host = URL(urlString).host.lowercase()
            host.removePrefix("www.")
        } catch (_: Exception) {
            "Web Source"
        }
    }
}

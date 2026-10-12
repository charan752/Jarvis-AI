package com.example.data.api.search

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Retrofit interface for Wikipedia Action and REST APIs.
 * Base URL: https://en.wikipedia.org/
 */
interface WikipediaSearchApi {
    @GET("w/api.php")
    suspend fun search(
        @Query("action") action: String = "query",
        @Query("list") list: String = "search",
        @Query("format") format: String = "json",
        @Query("utf8") utf8: Int = 1,
        @Query("srlimit") limit: Int = 10,
        @Query("srsearch") query: String
    ): Response<WikiSearchResponse>

    @GET("api/rest_v1/page/summary/{title}")
    suspend fun getPageSummary(
        @Path("title") title: String
    ): Response<WikiPageSummary>
}

/**
 * Retrofit interface for DuckDuckGo Instant Answer API.
 * Base URL: https://api.duckduckgo.com/
 */
interface DuckDuckGoSearchApi {
    @GET("/")
    suspend fun getInstantAnswer(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("no_html") noHtml: Int = 1,
        @Query("skip_disambig") skipDisambig: Int = 1
    ): Response<DdgInstantResponse>
}

/**
 * Retrofit interface for Hacker News Algolia Live News Search API.
 * Base URL: https://hn.algolia.com/
 */
interface HackerNewsSearchApi {
    @GET("api/v1/search_by_date")
    suspend fun searchNewsByDate(
        @Query("query") query: String,
        @Query("tags") tags: String = "story",
        @Query("hitsPerPage") hitsPerPage: Int = 10
    ): Response<HnSearchResponse>

    @GET("api/v1/search")
    suspend fun searchNews(
        @Query("query") query: String,
        @Query("tags") tags: String = "story",
        @Query("hitsPerPage") hitsPerPage: Int = 10
    ): Response<HnSearchResponse>
}

/**
 * Retrofit interface for Live Market and Cryptocurrency Data (CoinGecko).
 * Base URL: https://api.coingecko.com/
 */
interface CryptoMarketApi {
    @GET("api/v3/simple/price")
    suspend fun getPrices(
        @Query("ids") ids: String = "bitcoin,ethereum,solana",
        @Query("vs_currencies") vsCurrencies: String = "usd",
        @Query("include_24hr_change") includeChange: Boolean = true
    ): Response<CryptoPriceResponse>
}

/**
 * Generic dynamic web interface for direct endpoints
 */
interface DynamicSearchApi {
    @GET
    suspend fun getRaw(@Url url: String): Response<String>
}

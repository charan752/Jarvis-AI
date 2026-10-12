package com.example.data.api.search

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data Transfer Objects for Retrofit Search APIs.
 * Models Wikipedia, DuckDuckGo, Hacker News, and Market Data responses.
 */

// --- Wikipedia Search API DTOs ---

@JsonClass(generateAdapter = true)
data class WikiSearchResponse(
    @Json(name = "query") val query: WikiQuery? = null
)

@JsonClass(generateAdapter = true)
data class WikiQuery(
    @Json(name = "search") val search: List<WikiSearchResultItem>? = null
)

@JsonClass(generateAdapter = true)
data class WikiSearchResultItem(
    @Json(name = "title") val title: String? = null,
    @Json(name = "snippet") val snippet: String? = null,
    @Json(name = "pageid") val pageId: Long? = null,
    @Json(name = "size") val size: Int? = null,
    @Json(name = "wordcount") val wordCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class WikiPageSummary(
    @Json(name = "title") val title: String? = null,
    @Json(name = "extract") val extract: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "content_urls") val contentUrls: WikiContentUrls? = null
)

@JsonClass(generateAdapter = true)
data class WikiContentUrls(
    @Json(name = "desktop") val desktop: WikiDesktopUrl? = null
)

@JsonClass(generateAdapter = true)
data class WikiDesktopUrl(
    @Json(name = "page") val page: String? = null
)

// --- DuckDuckGo Instant Answer API DTOs ---

@JsonClass(generateAdapter = true)
data class DdgInstantResponse(
    @Json(name = "AbstractText") val abstractText: String? = null,
    @Json(name = "Heading") val heading: String? = null,
    @Json(name = "AbstractURL") val abstractUrl: String? = null,
    @Json(name = "AbstractSource") val abstractSource: String? = null,
    @Json(name = "RelatedTopics") val relatedTopics: List<DdgRelatedTopic>? = null
)

@JsonClass(generateAdapter = true)
data class DdgRelatedTopic(
    @Json(name = "Text") val text: String? = null,
    @Json(name = "FirstURL") val firstUrl: String? = null
)

// --- Hacker News Algolia Live News Search API DTOs ---

@JsonClass(generateAdapter = true)
data class HnSearchResponse(
    @Json(name = "hits") val hits: List<HnHitItem>? = null,
    @Json(name = "nbHits") val nbHits: Int? = null,
    @Json(name = "query") val query: String? = null
)

@JsonClass(generateAdapter = true)
data class HnHitItem(
    @Json(name = "title") val title: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "author") val author: String? = null,
    @Json(name = "points") val points: Int? = null,
    @Json(name = "num_comments") val numComments: Int? = null,
    @Json(name = "objectID") val objectId: String? = null
)

// --- Cryptocurrency Market Telemetry DTOs ---

@JsonClass(generateAdapter = true)
data class CryptoPriceResponse(
    @Json(name = "bitcoin") val bitcoin: CryptoCurrencyItem? = null,
    @Json(name = "ethereum") val ethereum: CryptoCurrencyItem? = null,
    @Json(name = "solana") val solana: CryptoCurrencyItem? = null
)

@JsonClass(generateAdapter = true)
data class CryptoCurrencyItem(
    @Json(name = "usd") val usd: Double? = null,
    @Json(name = "usd_24h_change") val usd24hChange: Double? = null
)

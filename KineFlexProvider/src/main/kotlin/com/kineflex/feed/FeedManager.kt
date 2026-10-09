package com.kineflex.feed

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.kineflex.models.KineFlexJson
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.app

/**
 * Handles network requests, parsing, deduplication, and transformation
 * of external JSON category feeds into CloudStream SearchResponse items.
 */
object FeedManager {

    /**
     * Fetches and parses a category feed from the specified URL.
     * Returns an empty list upon HTTP errors, timeouts, or empty feeds.
     */
    suspend fun loadFeed(url: String, providerName: String): List<SearchResponse> {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isEmpty() || !trimmedUrl.startsWith("http")) {
            return emptyList()
        }

        return try {
            val response = app.get(
                url = trimmedUrl,
                timeout = 25L,
                headers = mapOf(
                    "Accept" to "application/json, text/plain, */*",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                )
            )

            if (!response.isSuccessful) {
                return emptyList()
            }

            val responseBody = response.text
            if (responseBody.isBlank()) {
                return emptyList()
            }

            parseFeedBody(responseBody, providerName)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Parses the JSON text, handling both direct arrays and nested wrapper objects
     * like {"results": [...]}, {"data": [...]}, or {"items": [...]}.
     */
    fun parseFeedBody(jsonBody: String, providerName: String): List<SearchResponse> {
        val rawItems = try {
            val rootNode: JsonNode = KineFlexJson.mapper.readTree(jsonBody)

            val arrayNode: JsonNode = when {
                rootNode.isArray -> rootNode
                rootNode.has("results") && rootNode["results"].isArray -> rootNode["results"]
                rootNode.has("data") && rootNode["data"].isArray -> rootNode["data"]
                rootNode.has("items") && rootNode["items"].isArray -> rootNode["items"]
                rootNode.has("movies") && rootNode["movies"].isArray -> rootNode["movies"]
                else -> return emptyList()
            }

            KineFlexJson.mapper.convertValue(
                arrayNode,
                object : TypeReference<List<FeedItem>>() {}
            )
        } catch (_: Throwable) {
            emptyList()
        }

        if (rawItems.isNullOrEmpty()) {
            return emptyList()
        }

        val seenTmdbIds = HashSet<Int>()
        val resultList = ArrayList<SearchResponse>()

        for (item in rawItems) {
            val tmdbId = item.tmdbId ?: continue
            // Deduplicate across the feed list
            if (!seenTmdbIds.add(tmdbId)) continue

            val searchItem = item.toSearchResponse(providerName)
            if (searchItem != null) {
                resultList.add(searchItem)
            }
        }

        return resultList
    }
}

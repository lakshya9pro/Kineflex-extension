package com.kineflex.kineflex

import com.kineflex.config.KineFlexConfig
import com.kineflex.models.KineFlexJson
import com.kineflex.settings.KineFlexSettings
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

/**
 * Resolver client for the KineFlex streaming API.
 * Resolves authorized direct and HLS video playback streams for movies and TV episodes.
 */
object KineFlexApi {

    /**
     * Resolves a playable stream for a movie given its TMDB ID.
     */
    suspend fun resolveMovie(tmdbId: Int): ExtractorLink {
        val apiKey = KineFlexSettings.getKineFlexApiKey()
        if (apiKey.isEmpty()) {
            throw KineFlexAuthException("KineFlex API Key is not configured. Please open extension settings and configure your key.")
        }

        val url = "${KineFlexConfig.KINEFLEX_API_BASE_URL}/v1/movie/$tmdbId"
        return executeStreamRequest(url, apiKey, "Movie $tmdbId")
    }

    /**
     * Resolves a playable stream for a TV episode given its TMDB ID, season, and episode numbers.
     */
    suspend fun resolveTvEpisode(tmdbId: Int, season: Int, episode: Int): ExtractorLink {
        val apiKey = KineFlexSettings.getKineFlexApiKey()
        if (apiKey.isEmpty()) {
            throw KineFlexAuthException("KineFlex API Key is not configured. Please open extension settings and configure your key.")
        }

        val url = "${KineFlexConfig.KINEFLEX_API_BASE_URL}/v1/tv/$tmdbId/$season/$episode"
        return executeStreamRequest(url, apiKey, "TV $tmdbId S${season}E${episode}")
    }

    /**
     * Executes the authorized HTTP request to KineFlex, validates the response,
     * extracts streaming headers and media format, and builds the ExtractorLink.
     */
    @Suppress("DEPRECATION")
    private suspend fun executeStreamRequest(endpointUrl: String, apiKey: String, label: String): ExtractorLink {
        val response = try {
            app.get(
                url = endpointUrl,
                headers = mapOf(
                    "Authorization" to "Bearer $apiKey",
                    "Accept" to "application/json",
                    "User-Agent" to "KineFlex-CloudStream/1.0"
                ),
                timeout = 20L
            )
        } catch (e: Throwable) {
            throw KineFlexServerException("Network failure communicating with KineFlex: ${e.message ?: "Timeout"}")
        }

        when (response.code) {
            401, 403 -> throw KineFlexAuthException("Unauthorized: Your KineFlex API key is invalid or expired.")
            404 -> throw KineFlexNotFoundException("Stream not found for $label on KineFlex.")
            429 -> throw KineFlexQuotaException("Rate limit reached or daily points quota exhausted.")
            in 500..599 -> throw KineFlexServerException("KineFlex server returned error code ${response.code}.")
        }

        if (!response.isSuccessful) {
            throw KineFlexServerException("Unexpected KineFlex HTTP status ${response.code}.")
        }

        val kineFlexResponse = KineFlexJson.parseSafe<KineFlexResponse>(response.text)
            ?: throw KineFlexServerException("Received malformed JSON from KineFlex API.")

        if (!kineFlexResponse.success) {
            val errMsg = kineFlexResponse.message?.takeIf { it.isNotBlank() }
                ?: "KineFlex indicated request was unsuccessful."
            throw KineFlexServerException(errMsg)
        }

        val streamData = kineFlexResponse.data
            ?: throw KineFlexNotFoundException("No stream data provided by KineFlex for $label.")

        val streamUrl = streamData.url?.trim()
            ?: throw KineFlexNotFoundException("Stream URL is missing for $label.")

        if (streamUrl.isEmpty()) {
            throw KineFlexNotFoundException("Stream URL is blank for $label.")
        }

        // Check quota exhaustion in usage object if reported
        kineFlexResponse.usage?.remainingPoints?.let { remaining ->
            if (remaining <= 0) {
                // If points reached 0, log or warn; stream might still be valid for this call
            }
        }

        val headersMap = streamData.headers.orEmpty()
        val referer = headersMap["Referer"] ?: headersMap["referer"] ?: ""
        val isM3u8 = streamUrl.contains(".m3u8", ignoreCase = true)

        val streamName = streamData.name?.takeIf { it.isNotBlank() } ?: KineFlexConfig.PROVIDER_NAME

        return ExtractorLink(
            source = KineFlexConfig.PROVIDER_NAME,
            name = streamName,
            url = streamUrl,
            referer = referer,
            quality = Qualities.Unknown.value,
            isM3u8 = isM3u8,
            headers = headersMap
        )
    }

    /**
     * Validates connection with KineFlex using the user-provided API key.
     * Never logs the key.
     */
    suspend fun testConnection(apiKey: String): Result<String> {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(Exception("KineFlex API key is empty"))
        }

        // Test with reference movie 550 (Fight Club)
        val url = "${KineFlexConfig.KINEFLEX_API_BASE_URL}/v1/movie/550"

        return try {
            val response = app.get(
                url = url,
                headers = mapOf(
                    "Authorization" to "Bearer $trimmed",
                    "Accept" to "application/json"
                ),
                timeout = 10L
            )

            when (response.code) {
                200 -> {
                    val kineFlexResponse = KineFlexJson.parseSafe<KineFlexResponse>(response.text)
                    val points = kineFlexResponse?.usage?.remainingPoints
                    val msg = if (points != null) {
                        "Connected successfully! Remaining points: $points"
                    } else {
                        "Connected successfully!"
                    }
                    Result.success(msg)
                }
                401, 403 -> Result.failure(Exception("Invalid or expired API key (HTTP ${response.code})"))
                429 -> Result.failure(Exception("Points exhausted or rate limit hit (HTTP 429)"))
                else -> Result.failure(Exception("Server returned status HTTP ${response.code}"))
            }
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Connection timed out"))
        }
    }
}

package com.kineflex.tmdb

import com.fasterxml.jackson.core.type.TypeReference
import com.kineflex.config.KineFlexConfig
import com.kineflex.models.KineFlexJson
import com.kineflex.models.MediaData
import com.kineflex.settings.KineFlexSettings
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import java.net.URLEncoder

/**
 * Client for The Movie Database (TMDB) API.
 * Handles search, metadata retrieval for movies and TV shows, and image path resolution.
 */
object TmdbApi {

    /**
     * Builds request headers and parameters based on whether the credential
     * is a v4 Read-Only Bearer Token or a v3 API Key.
     */
    private fun getAuthHeadersAndParams(): Pair<Map<String, String>, Map<String, String>> {
        val credential = KineFlexSettings.getTmdbCredential()
        val headers = mutableMapOf(
            "Accept" to "application/json"
        )
        val params = mutableMapOf<String, String>()

        if (credential.isNotEmpty()) {
            if (credential.startsWith("eyJ") || credential.length > 40) {
                headers["Authorization"] = "Bearer $credential"
            } else {
                params["api_key"] = credential
            }
        }
        return Pair(headers, params)
    }

    /**
     * Resolves a fully-qualified TMDB image URL.
     */
    fun getImageUrl(path: String?, size: String = "w500"): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "https://image.tmdb.org/t/p/$size$cleanPath"
    }

    /**
     * Searches TMDB for both movies and TV series, returning standard CloudStream SearchResponse items.
     */
    suspend fun search(query: String, page: Int = 1): List<SearchResponse> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()

        val (headers, baseParams) = getAuthHeadersAndParams()
        val results = mutableListOf<SearchResponse>()

        // 1. Search Movies
        try {
            val movieParams = baseParams.toMutableMap().apply {
                put("query", cleanQuery)
                put("page", page.toString())
                put("include_adult", "false")
            }
            val movieUrl = "${KineFlexConfig.TMDB_API_BASE_URL}/search/movie"
            val movieResponse = app.get(movieUrl, headers = headers, params = movieParams, timeout = 15L)

            if (movieResponse.isSuccessful) {
                val searchResult: TmdbSearchResponse<TmdbMovieSearchItem>? =
                    KineFlexJson.mapper.readValue(
                        movieResponse.text,
                        object : TypeReference<TmdbSearchResponse<TmdbMovieSearchItem>>() {}
                    )

                searchResult?.results?.forEach { movie ->
                    val title = movie.title?.trim().orEmpty()
                    if (title.isNotEmpty()) {
                        val year = movie.releaseDate?.takeIf { it.length >= 4 }?.substring(0, 4)?.toIntOrNull()
                        val poster = getImageUrl(movie.posterPath)
                        val loadData = MediaData(
                            id = movie.id,
                            type = "movie",
                            title = title,
                            year = year,
                            posterUrl = poster
                        ).toJson()

                        results.add(
                            newMovieSearchResponse(title, loadData, TvType.Movie) {
                                this.posterUrl = poster
                                this.year = year
                            }
                        )
                    }
                }
            }
        } catch (_: Throwable) {}

        // 2. Search TV Shows
        try {
            val tvParams = baseParams.toMutableMap().apply {
                put("query", cleanQuery)
                put("page", page.toString())
                put("include_adult", "false")
            }
            val tvUrl = "${KineFlexConfig.TMDB_API_BASE_URL}/search/tv"
            val tvResponse = app.get(tvUrl, headers = headers, params = tvParams, timeout = 15L)

            if (tvResponse.isSuccessful) {
                val searchResult: TmdbSearchResponse<TmdbTvSearchItem>? =
                    KineFlexJson.mapper.readValue(
                        tvResponse.text,
                        object : TypeReference<TmdbSearchResponse<TmdbTvSearchItem>>() {}
                    )

                searchResult?.results?.forEach { tv ->
                    val title = tv.name?.trim().orEmpty()
                    if (title.isNotEmpty()) {
                        val year = tv.firstAirDate?.takeIf { it.length >= 4 }?.substring(0, 4)?.toIntOrNull()
                        val poster = getImageUrl(tv.posterPath)
                        val loadData = MediaData(
                            id = tv.id,
                            type = "tv",
                            title = title,
                            year = year,
                            posterUrl = poster
                        ).toJson()

                        results.add(
                            newTvSeriesSearchResponse(title, loadData, TvType.TvSeries) {
                                this.posterUrl = poster
                                this.year = year
                            }
                        )
                    }
                }
            }
        } catch (_: Throwable) {}

        return results
    }

    /**
     * Retrieves full details for a TMDB Movie.
     */
    suspend fun getMovieDetails(tmdbId: Int): TmdbMovieDetails? {
        val (headers, params) = getAuthHeadersAndParams()
        val url = "${KineFlexConfig.TMDB_API_BASE_URL}/movie/$tmdbId"
        val fullParams = params.toMutableMap().apply {
            put("append_to_response", "credits,external_ids")
        }

        return try {
            val response = app.get(url, headers = headers, params = fullParams, timeout = 15L)
            if (response.isSuccessful) {
                KineFlexJson.mapper.readValue(response.text, TmdbMovieDetails::class.java)
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Retrieves full details for a TMDB TV Series.
     */
    suspend fun getTvDetails(tmdbId: Int): TmdbTvDetails? {
        val (headers, params) = getAuthHeadersAndParams()
        val url = "${KineFlexConfig.TMDB_API_BASE_URL}/tv/$tmdbId"
        val fullParams = params.toMutableMap().apply {
            put("append_to_response", "credits,external_ids")
        }

        return try {
            val response = app.get(url, headers = headers, params = fullParams, timeout = 15L)
            if (response.isSuccessful) {
                KineFlexJson.mapper.readValue(response.text, TmdbTvDetails::class.java)
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Retrieves episode information for a specific TV Season.
     */
    suspend fun getSeasonEpisodes(tmdbId: Int, seasonNumber: Int): List<TmdbTvEpisode> {
        val (headers, params) = getAuthHeadersAndParams()
        val url = "${KineFlexConfig.TMDB_API_BASE_URL}/tv/$tmdbId/season/$seasonNumber"

        return try {
            val response = app.get(url, headers = headers, params = params, timeout = 15L)
            if (response.isSuccessful) {
                val seasonDetails = KineFlexJson.mapper.readValue(response.text, TmdbSeasonDetails::class.java)
                seasonDetails.episodes.orEmpty()
            } else {
                emptyList()
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Tests whether the configured TMDB credential is valid.
     */
    suspend fun testCredential(credential: String): Result<String> {
        val trimmed = credential.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(Exception("TMDB credential is empty"))
        }

        val headers = mutableMapOf("Accept" to "application/json")
        val params = mutableMapOf<String, String>()

        if (trimmed.startsWith("eyJ") || trimmed.length > 40) {
            headers["Authorization"] = "Bearer $trimmed"
        } else {
            params["api_key"] = trimmed
        }

        val url = "${KineFlexConfig.TMDB_API_BASE_URL}/authentication"

        return try {
            val response = app.get(url, headers = headers, params = params, timeout = 10L)
            if (response.isSuccessful) {
                Result.success("TMDB connection verified successfully")
            } else {
                Result.failure(Exception("TMDB returned HTTP ${response.code}"))
            }
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Network error connecting to TMDB"))
        }
    }
}

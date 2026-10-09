package com.kineflex.feed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.kineflex.config.KineFlexConfig
import com.kineflex.models.MediaData
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse

/**
 * Model representing a single media item in a JSON category feed.
 * Conforms to the real schema returned by https://www.jsonkeeper.com/b/IHDLH
 * while also gracefully accepting common variations in field names.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class FeedItem(
    @JsonProperty("id")
    val id: Any? = null,

    @JsonProperty("title")
    val title: String? = null,

    @JsonProperty("name")
    val name: String? = null,

    @JsonProperty("year")
    val year: Any? = null,

    @JsonProperty("release_date")
    val releaseDate: String? = null,

    @JsonProperty("first_air_date")
    val firstAirDate: String? = null,

    @JsonProperty("imgs")
    val imgs: String? = null,

    @JsonProperty("poster")
    val poster: String? = null,

    @JsonProperty("poster_path")
    val posterPath: String? = null,

    @JsonProperty("image")
    val image: String? = null,

    @JsonProperty("type")
    val type: String? = null,

    @JsonProperty("overview")
    val overview: String? = null,

    @JsonProperty("description")
    val description: String? = null,

    @JsonProperty("rating")
    val rating: Any? = null,

    @JsonProperty("vote_average")
    val voteAverage: Any? = null,
) {
    /**
     * Extracts and validates the integer TMDB ID.
     */
    val tmdbId: Int?
        get() = when (id) {
            is Number -> id.toInt()
            is String -> id.trim().toIntOrNull()
            else -> null
        }

    /**
     * Resolves the primary title of the item.
     */
    val displayTitle: String
        get() = title?.trim()?.takeIf { it.isNotEmpty() }
            ?: name?.trim()?.takeIf { it.isNotEmpty() }
            ?: ""

    /**
     * Resolves the 4-digit release year.
     */
    val displayYear: Int?
        get() {
            when (year) {
                is Number -> return year.toInt()
                is String -> {
                    val parsed = year.trim().toIntOrNull()
                    if (parsed != null) return parsed
                    if (year.length >= 4) {
                        val sub = year.substring(0, 4).toIntOrNull()
                        if (sub != null) return sub
                    }
                }
            }
            val dateStr = releaseDate ?: firstAirDate
            if (!dateStr.isNullOrBlank() && dateStr.length >= 4) {
                return dateStr.substring(0, 4).toIntOrNull()
            }
            return null
        }

    /**
     * Determines whether this item is a TV series or a movie.
     */
    val isTvSeries: Boolean
        get() {
            val t = type?.trim()?.lowercase()
            return t == "tv" || t == "series" || t == "tvshow" || t == "tv_series"
        }

    /**
     * Resolves a fully qualified poster URL.
     */
    val posterUrl: String?
        get() {
            val raw = imgs?.trim()?.takeIf { it.isNotEmpty() }
                ?: poster?.trim()?.takeIf { it.isNotEmpty() }
                ?: image?.trim()?.takeIf { it.isNotEmpty() }
                ?: posterPath?.trim()?.takeIf { it.isNotEmpty() }

            if (raw == null) return null
            if (raw.startsWith("http://") || raw.startsWith("https://")) {
                return raw
            }
            // If it's a TMDB relative path like "/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg"
            val sanitized = if (raw.startsWith("/")) raw else "/$raw"
            return "${KineFlexConfig.TMDB_IMAGE_BASE_W500}$sanitized"
        }

    /**
     * Maps the FeedItem into CloudStream's standard [SearchResponse]
     * preserving the TMDB ID, media type, and poster URL.
     */
    fun toSearchResponse(providerName: String): SearchResponse? {
        val resolvedId = tmdbId ?: return null
        val resolvedTitle = displayTitle
        if (resolvedTitle.isBlank()) return null

        val mediaType = if (isTvSeries) "tv" else "movie"
        val loadData = MediaData(
            id = resolvedId,
            type = mediaType,
            title = resolvedTitle,
            year = displayYear,
            posterUrl = posterUrl
        ).toJson()

        return if (isTvSeries) {
            newTvSeriesSearchResponse(resolvedTitle, loadData, TvType.TvSeries) {
                this.posterUrl = this@FeedItem.posterUrl
                this.year = this@FeedItem.displayYear
            }
        } else {
            newMovieSearchResponse(resolvedTitle, loadData, TvType.Movie) {
                this.posterUrl = this@FeedItem.posterUrl
                this.year = this@FeedItem.displayYear
            }
        }
    }
}

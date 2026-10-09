package com.kineflex.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Encapsulates the media reference passed through CloudStream's load and playback pipeline.
 * Retains the TMDB ID, media type ("movie" or "tv"), and TV episode coordinates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MediaData(
    @JsonProperty("id")
    val id: Int,

    @JsonProperty("type")
    val type: String, // "movie" or "tv"

    @JsonProperty("title")
    val title: String? = null,

    @JsonProperty("season")
    val season: Int? = null,

    @JsonProperty("episode")
    val episode: Int? = null,

    @JsonProperty("year")
    val year: Int? = null,

    @JsonProperty("posterUrl")
    val posterUrl: String? = null,
) {
    fun toJson(): String = KineFlexJson.stringify(this)

    companion object {
        fun fromJson(json: String?): MediaData? = KineFlexJson.parseSafe<MediaData>(json)
    }
}

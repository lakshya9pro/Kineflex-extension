package com.kineflex.tmdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbSearchResponse<T>(
    @JsonProperty("page") val page: Int? = null,
    @JsonProperty("results") val results: List<T>? = null,
    @JsonProperty("total_pages") val totalPages: Int? = null,
    @JsonProperty("total_results") val totalResults: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbMovieSearchItem(
    @JsonProperty("id") val id: Int,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("overview") val overview: String? = null,
    @JsonProperty("poster_path") val posterPath: String? = null,
    @JsonProperty("backdrop_path") val backdropPath: String? = null,
    @JsonProperty("release_date") val releaseDate: String? = null,
    @JsonProperty("vote_average") val voteAverage: Double? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbTvSearchItem(
    @JsonProperty("id") val id: Int,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("overview") val overview: String? = null,
    @JsonProperty("poster_path") val posterPath: String? = null,
    @JsonProperty("backdrop_path") val backdropPath: String? = null,
    @JsonProperty("first_air_date") val firstAirDate: String? = null,
    @JsonProperty("vote_average") val voteAverage: Double? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbGenre(
    @JsonProperty("id") val id: Int? = null,
    @JsonProperty("name") val name: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbMovieDetails(
    @JsonProperty("id") val id: Int,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("overview") val overview: String? = null,
    @JsonProperty("poster_path") val posterPath: String? = null,
    @JsonProperty("backdrop_path") val backdropPath: String? = null,
    @JsonProperty("release_date") val releaseDate: String? = null,
    @JsonProperty("runtime") val runtime: Int? = null,
    @JsonProperty("vote_average") val voteAverage: Double? = null,
    @JsonProperty("genres") val genres: List<TmdbGenre>? = null,
    @JsonProperty("tagline") val tagline: String? = null,
    @JsonProperty("status") val status: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbTvSeasonSummary(
    @JsonProperty("id") val id: Int? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("overview") val overview: String? = null,
    @JsonProperty("season_number") val seasonNumber: Int? = null,
    @JsonProperty("episode_count") val episodeCount: Int? = null,
    @JsonProperty("poster_path") val posterPath: String? = null,
    @JsonProperty("air_date") val airDate: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbTvDetails(
    @JsonProperty("id") val id: Int,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("overview") val overview: String? = null,
    @JsonProperty("poster_path") val posterPath: String? = null,
    @JsonProperty("backdrop_path") val backdropPath: String? = null,
    @JsonProperty("first_air_date") val firstAirDate: String? = null,
    @JsonProperty("episode_run_time") val episodeRunTime: List<Int>? = null,
    @JsonProperty("vote_average") val voteAverage: Double? = null,
    @JsonProperty("genres") val genres: List<TmdbGenre>? = null,
    @JsonProperty("number_of_seasons") val numberOfSeasons: Int? = null,
    @JsonProperty("number_of_episodes") val numberOfEpisodes: Int? = null,
    @JsonProperty("seasons") val seasons: List<TmdbTvSeasonSummary>? = null,
    @JsonProperty("status") val status: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbTvEpisode(
    @JsonProperty("id") val id: Int? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("overview") val overview: String? = null,
    @JsonProperty("episode_number") val episodeNumber: Int,
    @JsonProperty("season_number") val seasonNumber: Int,
    @JsonProperty("still_path") val stillPath: String? = null,
    @JsonProperty("vote_average") val voteAverage: Double? = null,
    @JsonProperty("air_date") val airDate: String? = null,
    @JsonProperty("runtime") val runtime: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbSeasonDetails(
    @JsonProperty("id") val id: Int? = null,
    @JsonProperty("season_number") val seasonNumber: Int? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("overview") val overview: String? = null,
    @JsonProperty("episodes") val episodes: List<TmdbTvEpisode>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbAuthCheckResponse(
    @JsonProperty("success") val success: Boolean = false,
    @JsonProperty("status_code") val statusCode: Int? = null,
    @JsonProperty("status_message") val statusMessage: String? = null,
)

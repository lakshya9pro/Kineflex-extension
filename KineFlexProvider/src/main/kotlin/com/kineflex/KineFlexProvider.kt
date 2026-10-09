package com.kineflex

import com.kineflex.config.KineFlexConfig
import com.kineflex.feed.FeedManager
import com.kineflex.kineflex.KineFlexApi
import com.kineflex.kineflex.KineFlexAuthException
import com.kineflex.kineflex.KineFlexNotFoundException
import com.kineflex.kineflex.KineFlexQuotaException
import com.kineflex.kineflex.KineFlexServerException
import com.kineflex.models.MediaData
import com.kineflex.settings.KineFlexSettings
import com.kineflex.tmdb.TmdbApi
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageData
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink

/**
 * CloudStream MainAPI implementation for KineFlex.
 * Integrates:
 * - Four configurable JSON category feeds for the Home screen
 * - TMDB for searching movies and TV series and fetching metadata
 * - KineFlex API for resolving authorized movie and TV episode streams
 */
class KineFlexProvider : MainAPI() {

    override var name = KineFlexConfig.PROVIDER_NAME
    override var mainUrl = KineFlexConfig.KINEFLEX_API_BASE_URL
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "en"
    override val hasMainPage = true
    override val hasQuickSearch = false

    /**
     * Define the 4 home page categories:
     * 1. Latest Now
     * 2. Trending Now
     * 3. Premium
     * 4. Other
     */
    override val mainPage: List<MainPageData>
        get() = KineFlexConfig.getMainPages { category ->
            KineFlexSettings.getCategoryUrl(category)
        }

    /**
     * Loads items for each home page category from its configured JSON URL.
     */
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val feedUrl = request.data
        if (feedUrl.isBlank()) {
            return newHomePageResponse(request.name, emptyList<SearchResponse>(), hasNext = false)
        }

        // Each category feed is loaded via FeedManager
        val items = FeedManager.loadFeed(feedUrl, name)

        return newHomePageResponse(request.name, items, hasNext = false)
    }

    /**
     * Searches TMDB for movies and TV shows matching the query string.
     */
    override suspend fun search(query: String): List<SearchResponse> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return emptyList<SearchResponse>()

        if (!KineFlexSettings.isTmdbConfigured()) {
            try {
                showToast("Notice: For best results, configure your TMDB credential in KineFlex settings.")
            } catch (_: Throwable) {}
        }

        return TmdbApi.search(cleanQuery)
    }

    /**
     * Loads full metadata from TMDB for a selected movie or TV series.
     */
    override suspend fun load(url: String): LoadResponse? {
        // Attempt to parse the structured MediaData
        var mediaData = MediaData.fromJson(url)

        // Fallback for direct URLs or raw IDs
        if (mediaData == null) {
            val tmdbId = url.substringAfterLast("/").toIntOrNull()
                ?: url.toIntOrNull()
                ?: return null

            val isTv = url.contains("tv", ignoreCase = true)
            mediaData = MediaData(
                id = tmdbId,
                type = if (isTv) "tv" else "movie"
            )
        }

        val tmdbId = mediaData.id
        val isTv = mediaData.type.equals("tv", ignoreCase = true)

        return if (isTv) {
            loadTvSeries(tmdbId, url)
        } else {
            loadMovie(tmdbId, url)
        }
    }

    /**
     * Fetches and constructs a MovieLoadResponse using TMDB metadata.
     */
    private suspend fun loadMovie(tmdbId: Int, originalUrl: String): LoadResponse? {
        val details = TmdbApi.getMovieDetails(tmdbId)

        val title = details?.title?.takeIf { it.isNotBlank() }
            ?: "Movie $tmdbId"

        val year = details?.releaseDate?.takeIf { it.length >= 4 }?.substring(0, 4)?.toIntOrNull()
        val poster = TmdbApi.getImageUrl(details?.posterPath)
        val backdrop = TmdbApi.getImageUrl(details?.backdropPath, size = "original")
        val plot = details?.overview
        val rating = details?.voteAverage?.times(10)?.toInt()
        val genres = details?.genres?.mapNotNull { it.name }
        val duration = details?.runtime

        // Playback data payload retained for loadLinks
        val playData = MediaData(
            id = tmdbId,
            type = "movie",
            title = title,
            year = year,
            posterUrl = poster
        ).toJson()

        return newMovieLoadResponse(title, originalUrl, TvType.Movie, playData) {
            this.posterUrl = poster
            this.backgroundPosterUrl = backdrop
            this.year = year
            this.plot = plot
            this.rating = rating
            this.tags = genres
            this.duration = duration
        }
    }

    /**
     * Fetches and constructs a TvSeriesLoadResponse using TMDB metadata,
     * including seasons and individual episodes.
     */
    private suspend fun loadTvSeries(tmdbId: Int, originalUrl: String): LoadResponse? {
        val details = TmdbApi.getTvDetails(tmdbId)

        val title = details?.name?.takeIf { it.isNotBlank() }
            ?: "TV Series $tmdbId"

        val year = details?.firstAirDate?.takeIf { it.length >= 4 }?.substring(0, 4)?.toIntOrNull()
        val poster = TmdbApi.getImageUrl(details?.posterPath)
        val backdrop = TmdbApi.getImageUrl(details?.backdropPath, size = "original")
        val plot = details?.overview
        val rating = details?.voteAverage?.times(10)?.toInt()
        val genres = details?.genres?.mapNotNull { it.name }

        val episodes = mutableListOf<Episode>()

        // Fetch episodes for all valid seasons
        details?.seasons?.forEach { seasonSummary ->
            val seasonNum = seasonSummary.seasonNumber ?: return@forEach
            // Skip Specials (Season 0) unless desired
            if (seasonNum < 1) return@forEach

            val seasonEpisodes = TmdbApi.getSeasonEpisodes(tmdbId, seasonNum)
            for (ep in seasonEpisodes) {
                val epData = MediaData(
                    id = tmdbId,
                    type = "tv",
                    title = title,
                    season = ep.seasonNumber,
                    episode = ep.episodeNumber
                ).toJson()

                episodes.add(
                    newEpisode(epData) {
                        this.name = ep.name?.takeIf { it.isNotBlank() } ?: "Episode ${ep.episodeNumber}"
                        this.season = ep.seasonNumber
                        this.episode = ep.episodeNumber
                        this.posterUrl = TmdbApi.getImageUrl(ep.stillPath)
                        this.description = ep.overview
                        this.rating = ep.voteAverage?.times(10)?.toInt()
                        this.runTime = ep.runtime
                    }
                )
            }
        }

        return newTvSeriesLoadResponse(title, originalUrl, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.backgroundPosterUrl = backdrop
            this.year = year
            this.plot = plot
            this.rating = rating
            this.tags = genres
        }
    }

    /**
     * Resolves the authorized streaming URL and headers from the KineFlex API.
     */
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val mediaData = MediaData.fromJson(data)
        if (mediaData == null) {
            try {
                showToast("Error: Missing media metadata for playback resolution.")
            } catch (_: Throwable) {}
            return false
        }

        if (!KineFlexSettings.isKineFlexConfigured()) {
            try {
                showToast("KineFlex API key is missing. Please configure it in extension settings.")
            } catch (_: Throwable) {}
            return false
        }

        return try {
            val link: ExtractorLink = if (mediaData.type.equals("tv", ignoreCase = true)) {
                val season = mediaData.season ?: 1
                val episode = mediaData.episode ?: 1
                KineFlexApi.resolveTvEpisode(mediaData.id, season, episode)
            } else {
                KineFlexApi.resolveMovie(mediaData.id)
            }

            callback(link)
            true
        } catch (e: KineFlexAuthException) {
            try {
                showToast("KineFlex Authentication Error: ${e.message}")
            } catch (_: Throwable) {}
            false
        } catch (e: KineFlexNotFoundException) {
            try {
                showToast("KineFlex Stream Unavailable: ${e.message}")
            } catch (_: Throwable) {}
            false
        } catch (e: KineFlexQuotaException) {
            try {
                showToast("KineFlex Quota Exceeded: ${e.message}")
            } catch (_: Throwable) {}
            false
        } catch (e: KineFlexServerException) {
            try {
                showToast("KineFlex Error: ${e.message}")
            } catch (_: Throwable) {}
            false
        } catch (e: Throwable) {
            try {
                showToast("Playback Error: ${e.message ?: "Could not resolve stream"}")
            } catch (_: Throwable) {}
            false
        }
    }
}

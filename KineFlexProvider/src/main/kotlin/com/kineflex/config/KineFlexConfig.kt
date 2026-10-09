package com.kineflex.config

import com.lagradost.cloudstream3.MainPageData

/**
 * Definition of a Home Page Category.
 * Adding a new category only requires adding an entry to [KineFlexConfig.CATEGORIES].
 */
data class FeedCategory(
    val id: String,
    val displayName: String,
    val defaultUrl: String,
    val isHorizontal: Boolean = false,
)

/**
 * Centralized configuration repository for KineFlex extension.
 * Manages category definitions, default feed URLs, and base API endpoints.
 */
object KineFlexConfig {
    const val PROVIDER_NAME = "KineFlex"
    const val KINEFLEX_API_BASE_URL = "https://api.kineflex.site"
    const val TMDB_API_BASE_URL = "https://api.themoviedb.org/3"
    const val TMDB_IMAGE_BASE_W500 = "https://image.tmdb.org/t/p/w500"
    const val TMDB_IMAGE_BASE_ORIGINAL = "https://image.tmdb.org/t/p/original"
    const val DEFAULT_TMDB_API_KEY = "e6333b32409e02a4a6eba6fb7ff866bb"

    // Initial category URLs
    const val URL_LATEST_NOW = "https://www.jsonkeeper.com/b/IHDLH"
    const val URL_TRENDING_NOW_DEFAULT = "https://api.kineflex.site/feeds/trending.json"
    const val URL_PREMIUM_DEFAULT = "https://api.kineflex.site/feeds/premium.json"
    const val URL_OTHER_DEFAULT = "https://api.kineflex.site/feeds/other.json"

    /**
     * The four official categories requested:
     * 1. Latest Now
     * 2. Trending Now
     * 3. Premium
     * 4. Other
     */
    val CATEGORIES = listOf(
        FeedCategory(
            id = "latest_now",
            displayName = "Latest Now",
            defaultUrl = URL_LATEST_NOW,
            isHorizontal = false
        ),
        FeedCategory(
            id = "trending_now",
            displayName = "Trending Now",
            defaultUrl = URL_TRENDING_NOW_DEFAULT,
            isHorizontal = false
        ),
        FeedCategory(
            id = "premium",
            displayName = "Premium",
            defaultUrl = URL_PREMIUM_DEFAULT,
            isHorizontal = false
        ),
        FeedCategory(
            id = "other",
            displayName = "Other",
            defaultUrl = URL_OTHER_DEFAULT,
            isHorizontal = false
        )
    )

    /**
     * Generates the List of MainPageData required by CloudStream's [MainAPI.mainPage].
     * Resolves user-configured override URLs from settings if available.
     */
    fun getMainPages(urlResolver: (category: FeedCategory) -> String): List<MainPageData> {
        return CATEGORIES.map { category ->
            val resolvedUrl = urlResolver(category)
            MainPageData(
                name = category.displayName,
                data = resolvedUrl,
                horizontalImages = category.isHorizontal
            )
        }
    }
}

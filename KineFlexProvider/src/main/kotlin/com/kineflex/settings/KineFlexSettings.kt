package com.kineflex.settings

import android.content.Context
import android.content.SharedPreferences
import com.kineflex.config.FeedCategory
import com.lagradost.cloudstream3.utils.DataStoreHelper.getKey
import com.lagradost.cloudstream3.utils.DataStoreHelper.removeKey
import com.lagradost.cloudstream3.utils.DataStoreHelper.setKey

/**
 * Manages persistent storage for KineFlex extension settings.
 * Securely manages KineFlex Bearer API Key, TMDB credentials, and custom feed URLs.
 * Integrates with CloudStream's DataStore and Android SharedPreferences.
 */
object KineFlexSettings {
    private const val PREFS_FILE = "kineflex_extension_prefs"

    // Storage Keys
    const val KEY_KINEFLEX_API_KEY = "kineflex_api_key_v1"
    const val KEY_TMDB_CREDENTIAL = "kineflex_tmdb_credential_v1"
    const val KEY_FEED_URL_PREFIX = "kineflex_feed_url_"

    // In-memory cache for fast access during player and provider calls
    @Volatile
    private var cachedKineFlexKey: String? = null

    @Volatile
    private var cachedTmdbCredential: String? = null

    private fun getPrefs(context: Context?): SharedPreferences? {
        return context?.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    }

    /**
     * Retrieves the configured KineFlex API key.
     * Never logs or exposes the key.
     */
    fun getKineFlexApiKey(context: Context? = null): String {
        cachedKineFlexKey?.let { return it }

        var key: String? = null
        try {
            key = getKey<String>(KEY_KINEFLEX_API_KEY)
        } catch (_: Throwable) {}

        if (key.isNullOrBlank()) {
            key = getPrefs(context)?.getString(KEY_KINEFLEX_API_KEY, null)
        }

        val resolved = key?.trim().orEmpty()
        if (resolved.isNotEmpty()) {
            cachedKineFlexKey = resolved
        }
        return resolved
    }

    /**
     * Persists the KineFlex API key.
     */
    fun setKineFlexApiKey(context: Context? = null, apiKey: String?) {
        val trimmed = apiKey?.trim().orEmpty()
        cachedKineFlexKey = trimmed

        try {
            if (trimmed.isEmpty()) {
                removeKey(KEY_KINEFLEX_API_KEY)
            } else {
                setKey(KEY_KINEFLEX_API_KEY, trimmed)
            }
        } catch (_: Throwable) {}

        getPrefs(context)?.edit()?.apply {
            if (trimmed.isEmpty()) {
                remove(KEY_KINEFLEX_API_KEY)
            } else {
                putString(KEY_KINEFLEX_API_KEY, trimmed)
            }
            apply()
        }
    }

    /**
     * Retrieves the configured TMDB credential (API Key or v4 Read Token).
     */
    fun getTmdbCredential(context: Context? = null): String {
        cachedTmdbCredential?.let { return it }

        var credential: String? = null
        try {
            credential = getKey<String>(KEY_TMDB_CREDENTIAL)
        } catch (_: Throwable) {}

        if (credential.isNullOrBlank()) {
            credential = getPrefs(context)?.getString(KEY_TMDB_CREDENTIAL, null)
        }

        val resolved = credential?.trim()?.takeIf { it.isNotEmpty() } ?: KineFlexConfig.DEFAULT_TMDB_API_KEY
        if (resolved.isNotEmpty()) {
            cachedTmdbCredential = resolved
        }
        return resolved
    }

    /**
     * Persists the TMDB credential.
     */
    fun setTmdbCredential(context: Context? = null, credential: String?) {
        val trimmed = credential?.trim().orEmpty()
        cachedTmdbCredential = trimmed

        try {
            if (trimmed.isEmpty()) {
                removeKey(KEY_TMDB_CREDENTIAL)
            } else {
                setKey(KEY_TMDB_CREDENTIAL, trimmed)
            }
        } catch (_: Throwable) {}

        getPrefs(context)?.edit()?.apply {
            if (trimmed.isEmpty()) {
                remove(KEY_TMDB_CREDENTIAL)
            } else {
                putString(KEY_TMDB_CREDENTIAL, trimmed)
            }
            apply()
        }
    }

    /**
     * Retrieves the configured URL for a feed category, falling back to its default URL.
     */
    fun getCategoryUrl(category: FeedCategory, context: Context? = null): String {
        val prefKey = KEY_FEED_URL_PREFIX + category.id
        var customUrl: String? = null
        try {
            customUrl = getKey<String>(prefKey)
        } catch (_: Throwable) {}

        if (customUrl.isNullOrBlank()) {
            customUrl = getPrefs(context)?.getString(prefKey, null)
        }

        return customUrl?.trim()?.takeIf { it.isNotEmpty() } ?: category.defaultUrl
    }

    /**
     * Saves a customized feed URL for a specific category.
     */
    fun setCategoryUrl(category: FeedCategory, context: Context? = null, url: String?) {
        val prefKey = KEY_FEED_URL_PREFIX + category.id
        val trimmed = url?.trim().orEmpty()

        try {
            if (trimmed.isEmpty()) {
                removeKey(prefKey)
            } else {
                setKey(prefKey, trimmed)
            }
        } catch (_: Throwable) {}

        getPrefs(context)?.edit()?.apply {
            if (trimmed.isEmpty()) {
                remove(prefKey)
            } else {
                putString(prefKey, trimmed)
            }
            apply()
        }
    }

    /**
     * Clears all saved credentials and resets caches.
     */
    fun clearAll(context: Context? = null) {
        cachedKineFlexKey = null
        cachedTmdbCredential = null

        try {
            removeKey(KEY_KINEFLEX_API_KEY)
            removeKey(KEY_TMDB_CREDENTIAL)
        } catch (_: Throwable) {}

        getPrefs(context)?.edit()?.apply {
            remove(KEY_KINEFLEX_API_KEY)
            remove(KEY_TMDB_CREDENTIAL)
            apply()
        }
    }

    fun isKineFlexConfigured(context: Context? = null): Boolean {
        return getKineFlexApiKey(context).isNotEmpty()
    }

    fun isTmdbConfigured(context: Context? = null): Boolean {
        return getTmdbCredential(context).isNotEmpty()
    }
}

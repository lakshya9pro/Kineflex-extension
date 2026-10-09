# KineFlex CloudStream Extension

Official CloudStream 3 plugin for **KineFlex**, featuring high-definition streaming playback, TMDB search and metadata integration, four configurable home categories, and a native API key configuration dialog.

---

## Table of Contents

- [Overview](#overview)
- [Architecture & Tech Stack](#architecture--tech-stack)
- [Home Page Categories](#home-page-categories)
- [JSON Feed Schema](#json-feed-schema)
- [TMDB Integration](#tmdb-integration)
- [KineFlex Streaming Resolver](#kineflex-streaming-resolver)
- [Settings & Credentials](#settings--credentials)
- [Project Structure](#project-structure)
- [Building & Deployment](#building--deployment)
  - [Building Locally](#building-locally)
  - [Automated Builds via GitHub Actions](#automated-builds-via-github-actions)
- [Installation Guide for CloudStream](#installation-guide-for-cloudstream)
- [Verification & Testing](#verification--testing)
- [Troubleshooting](#troubleshooting)

---

## Overview

The **KineFlex CloudStream Extension** allows CloudStream users to:
1. Browse four distinct content categories directly on the Home screen: **Latest Now**, **Trending Now**, **Premium**, and **Other**.
2. Search movies and TV series with real-time metadata, artwork, synopsis, and ratings provided by **The Movie Database (TMDB)**.
3. Stream movies and TV episodes with authorized Bearer tokens via the **KineFlex Streaming API**, resolving direct and HLS `.m3u8` streams with required `Origin` and `Referer` headers.
4. Easily configure, test, and persist API credentials through the native **SettingsDialog** Bottom Sheet.

---

## Architecture & Tech Stack

- **Language:** Kotlin
- **Framework:** CloudStream 3 Provider Framework (`MainAPI`, `Plugin`, `newMovieLoadResponse`, `newTvSeriesLoadResponse`, `ExtractorLink`)
- **HTTP Client:** `com.lagradost.cloudstream3.app` (NiceHttp / OkHttp wrapper)
- **JSON Serialization:** Jackson (`com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1`)
- **UI:** Android Material Bottom Sheet (`BottomSheetDialogFragment`)
- **Build System:** Gradle (Android Library Plugin with `com.lagradost.cloudstream3.gradle`)

---

## Home Page Categories

The extension home page is organized into four distinct content categories:

| Category | Display Name | Initial / Default URL | Description |
|---|---|---|---|
| 1 | **Latest Now** | `https://www.jsonkeeper.com/b/IHDLH` | Current new movie and TV releases |
| 2 | **Trending Now** | Configurable | Popular trending titles |
| 3 | **Premium** | Configurable | Curated high-tier selections |
| 4 | **Other** | Configurable | Miscellaneous & classic catalog |

### Centralized Feed Configuration

All feed definitions are centralized in `com.kineflex.config.KineFlexConfig`.  
Custom URLs can also be configured per category in `KineFlexSettings` without modifying source code.

To add new categories or modify existing ones, update the `CATEGORIES` list in [`KineFlexConfig.kt`](file:///home/linux/Desktop/New%20Folder/KineFlexProvider/src/main/kotlin/com/kineflex/config/KineFlexConfig.kt):

```kotlin
val CATEGORIES = listOf(
    FeedCategory(id = "latest_now", displayName = "Latest Now", defaultUrl = "https://www.jsonkeeper.com/b/IHDLH"),
    FeedCategory(id = "trending_now", displayName = "Trending Now", defaultUrl = "..."),
    FeedCategory(id = "premium", displayName = "Premium", defaultUrl = "..."),
    FeedCategory(id = "other", displayName = "Other", defaultUrl = "...")
)
```

---

## JSON Feed Schema

All category feeds support standard JSON arrays. The parser is lenient and supports both top-level arrays and container objects (e.g. `{"results": [...]}`).

### Documented Feed Item Format

```json
[
  {
    "id": 27205,
    "title": "Inception",
    "year": 2010,
    "imgs": "https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg",
    "type": "movie"
  },
  {
    "id": 1399,
    "title": "Game of Thrones",
    "year": 2011,
    "imgs": "https://image.tmdb.org/t/p/w500/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg",
    "type": "tv"
  }
]
```

### Schema Rules

- `id` *(required)*: The TMDB ID (integer or integer string).
- `title` or `name` *(required)*: Title of the film or series.
- `type` *(required)*: `"movie"` for movies, or `"tv"` / `"series"` for TV shows.
- `year` *(optional)*: 4-digit release year (integer or string) or `release_date` / `first_air_date`.
- `imgs` or `poster` or `poster_path` *(optional)*: Full image URL or TMDB path starting with `/`.

A formal JSON schema specification is located at [`schemas/feed_schema.json`](file:///home/linux/Desktop/New%20Folder/schemas/feed_schema.json).

---

## TMDB Integration

Metadata and search are powered by TMDB (`https://api.themoviedb.org/3`):

1. **Movie & TV Search:**
   - Searches movies via `/search/movie` and TV series via `/search/tv`.
   - Distinguishes media types and preserves TMDB IDs for playback resolution.
2. **Metadata Enrichment:**
   - Movie details fetched from `/movie/{id}` (overview, backdrop, poster, rating, runtime, genres).
   - TV details fetched from `/tv/{id}` and season details from `/tv/{id}/season/{season}` (seasons, episodes, still images, air dates).
3. **Authentication:**
   - Supports both TMDB v3 API Keys (32 hex characters) and TMDB v4 Read-Only Bearer Tokens (JWT tokens).
   - Acquired from [https://www.themoviedb.org/settings/api](https://www.themoviedb.org/settings/api).

---

## KineFlex Streaming Resolver

The KineFlex API resolves authorized streaming URLs:

### Movie Endpoint

`GET https://api.kineflex.site/v1/movie/{tmdb_id}`  
Header: `Authorization: Bearer YOUR_KINEFLEX_API_KEY`

### TV Endpoint

`GET https://api.kineflex.site/v1/tv/{tmdb_id}/{season}/{episode}`  
Header: `Authorization: Bearer YOUR_KINEFLEX_API_KEY`

### Response Parsing & Headers

```json
{
  "success": true,
  "data": {
    "id": 550,
    "name": "Fight Club",
    "type": "movie",
    "url": "https://stream.example.com/movie/550.m3u8",
    "headers": {
      "Origin": "https://example.com",
      "Referer": "https://example.com/"
    }
  },
  "usage": {
    "cost": 5,
    "remaining_points": 95
  }
}
```

The resolver verifies `success == true`, parses the playable stream URL, forwards required HTTP headers (`Origin`, `Referer`), detects HLS `.m3u8` streams, and constructs an `ExtractorLink` for CloudStream's player.

---

## Settings & Credentials

Access the settings dialog by clicking the gear icon on the KineFlex extension in CloudStream:

- **KineFlex API Key:** Bearer API key acquired from [https://api.kineflex.site](https://api.kineflex.site).
- **TMDB Credential:** TMDB API Key or Read-Only Access Token from [https://www.themoviedb.org/settings/api](https://www.themoviedb.org/settings/api).
- **Show / Hide Password:** Toggle visibility for secure input.
- **Test Connection:** Live asynchronous verification of both KineFlex and TMDB keys.
- **Clear Keys:** Securely wipe stored credentials.
- **Save:** Persist keys across CloudStream restarts using `DataStoreHelper` and `SharedPreferences`.

> [!IMPORTANT]
> API keys are never logged, never exposed in error messages, and never committed to source control.

---

## Project Structure

```
├── .github/
│   └── workflows/
│       └── build.yml               # GitHub Actions CI build workflow
├── KineFlexProvider/
│   ├── build.gradle.kts            # Extension Gradle subproject configuration
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/kineflex/
│       │   ├── KineFlexPlugin.kt   # @CloudstreamPlugin entrypoint
│       │   ├── KineFlexProvider.kt # MainAPI provider implementation
│       │   ├── config/
│       │   │   └── KineFlexConfig.kt   # Centralized feeds & API endpoints
│       │   ├── feed/
│       │   │   ├── FeedManager.kt      # Feed fetching & deduplication
│       │   │   └── FeedModels.kt       # Flexible feed item data models
│       │   ├── kineflex/
│       │   │   ├── KineFlexApi.kt      # Stream resolver & quota handler
│       │   │   └── KineFlexModels.kt   # KineFlex response models & exceptions
│       │   ├── models/
│       │   │   ├── KineFlexJson.kt     # Configured Jackson ObjectMapper
│       │   │   └── MediaData.kt        # Serialized playback metadata payload
│       │   ├── settings/
│       │   │   ├── KineFlexSettings.kt # Persistent storage manager
│       │   │   └── SettingsDialog.kt   # BottomSheet settings UI
│       │   └── tmdb/
│       │       ├── TmdbApi.kt          # TMDB client (search, details, episodes)
│       │       └── TmdbModels.kt       # TMDB data classes
│       └── res/
│           ├── drawable/           # Vector icons (visibility, info, status)
│           ├── layout/
│           │   └── dialog_settings.xml # Settings bottom sheet layout
│           └── values/
│               ├── colors.xml
│               └── strings.xml
├── schemas/
│   ├── feed_schema.json            # JSON Schema for category feeds
│   ├── sample_latest_now.json      # Real schema sample for Latest Now
│   ├── sample_trending_now.json    # Sample feed for Trending Now
│   ├── sample_premium.json         # Sample feed for Premium
│   └── sample_other.json           # Sample feed for Other
├── test/
│   └── verify_kineflex_extension.py # Test verification suite
├── build.gradle.kts                # Root build configuration
├── settings.gradle.kts             # Subproject includes
└── gradle.properties
```

---

## Building & Deployment

### Building Locally

To build the plugin on a development machine with Android SDK and JDK 17 configured:

```bash
# Build the plugin .cs3 artifact
./gradlew make

# Build the repository plugins.json index
./gradlew makePluginsJson

# Or deploy directly to a connected Android device / emulator via ADB:
./gradlew KineFlexProvider:deployWithAdb
```

Build outputs:
- Extension bundle: `KineFlexProvider/build/KineFlex.cs3`
- Repository index: `build/plugins.json`

### Automated Builds via GitHub Actions

This repository includes `.github/workflows/build.yml`. When pushed to GitHub:
1. GitHub Actions checks out the repository.
2. Sets up JDK 17 and Android SDK.
3. Runs `./gradlew make makePluginsJson`.
4. Publishes `KineFlex.cs3` and `plugins.json` to the `builds` branch.

---

## Installation Guide for CloudStream

### Method 1: Extension Repository (Recommended)

1. Host this repository on GitHub.
2. In CloudStream on your Android device or Android TV, navigate to:
   **Settings > Extensions > Add Repository**
3. Enter your repository URL (e.g. `https://raw.githubusercontent.com/<username>/<repo>/builds/plugins.json` or your repo URL).
4. Tap **Download / Install** on **KineFlex**.
5. Tap the gear icon next to KineFlex to enter your **KineFlex API Key** and **TMDB Credential**.

### Method 2: Manual Installation via Local File

1. Transfer `KineFlex.cs3` to your Android device or Android TV.
2. In CloudStream, open **Settings > Extensions > Install from .cs3 file**.
3. Select `KineFlex.cs3`.

---

## Verification & Testing

To run the verification suite testing JSON parsing, schema validation, TMDB mapping, and security checks:

```bash
python3 test/verify_kineflex_extension.py
```

Output:
```
=== Running KineFlex CloudStream Extension Verification Suite ===
[1/7] Testing Real Feed Parsing (Latest Now)...
  ✓ Real feed parsed successfully (5 movies, 5 TV shows validated).
[2/7] Testing Sample Feeds...
  ✓ schemas/sample_latest_now.json validated successfully.
  ✓ schemas/sample_trending_now.json validated successfully.
  ✓ schemas/sample_premium.json validated successfully.
  ✓ schemas/sample_other.json validated successfully.
[3/7] Testing Flexible Feed Parsing edge cases...
  ✓ Flexible parsing and deduplication passed.
[4/7] Testing TMDB Response Mapping...
  ✓ TMDB movie details mapping passed.
  ✓ TMDB TV episode mapping passed.
[5/7] Testing KineFlex Resolver Response Validation...
  ✓ KineFlex movie stream resolution passed.
  ✓ KineFlex TV episode stream resolution passed.
[6/7] Testing Error Handling Logic...
  ✓ Error codes (401, 403, 404, 429, 500) mapped correctly.
[7/7] Verifying that no secret credentials are hardcoded...
  ✓ Verified: No secret credentials or private tokens hardcoded in source files.
=== All 7 Test Suites Passed Successfully! ===
```

---

## Troubleshooting

| Issue | Cause | Solution |
|---|---|---|
| **"KineFlex API key is missing"** | No API key configured in extension settings. | Open CloudStream Settings > Extensions > KineFlex > Enter Bearer token. |
| **"Unauthorized (HTTP 401/403)"** | Invalid or expired KineFlex or TMDB key. | Verify and re-enter your key in Settings. Tap "Test Connection" to check. |
| **"Stream Unavailable (HTTP 404)"** | Title not currently hosted on KineFlex. | Try another movie or episode. |
| **"Quota Exceeded (HTTP 429)"** | Daily points exhausted on KineFlex account. | Check your remaining points on https://api.kineflex.site. |
| **Blank Home Category** | Network connection issue or invalid feed URL. | Check internet connection or verify the feed URL in settings. |

dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.core:core-ktx:1.13.1")
}

// Version code for the plugin
version = 1

cloudstream {
    description = "CloudStream provider for KineFlex streaming with TMDB metadata integration"
    authors = listOf("KineFlex")

    /**
     * Status int:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta-only
     **/
    status = 1

    tvTypes = listOf("Movie", "TvSeries")
    requiresResources = true
    language = "en"
    iconUrl = "https://raw.githubusercontent.com/recloudstream/cloudstream/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"
}

android {
    namespace = "com.kineflex"

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.campusconnect"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.campusconnect"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.compose.material3)
    implementation(libs.ads.mobile.sdk)
    val nav_version = "2.7.5"

    implementation("androidx.navigation:navigation-compose:$nav_version")

    // Compose BOM manages versions for Compose artifacts
    implementation(platform(libs.androidx.compose.bom))

    // Compose UI + tooling
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Material 2 (for BottomNavigation, etc.) and Material 3
    implementation("androidx.compose.material:material")
    implementation(libs.androidx.material3)

    // AndroidX + tests
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Maintenance task: delete known-unreferenced drawables
tasks.register("pruneUnusedDrawables") {
    group = "maintenance"
    description = "Delete unreferenced drawable resources."
    doLast {
        delete(
            "src/main/res/drawable/notes.png",
            "src/main/res/drawable/ic_subscribe.xml",
            "src/main/res/drawable/outline_album_24.xml",
            "src/main/res/drawable/outline_genres_24.xml",
            "src/main/res/drawable/outline_library_music_24.xml",
            "src/main/res/drawable/outline_mic_24.xml",
            "src/main/res/drawable/outline_music_note_24.xml",
            "src/main/res/drawable/outline_play_circle_24.xml",
            "src/main/res/drawable/sharp_playlist_play_24.xml",
            "src/main/res/drawable/ic_account.xml"
        )
    }
}

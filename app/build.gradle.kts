plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.campusconnect"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.campusconnect"
        minSdk = 29 // Changed from 34 to 29
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
            buildConfigField("boolean", "FORCE_WELCOME", "false")
        }
        debug {
            // Force showing the Welcome screens in debug builds for quick verification
            buildConfigField("boolean", "FORCE_WELCOME", "true")
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
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    // Firebase - make sure these are included
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Your existing dependencies
    implementation(libs.androidx.compose.material3)
    implementation(libs.ads.mobile.sdk)
    implementation(libs.material3)
    implementation(libs.androidx.camera.core)
    val nav_version = "2.7.5"

    implementation("androidx.navigation:navigation-compose:$nav_version")

    // Compose BOM manages versions for Compose artifacts
    implementation(platform(libs.androidx.compose.bom))

    // Compose UI + tooling
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Coil for image loading in Compose
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Material 2 (for BottomNavigation, etc.) and Material 3
    implementation("androidx.compose.material:material")
    implementation(libs.androidx.material3)
    // Check for your Material 3 dependency
    implementation("androidx.compose.material3:material3:1.2.1")

    // AndroidX + tests
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.google.flexbox)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

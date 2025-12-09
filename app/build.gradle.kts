plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.firebase.crashlytics")
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

        // Enable 16KB page size support for Android 15+
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
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
        languageVersion = "1.9"
    }
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }

    packaging {
        jniLibs {
            // Enable proper alignment for 16KB page sizes
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

configurations.all {
    resolutionStrategy {
        force("com.squareup:javapoet:1.13.0")
    }
}

dependencies {
    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Hilt Testing
    testImplementation("com.google.dagger:hilt-android-testing:2.48")
    kaptTest("com.google.dagger:hilt-android-compiler:2.48")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.48")

    // Firebase - Updated to latest BOM for 16KB page size compatibility
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Cloudinary for file storage - Updated to latest version for 16KB compatibility
    implementation("com.cloudinary:cloudinary-android:3.0.2") {
        exclude(group = "com.google.firebase")
    }
    implementation("com.cloudinary:cloudinary-core:1.39.0") {
        exclude(group = "com.google.firebase")
    }

    // Activity Result API for file picker
    implementation("androidx.activity:activity-ktx:1.9.3")

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

    // Room (Offline-First groundwork)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    testImplementation("androidx.room:room-testing:2.6.1")

    // Paging 3 for large lists
    implementation("androidx.paging:paging-runtime-ktx:3.2.1")
    implementation("androidx.paging:paging-compose:3.2.1")

    // WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")

    // Coil for image optimization (already added but updating)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Coroutine Flow testing
    testImplementation("app.cash.turbine:turbine:1.0.0")

    // Mockito for mocking Firebase/Firestore
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

    // LeakCanary for memory leak detection
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

// Read local.properties for API_BASE_URL (same approach as ResturantAndroid)
val localPropertiesFile = rootProject.file("local.properties")

android {
    namespace = "com.driver.resturantandroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.driver.resturantandroid"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // API Base URL: read local.properties first, then gradle.properties, else default (emulator only)
        // Emulator: 10.0.2.2 = host. Physical device: use your computer's LAN IP (same WiFi) in local.properties
        val apiBaseUrlRaw = if (localPropertiesFile.exists()) {
            localPropertiesFile.readLines()
                .firstOrNull { it.trimStart().startsWith("API_BASE_URL=") }
                ?.substringAfter("API_BASE_URL=")
                ?.trim()
                ?.removeSurrounding("\"")
                ?: project.findProperty("API_BASE_URL")?.toString()
                ?: "http://10.0.2.2:3000/api/"
        } else {
            project.findProperty("API_BASE_URL")?.toString() ?: "http://10.0.2.2:3000/api/"
        }
        // Normalize: trim, strip all leading slashes, must start with http(s):// (avoids "Failed to connect to /192.168.1.11:3000")
        var apiBaseUrl = apiBaseUrlRaw.trim().trimStart('/')
        if (!apiBaseUrl.startsWith("http://") && !apiBaseUrl.startsWith("https://")) {
            apiBaseUrl = "http://$apiBaseUrl"
        }
        if (!apiBaseUrl.endsWith("/")) apiBaseUrl += "/"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        // Firebase Realtime Database URL - MUST match backend FIREBASE_DATABASE_URL (europe-west1)
        buildConfigField("String", "FIREBASE_DATABASE_URL", "\"${project.findProperty("FIREBASE_DATABASE_URL") ?: "https://tashkeela-8cab1-default-rtdb.europe-west1.firebasedatabase.app"}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    
    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Firebase Cloud Messaging for push notifications
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    // Firebase Realtime Database for real-time order updates (read-only)
    implementation("com.google.firebase:firebase-database-ktx")
    
    // SharedPreferences
    implementation("androidx.preference:preference-ktx:1.2.1")
    
    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Image Loading
    implementation("io.coil-kt:coil:2.5.0")
    
    // Activity Result API
    implementation("androidx.activity:activity-ktx:1.8.2")
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
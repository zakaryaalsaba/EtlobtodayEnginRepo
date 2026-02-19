plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.parcelize")
}

// Google Maps API key (override via MAPS_API_KEY in local.properties if needed)
val localPropertiesFile = rootProject.file("local.properties")
val mapsApiKey: String = if (localPropertiesFile.exists()) {
    localPropertiesFile.readLines()
        .firstOrNull { it.startsWith("MAPS_API_KEY=") }
        ?.substringAfter("MAPS_API_KEY=")
        ?.trim()
        ?.removeSurrounding("\"")
        ?: "AIzaSyBXEtwSe6F6dxuB7Y3Lffg1-jJ9iKSXMzo"
} else {
    "AIzaSyBXEtwSe6F6dxuB7Y3Lffg1-jJ9iKSXMzo"
}

android {
    namespace = "com.mnsf.resturantandroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mnsf.resturantandroid"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // API Base URL: read local.properties first, then gradle.properties, else default (emulator only)
        // Emulator: 10.0.2.2 = host. Real device: use your computer's LAN IP (same WiFi) in local.properties
        val apiBaseUrlRaw = if (localPropertiesFile.exists()) {
            localPropertiesFile.readLines()
                .firstOrNull { it.startsWith("API_BASE_URL=") }
                ?.substringAfter("API_BASE_URL=")
                ?.trim()
                ?.removeSurrounding("\"")
                ?: project.findProperty("API_BASE_URL")?.toString()
                ?: "http://10.0.2.2:3000/api/"
        } else {
            project.findProperty("API_BASE_URL")?.toString() ?: "http://10.0.2.2:3000/api/"
        }
        // Normalize: no leading slash, must start with http:// or https:// (avoids "failed to connect to /192.168.1.100:3000")
        var apiBaseUrl = apiBaseUrlRaw.trimStart('/')
        if (!apiBaseUrl.startsWith("http://") && !apiBaseUrl.startsWith("https://")) {
            apiBaseUrl = "http://$apiBaseUrl"
        }
        if (!apiBaseUrl.endsWith("/")) apiBaseUrl += "/"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        // PayTabs: set in local.properties as PAYTABS_PROFILE_ID, PAYTABS_SERVER_KEY, PAYTABS_CLIENT_KEY
        val ptProfile = if (localPropertiesFile.exists()) {
            localPropertiesFile.readLines().firstOrNull { it.startsWith("PAYTABS_PROFILE_ID=") }?.substringAfter("=")?.trim()?.removeSurrounding("\"") ?: ""
        } else ""
        val ptServer = if (localPropertiesFile.exists()) {
            localPropertiesFile.readLines().firstOrNull { it.startsWith("PAYTABS_SERVER_KEY=") }?.substringAfter("=")?.trim()?.removeSurrounding("\"") ?: ""
        } else ""
        val ptClient = if (localPropertiesFile.exists()) {
            localPropertiesFile.readLines().firstOrNull { it.startsWith("PAYTABS_CLIENT_KEY=") }?.substringAfter("=")?.trim()?.removeSurrounding("\"") ?: ""
        } else ""
        buildConfigField("String", "PAYTABS_PROFILE_ID", "\"$ptProfile\"")
        buildConfigField("String", "PAYTABS_SERVER_KEY", "\"$ptServer\"")
        buildConfigField("String", "PAYTABS_CLIENT_KEY", "\"$ptClient\"")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
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

// Check if google-services.json exists in common locations
val googleServicesFile = when {
    file("google-services.json").exists() -> file("google-services.json")
    file("src/debug/google-services.json").exists() -> file("src/debug/google-services.json")
    file("src/google-services.json").exists() -> file("src/google-services.json")
    else -> null
}

// Only apply Google Services plugin if the file exists
if (googleServicesFile != null) {
    apply(plugin = "com.google.gms.google-services")
    println("✓ Google Services plugin applied - Firebase is configured")
} else {
    println("⚠ Google Services plugin skipped - google-services.json not found")
    println("  Firebase features will be disabled. To enable:")
    println("  1. Create a Firebase project at https://console.firebase.google.com")
    println("  2. Add your Android app to the project")
    println("  3. Download google-services.json and place it in the app/ directory")
    
    // Disable Google Services tasks to prevent build errors
    afterEvaluate {
        tasks.matching { it.name.contains("GoogleServices", ignoreCase = true) }.configureEach {
            enabled = false
        }
    }
}

// PayTabs: avoid duplicate kotlinx-coroutines-debug; pin AndroidX core so we don't need compileSdk 36 / AGP 8.9.1 (PayTabs pulls in core 1.17.0 otherwise).
configurations.all {
    resolutionStrategy {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-debug")
        force("androidx.core:core:1.15.0")
        force("androidx.core:core-ktx:1.15.0")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Image loading
    implementation(libs.glide)
    
    // Data storage
    implementation(libs.datastore.preferences)
    
    // Firebase Cloud Messaging and Authentication
    // Always include dependencies for compile-time, plugin is conditional
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    
    // PayTabs payment SDK (card payments). Using 6.6.2 for stability on API 33–35 (6.8.1 caused process kill after startCardPayment).
    implementation("com.paytabs:payment-sdk:6.6.2")

    // Location services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    // Google Maps SDK for Android (required for Confirm Location / New Address map)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
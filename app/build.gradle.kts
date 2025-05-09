plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Removed: alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.valvetight"
    compileSdk = 34 // Using latest stable Android SDK

    defaultConfig {
        applicationId = "com.example.valvetight"
        minSdk = 29 // As per your initial requirement
        targetSdk = 34 // Target latest stable SDK for better compatibility and features
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Keep false for easier debugging initially
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // Common setting
        targetCompatibility = JavaVersion.VERSION_1_8 // Common setting
    }
    kotlinOptions {
        jvmTarget = "1.8" // Match Java compatibility
    }
    buildFeatures {
        viewBinding = true // Recommended for easy view access in XML layouts
    }
}

// --- ADDED RESOLUTION STRATEGY ---
configurations.all {
    resolutionStrategy {
        // Force the version of androidx.activity libraries to what's defined in libs.versions.toml
        // This ensures we use activityKtx = "1.9.0" (or whatever you set for activityKtx)
        force(libs.androidx.activity.ktx)

        // If other libraries cause similar issues, you can add more 'force' lines:
        // e.g., force(libs.androidx.fragment.ktx) // if you had a fragmentKtx alias and version
        // For now, we only explicitly force activity.ktx
    }
}
// --- END OF ADDED RESOLUTION STRATEGY ---

dependencies {
    // Core Android & Kotlin
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx) // Good for lifecycle components

    // UI Libraries for XML Layouts
    implementation(libs.androidx.appcompat)           // For AppCompatActivity, Toolbar, etc.
    implementation(libs.androidx.activity.ktx)     // <<< CHANGED from libs.androidx.activity to use the ktx version
    implementation(libs.google.android.material)     // For Material Design Components (TextInputLayout, etc.)
    implementation(libs.androidx.constraintlayout)   // For ConstraintLayout

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
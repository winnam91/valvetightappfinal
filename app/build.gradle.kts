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
        // If you specifically need Java 11 features, you can revert to VERSION_11
        // but VERSION_1_8 is widely compatible.
    }
    kotlinOptions {
        jvmTarget = "1.8" // Match Java compatibility
    }
    buildFeatures {
        // compose = false // Or remove the buildFeatures block if only compose was in it
        viewBinding = true // Recommended for easy view access in XML layouts
    }
}

dependencies {
    // Core Android & Kotlin
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx) // Good for lifecycle components

    // UI Libraries for XML Layouts
    implementation(libs.androidx.appcompat)           // For AppCompatActivity, Toolbar, etc.
    implementation(libs.google.android.material)     // For Material Design Components (TextInputLayout, etc.)
    implementation(libs.androidx.constraintlayout)   // For ConstraintLayout

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Removed Compose dependencies:
    // implementation(libs.androidx.activity.compose)
    // implementation(platform(libs.androidx.compose.bom))
    // implementation(libs.androidx.ui)
    // implementation(libs.androidx.ui.graphics)
    // implementation(libs.androidx.ui.tooling.preview)
    // implementation(libs.androidx.material3)
    // androidTestImplementation(platform(libs.androidx.compose.bom))
    // androidTestImplementation(libs.androidx.ui.test.junit4)
    // debugImplementation(libs.androidx.ui.tooling)
    // debugImplementation(libs.androidx.ui.test.manifest)
}
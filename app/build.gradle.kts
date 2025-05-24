plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Removed: alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.valvetight"
    compileSdk = 35 // Using latest stable Android SDK

    defaultConfig {
        applicationId = "com.example.valvetight"
        minSdk = 29 // As per your initial requirement
        targetSdk = 35 // Target latest stable SDK for better compatibility and features
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11" // Match Java compatibility
    }
    buildFeatures {
        viewBinding = true // Recommended for easy view access in XML layouts
    }
}

// Inside app/build.gradle.kts
configurations.all {
    resolutionStrategy {
        // Force the specific KTX artifact.
        // This should also influence the base 'activity' artifact if they share the same group and versioning scheme.
        force(libs.androidx.activity.ktx)

        // As an additional, more direct measure, we can force the base 'activity' artifact specifically by string
        // if the 'libs' alias isn't set up for the non-ktx version or if the above isn't enough.
        // Make sure the version "1.10.1" is what you intend for compatibility with SDK 35.
        force("androidx.activity:activity:1.10.1")
    }
}
// --- END OF ADDED RESOLUTION STRATEGY ---

// Lint reported 'Unnecessary module dependency' for test modules not depending on app.main.
// Standard Android project structure with test code in src/test and src/androidTest implicitly creates this dependency.
// If actual tests are failing to resolve app code, further build configuration review would be needed.
dependencies {
    // Core Android & Kotlin
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx) // Good for lifecycle components

    // UI Libraries for XML Layouts
    implementation(libs.androidx.appcompat)           // For AppCompatActivity, Toolbar, etc.
    implementation(libs.androidx.activity.ktx)     // <<< CHANGED from libs.androidx.activity to use the ktx version
    implementation(libs.google.android.material)     // For Material Design Components (TextInputLayout, etc.)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity)   // For ConstraintLayout

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
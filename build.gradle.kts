// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // We are removing: alias(libs.plugins.kotlin.compose) apply false
    // because the main app module will not use Jetpack Compose directly
    // as per the XML layout plan.
}
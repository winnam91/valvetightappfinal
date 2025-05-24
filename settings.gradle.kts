// Note: This file uses Gradle APIs marked as @Incubating by lint (e.g., getRepositoriesMode, FAIL_ON_PROJECT_REPOS).
// These are used as per current Gradle recommendations and are subject to change in future Gradle versions.
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack can be added if any library needs it, but not for standard ones
        // maven { url = uri("https.jitpack.io") }
    }
}

rootProject.name = "ValveTight" // Consistent naming if your project folder is ValveTight
include(":app")
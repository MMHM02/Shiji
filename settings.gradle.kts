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
        gradlePluginPortal()
    }
}

rootProject.name = "ShiJi"

// App module
include(":app")

// Core modules
include(":core:common")
include(":core:data")
include(":core:ai")
include(":core:camera")
include(":core:voice")

// Feature modules
include(":feature:diet")
include(":feature:health")
include(":feature:settings")

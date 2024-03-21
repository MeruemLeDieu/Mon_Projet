@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

buildCache {
    local {
        directory = File(rootDir, "build/mon_projet-build-cache")
        removeUnusedEntriesAfterDays = 7
    }
}

rootProject.name = "camera_app"
include(":app")
 
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

        // ✅ Naver Map SDK repository
        maven("https://repository.map.naver.com/archive/maven")
    }
}

rootProject.name = "MarthianClean"
include(":app")

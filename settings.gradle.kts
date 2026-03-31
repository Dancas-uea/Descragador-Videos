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
        // JitPack — ya no es necesario para youtubedl-android (migró a Maven Central en 0.16.0)
        // pero lo dejamos por si usas otras libs de JitPack
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "DescargadorVideos"
include(":app")
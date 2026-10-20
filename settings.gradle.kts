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
    // Offline mirror for artifacts the cloud build box cannot fetch through Gradle.
    maven { url = uri("app/libs/m2") }
  }
}
rootProject.name = "GlyphCam"
include(":app")

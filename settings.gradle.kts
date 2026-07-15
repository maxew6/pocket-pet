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

rootProject.name = "Pocket Pet"

include(
    ":app",
    ":core:model",
    ":core:domain",
    ":core:database",
    ":core:system",
    ":core:data",
    ":core:designsystem",
    ":feature:onboarding",
    ":feature:home",
    ":feature:settings",
    ":service:overlay",
)

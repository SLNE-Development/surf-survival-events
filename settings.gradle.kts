rootProject.name = "surf-survival-events"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://reposilite.slne.dev/releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.slne.surf.api.gradle.settings") version "+"
}


include(":surf-survival-events-base")
include(":surf-survival-events-events:surf-survival-event-example")
include(":surf-survival-events-events:surf-survival-event-race")
include(":surf-survival-events-events:surf-survival-event-hide-and-seek")
include(":surf-survival-events-events:surf-survival-event-simon-says")
include(":surf-survival-events-freebuild")
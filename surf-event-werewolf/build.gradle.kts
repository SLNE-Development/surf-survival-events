import dev.slne.surf.api.gradle.util.withSurfApiBukkit

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

version = "1.0.0-SNAPSHOT"

dependencies {
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.event.werewolf.PaperMain")
    authors.addAll("Jo_field")

    runServer {
        withSurfApiBukkit()
    }
}
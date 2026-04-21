import dev.slne.surf.api.gradle.util.registerRequired
import dev.slne.surf.api.gradle.util.withSurfApiBukkit

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

version = "1.0.0-SNAPSHOT"
group = "dev.slne.surf.survival.events.werewolf"

dependencies {
    compileOnly("de.maxhenkel.voicechat:voicechat-api:2.5.0")
}
repositories {
    maven { url = uri("https://maven.maxhenkel.de/repository/public") }
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.event.werewolf.PaperMain")
    authors.addAll("Jo_field")

    runServer {
        withSurfApiBukkit()
    }

    serverDependencies {
        registerRequired("VoiceChat")
    }
}
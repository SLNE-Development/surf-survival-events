import dev.slne.surf.api.gradle.util.registerRequired

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

dependencies {
    compileOnly("dev.slne.surf.npc:surf-npc-api:+")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.survival.events.base.PaperMain")
    generateLibraryLoader(false)
    foliaSupported(true)

    authors.addAll("red", "mikey", "jo_field")
    serverDependencies {
        registerRequired("surf-npc-paper")
    }
}

kotlin {
    explicitApi()
}
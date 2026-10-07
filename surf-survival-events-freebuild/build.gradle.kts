import dev.slne.surf.api.gradle.util.registerSoft

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.survival.events.freebuild.PaperMain")
    generateLibraryLoader(false)
    foliaSupported(true)

    withSurfNpc()
    withCorePaper()

    authors.addAll("red")

    serverDependencies {
        registerSoft("surf-hud-paper")
    }
}

dependencies {
    compileOnly("dev.slne.surf.hud:surf-hud-api:+")
}
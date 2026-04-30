plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.survival.events.parkour.PaperMain")
    generateLibraryLoader(false)
    foliaSupported(true)
}
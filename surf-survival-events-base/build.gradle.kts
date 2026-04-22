plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.survival.events.base.PaperMain")
    generateLibraryLoader(false)
    foliaSupported(true)

    authors.addAll("red", "mikey", "jo_field")
}
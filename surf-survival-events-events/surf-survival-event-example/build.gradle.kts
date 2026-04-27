plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

dependencies {
    compileOnly(project(":surf-survival-events-api"))
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.survival.events.example.PaperMain")
    generateLibraryLoader(false)
    foliaSupported(true)
}
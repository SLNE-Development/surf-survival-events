import dev.slne.surf.api.gradle.util.registerRequired

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

dependencies {
    compileOnly(project(":surf-survival-events-base"))
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.survival.events.example.PaperMain")
    generateLibraryLoader(false)
    foliaSupported(true)

    serverDependencies {
        registerRequired("surf-survival-events-base")
    }
}
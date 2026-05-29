import dev.slne.surf.api.gradle.util.registerRequired

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

dependencies {
    compileOnly(project(":surf-survival-events-base"))
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.survival.events.race.PaperMain")
    generateLibraryLoader(false)
    foliaSupported(true)

    authors.addAll("mikey", "red")

    serverDependencies {
        registerRequired("surf-survival-events-base")
    }
}

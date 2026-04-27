plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

dependencies {
    compileOnly(project(":surf-survival-events-api"))
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.survival.events.base.PaperMain")
    generateLibraryLoader(false)
    foliaSupported(true)

    authors.addAll("red", "mikey", "jo_field")
}
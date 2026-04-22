
plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

version = findProperty("version") as String
group = "dev.slne.surf.survival.events"


surfPaperPluginApi {
    mainClass("dev.slne.surf.survival.events.paper.PaperMain")
    generateLibraryLoader(false)
    foliaSupported(true)

    authors.addAll("red", "mikey", "jo_field")


}
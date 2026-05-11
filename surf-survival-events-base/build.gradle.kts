import dev.slne.surf.api.gradle.util.registerRequired
import dev.slne.surf.api.gradle.util.registerSoft

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

dependencies {
    compileOnly("dev.slne.surf.npc:surf-npc-api:+")
    compileOnly(project(":surf-survival-events-events:surf-survival-event-example"))
    compileOnly(project(":surf-survival-events-events:surf-survival-event-race"))
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.survival.events.base.PaperMain")
    generateLibraryLoader(false)
    foliaSupported(true)

    authors.addAll("red", "mikey", "jo_field")
    serverDependencies {
        registerRequired("surf-npc-paper")
        registerSoft("surf-survival-event-example")
        registerSoft("surf-survival-event-race")
    }
}
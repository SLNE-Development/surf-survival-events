import dev.slne.surf.api.gradle.util.registerSoft

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

dependencies {
    compileOnly(project(":surf-survival-events-events:surf-survival-event-example"))
    compileOnly(project(":surf-survival-events-events:surf-survival-event-werewolf"))
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.survival.events.base.PaperMain")
    generateLibraryLoader(false)
    foliaSupported(true)

    authors.addAll("red", "mikey", "jo_field", "ProxyFyy")

    serverDependencies {
//        registerRequired("surf-npc-paper")
        registerSoft("surf-survival-event-example")
        registerSoft("surf-survival-event-werewolf")
        registerSoft("surf-survival-event-race")
    }
}
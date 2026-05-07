import dev.slne.surf.api.gradle.util.registerSoft

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

dependencies {
    implementation("de.maxhenkel.voicechat:voicechat-api:2.5.0")
}

surfPaperPluginApi {
    mainClass("dev.slne.surf.survival.events.werewolf.PaperMain")
    generateLibraryLoader(false)
    foliaSupported(true)

    serverDependencies {
        registerSoft("voicechat")
    }
}
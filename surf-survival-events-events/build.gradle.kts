import dev.slne.surf.api.gradle.platform.paper.plugin.PaperPluginSurfExtension
import dev.slne.surf.api.gradle.util.registerRequired


subprojects {
    pluginManager.apply("dev.slne.surf.api.gradle.paper-plugin")

    dependencies {
        "compileOnly"(project(":surf-survival-events-base"))
    }

    val main: String? by project
    val authors: String? by project

    configure<PaperPluginSurfExtension> {
        main?.let { mainClass(it) }

        generateLibraryLoader(false)
        foliaSupported(true)

        authors?.let {
            this.authors.addAll(it.split(",").map(String::trim))
        }

        serverDependencies {
            registerRequired("surf-survival-events-base")

            if (project.name == "surf-survival-event-werewolf") {
                registerRequired("voicechat")
            }
        }
    }
}
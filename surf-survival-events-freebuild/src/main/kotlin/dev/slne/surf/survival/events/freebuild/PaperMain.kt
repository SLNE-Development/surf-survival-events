package dev.slne.surf.survival.events.freebuild

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.folia.launch
import com.sksamuel.aedile.core.expireAfterWrite
import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.util.toObjectSet
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.core.api.paper.util.surfPlayer
import dev.slne.surf.npc.api.dsl.npc
import dev.slne.surf.npc.api.event.NpcInteractEvent
import dev.slne.surf.npc.api.npc.skin.NpcSkin
import dev.slne.surf.npc.api.npc.skin.NpcSkinPart
import dev.slne.surf.survival.events.freebuild.command.reloadFreebuildSurvivalEventsConfigCommand
import dev.slne.surf.survival.events.freebuild.command.switchFreebuildSurvivalServerEventsServerNpcCommand
import dev.slne.surf.survival.events.freebuild.config.FreebuildPartConfig
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.time.Duration.Companion.seconds

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)

private const val NPC_UNIQUE_NAME = "survival_events_freebuild_server_npc"
private val npcCooldown = Caffeine.newBuilder()
    .expireAfterWrite(2.seconds)
    .build<UUID, Unit>()

class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        switchFreebuildSurvivalServerEventsServerNpcCommand()
        reloadFreebuildSurvivalEventsConfigCommand()
    }

    override suspend fun onEnableAsync() {
        npc {
            uniqueName = NPC_UNIQUE_NAME
            displayName {
                variableValue("Arty".toSmallCaps(), TextDecoration.BOLD)
            }
            location = Location(Bukkit.getWorlds().first(), -4.5, 73.0, 8.5, -140f, 0f)
            type = EntityType.MANNEQUIN
            skin = NpcSkin(
                "Events",
                "ewogICJ0aW1lc3RhbXAiIDogMTc4MDA5NzQ1NTU4MiwKICAicHJvZmlsZUlkIiA6ICJlNmEwMjI4ZTdkNmU0NWNkODkyOGZmN2Q1OTdlOWYxMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJhbGVrdHJvX2JvYmVyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2NkNjBjYmFkZmYzMmMxZDE2YmQ1M2IwYWFiZTMzZjJkNzUxNTgyNzRhNDU2NmZiYmU3MDg0YzNhMmRkM2NlY2QiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfSwKICAgICJDQVBFIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yOGRlNGE4MTY4OGFkMThiNDllNzM1YTI3M2UwODZjMThmMWUzOTY2OTU2MTIzY2NiNTc0MDM0YzA2ZjVkMzM2IgogICAgfQogIH0KfQ==",
                "iiU2+CVtebLivF8wAagmR3gdPksylEtXhoARGkGAXBkGrLkqkl6RG2URGfLzMsOTiAaufQftasYqkeVFUd6IsVe6D9Xwp7a421cf06MsNUeeKkGqH66jcLqt8ZngJaI52+l+YQslBscUfME76kYUl/mWQfOomLPVeBnHLhiiNzfux1vJkL/IKgMecjzG9pC6An0XcQv4/ZKfMnCtpLwsxgnP3qHK1VxHFNUyzfUyiD3ZOmInS/vUSwBV6Y+DJYppW7y5JSwx/M+rzVIAPC5qm8IZBS5ZuOOUh7X0LVAxaUJDFNFVKWRHXw4vpdnhBvQgp5UYenCEC9vzZwTcr7Jx5N96z8EscrWDHu+X2CQdSbX9ujmm/8ZsbTZ9cgNXH+30Fp9cDQwSNPgTndvF+oFX4ONAYCQMeJOZc5nKeiLb3LGh7pIowq51WXdw3lt5O5XHvj5vW4y11HV88bZSYoABf1Sg22zUDVeXybjBSQWfkx7LUzKu5fE82mM+hPdjs+JxGVccT0xjb93CBLfWZuVKBe0jC3D5u4uj0relLoAqPnv47l/klkCGQcoRH7euj4yNoD1cLiRf9GBt4oKMm7xoyIBV4hyP34XCdXVwHpH9ZqAqY/oRcIJEvcaluQZSX9A+ksFot+RHQtZjKlR6V2aDYPez3r4CuePkvNAWY5n2Ezo=",
                NpcSkinPart.entries.filter { it != NpcSkinPart.CAPE }.toObjectSet()
            )

            withEventHandler<NpcInteractEvent> { event ->
                if (event.npc.uniqueName != NPC_UNIQUE_NAME) {
                    return@withEventHandler
                }

                if (!FreebuildPartConfig.getConfig().enableSurvivalEventsNpc) {
                    event.player.sendText {
                        appendErrorPrefix()
                        error("Derzeit findet kein Survival Event statt. Bitte versuche es später erneut.")
                    }
                    return@withEventHandler
                }

                val surfServer =
                    SurfCoreApi.getServerByName(FreebuildPartConfig.getConfig().eventServerName)

                if (surfServer == null) {
                    event.player.sendText {
                        appendErrorPrefix()
                        error("Der Survival Events Server ist derzeit nicht erreichbar. Bitte versuche es später erneut.")
                    }
                    return@withEventHandler
                }

                if (npcCooldown.getIfPresent(event.player.uniqueId) != null) {
                    event.player.sendText {
                        appendErrorPrefix()
                        error("Bitte warte einen Moment bevor du erneut mit Arty interagierst.")
                    }
                    return@withEventHandler
                }

                plugin.launch {
                    val surfPlayer = event.player.surfPlayer
                    val result = SurfCoreApi.sendPlayerAwaiting(surfPlayer, surfServer)

                    if (!result.isSuccessful()) {
                        event.player.sendText {
                            appendErrorPrefix()
                            error("Beim Verbinden zum Survival Event Server ist ein Fehler aufgetreten. Bitte versuche es später erneut. (${result.status})")
                        }
                    }

                    npcCooldown.put(event.player.uniqueId, Unit)
                }
            }
        }
    }
}
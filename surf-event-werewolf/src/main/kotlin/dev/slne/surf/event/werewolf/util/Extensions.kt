import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.event.werewolf.service.WerewolfService
import dev.slne.surf.event.werewolf.util.WerewolfPlayer
import org.bukkit.entity.Player
import java.util.*

fun UUID.toBukkitPlayer(): Player? = server.getPlayer(this)

fun UUID.toWerewolfPlayer(service: WerewolfService): WerewolfPlayer? = service.players[this]

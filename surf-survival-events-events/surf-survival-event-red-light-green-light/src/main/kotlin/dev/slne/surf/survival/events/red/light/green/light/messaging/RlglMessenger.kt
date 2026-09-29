package dev.slne.surf.survival.events.red.light.green.light.messaging

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.showTitle
import dev.slne.surf.api.core.messages.adventure.title
import dev.slne.surf.survival.events.red.light.green.light.service.RlglState
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.UUID

object RlglMessenger {
    fun sendRoundInvite(invitee: Player, sender: Player, roundId: String) {
        invitee.sendText {
            appendInfoPrefix()
            info("Du wurdest von")
            appendSpace()
            variableValue(sender.name)
            appendSpace()
            info("zu einer Runde Red Light, Green Light eingeladen!")

            appendNewInfoPrefixedLine()
            info("Klicke")
            appendSpace()
            append(joinClickable(roundId))
            appendSpace()
            info("um beizutreten.")
        }
    }

    private fun joinClickable(roundId: String) = buildText {
        variableValue("HIER", TextDecoration.UNDERLINED)
        hoverEvent(buildText { info("Klicke hier, um der Runde '$roundId' beizutreten!") })
        clickEvent(ClickEvent.runCommand("/rlgl join $roundId"))
    }

    fun notifyOpenerOfJoin(opener: Player, joined: Player, roundSize: Int) {
        opener.sendText {
            appendSuccessPrefix()
            variableValue(joined.name)
            appendSpace()
            success("ist der Runde beigetreten.")
            appendSpace()
            spacer("(")
            variableValue(roundSize.toString())
            spacer(" Spieler)")
        }
    }

    fun notifyOpenerOfLeave(opener: Player, left: Player, roundSize: Int) {
        opener.sendText {
            appendErrorPrefix()
            variableValue(left.name)
            appendSpace()
            error("hat die Runde verlassen.")
            appendSpace()
            spacer("(")
            variableValue(roundSize.toString())
            spacer(" Spieler)")
        }
    }

    fun notifyRemovedFromOpenRound(target: Player, remover: Player) {
        target.sendText {
            appendErrorPrefix()
            error("Du wurdest von")
            appendSpace()
            variableValue(remover.name)
            appendSpace()
            error("aus der Runde entfernt.")
        }
    }

    fun playEliminationEffects(player: Player) {
        player.location.world.strikeLightningEffect(player.location)

        player.playSound(true) {
            type(Sound.ENTITY_LIGHTNING_BOLT_THUNDER)
            volume(0.5f)
            pitch(1f)
        }
    }

    fun broadcastElimination(recipients: Collection<Player>, player: Player) {
        recipients.forEach { viewer ->
            viewer.sendText {
                appendErrorPrefix()
                variableValue(player.name)
                appendSpace()
                error("hat sich bei Rot bewegt und ist raus!")
            }
        }
    }

    fun broadcastKick(recipients: Collection<Player>, player: Player) {
        recipients.forEach { viewer ->
            viewer.sendText {
                appendErrorPrefix()
                variableValue(player.name)
                appendSpace()
                error("wurde von einem Community-Manager aus der Runde entfernt.")
            }
        }
    }

    fun playFinishSound(player: Player) {
        player.playSound(true) {
            type(Sound.UI_TOAST_CHALLENGE_COMPLETE)
            volume(0.5f)
            pitch(1f)
        }
    }

    fun broadcastFinish(recipients: Collection<Player>, player: Player, placement: Int) {
        recipients.forEach { viewer ->
            viewer.sendText {
                appendSuccessPrefix()
                variableValue("#$placement")
                appendSpace()
                variableValue(player.name)
                appendSpace()
                success("hat das Ziel erreicht!")
            }
        }
    }

    fun broadcastCountdown(recipients: Collection<Player>, secondsRemaining: Int) {
        recipients.forEach { player ->
            player.showTitle {
                title { text(secondsRemaining.toString(), NamedTextColor.GOLD) }
                times {
                    fadeIn(0)
                    stay(20)
                    fadeOut(0)
                }
            }

            player.playSound(true) {
                type(Sound.BLOCK_NOTE_BLOCK_HAT)
                volume(0.6f)
                pitch(1f)
            }
        }
    }

    fun broadcastCountdownGo(recipients: Collection<Player>) {
        recipients.forEach { player ->
            player.showTitle {
                title { text("LOS!", NamedTextColor.GREEN) }
                times {
                    fadeIn(0)
                    stay(20)
                    fadeOut(0)
                }
            }

            player.playSound(true) {
                type(Sound.ENTITY_PLAYER_LEVELUP)
                volume(0.7f)
                pitch(1f)
            }
        }
    }

    fun broadcastMilestone(recipients: Collection<Player>, aliveCount: Int) {
        recipients.forEach { player ->
            player.showTitle {
                title { text("NUR NOCH $aliveCount", NamedTextColor.GOLD) }
                times {
                    fadeIn(5)
                    stay(40)
                    fadeOut(10)
                }
            }

            player.playSound(true) {
                type(Sound.BLOCK_BELL_USE)
                volume(0.7f)
                pitch(1f)
            }
        }

        recipients.forEach { viewer ->
            viewer.sendText {
                appendInfoPrefix()
                info("Nur noch")
                appendSpace()
                variableValue(aliveCount.toString())
                appendSpace()
                info("Spieler übrig!")
            }
        }
    }

    fun broadcastPhase(recipients: Collection<Player>, state: RlglState) {
        recipients.forEach { player ->
            player.showTitle {
                title {
                    when (state) {
                        RlglState.GREEN -> text("GRÜN", NamedTextColor.GREEN)
                        RlglState.RED -> text("ROT", NamedTextColor.RED)
                        else -> text("")
                    }
                }
                times {
                    fadeIn(0)
                    stay(40)
                    fadeOut(0)
                }
            }

            player.playSound(true) {
                type(if (state == RlglState.GREEN) Sound.BLOCK_NOTE_BLOCK_PLING else Sound.BLOCK_NOTE_BLOCK_BASS)
                volume(0.7f)
                pitch(if (state == RlglState.GREEN) 1.5f else 0.6f)
            }
        }
    }

    fun broadcastResults(recipients: Collection<Player>, winners: List<UUID>) {
        recipients.forEach { player ->
            player.sendText {
                appendSuccessPrefix()
                success("Red Light, Green Light ist beendet.")
                if (winners.isNotEmpty()) {
                    appendNewline()
                    info("Gewinner:")
                    winners.forEachIndexed { index, uuid ->
                        appendNewline()
                        variableValue("#${index + 1}")
                        appendSpace()
                        variableValue(Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString())
                    }
                }
            }
        }
    }
}
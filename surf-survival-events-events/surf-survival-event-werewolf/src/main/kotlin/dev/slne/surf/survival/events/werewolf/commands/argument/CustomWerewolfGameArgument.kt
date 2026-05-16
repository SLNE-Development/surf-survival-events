package dev.slne.surf.survival.events.werewolf.commands.argument

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.service.WerewolfService

class WerewolfGameArgument(nodeName: String) :
    CustomArgument<WerewolfService, String>(StringArgument(nodeName), { info ->
        WerewolfGameManager.getGame(info.currentInput) ?: throw CustomArgumentException.fromAdventureComponent(
            buildText {
                appendErrorPrefix()
                error("Das Spiel")
                appendSpace()
                variableValue(info.input)
                appendSpace()
                error("wurde nicht gefunden.")
            })
    }) {
    init {
        this.replaceSuggestions(
            ArgumentSuggestions.stringCollection {
                WerewolfGameManager.getAllGames().keys
            }
        )
    }
}

inline fun CommandTree.werewolfGameArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandTree = then(
    WerewolfGameArgument(nodeName).setOptional(optional).apply(block)
)

inline fun Argument<*>.werewolfGameArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): Argument<*> = then(
    WerewolfGameArgument(nodeName).setOptional(optional).apply(block)
)

inline fun CommandAPICommand.werewolfGameArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandAPICommand =
    withArguments(WerewolfGameArgument(nodeName).setOptional(optional).apply(block))

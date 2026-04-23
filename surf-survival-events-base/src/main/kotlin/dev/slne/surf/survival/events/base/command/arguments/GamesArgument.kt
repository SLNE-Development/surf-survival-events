package dev.slne.surf.survival.events.base.command.arguments


import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.CustomArgument.CustomArgumentException.fromAdventureComponent
import dev.jorel.commandapi.arguments.StringArgument
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.survival.events.base.games.util.Games

class GameArgument(nodeName: String) : CustomArgument<Games, String>(
    StringArgument(nodeName),
    { info ->
        val normalizedInput = info.input.trim()

        Games.getGame(normalizedInput)
            ?: throw fromAdventureComponent(
                buildText {
                    appendErrorPrefix()
                    error("Das Spiel")
                    appendSpace()
                    variableValue(normalizedInput)
                    appendSpace()
                    error("existiert nicht.")
                }
            )
    }) {
    init {
        replaceSuggestions(
            ArgumentSuggestions.strings {
                Games.entries
                    .filter { Games.isGameEnabled(it.name) }
                    .map { it.name }
                    .toTypedArray()
            }
        )
    }
}

inline fun CommandAPICommand.gameArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandAPICommand = withArguments(GameArgument(nodeName).setOptional(optional).apply(block))
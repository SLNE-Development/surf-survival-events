package dev.slne.surf.survival.events.base.command.argument

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.CustomArgument.CustomArgumentException.fromAdventureComponent
import dev.jorel.commandapi.arguments.StringArgument
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.survival.events.base.game.GameKey
import dev.slne.surf.survival.events.base.game.GameRegistry

class GameKeyArgument(nodeName: String) : CustomArgument<GameKey<*>, String>(
    StringArgument(nodeName),
    { info ->
        val normalizedInput = info.input.trim()

        GameRegistry.getRegisteredGameKey(normalizedInput)
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
    }
) {
    init {
        replaceSuggestions(
            ArgumentSuggestions.stringCollection {
                GameRegistry.getRegisteredGames().map { it.key }
            }
        )
    }
}

inline fun CommandAPICommand.gameKeyArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandAPICommand {
    return withArguments(GameKeyArgument(nodeName).setOptional(optional).apply(block))
}
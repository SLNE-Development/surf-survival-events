package dev.slne.surf.survival.events.freebuild.papi

import dev.slne.surf.api.paper.hook.papi.expansion.PapiExpansion
import dev.slne.surf.survival.events.freebuild.papi.placeholder.IsEventActivePlaceholder

object PapiExpansion : PapiExpansion(
    "surf-survival-events-freebuild", listOf(
        IsEventActivePlaceholder
    ), "red"
)
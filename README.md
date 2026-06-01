# surf-survival-events

Modular event plugins for a dedicated Paper/Folia Minecraft event server.

The base plugin owns the parts that every event needs: commands, player
selection, session state, the shared lobby, late joins, leaving, kicking and
stopping. Each event plugin only implements its own gameplay.

This README is written for two audiences:

- Server staff who need to run and configure events.
- Developers who want to create a new event, even if they are new to this
  project.

## Table of contents

1. [Project structure](#project-structure)
2. [How an event starts](#how-an-event-starts)
3. [Build and deployment](#build-and-deployment)
4. [Base plugin configuration](#base-plugin-configuration)
5. [Base commands and permissions](#base-commands-and-permissions)
6. [Creating a new event](#creating-a-new-event)
7. [Important concepts](#important-concepts)
8. [Race event](#race-event)

## Project structure

```text
surf-survival-events
|-- surf-survival-events-base
|   Shared base plugin. Must be installed on the event server.
|
|-- surf-survival-events-events
|   |-- surf-survival-event-example
|   |   Copy this module when creating a new event.
|   |
|   `-- surf-survival-event-race
|       Current race event implementation.
|
`-- surf-survival-events-freebuild
    Plugin for the freebuild/survival server side integration.
```

Every event is its own Gradle subproject below
`surf-survival-events-events`. Event jars are deployed next to the base jar.

## How an event starts

```text
Players join the dedicated event server.
        |
        v
If no event is running, the base plugin sends them to the server lobby.
        |
        v
A community manager runs /survivalevents start <game>.
        |
        v
The base plugin:
  1. finds the registered GameHandler for <game>
  2. reads its GameOptions
  3. checks that enough eligible players are online
  4. loads or creates the default event world
  5. splits players into active players, reserves and spectators
  6. calls the event handler hooks
        |
        v
The event plugin teleports players, gives items and runs the actual game.
        |
        v
The event is stopped by command or by the handler itself.
```

Only one base event session can be active at a time.

## Build and deployment

Build the project with Gradle:

```powershell
.\gradlew.bat build
```

For a normal event server deployment, install at least:

- `surf-survival-events-base`
- one or more event plugins, for example `surf-survival-event-race`

Event plugins declare a required server dependency on
`surf-survival-events-base`, so the base plugin must be present and enabled.

## Base plugin configuration

The base config is generated at:

```text
plugins\surf-survival-events-base\config.yml
```

Current structure:

```yaml
# Lobby of the event server. Players wait here before an event starts.
serverLobby:
  world_key: minecraft:overworld
  x: 0.0
  y: 64.0
  z: 0.0
  pitch: 0.0
  yaw: 0.0

# How online players are selected when /survivalevents start <game> is used.
start:
  # Players with any of these permissions are ignored at event start.
  excludedPermissions: [ ]

  # Broadcasts a message after the event was handed to the handler.
  announceStart: true

# Behaviour for players joining the event server.
join:
  # Send players to the server lobby if they join while no event is active.
  teleportToServerLobbyWhenIdle: true

  # Auto-register players who join while an event is already running.
  autoJoinRunningEvent: true

  # Send a hint if a player joins during an event but cannot auto-join.
  announceRunningEventOnJoin: true
```

Use `/survivalevents set-server-lobby` in game to update `serverLobby`.
Handlers can send a player back to this lobby with:

```kotlin
GameService.teleportToServerLobby(player)
```

## Base commands and permissions

Root command: `/survivalevents`

| Command                               | Permission                                       | Description                                 |
|---------------------------------------|--------------------------------------------------|---------------------------------------------|
| `/survivalevents start <game>`        | `surf.survival.events.command.community_manager` | Start a registered event.                   |
| `/survivalevents stop`                | `surf.survival.events.command.community_manager` | Stop the active event.                      |
| `/survivalevents spectator`           | `surf.survival.events.command.spectator`         | Join the active event as spectator.         |
| `/survivalevents leave`               | `surf.survival.events.command.player`            | Leave the active event or spectator list.   |
| `/survivalevents participants [page]` | `surf.survival.events.command.spectator`         | Show active, reserve and spectator players. |
| `/survivalevents kick <targetPlayer>` | `surf.survival.events.command.spectator`         | Remove a player from the active event.      |
| `/survivalevents menu`                | `surf.survival.events.command.community_manager` | Open the event overview menu.               |
| `/survivalevents set-server-lobby`    | `surf.survival.events.command.community_manager` | Save your current position as server lobby. |
| `/survivalevents reload`              | `surf.survival.events.command.community_manager` | Reload the base config.                     |

## Creating a new event

The fastest and safest path is to copy
`surf-survival-events-events\surf-survival-event-example` and rename it.

The example module already shows the intended patterns:

- `ExampleConfig` for event-specific config.
- `ExampleGame` for lifecycle hooks.
- `PaperMain` for config loading and handler registration.

### Step 1: Create the Gradle module

Create a new folder:

```text
surf-survival-events-events\surf-survival-event-my-event
```

Add it to the root `settings.gradle.kts`:

```kotlin
include(":surf-survival-events-events:surf-survival-event-my-event")
```

Create `gradle.properties` in your new module:

```properties
main=dev.slne.surf.survival.events.myevent.PaperMain
authors=your-name
```

The parent build file `surf-survival-events-events\build.gradle.kts` already
applies the Paper plugin and adds `compileOnly(project(":surf-survival-events-base"))`
for every event subproject. Your event module still needs a `build.gradle.kts`,
but it can be empty unless you need extra dependencies.

### Step 2: Choose a stable event key

Every event needs a `GameKey`.

```kotlin
val KEY = GameKey.builder<MyEventGame>()
    .key("my_event")
    .displayName("MY EVENT")
    .skullTexture("base64 skull texture")
    .build()
```

The key is important because it is used for:

- the start command: `/survivalevents start my_event`
- registry lookup in `GameRegistry`
- the default event world

Do not rename the key after the event is released unless you also migrate
configs, commands and world data.

The base plugin creates or loads the default event world with
`GameWorldService.worldKeyFor(KEY)`. For key `my_event`, that world key is
`surf-event:my_event`. In handler hooks, use `context.eventWorld` instead of
hardcoding a world name.

### Step 3: Add an event config

Use `GamePosition` for locations inside the event world. It stores only
coordinates and rotation. The world comes from the current `GameContext`.

```kotlin
package dev.slne.surf.survival.events.myevent

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import dev.slne.surf.survival.events.base.game.RunningJoinPolicy
import dev.slne.surf.survival.events.base.game.StartOverflowPolicy
import dev.slne.surf.survival.events.base.util.GamePosition
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MyEventConfig(
    var gameplay: GameplayConfig = GameplayConfig(),
    var playerSpawn: GamePosition = GamePosition(),
    var spectatorSpawn: GamePosition = GamePosition(),
    var reserveSpawn: GamePosition = GamePosition()
) {
    companion object : SpongeYmlConfigClass<MyEventConfig>(
        MyEventConfig::class.java,
        plugin.dataPath,
        "config.yml"
    )

    @ConfigSerializable
    data class GameplayConfig(
        var minPlayersToStart: Int = 1,
        var activePlayerLimit: Int? = null,
        var overflowPolicy: StartOverflowPolicy = StartOverflowPolicy.SPECTATOR,
        var runningJoinPolicy: RunningJoinPolicy = RunningJoinPolicy.SPECTATOR,
        var autoJoinRunningPlayers: Boolean = true
    )
}
```

Generated config path:

```text
plugins\surf-survival-event-my-event\config.yml
```

### Step 4: Implement `GameHandler`

`GameHandler` is the event lifecycle contract. Simple events usually override:

- `options`
- `onStarted`
- `onRunningSpectatorJoin`
- `onParticipantRemove`
- `onStop`

Minimal beginner-friendly example:

```kotlin
package dev.slne.surf.survival.events.myevent

import dev.slne.surf.survival.events.base.game.*
import dev.slne.surf.survival.events.base.service.GameService
import kotlinx.coroutines.future.await
import org.bukkit.Bukkit
import org.bukkit.GameMode.ADVENTURE
import org.bukkit.entity.Player
import java.util.UUID

class MyEventGame : GameHandler {
    companion object {
        val KEY = GameKey.builder<MyEventGame>()
            .key("my_event")
            .displayName("MY EVENT")
            .skullTexture("base64 skull texture")
            .build()
    }

    override val options: GameOptions
        get() {
            val gameplay = MyEventConfig.getConfig().gameplay

            return GameOptions(
                minPlayersToStart = gameplay.minPlayersToStart,
                mode = if (gameplay.activePlayerLimit == null) {
                    GameMode.ALL_AT_ONCE
                } else {
                    GameMode.BATCHED
                },
                start = GameStartOptions(
                    activePlayerLimit = gameplay.activePlayerLimit,
                    overflow = gameplay.overflowPolicy
                ),
                spectatorsEnabled = true,
                runningJoinPolicy = gameplay.runningJoinPolicy,
                autoJoinRunningPlayers = gameplay.autoJoinRunningPlayers
            )
        }

    context(context: GameContext)
    override suspend fun onStarted() {
        context.gamePlayers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { setupPlayer(it) }
        }

        context.reservePlayers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { setupReserve(it) }
        }

        context.spectators.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { setupSpectator(it) }
        }
    }

    context(context: GameContext)
    override suspend fun onRunningSpectatorJoin(player: Player) {
        setupSpectator(player)
    }

    context(context: GameContext)
    override suspend fun onParticipantRemove(
        uuid: UUID,
        player: Player?,
        role: ParticipantRole,
        reason: PlayerRemoveReason
    ) {
        player?.inventory?.clear()

        if (player != null && reason != PlayerRemoveReason.DISCONNECT) {
            GameService.teleportToServerLobby(player)
        }
    }

    context(context: GameContext)
    override suspend fun onStop(reason: GameStopReason) {
        context.onlineEventPlayers.forEach { player ->
            player.inventory.clear()
            GameService.teleportToServerLobby(player)
        }
    }

    context(context: GameContext)
    private suspend fun setupPlayer(player: Player) {
        player.gameMode = ADVENTURE
        player.teleportAsync(MyEventConfig.getConfig().playerSpawn.toLocation()).await()
    }

    context(context: GameContext)
    private suspend fun setupReserve(player: Player) {
        player.gameMode = ADVENTURE
        player.teleportAsync(MyEventConfig.getConfig().reserveSpawn.toLocation()).await()
    }

    context(context: GameContext)
    private suspend fun setupSpectator(player: Player) {
        player.teleportAsync(MyEventConfig.getConfig().spectatorSpawn.toLocation()).await()
    }
}
```

Important beginner notes:

- `context.gamePlayers` contains UUIDs of active players.
- `context.reservePlayers` contains UUIDs that wait for the event handler.
- `context.spectators` contains UUIDs of spectators.
- `context.onlineGamePlayers`, `context.onlineReservePlayers` and
  `context.onlineSpectators` skip offline players for you.
- Always check `reason != PlayerRemoveReason.DISCONNECT` before teleporting a
  removed player. A disconnected player cannot be teleported.

### Step 5: Register the handler in `PaperMain`

```kotlin
package dev.slne.surf.survival.events.myevent

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.survival.events.base.game.GameRegistry
import dev.slne.surf.survival.events.base.game.GameStopReason
import dev.slne.surf.survival.events.base.service.GameService
import org.bukkit.plugin.java.JavaPlugin

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)

class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onEnableAsync() {
        MyEventConfig.init()
        GameRegistry.register(MyEventGame.KEY, MyEventGame())
    }

    override suspend fun onDisableAsync() {
        if (GameService.isActiveGame(MyEventGame.KEY)) {
            GameService.stopGameAndWait(GameStopReason.PLUGIN_DISABLE)
        }

        GameRegistry.unregister(MyEventGame.KEY)
    }
}
```

### Step 6: Test the event in game

1. Build and deploy the base jar and your event jar.
2. Start the server and confirm both plugins enable without errors.
3. Configure the base lobby with `/survivalevents set-server-lobby`.
4. Configure your event spawns in your event config.
5. Join with at least `minPlayersToStart` eligible players.
6. Run `/survivalevents start my_event`.
7. Test leaving, disconnecting, spectating and stopping.

## Important concepts

### `GameOptions`

`GameOptions` controls the generic behaviour that the base plugin can handle
for every event.

```kotlin
GameOptions(
    minPlayersToStart = 1,
    mode = GameMode.ALL_AT_ONCE,
    start = GameStartOptions(
        activePlayerLimit = null,
        overflow = StartOverflowPolicy.SPECTATOR
    ),
    spectatorsEnabled = true,
    runningJoinPolicy = RunningJoinPolicy.SPECTATOR,
    autoJoinRunningPlayers = true
)
```

| Option                    | Meaning                                                                            |
|---------------------------|------------------------------------------------------------------------------------|
| `minPlayersToStart`       | Minimum eligible online players required to start.                                 |
| `mode`                    | Descriptive category for the handler: all at once, batched, elimination or custom. |
| `start.activePlayerLimit` | Maximum active players at session start. `null` means all selected players.        |
| `start.overflow`          | What happens to eligible players beyond the active limit.                          |
| `spectatorsEnabled`       | Whether the base plugin may register spectators.                                   |
| `runningJoinPolicy`       | Default result for players joining after the event started.                        |
| `autoJoinRunningPlayers`  | Whether join events may auto-call the running join flow.                           |

`GameOptions` is read when the session starts. It is safe to read your config
inside the `options` getter.

### `StartOverflowPolicy`

| Value       | Behaviour                                                 |
|-------------|-----------------------------------------------------------|
| `SPECTATOR` | Extra selected players become spectators.                 |
| `RESERVE`   | Extra selected players wait in `context.reservePlayers`.  |
| `IGNORE`    | Extra selected players are not registered in the session. |

### `RunningJoinPolicy`

| Value       | Behaviour                                                          |
|-------------|--------------------------------------------------------------------|
| `DENY`      | Late joiners cannot join the running event.                        |
| `SPECTATOR` | Late joiners become spectators.                                    |
| `PLAYER`    | Late joiners become active players.                                |
| `RESERVE`   | Late joiners join the reserve list.                                |
| `CUSTOM`    | Override `onRunningJoin(player)` and return a `RunningJoinResult`. |

### Useful `GameHandler` hooks

| Hook                                                   | When it runs                                                          |
|--------------------------------------------------------|-----------------------------------------------------------------------|
| `customizeWorldCreator(creator)`                       | Before the default event world is created.                            |
| `customizeEventWorld(world)`                           | After the default event world was created or loaded.                  |
| `onStarting()`                                         | Session exists, world is loaded, status is still `STARTING`.          |
| `onStarted()`                                          | Session is `RUNNING`; teleport and initialize players here.           |
| `onRunningJoin(player)`                                | A player joined after start; return a `RunningJoinResult`.            |
| `onRunningPlayerJoin(player)`                          | Late join was registered as active player.                            |
| `onRunningReserveJoin(player)`                         | Late join was registered as reserve.                                  |
| `onRunningSpectatorJoin(player)`                       | Late join was registered as spectator.                                |
| `onParticipantRoleChange(uuid, previousRole, newRole)` | `GameService.setParticipantRole` changed a role.                      |
| `onParticipantRemove(uuid, player, role, reason)`      | Player left, was kicked, disconnected or the event stopped.           |
| `onStop(reason)`                                       | Event is stopping; clean up tasks, entities, scoreboards and players. |

All hook functions that declare `context(context: GameContext)` can access the
current snapshot as `context`.

### `GameContext`

`GameContext` is an immutable snapshot of the base session. If you need the
latest state after joins, kicks or role changes, call `GameService.snapshot()`
again.

Common properties:

| Property               | Description                                     |
|------------------------|-------------------------------------------------|
| `key`                  | The active event key.                           |
| `options`              | Options captured when the event started.        |
| `status`               | Current base lifecycle status.                  |
| `eventWorld`           | Default world loaded for this event.            |
| `eventWorldName`       | Name of `eventWorld`.                           |
| `gamePlayers`          | Active player UUIDs.                            |
| `reservePlayers`       | Reserve player UUIDs.                           |
| `spectators`           | Spectator UUIDs.                                |
| `participantPlayers`   | Active plus reserve UUIDs.                      |
| `allEventPlayers`      | Active, reserve and spectator UUIDs.            |
| `onlineGamePlayers`    | Online active Bukkit players.                   |
| `onlineReservePlayers` | Online reserve Bukkit players.                  |
| `onlineSpectators`     | Online spectator Bukkit players.                |
| `onlineEventPlayers`   | All online event players, including spectators. |

### Moving players between roles

Round or bracket events can keep their own state and update the base snapshot:

```kotlin
GameService.setParticipantRole(uuid, ParticipantRole.PLAYER)
GameService.setParticipantRole(uuid, ParticipantRole.RESERVE)
GameService.setParticipantRole(uuid, ParticipantRole.SPECTATOR)
```

This changes only the base role. Your event still needs to teleport players,
give or remove items and update its own state.

## Race event

The race event is registered with key `race`.

Start it through the base system:

```text
/survivalevents start race
```

The base plugin starts a `BATCHED` event:

- `playersPerRound` players become active racers.
- all other selected players become reserves.
- spectators are enabled.
- late joins are handled by custom race logic.

### Race flow

1. `/survivalevents start race` starts the base session.
2. `/race start` moves racers to the start area.
3. `/race start` again starts the countdown when the race is waiting.
4. `/race next-round [advance-count]` evaluates the current heat or final.
5. Repeat `/race start` and `/race next-round` until the final is finished.

### Race config

Generated at:

```text
plugins\surf-survival-event-race\config.yml
```

Important fields:

```yaml
gameplay:
  playersPerRound: 10
  qualifiersPerRound: 1
  finalWinnerCount: 3
  laps: 2
  lateJoinAsSpectator: true

roundLobbyLocation:
  x: 0.0
  y: 0.0
  z: 0.0
  yaw: 0.0
  pitch: 0.0

spectatorLocation:
  x: 0.0
  y: 0.0
  z: 0.0
  yaw: 0.0
  pitch: 0.0

checkpoints: [ ]
barriers: [ ]
starts: [ ]
```

Like other events, race locations use `GamePosition` and do not store a world.
They are resolved in the race event world from `GameContext`.

### Race commands

Root command: `/race`

| Command                                      | Permission                                            | Description                                                     |
|----------------------------------------------|-------------------------------------------------------|-----------------------------------------------------------------|
| `/race start`                                | `surf.survival.events.race.command.community_manager` | Prepare racers or start the countdown, depending on race state. |
| `/race next-round [advance-count]`           | `surf.survival.events.race.command.community_manager` | Evaluate the current heat/final and prepare the next step.      |
| `/race stop`                                 | `surf.survival.events.race.command.community_manager` | Stop the active race event.                                     |
| `/race reload`                               | `surf.survival.events.race.command.community_manager` | Reload race config.                                             |
| `/race leaderboard`                          | `surf.survival.events.race.command.spectator`         | Show the current race placement list.                           |
| `/race kick <player>`                        | `surf.survival.events.race.command.spectator`         | Remove a player from the race.                                  |
| `/race set-laps <value>`                     | currently no explicit permission in code              | Set lap count.                                                  |
| `/race set-players-per-round <value>`        | `surf.survival.events.race.command.community_manager` | Set active racers per heat.                                     |
| `/race set-qualifiers-per-round <value>`     | `surf.survival.events.race.command.community_manager` | Set default qualifiers for normal heats.                        |
| `/race set-final-winner-count <value>`       | `surf.survival.events.race.command.community_manager` | Set default winner count for the final.                         |
| `/race set-lobby <location> <rotation>`      | currently no explicit permission in code              | Set the round lobby location.                                   |
| `/race set-spectator <location> <rotation>`  | currently no explicit permission in code              | Set the spectator location.                                     |
| `/race set-checkpoint <pos1                  | pos2                                                  | create> <location>`                                             | `surf.survival.events.race.command.community_manager` | Create a checkpoint region. |
| `/race remove-checkpoint <checkpointNumber>` | `surf.survival.events.race.command.community_manager` | Remove a checkpoint by id.                                      |
| `/race set-start <pos1                       | pos2                                                  | create> <location> <rotation>`                                  | `surf.survival.events.race.command.community_manager` | Create the start region and rotation. |
| `/race set-barrier <pos1                     | pos2                                                  | create> <location>`                                             | `surf.survival.events.race.command.community_manager` | Create the barrier fill region. |
| `/race list <checkpoints                     | start                                                 | barrier>`                                                       | `surf.survival.events.race.command.community_manager` | Show configured race regions. |

### Race reconnect and late join behaviour

- Disconnecting racers keep their race state.
- Reconnecting players are restored to their previous race role when possible.
- New late joiners can join as reserves while the race still accepts late
  participants.
- Later new joiners become spectators.

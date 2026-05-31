# surf-survival-events

Modulares Survival-Event-System für einen dedizierten Event-Server.

Der alte Ablauf mit Event-NPC, Pre-Game-Queue und „erst öffnen, dann nochmal starten“ ist entfernt. Der Event-Server selbst ist jetzt die Lobby: Spieler verbinden auf den Event-Server, warten dort in der Server-Lobby und ein Community-Manager startet das Event direkt mit:

```text
/survivalevents start <game>
```

Dabei nimmt der Base-Service die aktuell online befindlichen Spieler auf dem Event-Server, sortiert sie nach Join-Reihenfolge und übergibt sie an den registrierten `GameHandler`.

## Ablauf

1. Spieler joinen vom Survival-/Freebuild-Server auf den dedizierten Event-Server.
2. Wenn kein Event läuft, teleportiert die Base die Spieler in `SurvivalEventsConfig.serverLobby`.
3. Ein Manager startet ein Event mit `/survivalevents start <game>`.
4. Die Base erstellt eine Session, wählt die Online-Spieler aus und splittet sie je nach `GameOptions` in:
   - `gamePlayers`: aktive Spieler
   - `reservePlayers`: Reserve für Batch-/Runden-Events
   - `spectators`: Zuschauer
5. Der Event-Handler bekommt `onStarting(context)` und danach `onStarted(context)`.
6. Spieler, die während eines laufenden Events joinen, werden je nach `runningJoinPolicy` abgelehnt, als Zuschauer, als Reserve oder als aktive Spieler registriert.
7. `/survivalevents stop` beendet die Session und ruft `onStop(context, reason)` auf.

## Base-Config

`surf-survival-events-base/config.yml` ist jetzt auf den Event-Server zugeschnitten:

```yaml
serverLobby:
  world: world
  x: 0.5
  y: 73.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0

start:
  includeOperators: true
  excludedPermissions: []
  announceStart: true

join:
  teleportToServerLobbyWhenIdle: true
  autoJoinRunningEvent: true
  announceRunningEventOnJoin: true
```

Wichtige Punkte:

- `serverLobby` ist die zentrale Lobby des Event-Servers.
- `start.excludedPermissions` kann genutzt werden, um Staff aus der automatischen Startauswahl auszuschließen.
- `join.autoJoinRunningEvent` sorgt dafür, dass Spieler, die während eines laufenden Events joinen, automatisch über den `GameHandler` eingeordnet werden.
- NPC- und Queue-Config gibt es in der Base nicht mehr. Der Survival-/Freebuild-NPC verbindet nur noch auf den Event-Server und liegt im `surf-survival-events-freebuild`-Modul.

## Commands

```text
/survivalevents start <game>
/survivalevents stop
/survivalevents spectator
/survivalevents leave
/survivalevents participants [page]
/survivalevents kick <player>
/survivalevents menu
/survivalevents set-server-lobby <location> <rotation>
```

`/survivalevents start <game>` startet direkt. Es gibt keinen zweiten Start-Schritt und keine Base-Warteliste mehr.

## Neues Event erstellen

Ein neues Event braucht typischerweise:

1. eigene Config
2. eigenen `GameKey`
3. eine `GameHandler`-Implementierung
4. Registrierung in `PaperMain`

### 1. Config

```kotlin
@ConfigSerializable
data class MyEventConfig(
    var eventWorld: String = "event_my_event",
    var gameplay: GameplayConfig = GameplayConfig(),
    var playerSpawn: EventLocationConfig = EventLocationConfig(world = "event_my_event"),
    var spectatorSpawn: EventLocationConfig = EventLocationConfig(world = "event_my_event")
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
        var runningJoinPolicy: String = "SPECTATOR"
    )
}
```

### 2. GameHandler

```kotlin
class MyEventGame : GameHandler {
    companion object {
        val KEY = GameKey.builder<MyEventGame>()
            .key("my_event")
            .displayName("MY EVENT")
            .skullTexture("...")
            .build()
    }

    override val options: GameOptions
        get() {
            val config = MyEventConfig.getConfig()
            val gameplay = config.gameplay

            return GameOptions(
                eventWorld = config.eventWorld,
                minPlayersToStart = gameplay.minPlayersToStart,
                mode = GameMode.ALL_AT_ONCE,
                start = GameStartOptions(
                    activePlayerLimit = gameplay.activePlayerLimit,
                    overflow = StartOverflowPolicy.SPECTATOR
                ),
                spectatorsEnabled = true,
                runningJoinPolicy = RunningJoinPolicy.SPECTATOR,
                autoJoinRunningPlayers = true
            )
        }

    override suspend fun onStarted(context: GameContext) {
        context.onlineGamePlayers.forEach { player ->
            player.teleportAsync(MyEventConfig.getConfig().playerSpawn.toLocation()).await()
        }

        context.onlineSpectators.forEach { spectator ->
            spectator.teleportAsync(MyEventConfig.getConfig().spectatorSpawn.toLocation()).await()
        }
    }

    override suspend fun onRunningJoin(context: GameContext, player: Player): RunningJoinResult {
        return RunningJoinResult.JOINED_AS_SPECTATOR
    }

    override suspend fun onRunningSpectatorJoin(context: GameContext, player: Player) {
        player.teleportAsync(MyEventConfig.getConfig().spectatorSpawn.toLocation()).await()
    }

    override suspend fun onParticipantRemove(
        context: GameContext,
        uuid: UUID,
        player: Player?,
        role: ParticipantRole,
        reason: PlayerRemoveReason
    ) {
        if (player != null && reason != PlayerRemoveReason.DISCONNECT) {
            player.teleportAsync(SurvivalEventsConfig.getConfig().serverLobby.toLocation()).await()
        }
    }

    override suspend fun onStop(context: GameContext, reason: GameStopReason) {
        val lobby = SurvivalEventsConfig.getConfig().serverLobby.toLocation()
        context.onlineEventPlayers.forEach { player ->
            player.teleportAsync(lobby).await()
        }
    }
}
```

### 3. Registrierung

```kotlin
class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        MyEventConfig.init()
    }

    override suspend fun onEnableAsync() {
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

Im Projekt ist `surf-survival-event-example` als ausführlicheres Template enthalten.

## GameOptions für verschiedene Event-Typen

### Alle spielen gleichzeitig

```kotlin
GameOptions(
    mode = GameMode.ALL_AT_ONCE,
    start = GameStartOptions(activePlayerLimit = null),
    runningJoinPolicy = RunningJoinPolicy.SPECTATOR
)
```

Alle online ausgewählten Spieler landen in `context.gamePlayers`.

### Nur N Spieler aktiv, Rest schaut zu

```kotlin
GameOptions(
    mode = GameMode.BATCHED,
    start = GameStartOptions(
        activePlayerLimit = 10,
        overflow = StartOverflowPolicy.SPECTATOR
    ),
    runningJoinPolicy = RunningJoinPolicy.SPECTATOR
)
```

Die ersten 10 Spieler nach Join-Reihenfolge landen in `context.gamePlayers`; alle weiteren online Spieler landen in `context.spectators`.

### Runden-/Batch-Event mit Reserve

```kotlin
GameOptions(
    mode = GameMode.BATCHED,
    start = GameStartOptions(
        activePlayerLimit = 10,
        overflow = StartOverflowPolicy.RESERVE
    ),
    runningJoinPolicy = RunningJoinPolicy.RESERVE
)
```

Die ersten 10 Spieler starten aktiv. Weitere Spieler landen in `context.reservePlayers`, damit der Handler sie später selbst in Runden einteilen kann.

## GameHandler-Hooks

- `onStarting(context)`: Session wurde erzeugt, Status ist `STARTING`. Gut für Vorbereitungen.
- `onStarted(context)`: Status ist `RUNNING`. Hier Spieler teleportieren, Items geben und den Event-State starten.
- `onRunningJoin(context, player)`: entscheidet, was mit Late-Joins passiert.
- `onRunningPlayerJoin(context, player)`: Late-Join wurde als aktiver Spieler registriert.
- `onRunningReserveJoin(context, player)`: Late-Join wurde als Reserve registriert.
- `onRunningSpectatorJoin(context, player)`: Late-Join wurde als Zuschauer registriert.
- `onParticipantRemove(context, uuid, player, role, reason)`: Teilnehmer/Zuschauer wurde entfernt, gekickt oder ist disconnected.
- `onStop(context, reason)`: Event wird beendet; Cleanup, Tasks canceln, Spieler zurück in die Server-Lobby.

## Race

Race ist jetzt ein Beispiel für ein Batched-Event auf dem dedizierten Event-Server:

- `/survivalevents start race` startet die Race-Session direkt.
- `gameplay.playersPerRound` bestimmt die aktiven Racer am Start, standardmäßig `10`.
- Weitere online Spieler werden automatisch Zuschauer.
- Late-Joins werden je nach `gameplay.lateJoinAsSpectator` als Zuschauer hinzugefügt oder abgelehnt.
- `/race start` steuert nur noch die Race-interne Phase: erst an den Start setzen, danach Countdown starten.
- `/race stop` stoppt Race und, falls aktiv, auch die Base-Session.

Race-Config-Auszug:

```yaml
eventWorld: event_race

gameplay:
  minPlayersToStart: 1
  playersPerRound: 10
  laps: 5
  lateJoinAsSpectator: true
  autoJoinServerPlayersAsSpectators: true

roundLobbyLocation:
  world: event_race

spectatorLocation:
  world: event_race
```

## Thread-Safety

Der Base-Service verwendet weiterhin einen gemeinsamen `ReentrantLock`, aber ohne alte Queue-Komplexität. Session-Status, Join-Reihenfolge, aktive Spieler, Reserve und Zuschauer werden dadurch atomar geändert. Suspend-Hooks werden niemals unter dem Lock ausgeführt.

Eine `ConcurrentBlockingQueue` ist hier nicht nötig, weil es keine isolierte Warteschlange mehr gibt. Für Race-ähnliche Events wird die Auswahl direkt beim Start aus der Join-Reihenfolge des Event-Servers berechnet.

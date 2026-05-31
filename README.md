# surf-survival-events

Modulares Survival-Event-System für einen dedizierten Event-Server.

Der Event-Server selbst ist die Lobby: Spieler verbinden auf den Event-Server, warten dort in der Server-Lobby und ein Community-Manager startet ein registriertes Event direkt mit:

```text
/survivalevents start <game>
```

Dabei nimmt der Base-Service die aktuell online befindlichen Spieler auf dem Event-Server, sortiert sie nach Join-Reihenfolge und übergibt sie an den registrierten `GameHandler`.

## Ablauf

1. Spieler joinen vom Survival-/Freebuild-Server auf den dedizierten Event-Server.
2. Wenn kein Event läuft, teleportiert die Base die Spieler in `SurvivalEventsConfig.serverLobby`.
3. Ein Manager startet ein Event mit `/survivalevents start <game>`.
4. Die Base lädt/erstellt die Standard-Event-Welt anhand des `GameKey`: `race` wird zu `event_race`, `example` zu `event_example`.
5. Die Base erstellt eine Session, wählt die Online-Spieler aus und splittet sie je nach `GameOptions.start` in:
   - `gamePlayers`: aktive Spieler
   - `reservePlayers`: Reserve für Batch-/Runden-Events
   - `spectators`: Zuschauer
6. Der Event-Handler bekommt `onStarting(context)` und danach `onStarted(context)`. Die geladene Standardwelt liegt in `context.eventWorld`.
7. Spieler, die während eines laufenden Events joinen, werden je nach `runningJoinPolicy` abgelehnt, als Zuschauer, als Reserve oder als aktive Spieler registriert.
8. `/survivalevents stop` beendet die Session und ruft `onStop(context, reason)` auf.

Die Base implementiert bewusst keine feste Rundenlogik. Nicht jedes Event hat Runden, und Race/Bracket/Parallel-Arena-Formate unterscheiden sich stark. Runden-Events nutzen `reservePlayers` und `GameService.setParticipantRole(...)`, verwalten ihre eigentliche Rundenlogik aber selbst.

## Base-Config

`surf-survival-events-base/config.yml` ist auf den Event-Server zugeschnitten:

```yaml
serverLobby:
  world: world
  x: 0.5
  y: 73.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0

start:
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
- `GameService.teleportToServerLobby(player)` ist die öffentliche Helper-Funktion, um Spieler nach einem Event oder Kick wieder in die Server-Lobby zu schicken.
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

1. eigene Config für event-spezifische Einstellungen
2. eigenen `GameKey`
3. eine `GameHandler`-Implementierung
4. Registrierung in `PaperMain`

### 1. Config

Die Standard-Event-Welt muss nicht in die Config. Sie wird aus dem `GameKey` abgeleitet. Bei `key("my_event")` lädt die Base `event_my_event` und stellt sie über `context.eventWorld` bereit.

```kotlin
@ConfigSerializable
data class MyEventConfig(
    var gameplay: GameplayConfig = GameplayConfig()
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
            val gameplay = MyEventConfig.getConfig().gameplay

            return GameOptions(
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
            player.teleportAsync(context.eventWorld.spawnLocation).await()
        }
    }

    override suspend fun onRunningJoin(context: GameContext, player: Player): RunningJoinResult {
        return RunningJoinResult.JOINED_AS_SPECTATOR
    }

    override suspend fun onRunningSpectatorJoin(context: GameContext, player: Player) {
        player.teleportAsync(context.eventWorld.spawnLocation).await()
    }

    override suspend fun onParticipantRemove(
        context: GameContext,
        uuid: UUID,
        player: Player?,
        role: ParticipantRole,
        reason: PlayerRemoveReason
    ) {
        if (player != null && reason != PlayerRemoveReason.DISCONNECT) {
            GameService.teleportToServerLobby(player)
        }
    }

    override suspend fun onStop(context: GameContext, reason: GameStopReason) {
        context.onlineEventPlayers.forEach { player ->
            GameService.teleportToServerLobby(player)
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

Im Projekt ist `surf-survival-event-example` als Template enthalten.

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
    runningJoinPolicy = RunningJoinPolicy.SPECTATOR
)
```

Die ersten 10 Spieler starten aktiv. Weitere Spieler landen in `context.reservePlayers`, damit der Handler sie später selbst in Heats, Runden oder Brackets einteilen kann.

Für den Rollenwechsel nutzt der Handler:

```kotlin
GameService.setParticipantRole(uuid, ParticipantRole.PLAYER)
GameService.setParticipantRole(uuid, ParticipantRole.RESERVE)
GameService.setParticipantRole(uuid, ParticipantRole.SPECTATOR)
```

## GameHandler-Hooks

- `onStarting(context)`: Session wurde erzeugt, `context.eventWorld` ist geladen, Status ist `STARTING`. Gut für Vorbereitungen.
- `onStarted(context)`: Status ist `RUNNING`. Hier Spieler teleportieren, Items geben und den Event-State starten.
- `onRunningJoin(context, player)`: entscheidet, was mit Late-Joins passiert.
- `onRunningPlayerJoin(context, player)`: Late-Join wurde als aktiver Spieler registriert.
- `onRunningReserveJoin(context, player)`: Late-Join wurde als Reserve registriert.
- `onRunningSpectatorJoin(context, player)`: Late-Join wurde als Zuschauer registriert.
- `onParticipantRoleChange(context, uuid, previousRole, newRole)`: Die Base-Rolle wurde durch `GameService.setParticipantRole` geändert.
- `onParticipantRemove(context, uuid, player, role, reason)`: Teilnehmer/Zuschauer wurde entfernt, gekickt oder ist disconnected.
- `onStop(context, reason)`: Event wird beendet; Cleanup, Tasks canceln, Spieler zurück in die Server-Lobby.

## Race

Race ist jetzt ein Batched-/Round-Event auf dem dedizierten Event-Server:

- `/survivalevents start race` startet die Base-Session und lädt automatisch `event_race`.
- `gameplay.playersPerRound` bestimmt die aktiven Racer pro Heat, standardmäßig `10`.
- Weitere online Spieler werden als `reservePlayers` registriert und vom Race-Service nacheinander in Heats aktiviert.
- `/race start` steuert die Race-interne Phase: zuerst an den Start setzen, danach Countdown starten.
- `/race next-round [advance-count]` wertet den laufenden Heat aus. Ohne Argument nutzt Race in Qualifikations-Heats `gameplay.qualifiersPerRound` und im Finale `gameplay.finalWinnerCount`.
- Sind noch Reservespieler vorhanden, wird automatisch der nächste Heat vorbereitet.
- Sind keine Reservespieler mehr vorhanden, werden die qualifizierten Spieler ins Finale gesetzt.
- Ist das Finale ausgewertet, geht Race in `FINISHED`, bleibt aber bis `/race stop` oder `/survivalevents stop` als Base-Session sichtbar.
- Late-Joins werden je nach `gameplay.lateJoinAsSpectator` als Zuschauer hinzugefügt oder abgelehnt.

Beispiel mit 100 Spielern, `playersPerRound = 10`, `qualifiersPerRound = 1`, `finalWinnerCount = 3`:

1. `/survivalevents start race`: 10 aktive Racer, 90 Reserven.
2. `/race start`, nach dem Heat `/race next-round`: 1 Spieler qualifiziert sich, nächste 10 Reserven werden vorbereitet.
3. Nach 10 Heats stehen 10 Qualifizierte im Finale.
4. Finale starten und mit `/race next-round` auswerten: Top 3 bleiben als Gewinner übrig.

Race-Config-Auszug:

```yaml
gameplay:
  minPlayersToStart: 1
  playersPerRound: 10
  qualifiersPerRound: 1
  finalWinnerCount: 3
  laps: 5
  lateJoinAsSpectator: true
  autoJoinServerPlayersAsSpectators: true

roundLobbyLocation:
  world: event_race

spectatorLocation:
  world: event_race
```

## Thread-Safety

Der Base-Service verwendet einen gemeinsamen `ReentrantLock`, aber ohne alte Queue-Komplexität. Session-Status, Join-Reihenfolge, aktive Spieler, Reserve und Zuschauer werden dadurch atomar geändert. Suspend-Hooks werden niemals unter dem Lock ausgeführt.

Eine globale Base-Warteschlange ist nicht nötig, weil der Event-Server selbst die Lobby ist. Für Race-ähnliche Events wird die Auswahl direkt beim Start aus der Join-Reihenfolge des Event-Servers berechnet; danach verwaltet das Event seine Heats selbst.

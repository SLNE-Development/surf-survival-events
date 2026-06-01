# Copilot instructions for `surf-survival-events`

## Build and test

- Build all modules: `.\gradlew.bat build`
- Run all tests: `.\gradlew.bat test`
- Run a single test class or method: `.\gradlew.bat test --tests "<fully.qualified.TestClass>"` or `.\gradlew.bat test --tests "<fully.qualified.TestClass>.<methodName>"`
- Build or test a single module when needed, for example:
  - `.\gradlew.bat :surf-survival-events-base:build`
  - `.\gradlew.bat :surf-survival-events-events:surf-survival-event-race:test --tests "<fully.qualified.TestClass>"`

## Architecture

- This is a multi-module Kotlin/Paper/Folia project.
- `surf-survival-events-base` owns the shared server lifecycle: commands, join/quit handling, session state, lobby teleporting, world loading, and the active event registry.
- Event modules under `surf-survival-events-events` implement `GameHandler`, register exactly one handler in `onEnableAsync()`, and unregister it in `onDisableAsync()`.
- `surf-survival-event-example` is the template module for new events; `surf-survival-event-race` is the main concrete game implementation.
- `surf-survival-events-freebuild` is a separate integration module for the freebuild/server-side NPC flow.
- `GameService` is the runtime owner of the active event session; `GameRegistry` connects event plugins to the base plugin; `GameWorldService` creates the default per-event world.

## Key conventions

- `GameKey` is the stable identity of an event. Keep its `key` lowercase and stable after release because it drives commands, config, and world names.
- Default event worlds use the `surf-event:<key>` namespace via `GameWorldService.worldKeyFor(...)`.
- Handler hooks use Kotlin context receivers (`context(context: GameContext)`) and usually rely on the snapshot passed by the base service.
- Event configuration is loaded through `SpongeYmlConfigClass` from `plugin.dataPath` and `config.yml`; base config initializes on enable, event configs initialize when the module is ready.
- The base command root is `/survivalevents`, with subcommands registered from `base/command/subcommand`.
- Permission constants live in `PermissionRegistry` and follow the `surf.survival.events.command.*` prefix.
- `PaperMain` classes are thin bootstrap entry points: load config, register commands/listeners/handlers, and stop the active game on disable.
- The base plugin assumes Folia/global-region world creation rules; world creation happens through `GameWorldService` on the global tick thread.

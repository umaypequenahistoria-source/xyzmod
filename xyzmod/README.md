# Xyz — Minecraft Forge 1.20.1 horror mod

Requires: Forge 1.20.1 (47.x), GeckoLib 4.4.x, Java 17.

## Build / run
1. Open the folder in IntelliJ or Eclipse (import as Gradle project), or drop in the Gradle wrapper from the Forge 1.20.1 MDK
   (`gradlew`, `gradle/wrapper/`) and run `./gradlew runClient` / `./gradlew build`.
2. The jar lands in `build/libs/`. Players need GeckoLib installed too.

## The story, in stages (per player, counted in play time; see `config/xyzmod-common.toml`)
- Stage 0, Wrongness: no Xyz yet. Whispers, doors opening, torches going out, footsteps behind you.
- Stage 1, Presence (8 min): "Xyz joined the game". He appears far away, docile, tells his story in order
  (he is code made for a video, he doesn't want to be deleted). Vanishes if you stare or approach.
  Setting a bed spawn makes him ask: "Are you going to stay here forever?" (3+ beds jumps to stage 2).
- Stage 2, Stalking (20 min): fake errors on screen, rain, darkness pulses, he follows you, teleports behind you when you stare.
- Stage 3, Hunt (40 min): night + thunder, constant Darkness, he chases and attacks to kill. If he kills you he lets you
  breathe (back to stage 2 for ~3 minutes) and it begins again.
- Hit him 4 times quickly and he snaps straight to stage 3. He takes damage but heals instead of dying; only players can hurt him.
- Fourth wall breaking stays inside the game (chat, on-screen text, fake error lines). He never touches your files or closes the game.

## Test commands (op)
`/xyz stage <0-3>`, `/xyz summon`, `/xyz event`, `/xyz reset`

## Where to edit
- Dialogue: `XyzDialogue.java` · Timeline: config file · Events/sounds: `XyzEvents.java`
- Behaviour: `entity/XyzEntity.java` (speeds, damage, vanish rules) · Story engine: `XyzDirector.java`
- Sounds are vanilla placeholders (Warden/Enderman, pitched down). Swap them in `XyzEntity` / `XyzEvents`.

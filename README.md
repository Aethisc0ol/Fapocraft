# Fapcraft 1.1.0: unofficial NeoForge 1.21.1 port (step 1 of N)

Adult content (18+). For personal use only. The original mod is closed-source, so don't redistribute this.

## Building

Built against NeoForge 21.1.206 and GeckoLib 4.7.1 (`gradle.properties`), using ModDevGradle 2.0.107 and Java 21.

The Gradle wrapper isn't included because it couldn't be generated offline. To add it:

1. Download the NeoForge 1.21.1 ModDevGradle MDK (github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle).
2. Copy `gradlew`, `gradlew.bat` and the `gradle/` folder into this project.
3. Run `./gradlew build` (or `gradlew.bat build` on Windows). The jar ends up in `build/libs/`.
4. Put that jar plus `geckolib-neoforge-1.21.1-4.7.1.jar` (or newer 4.x) in your mods folder.
   For a quick test you can run `./gradlew runClient` instead.

If Gradle can't find a version, use the plugin and `neo_version` numbers from the MDK's own files.

## What works in this step

- **18+ warning screen** before the title screen. "Don't ask again" is saved in the client config.
  The under-18 button closes the game. It does **not** delete files like the original did.
- **Jenny**: model, texture, all animations, a dressed and a nude model, and voice lines and sounds.
  Get her from the spawn egg in the Spawn Eggs creative tab.
- **Right-click menu**: Strip / Dress up, Blowjob, Boobjob, Doggy, Follow me / Stop following, Set home, Go home.
- **Prices** are taken from the original: Strip = 1 gold ingot, Blowjob = 3 emeralds,
  Boobjob = 2 ender pearls, Doggy = 2 diamonds. Free in creative.
- **Scenes** play with the original dialogue and sound timings:
  - Blowjob: intro, then a loop. Hold the speed key to thrust, finish, fade to black.
  - Boobjob: strips her first if she's dressed, then intro, loop, faster, finish.
  - Doggy: she walks to the nearest bed (within 8 blocks), climbs on and waits.
    Right-click her (or press the speed key) to start. Then slow, faster, finish.
- **During a scene** your character is drawn as part of Jenny's model with your own skin
  (Steve or Alex arms to match). Your real model is hidden, movement is frozen, and the camera
  switches to third-person. Mouse look orbits the camera.
- **Scene keys** (change them under Controls, "Fapcraft"):
  - **G**: hold to go faster.
  - **H**: finish, once the pleasure meter is full.
  - **J**: leave the scene.
- **Follow and home**: she follows you and teleports if she falls far behind. With a home set,
  she wanders within 12 blocks of it. Home and following are saved with the world.

## Differences from the original

- The menu is a plain column of buttons, not the original radial UI.
- The camera is vanilla third-person (front), not the original's scripted camera moves.
- The HUD meter is a simple bar, not the `hornymeter.png` artwork.
- Each sound folder is now one sound event with random variants, instead of one event per file.
- The slow boobjob loop also fills the meter. In the original its keyframe did nothing.
- Item exchange happens when you pick the action, not mid-animation, and the items aren't drawn in her hand.

## Not ported yet

- Jenny's combat (sword and bow attacks, ender pearl throwing, downed state), armour and held-item
  rendering, and bra-string rendering.
- Natural spawning and her house structure. 1.12 structures, loot tables, recipes and blockstates
  need converting.
- Ellie, Slime girl, Bia, Luna, Allie, Galath, Manglelie, and the Bee (which still needs a
  closer look). Their models, animations, textures and sounds are already in the project for
  the next steps.
- The horny potion, Allie's lamp, the Galath coin, gender swap and multiplayer sex requests.

The Goblin and Kobold, and everything tied to them (kobold eggs, tribes, their assets, sounds and
text), are intentionally left out of this port.

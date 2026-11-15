# VapeLike (Singleplayer, Minecraft 1.21.11, Fabric)

Singleplayer-only utility client with a Vape V4 style click GUI.

- **Right Shift** toggles the GUI (Esc also closes it).
- **Left click** a module: toggle. **Right click** or the dots: show its sliders.
- Drag panel headers to move them. Click the sidebar entries to show/hide panels.
- Modules automatically switch off when you are not in a singleplayer world.

## Build
GitHub: push this folder to a repo, open **Actions -> Build**, download the `vapelike-mod` artifact.
Local: `gradle build` (Gradle 9.x, JDK 21) -> `build/libs/vapelike-1.0.0.jar`.

Install: Fabric Loader 0.18.4+ and Fabric API for 1.21.11, put the jar into `mods/`.

## Modules
Combat: AimAssist, AutoClicker, Reach, RightClicker, SilentAura, Sprint
Render: ESP, Fullbright
Utility: Scaffold, Fly
World: Anti-AFK, Parkour

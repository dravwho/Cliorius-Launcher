# Cliorius Launcher

[![Release](https://img.shields.io/github/v/release/dravwho/Cliorius-Launcher)](https://github.com/dravwho/Cliorius-Launcher/releases)
[![License](https://img.shields.io/github/license/dravwho/Cliorius-Launcher)](LICENSE)

**Cliorius Launcher** is a **Minecraft: Java Edition launcher for Android** — a fork of
[Pojav Glow-Worm](https://github.com/Vera-Firefly/Pojav-Glow-Worm) (itself based on
ZalithLauncher) with a modern UI, real performance tooling, and a mod manager.

## Download

Grab the APK that matches your device from [Releases](https://github.com/dravwho/Cliorius-Launcher/releases):

| File | For |
|---|---|
| `Cliorius-Launcher-arm64.apk` | Modern Android phones (2017+, 64-bit) — **most common** |
| `Cliorius-Launcher-arm.apk` | Older / 32-bit devices |
| `Cliorius-Launcher-x86_64.apk` | Emulators, Chromebooks, 64-bit x86 |
| `Cliorius-Launcher-x86.apk` | Legacy 32-bit x86 emulators |
| `Cliorius-Launcher-universal-arm.apk` | One APK for every ARM device (safest) |

Install: allow "Install unknown apps" → open the APK → Install. On first launch the
launcher downloads the Java runtime and Minecraft assets automatically (internet and
roughly 1–2 GB of free space required).

## Features

- **Runs Minecraft Java** — nearly every version from rd-132211 to the latest snapshot,
  with Forge / Fabric / NeoForge / Quilt / OptiFine support.
- **Modpack importer (.mrpack)** — install a Modrinth modpack in one tap; mods are
  downloaded and verified automatically.
- **Mod manager** — browse and download mods, shaders, resource packs and datapacks from
  Modrinth, with automatic dependency installation, update checking and conflict detection.
- **Performance & FPS** — custom RAM allocation, GC selection (ZGC / G1GC), FPS cap,
  in-game FPS overlay, and Auto-Tune that adapts to your device.
- **Renderers** — gl4es (OpenGL ES 2/3), nggl4es, MobileGlues, Mesa/OSMesa (6 variants),
  Vulkan Zink, virgl, freedreno, panfrost, with automatic GPU detection.
- **Multi-instance** — isolated game directories per version.
- **Backup & restore** — export a whole instance or a single world as `.zip`.
- **World manager** — inspect, back up and delete worlds.
- **Crash viewer** — reads the last crash and suggests a one-tap fix.
- **Security** — app PIN lock, session list, logout all sessions.
- **20 UI languages** — change the whole launcher language from Settings.
- **Login** — Microsoft, Ely.by / Yggdrasil (authlib-injector), and offline accounts.

## License & credits

- Licensed under the **GNU GPLv3** (inherited from Pojav). See [LICENSE](LICENSE).
- Fork of **Pojav Glow-Worm** by Vera-Firefly and the PojavLauncher contributors.
- Developed by **dravwho** — [github.com/dravwho](https://github.com/dravwho)

## Building from source

```bash
git clone https://github.com/dravwho/Cliorius-Launcher.git
cd Cliorius-Launcher
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew assembleDebug                 # all ABIs
./gradlew -Darch=arm64 assembleDebug    # a single ABI
```

Requires JDK 17, Android SDK (platform 35, build-tools 35, NDK 25.2.9519653). The GitHub
Actions workflow builds every ABI and publishes a release when a `v*` tag is pushed.

See also: [CHANGELOG.md](CHANGELOG.md) · [CONTRIBUTING.md](CONTRIBUTING.md) · [SECURITY.md](SECURITY.md)

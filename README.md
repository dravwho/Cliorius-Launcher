# Cliorius Launcher

[![Release](https://img.shields.io/github/v/release/dravwho/Cliorius-Launcher)](https://github.com/dravwho/Cliorius-Launcher/releases)
[![License](https://img.shields.io/github/license/dravwho/Cliorius-Launcher)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3ddc84)](https://developer.android.com)

**Cliorius Launcher** runs **Minecraft: Java Edition** on Android. It is a fork of
[Pojav Glow-Worm](https://github.com/Vera-Firefly/Pojav-Glow-Worm) (itself based on
ZalithLauncher) with a redesigned interface, real performance tooling, and an
integrated mod manager.

---

## Download

Grab the APK that matches your device from
[Releases](https://github.com/dravwho/Cliorius-Launcher/releases):

| File | Target device |
| --- | --- |
| `Cliorius-Launcher-arm64.apk` | Modern Android phones (2017+, 64-bit) — **most common** |
| `Cliorius-Launcher-arm.apk` | Older / 32-bit ARM devices |
| `Cliorius-Launcher-x86_64.apk` | Emulators, Chromebooks, 64-bit Intel |
| `Cliorius-Launcher-x86.apk` | Legacy 32-bit Intel emulators |
| `Cliorius-Launcher-universal-arm.apk` | One APK for every ARM phone (safest choice) |

### Installation

1. Download the APK for your architecture.
2. Allow *Install unknown apps* for your file manager or browser.
3. Open the APK and install.
4. On first launch the launcher downloads a Java runtime and the Minecraft
   assets automatically. This requires an internet connection and roughly
   1–2 GB of free storage.

**Minimum requirements:** Android 8.0 (API 26), 2 GB RAM recommended,
OpenGL ES 2.0 capable GPU.

---

## Features

### Game support
- Runs almost every Minecraft Java version, from `rd-132211` up to the latest
  snapshot.
- Mod loaders: **Forge**, **NeoForge**, **Fabric**, **Quilt**, **OptiFine**.
- Version catalog with search and filters (release, snapshot, April Fools,
  alpha/beta), downloaded straight from Mojang's `version_manifest_v2.json`
  with a BMCLAPI mirror fallback.

### Mod Manager
- Search and install mods from **Modrinth** without leaving the launcher.
- Mod icons are pulled from the Modrinth CDN.
- Downloads start on a single tap — no confirmation dialogs.
- Enable/disable installed mods by renaming `.jar` to `.disabled`, or delete
  them outright.

### Performance & FPS
- **Custom RAM allocation** globally, plus a **per-version override** so a heavy
  1.21 modpack and a light 1.8 PvP instance can use different budgets.
- Garbage collector presets: ZGC (JRE 21+) or G1GC with a 200 ms pause target.
- Tiered compilation, string deduplication, class-data sharing, explicit-GC
  suppression and fast-throw flags.
- **FPS cap** exported to the renderer through `LIBGL_SYNC_MAXFPS` / `SDL_MAXFPS`
  and `-Dcliorius.maxFps`.
- **Auto-Tune** inspects device RAM and GPU and applies a recommended profile.
- **In-game FPS overlay**.
- Benchmark screen: device probe, RAM allocation test, and a native MultiDraw
  micro-benchmark that ranks renderer backends.

### Renderers
| Renderer | Backend | Notes |
| --- | --- | --- |
| `opengles3_nggl4es` | gl4es (NG) | Good default for most devices |
| `opengles2` | gl4es 1.14 | Widest compatibility |
| `opengles2_ptitseb` | gl4es (ptitseb) | Alternative GLES2 path |
| `opengles2_vgpu` / `_1` | vGPU | VirGL-based |
| `opengles3_mges` | MobileGlues | GLES 3.x translation |
| `opengles3_mggl` | MobileGL | GLES 3.x translation |
| `mesa_3d` | Mesa / OSMesa | Six builds: 8, 2121, 2205, 2300d, 2304, 2320d |
| `vulkan_zink` | Zink on Vulkan | Fastest on modern Adreno |
| `gallium_virgl` | VirGL | Virtualised environments |
| `gallium_freedreno` | Freedreno | Qualcomm Adreno |
| `gallium_panfrost` | Panfrost | ARM Mali |

GPU detection is real: the launcher creates an EGL context and reads
`GL_VENDOR` / `GL_RENDERER` from the driver, then adapts `LD_PRELOAD`, Zink
driver selection and the visibility of Adreno-only options accordingly.

### Accounts
- **Microsoft** — official OAuth flow in an isolated WebView.
- **Ely.by / Yggdrasil** — authlib-injector compatible; Ely.by and LittleSkin
  are preconfigured, and additional servers can be added.
- **Offline** — for singleplayer and LAN.

> **Account security.** Cliorius Launcher never stores your Microsoft password.
> Sign-in happens on the official `login.live.com` page inside an isolated
> WebView, and the launcher only receives an access token that you can revoke at
> any time from your Microsoft account. Tokens are kept in the app's private
> storage and are never sent to a third-party server. You can revoke access at
> `account.microsoft.com` → *Privacy* → *Apps and services*.

### Security
- Optional **PIN app lock**.
- **Log out all sessions** in one tap.

### Interface
- Dark, flat design with a neutral `#B3B3B3` accent.
- Vector icons only — no emoji anywhere in the UI.
- Responsive layout for portrait and landscape.
- 20+ UI languages, switchable from Settings.

---

## Building from source

### Requirements
- JDK 17
- Android SDK with platform 35 and build-tools 35.0.0
- Android NDK 25.2.9519653 (only needed if you rebuild the native libraries)

### Steps

```bash
git clone https://github.com/dravwho/Cliorius-Launcher.git
cd Cliorius-Launcher

# Point Gradle at your SDK
echo "sdk.dir=/path/to/android-sdk" > local.properties

# Build a single ABI (arm64, arm, x86, x86_64) or all of them
JAVA_HOME=/path/to/jdk17 ./gradlew -Darch=arm64 assembleDebug
JAVA_HOME=/path/to/jdk17 ./gradlew -Darch=all   assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

### Continuous integration
`.github/workflows/build-apk.yml` builds every ABI on push and attaches the
resulting APKs to a GitHub Release when a `v*` tag is pushed.

---

## Project layout

```
app/src/main/java/net/kdt/pojavlaunch/firefly/   launcher core (Pojav engine)
app/src/main/java/com/movtery/                  UI layer and mod parsing
app/src/main/java/net/kdt/pojavlaunch/firefly/version/   version catalog, installer, Modrinth API
app/src/main/jniLibs/                           prebuilt native libraries per ABI
app/src/main/assets/                            renderers, JRE archives, LWJGL components
```

---

## Credits

Cliorius Launcher would not exist without the work of the people below.

- **dravwho** — project owner, Cliorius UI, performance tooling, Mod Manager,
  per-version settings, packaging and releases.
- **Vera-Firefly** and the **Pojav Glow-Worm** contributors — the launcher engine
  this project is forked from.
- **PojavLauncherTeam** — the original PojavLauncher, and the Boardwalk JVM
  bridge underneath it.
- **ZalithLauncher** contributors — UI architecture and version-management ideas.
- **ArtDeell**, **Mathias-Boulanger** and every other upstream contributor whose
  code is still running inside this app.
- The **gl4es**, **Mesa**, **ANGLE**, **MobileGlues**, **MobileGL**, **LWJGL** and
  **authlib-injector** projects.

See [`CONTRIBUTING.md`](CONTRIBUTING.md) if you want to help.

---

## License

Cliorius Launcher is distributed under the **GNU General Public License v3.0**,
inherited from Pojav Glow-Worm. See [`LICENSE`](LICENSE) for the full text.

This program is free software: you can redistribute it and/or modify it under
the terms of the GPL as published by the Free Software Foundation, either
version 3 of the License, or (at your option) any later version.

Minecraft is a trademark of Mojang Synergies AB. This project is not affiliated
with or endorsed by Mojang or Microsoft.

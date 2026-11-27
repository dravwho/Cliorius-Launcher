# Changelog

All notable changes to Cliorius Launcher are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## v8.0.0 — 2026-11-27

### Added
- **Backup & Restore instance** — export the whole active instance (version files, mods,
  config, worlds) into a single `.zip` under `Downloads/Cliorius`, and import such a
  `.zip` back on any device. Reachable from Settings → Tools & Diagnostics.
- **Automatic mod dependency install** — when a mod is downloaded from Modrinth, every
  dependency marked `required` is resolved and installed recursively (Fabric API,
  Cloth Config, etc.), so mods no longer fail to load because of missing libraries.
- **Safe Mode** — one tap disables every mod in the active instance (`.jar` →
  `.jar.disabled`) for crash diagnosis, and one tap restores them. Nothing is deleted.
- **Performance presets** — PvP / Survival / Balanced presets that write the real
  preference keys consumed by `JREUtils` at launch (RAM, renderer, FPS cap, GC flags).

### Fixed
- `versionCode` and `base_version_code` are now aligned (8000), so the in-app update
  checker can actually detect a newer release.
- Repository cleanup: removed leftover `.bak-nexhost` files and extended `.gitignore`.

## v7.0.0 — 2026-11-27

### Added
- **20 launcher UI languages** — the language picker in Settings now actually works.
  Previously only English and two Chinese variants shipped, so every other choice
  silently fell back to English.
- **Crash Viewer** — reads `latestcrash.txt`, detects common failure patterns
  (OutOfMemory, OpenGL context failure, wrong Java version, broken mod) and offers a
  one-tap fix for each.
- **Multi-type mod manager** — Mod / Shader / Resource Pack / Datapack tabs, each
  installing into the correct folder (`mods/`, `shaderpacks/`, `resourcepacks/`,
  `datapacks/`).

### Fixed
- The update checker pointed at the upstream `Vera-Firefly/Pojav-Glow-Worm` repository,
  which would have offered a Pojav APK instead of Cliorius. It now targets
  `dravwho/Cliorius-Launcher` with the correct per-ABI asset names.

## v6.0.0 — 2026-11-27

### Added
- Mod icons loaded from the Modrinth CDN, cached in memory.
- One-tap mod download — no confirmation dialog, no external-URL prompt.
- Per-version RAM allocation (`PgwVersionConfig.ramAllocation`), read by
  `JREUtils.launchJavaVM()` as `-Xms`/`-Xmx`. `0` means "follow the global setting".

### Changed
- All repository documentation rewritten in English.

## v5.0.0 — 2026-11-26

### Added
- Integrated Mod Manager: search and download mods from Modrinth inside the app.
- Performance & FPS settings: modern GC (ZGC/G1GC), FPS cap, in-game FPS overlay,
  one-tap Auto-Tune.
- Security & Account settings: app PIN lock, active session list, log out of all sessions.
- UI language picker (20+ languages).
- Multi-instance / per-version profile isolation.
- Multi-method login: Microsoft, Ely.by / Yggdrasil, and offline, with a Microsoft
  account security notice.
- Cliorius UI v3: minimal home screen (RAM / Renderer / Java card), a single settings
  entry point, smooth press animations.

## v1.0.0 — 2026-11-26

### Added
- First public release: full Pojav Glow-Worm engine (JRE 8/17/21/25, gl4es, nggl4es,
  MobileGlues, Mesa/OSMesa, Vulkan Zink, virgl, freedreno, panfrost), Cliorius branding,
  per-ABI APKs.

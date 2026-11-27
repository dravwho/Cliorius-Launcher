# Changelog

All notable changes to Cliorius Launcher are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [6.0.0] - 2026-11-27

### Added

#### Mod Manager
- **Mod icons from Modrinth** — every mod in the online catalog now shows its
  official icon, fetched directly from the Modrinth CDN (`icon_url`) and cached
  in memory so scrolling stays smooth.
- **One-tap download** — pressing *Download* starts the transfer immediately.
  There is no confirmation dialog and no "accept this URL" prompt. Pick a
  version, fill in the details, and the file lands in the instance `mods/`
  folder.
- Automatic Minecraft-version matching: the newest release compatible with the
  active instance is selected for you.

#### Per-version settings
- **Per-version RAM allocation** — each installed version can now define its own
  memory budget (0–8192 MB) instead of inheriting the global launcher value.
  A value of `0` keeps the global setting. The override is written to the
  version config (`ramAllocation`) and injected into the JVM as `-Xms`/`-Xmx`
  at launch time.
- The per-version settings page gained a RAM slider with a live MB readout.

### Changed
- Version settings page layout reworked so the RAM control sits next to the JVM
  arguments field.

### Fixed
- Java compile error caused by a non-final local variable captured in a lambda
  inside `JREUtils.launchJavaVM`.
- Kotlin data class syntax error in `PgwVersionConfig` (missing comma before the
  new `ramAllocation` field).

## [5.0.0] - 2026-11-26

### Added
- Integrated Mod Manager backed by the Modrinth API.
- Performance & FPS settings: GC selection (ZGC/G1GC), tiered compilation,
  string deduplication, class sharing, explicit-GC suppression, fast throw,
  FPS cap, and VSync toggle.
- Auto-Tune: reads device RAM and GPU, then applies a recommended profile.
- In-game FPS overlay.
- App lock with PIN.
- "Log out all sessions".
- UI language selector covering 20+ languages.
- Multi-instance support with per-version data isolation.

## [1.0.0] - 2026-11-23

### Added
- First public build: full Pojav Glow-Worm engine with the Cliorius UI.
- Renderers: gl4es, nggl4es, MobileGlues, Mesa/OSMesa (6 variants), Vulkan Zink,
  virgl, freedreno, panfrost.
- JRE 8 / 17 / 21 / 25 runtime loader.
- Microsoft, Ely.by/Yggdrasil and offline account support.
- Benchmark screen (device info, RAM test, renderer micro-benchmark).

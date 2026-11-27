# Changelog

All notable changes to Cliorius Launcher are documented in this file.

## v7.0.0 — 2026-11-27

### Added
- **20 launcher UI languages** — the language picker in Settings now actually works.
  Full translations for: Indonesian, English, Simplified Chinese, Traditional Chinese,
  Japanese, Korean, German, French, Spanish, Portuguese, Russian, Arabic, Turkish,
  Vietnamese, Thai, Italian, Dutch, Polish, Ukrainian, plus "Follow system".
- **Crash Viewer** — reads `latestcrash.txt`, detects common failure patterns
  (OutOfMemory, OpenGL context failure, wrong Java version, broken mod) and offers
  one-tap automatic fixes (raise RAM, switch renderer, change JRE).
- **Mod Manager multi-type** — tabbed catalog for Mods, Shaders, Resource Packs and
  Datapacks. Files are installed into the correct folder (`mods/`, `shaderpacks/`,
  `resourcepacks/`, `datapacks/`).
- **Mod icons from Modrinth** — catalog entries show the project icon, cached in memory.
- **One-tap mod download** — no confirmation dialog, no "accept this URL" prompt.
- **Per-version settings** — each Minecraft instance can override RAM allocation
  independently of the global launcher setting.

### Fixed
- **Update checker pointed at the upstream Pojav repository** — it now checks
  `dravwho/Cliorius-Launcher` and downloads the correct Cliorius APK for the device ABI.
- **`versionCode` was hardcoded to 9999999** — now a real, incrementing version code
  so in-app update detection works.
- Apostrophe escaping in French and Italian string resources that broke resource
  compilation.

### Changed
- Repository documentation is English-only (`README-ZH_CN.md` removed).

## v6.0.0 — 2026-11-27

### Added
- Mod icons from the Modrinth CDN in the catalog.
- One-tap mod download without a confirmation dialog.
- Per-version RAM allocation (`PgwVersionConfig.ramAllocation`), read by
  `JREUtils.launchJavaVM()` and applied as `-Xms`/`-Xmx`.

### Changed
- All repository documentation rewritten in English.

## v5.0.0 — 2026-11-26

### Added
- Integrated Mod Manager backed by the Modrinth API.
- Performance & FPS settings: GC selection (ZGC/G1GC), tiered compilation,
  string deduplication, class sharing, FPS limit, VSync control, FPS overlay.
- Auto-Tune: reads device RAM and GPU, then applies a matching performance profile.
- Security & Account settings: PIN app lock, session list, log out of all sessions.
- Launcher UI language picker.
- Multi-instance support through per-version data isolation.

## v1.0.0 — 2026-11-25

### Added
- First public build: full Pojav Glow-Worm engine with the Cliorius UI.
- Renderers: gl4es, nggl4es, MobileGlues, Mesa/OSMesa (6 variants), Vulkan Zink,
  virgl, freedreno, panfrost.
- Microsoft, Ely.by/Yggdrasil and offline account login.
- Per-ABI APKs: arm64-v8a, armeabi-v7a, x86, x86_64 and a universal ARM build.

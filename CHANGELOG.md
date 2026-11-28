# Changelog

All notable changes to Cliorius Launcher are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## v9.0.0 — 2026-11-28

### Added
- **Modpack importer (.mrpack)** — install a Modrinth modpack in one tap. Implements the
  official format: reads `modrinth.index.json`, downloads every declared file to its
  relative path, verifies SHA-1, and copies `overrides/` + `client-overrides/` on top.
  Path traversal (`..`, absolute paths, drive letters) is rejected per the specification.
- **Mod update checker** — resolves each installed jar to its Modrinth project via
  `/v2/version_file/{sha1}` and offers "Update all" when newer compatible versions exist.
- **Mod conflict detection** — flags duplicate mod ids, mods built for the wrong loader,
  and missing required dependencies (e.g. Fabric API) before you launch.
- **World manager** — lists worlds in `saves/` with size and last-played time; back up a
  single world to `.zip` or delete it.

### Fixed
- Duplicate `cli_mods_installed_deps` string resource broke `mergeDebugResources`.
- `sha1Of()` returns a nullable string; comparison now uses a null-safe `equals`.
- Removed a hardcoded `base_version_code` string that shadowed the Gradle `resValue`,
  which made the in-app update checker compare against the wrong number.

## v8.0.0 — 2026-11-27

### Added
- Backup & restore a full instance as `.zip`.
- Automatic installation of required Modrinth dependencies (recursive).
- Safe Mode: disable/enable all mods without deleting anything.
- Performance presets (PvP / Survival / Balanced) that write real JVM and renderer prefs.

### Fixed
- `versionCode` and `base_version_code` now match, so update checks work.

## v7.0.0 — 2026-11-27

### Added
- 20 UI languages with real translations.
- Crash viewer with automatic fix suggestions (OOM, GL, Java, mod).
- Multi-type mod manager: mods, shaders, resource packs, datapacks.

### Fixed
- Update checker pointed at the upstream Pojav repository instead of Cliorius.

## v6.0.0 — 2026-11-27

### Added
- Mod icons from the Modrinth CDN in the catalog.
- One-tap download with no confirmation dialog.
- Per-version RAM allocation.

## v5.0.0 — 2026-11-26

### Added
- Modrinth mod manager, performance & FPS tools (auto-tune, FPS overlay, FPS cap),
  app PIN lock, logout all sessions, multi-instance profiles, 20+ UI languages,
  Ely.by / Yggdrasil login.

## v1.0.0 — 2026-11-25

### Added
- First release: fork of Pojav Glow-Worm with Cliorius branding.

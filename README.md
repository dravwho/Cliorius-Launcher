# Cliorius Launcher

[![Release](https://img.shields.io/github/v/release/dravwho/Cliorius-Launcher)](https://github.com/dravwho/Cliorius-Launcher/releases)
[![License](https://img.shields.io/github/license/dravwho/Cliorius-Launcher)](LICENSE)

**Cliorius Launcher** adalah launcher **Minecraft: Java Edition untuk Android** — fork dari [Pojav Glow-Worm](https://github.com/Vera-Firefly/Pojav-Glow-Worm) (basis ZalithLauncher) dengan UI modern, alat performa & FPS, dan fitur keamanan.

## Unduh

Ambil APK sesuai arsitektur HP Anda di [Releases](https://github.com/dravwho/Cliorius-Launcher/releases):

| File | Untuk perangkat |
|---|---|
| `Cliorius-Launcher-arm64.apk` | HP Android modern (2017+, 64-bit) — **paling umum** |
| `Cliorius-Launcher-arm.apk` | HP lama / 32-bit |
| `Cliorius-Launcher-x86_64.apk` | Emulator / Chromebook / PC Android 64-bit |
| `Cliorius-Launcher-x86.apk` | Emulator 32-bit lama |
| `Cliorius-Launcher-universal-arm.apk` | Satu APK untuk semua HP ARM (paling aman) |

Install: izinkan "Instal aplikasi tidak dikenal" → buka APK → Install. Saat pertama kali main, launcher mengunduh JRE + aset Minecraft otomatis (butuh internet & ruang ±1–2 GB).

## Fitur

- **Menjalankan Minecraft Java** hampir semua versi (rd-132211 s/d snapshot terbaru), Forge / Fabric / NeoForge / Quilt / OptiFine
- **Mod Manager terintegrasi** — cari & unduh mod langsung dari **Modrinth** dari dalam app
- **Performa & FPS**: alokasi RAM kustom, pilihan GC (ZGC/G1GC), batas FPS, **FPS overlay in-game**, **Auto-Tune** (optimasi otomatis sesuai RAM/GPU perangkat)
- **Renderer lengkap**: gl4es (OpenGL ES 2/3), nggl4es, MobileGlues, Mesa/OSMesa (6 varian), Vulkan Zink, virgl, freedreno, panfrost — dengan deteksi GPU otomatis (Adreno dsb.)
- **Multi-instance / profil terpisah** per versi (isolasi folder data)
- **Login multi-metode**: Microsoft, Ely.by / Yggdrasil (authlib-injector), dan offline — dengan catatan keamanan untuk akun Microsoft
- **Keamanan**: kunci PIN/aplikasi, lihat & **logout semua sesi** akun
- **Pilih bahasa UI** (20+ bahasa) di Settings
- Kontrol kustom (tombol layar), gamepad, terminal, share log, benchmark renderer/RAM

## Lisensi & kredit

- Lisensi **GPLv3** (mengikuti Pojav). Lihat [LICENSE](LICENSE).
- Fork dari **Pojav Glow-Worm** oleh Vera-Firefly dan kontributor PojavLauncher.
- Dikembangkan oleh **dravwho** — [github.com/dravwho](https://github.com/dravwho)

## Build dari source

```bash
git clone https://github.com/dravwho/Cliorius-Launcher.git
cd Cliorius-Launcher
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew assembleDebug          # semua ABI
./gradlew -Darch=arm64 assembleDebug   # satu ABI saja
```

Butuh JDK 17, Android SDK (platform 35, build-tools 35, NDK 25.2.9519653). CI GitHub Actions otomatis build semua ABI & membuat release saat tag `v*`.

Lihat juga: [CHANGELOG.md](CHANGELOG.md) · [CONTRIBUTING.md](CONTRIBUTING.md) · [SECURITY.md](SECURITY.md)

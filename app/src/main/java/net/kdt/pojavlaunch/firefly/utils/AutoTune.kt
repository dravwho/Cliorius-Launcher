/*
 * Cliorius Launcher — Auto-Tune performa
 * Membaca RAM perangkat, arsitektur, dan GPU, lalu mengatur pengaturan
 * performa secara otomatis. Semua nilai ditulis ke SharedPreferences yang
 * benar-benar dibaca JREUtils saat launch (bukan gimmick).
 */
package net.kdt.pojavlaunch.firefly.utils

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import net.kdt.pojavlaunch.firefly.Architecture
import com.firefly.utils.PGWTools
import net.kdt.pojavlaunch.firefly.Tools
import net.kdt.pojavlaunch.firefly.prefs.LauncherPreferences

object AutoTune {
    data class Result(
        val ramMb: Int,
        val allocationMb: Int,
        val renderer: String,
        val rendererReason: String,
        val maxFps: Int,
        val gc: Boolean,
        val notes: List<String>
    )

    fun run(context: Context): Result {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalRamMb = (memInfo.totalMem / 1048576L).toInt()
        val is64 = !Architecture.is32BitsDevice()
        val isAdreno = PGWTools.isAdrenoGPU()
        val notes = mutableListOf<String>()

        // 1. RAM: pakai tabel findBestRAMAllocation (sudah aman untuk 32-bit)
        val allocation = computeAllocation(totalRamMb, is64)
        prefs.edit().putInt("allocation", allocation).apply()
        notes.add("RAM dialokasikan ${allocation} MB (dari total ${totalRamMb} MB)")

        // 2. Renderer: pilih yang paling cocok dengan GPU & arsitektur
        val renderer: String
        val reason: String
        when {
            isAdreno && is64 && totalRamMb >= 4096 -> {
                renderer = "vulkan_zink"
                reason = "GPU Adreno 64-bit + RAM cukup: Zink (Vulkan) biasanya FPS paling tinggi"
            }
            is64 -> {
                renderer = "opengles3_nggl4es"
                reason = "gl4es modern (nggl4es): kompatibel luas & cepat untuk GPU non-Adreno"
            }
            else -> {
                renderer = "opengles2"
                reason = "Perangkat 32-bit: gl4es klasik paling stabil & hemat memori"
            }
        }
        prefs.edit().putString("renderer", renderer).apply()
        notes.add("Renderer diatur ke $renderer — $reason")

        // 3. FPS: perangkat lemah dibatasi 60 agar stabil, kuat biarkan tanpa batas
        val maxFps = when {
            totalRamMb < 3072 -> 60
            totalRamMb < 6144 -> 120
            else -> 0
        }
        prefs.edit().putInt("perf_max_fps", maxFps).apply()
        notes.add(if (maxFps == 0) "Batas FPS dimatikan (perangkat kuat)" else "Batas FPS diatur $maxFps")

        // 4. GC modern selalu on (aman untuk semua JRE — dipilih otomatis di JREUtils)
        prefs.edit().putBoolean("perf_gc", true).apply()
        prefs.edit().putBoolean("perf_tiered", true).apply()
        prefs.edit().putBoolean("perf_string_dedup", totalRamMb >= 3072).apply()
        notes.add("GC modern + Tiered Compilation aktif" + if (totalRamMb < 3072) " (String Dedup mati: RAM terbatas)" else "")

        // 5. VSync: matikan saat main agar FPS bebas (kecuali renderer zink yang butuh vsync internal)
        if (renderer != "vulkan_zink") {
            prefs.edit().putBoolean("perf_disable_vsync_game", true).apply()
            notes.add("VSync dimatikan saat bermain (FPS bebas)")
        }

        // Reload preferensi agar nilai baru langsung terbaca saat launch
        LauncherPreferences.loadPreferences(context)
        return Result(totalRamMb, allocation, renderer, reason, maxFps, true, notes)
    }

    /** Sama dengan tabel findBestRAMAllocation bawaan (dibuat publik lokal agar tidak bergantung private). */
    private fun computeAllocation(deviceRam: Int, is64: Boolean): Int {
        if (deviceRam < 1024) return 296
        if (deviceRam < 1536) return 448
        if (deviceRam < 2048) return 656
        if (!is64) return 696
        if (deviceRam < 3064) return 936
        if (deviceRam < 4096) return 1144
        if (deviceRam < 6144) return 1536
        return 2048
    }
}

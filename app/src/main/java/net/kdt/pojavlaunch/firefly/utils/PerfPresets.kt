/*
 * Cliorius Launcher — Performance presets
 * One-tap tuning that writes the real preference keys consumed by JREUtils at launch.
 */
package net.kdt.pojavlaunch.firefly.utils

import android.content.Context
import net.kdt.pojavlaunch.firefly.prefs.LauncherPreferences

object PerfPresets {

    data class Preset(
        val id: String,
        val labelRes: Int,
        val ramMb: Int,
        val renderer: String,
        val maxFps: Int,
        val gc: Boolean,
        val tiered: Boolean,
        val dedup: Boolean,
        val disableVsync: Boolean
    )

    val PVP = Preset(
        id = "pvp",
        labelRes = net.kdt.pojavlaunch.firefly.R.string.cli_preset_pvp,
        ramMb = 1024,
        renderer = "opengles2",
        maxFps = 0,
        gc = false,
        tiered = true,
        dedup = false,
        disableVsync = true
    )

    val SURVIVAL = Preset(
        id = "survival",
        labelRes = net.kdt.pojavlaunch.firefly.R.string.cli_preset_survival,
        ramMb = 2048,
        renderer = "opengles3_nggl4es",
        maxFps = 60,
        gc = true,
        tiered = true,
        dedup = true,
        disableVsync = false
    )

    val BALANCED = Preset(
        id = "balanced",
        labelRes = net.kdt.pojavlaunch.firefly.R.string.cli_preset_balanced,
        ramMb = 1536,
        renderer = "opengles3_nggl4es",
        maxFps = 60,
        gc = true,
        tiered = true,
        dedup = true,
        disableVsync = false
    )

    val ALL = listOf(PVP, SURVIVAL, BALANCED)

    /** Terapkan preset ke SharedPreferences nyata lalu sinkronkan ke LauncherPreferences. */
    fun apply(context: Context, preset: Preset) {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit()
            .putInt("allocation", preset.ramMb)
            .putString("renderer", preset.renderer)
            .putInt("perf_max_fps", preset.maxFps)
            .putBoolean("perf_gc", preset.gc)
            .putBoolean("perf_tiered", preset.tiered)
            .putBoolean("perf_string_dedup", preset.dedup)
            .putBoolean("perf_disable_vsync_game", preset.disableVsync)
            .apply()
        LauncherPreferences.loadPreferences(context)
    }
}

/*
 * Cliorius Launcher — Safe Mode
 * Disables every mod in the active instance at once (rename .jar -> .jar.disabled),
 * so a broken mod can be isolated without deleting anything. Fully reversible.
 */
package net.kdt.pojavlaunch.firefly.utils

import net.kdt.pojavlaunch.firefly.Tools
import net.kdt.pojavlaunch.firefly.prefs.LauncherPreferences
import net.kdt.pojavlaunch.firefly.version.PgwVersionRepository
import net.kdt.pojavlaunch.firefly.version.VersionIsolation
import java.io.File

object SafeMode {

    private fun modsDir(): File {
        val home = File(Tools.DIR_GAME_HOME)
        val current = PgwVersionRepository.current()
        val dir = VersionIsolation.resolveGameDirectory(
            home, null, current?.id, LauncherPreferences.PREF_VERSION_ISOLATION
        )
        return File(dir, "mods")
    }

    /** @return jumlah mod yang dinonaktifkan */
    fun disableAll(): Int {
        val dir = modsDir()
        if (!dir.exists()) return 0
        var n = 0
        dir.listFiles()?.forEach { f ->
            if (f.isFile && f.name.endsWith(".jar")) {
                if (f.renameTo(File(dir, f.name + ".disabled"))) n++
            }
        }
        return n
    }

    /** @return jumlah mod yang diaktifkan kembali */
    fun enableAll(): Int {
        val dir = modsDir()
        if (!dir.exists()) return 0
        var n = 0
        dir.listFiles()?.forEach { f ->
            if (f.isFile && f.name.endsWith(".jar.disabled")) {
                val target = File(dir, f.name.removeSuffix(".disabled"))
                if (f.renameTo(target)) n++
            }
        }
        return n
    }

    fun disabledCount(): Int =
        modsDir().listFiles()?.count { it.isFile && it.name.endsWith(".jar.disabled") } ?: 0
}

/*
 * Cliorius Launcher — Crash Viewer
 * Membaca latestcrash.txt, mendeteksi pola error umum, lalu memberi saran perbaikan
 * yang bisa langsung diterapkan (naikkan RAM / ganti renderer / matikan mod).
 */
package net.kdt.pojavlaunch.firefly.fragments

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.firefly.R
import net.kdt.pojavlaunch.firefly.Tools
import net.kdt.pojavlaunch.firefly.prefs.LauncherPreferences
import java.io.File

class CrashViewerFragment : Fragment(R.layout.fragment_crash_viewer) {
    companion object { const val TAG = "CrashViewerFragment" }

    private data class Diagnosis(
        val title: String,
        val advice: String,
        val fixLabel: String?,
        val fix: (() -> Unit)?
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val content = view.findViewById<TextView>(R.id.crash_content)
        val diagTitle = view.findViewById<TextView>(R.id.crash_diag_title)
        val diagAdvice = view.findViewById<TextView>(R.id.crash_diag_advice)
        val fixButton = view.findViewById<com.kdt.mcgui.MineButton>(R.id.crash_fix)
        val refresh = view.findViewById<View>(R.id.crash_refresh)

        fun load() {
            lifecycleScope.launch {
                val text = withContext(Dispatchers.IO) { readCrash() }
                if (text.isNullOrBlank()) {
                    content.text = getString(R.string.cli_crash_none)
                    diagTitle.text = getString(R.string.cli_crash_none_title)
                    diagAdvice.text = getString(R.string.cli_crash_none_advice)
                    fixButton.visibility = View.GONE
                    return@launch
                }
                content.text = text
                val d = diagnose(text)
                diagTitle.text = d.title
                diagAdvice.text = d.advice
                if (d.fix != null && d.fixLabel != null) {
                    fixButton.visibility = View.VISIBLE
                    fixButton.text = d.fixLabel
                    fixButton.setOnClickListener {
                        d.fix.invoke()
                        Toast.makeText(requireContext(), R.string.cli_crash_fix_applied, Toast.LENGTH_LONG).show()
                    }
                } else {
                    fixButton.visibility = View.GONE
                }
            }
        }

        refresh.setOnClickListener { load() }
        load()
    }

    private fun readCrash(): String? {
        val candidates = listOf(
            File(Tools.DIR_GAME_HOME, "latestcrash.txt"),
            File(Tools.DIR_DATA, "latestcrash.txt")
        )
        return candidates.firstOrNull { it.exists() && it.length() > 0 }?.readText()
    }

    /** Deteksi pola error umum dan beri saran + tombol perbaikan otomatis. */
    private fun diagnose(text: String): Diagnosis {
        val lower = text.lowercase()
        return when {
            lower.contains("outofmemoryerror") || lower.contains("java heap space") -> {
                val current = LauncherPreferences.PREF_RAM_ALLOCATION
                val target = (current + 512).coerceAtMost(4096)
                Diagnosis(
                    getString(R.string.cli_crash_oom_title),
                    getString(R.string.cli_crash_oom_advice, current, target),
                    getString(R.string.cli_crash_oom_fix, target)
                ) {
                    LauncherPreferences.DEFAULT_PREF.edit()
                        .putInt("allocation", target).apply()
                    LauncherPreferences.PREF_RAM_ALLOCATION = target
                }
            }
            lower.contains("gl_invalid") || lower.contains("egl_bad") ||
            lower.contains("failed to load renderer") || lower.contains("libgl4es") -> {
                Diagnosis(
                    getString(R.string.cli_crash_gl_title),
                    getString(R.string.cli_crash_gl_advice),
                    getString(R.string.cli_crash_gl_fix)
                ) {
                    LauncherPreferences.DEFAULT_PREF.edit()
                        .putString("renderer", "opengles2").apply()
                    LauncherPreferences.PREF_RENDERER = "opengles2"
                }
            }
            lower.contains("unsupportedclassversion") || lower.contains("java.lang.noclassdeffound") -> {
                Diagnosis(
                    getString(R.string.cli_crash_java_title),
                    getString(R.string.cli_crash_java_advice),
                    null, null
                )
            }
            lower.contains("mod") && (lower.contains("exception") || lower.contains("crash")) -> {
                Diagnosis(
                    getString(R.string.cli_crash_mod_title),
                    getString(R.string.cli_crash_mod_advice),
                    null, null
                )
            }
            else -> Diagnosis(
                getString(R.string.cli_crash_generic_title),
                getString(R.string.cli_crash_generic_advice),
                null, null
            )
        }
    }
}

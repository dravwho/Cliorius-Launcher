/*
 * Cliorius Launcher — Backup & Restore instance
 * Export the active instance (version dir + mods + config + saves) into a single .zip,
 * and import such a .zip back into the game home. Works across devices.
 */
package net.kdt.pojavlaunch.firefly.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.firefly.R
import net.kdt.pojavlaunch.firefly.Tools
import net.kdt.pojavlaunch.firefly.prefs.LauncherPreferences
import net.kdt.pojavlaunch.firefly.version.PgwVersionRepository
import net.kdt.pojavlaunch.firefly.version.VersionIsolation
import net.kdt.pojavlaunch.firefly.utils.ModpackImporter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRestoreFragment : Fragment(R.layout.fragment_backup_restore) {
    companion object { const val TAG = "BackupRestoreFragment" }

    private lateinit var status: TextView

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> doImport(uri) }
        }
    }

    private val modpackLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> doImportModpack(uri) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        status = view.findViewById(R.id.backup_status)
        view.findViewById<View>(R.id.backup_export).setOnClickListener { doExport() }
        view.findViewById<View>(R.id.backup_import).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
            }
            importLauncher.launch(intent)
        }
        view.findViewById<View>(R.id.backup_import_modpack)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            modpackLauncher.launch(intent)
        }
        updateStatus()
    }

    /**
     * Cliorius v9: import a Modrinth .mrpack into the active instance.
     * The pack's mods are downloaded at their declared relative paths and the
     * overrides folders are copied on top, per the official format spec.
     */
    private fun doImportModpack(uri: Uri) {
        val target = instanceDir()
        Toast.makeText(requireContext(), R.string.cli_modpack_importing, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val cache = File(requireContext().cacheDir, "import.mrpack")
                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        cache.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw java.io.IOException("Cannot read selected file")
                    ModpackImporter.import(cache, target) { p ->
                        // progress is reported on the IO thread; UI toast is best-effort
                    }.also { cache.delete() }
                }
            }
            result.onSuccess { r ->
                Toast.makeText(
                    requireContext(),
                    getString(R.string.cli_modpack_done, r.name, r.filesInstalled, r.overridesCopied),
                    Toast.LENGTH_LONG
                ).show()
            }.onFailure {
                Toast.makeText(requireContext(), R.string.cli_modpack_failed, Toast.LENGTH_LONG).show()
            }
            updateStatus()
        }
    }

    private fun instanceDir(): File {
        val home = File(Tools.DIR_GAME_HOME)
        val current = PgwVersionRepository.current()
        return VersionIsolation.resolveGameDirectory(
            home, null, current?.id, LauncherPreferences.PREF_VERSION_ISOLATION
        )
    }

    private fun updateStatus() {
        val dir = instanceDir()
        val size = runCatching { dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } }.getOrDefault(0L)
        status.text = getString(
            R.string.cli_backup_status,
            dir.name, String.format("%.1f", size / 1048576.0)
        )
    }

    private fun doExport() {
        val dir = instanceDir()
        if (!dir.exists()) {
            Toast.makeText(requireContext(), R.string.cli_backup_no_instance, Toast.LENGTH_SHORT).show()
            return
        }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US).format(Date())
        val outDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Cliorius"
        ).apply { mkdirs() }
        val outFile = File(outDir, "Cliorius-${dir.name}-$stamp.zip")
        Toast.makeText(requireContext(), getString(R.string.cli_backup_exporting, dir.name), Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    ZipOutputStream(outFile.outputStream().buffered()).use { zos ->
                        val base = dir.toPath()
                        dir.walkTopDown().filter { it.isFile }.forEach { f ->
                            val rel = base.relativize(f.toPath()).toString()
                            zos.putNextEntry(ZipEntry(rel))
                            f.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                    true
                }.getOrDefault(false)
            }
            if (ok) {
                Toast.makeText(requireContext(), getString(R.string.cli_backup_done, outFile.name), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), R.string.cli_backup_failed, Toast.LENGTH_LONG).show()
            }
            updateStatus()
        }
    }

    private fun doImport(uri: Uri) {
        val target = instanceDir()
        Toast.makeText(requireContext(), getString(R.string.cli_backup_importing, uri.lastPathSegment ?: "zip"), Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val count = withContext(Dispatchers.IO) {
                runCatching {
                    var n = 0
                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        ZipInputStream(input.buffered()).use { zis ->
                            var entry: ZipEntry? = zis.nextEntry
                            while (entry != null) {
                                val name = entry.name
                                if (!name.contains("..")) {
                                    val dest = File(target, name)
                                    if (entry.isDirectory) {
                                        dest.mkdirs()
                                    } else {
                                        dest.parentFile?.mkdirs()
                                        dest.outputStream().use { zis.copyTo(it) }
                                        n++
                                    }
                                }
                                zis.closeEntry()
                                entry = zis.nextEntry
                            }
                        }
                    }
                    n
                }.getOrDefault(-1)
            }
            if (count >= 0) {
                Toast.makeText(requireContext(), getString(R.string.cli_backup_imported, "$count files"), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), R.string.cli_backup_failed, Toast.LENGTH_LONG).show()
            }
            updateStatus()
        }
    }
}

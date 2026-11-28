/*
 * Cliorius Launcher — World manager
 * Lists the worlds of the active instance (saves/), shows size and last-played
 * time, and lets the user back up a single world, delete it, or share it.
 */
package net.kdt.pojavlaunch.firefly.fragments

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.firefly.R
import net.kdt.pojavlaunch.firefly.Tools
import net.kdt.pojavlaunch.firefly.prefs.LauncherPreferences
import net.kdt.pojavlaunch.firefly.version.PgwVersionRepository
import net.kdt.pojavlaunch.firefly.version.VersionIsolation
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class WorldManagerFragment : Fragment(R.layout.fragment_world_manager) {
    companion object { const val TAG = "WorldManagerFragment" }

    data class World(val dir: File, val name: String, val sizeMb: Double, val lastPlayed: Long)

    private lateinit var adapter: WorldAdapter
    private lateinit var empty: TextView
    private lateinit var status: TextView

    private fun savesDir(): File {
        val home = File(Tools.DIR_GAME_HOME)
        val current = PgwVersionRepository.current()
        val dir = VersionIsolation.resolveGameDirectory(
            home, null, current?.id, LauncherPreferences.PREF_VERSION_ISOLATION
        )
        return File(dir, "saves").apply { mkdirs() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        empty = view.findViewById(R.id.world_empty)
        status = view.findViewById(R.id.world_status)
        val list = view.findViewById<RecyclerView>(R.id.world_list)
        adapter = WorldAdapter(
            onBackup = { w -> backupWorld(w) },
            onDelete = { w -> confirmDelete(w) }
        )
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        view.findViewById<View>(R.id.world_refresh).setOnClickListener { reload() }
        reload()
    }

    private fun reload() {
        lifecycleScope.launch {
            val worlds = withContext(Dispatchers.IO) { scanWorlds() }
            adapter.submit(worlds)
            empty.visibility = if (worlds.isEmpty()) View.VISIBLE else View.GONE
            status.text = getString(R.string.cli_world_status, worlds.size)
        }
    }

    private fun scanWorlds(): List<World> {
        val dir = savesDir()
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return dir.listFiles()?.filter { it.isDirectory }?.map { d ->
            val size = runCatching {
                d.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }.getOrDefault(0L)
            val levelDat = File(d, "level.dat")
            val last = if (levelDat.exists()) levelDat.lastModified() else d.lastModified()
            World(d, d.name, size / 1048576.0, last)
        }?.sortedByDescending { it.lastPlayed } ?: emptyList()
    }

    private fun backupWorld(world: World) {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        val outDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Cliorius/worlds"
        ).apply { mkdirs() }
        val outFile = File(outDir, "${world.name}-$stamp.zip")
        Toast.makeText(requireContext(), getString(R.string.cli_world_backing_up, world.name), Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    ZipOutputStream(outFile.outputStream().buffered()).use { zos ->
                        val base = world.dir.toPath()
                        world.dir.walkTopDown().filter { it.isFile }.forEach { f ->
                            val rel = base.relativize(f.toPath()).toString()
                            zos.putNextEntry(ZipEntry(rel))
                            f.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                    true
                }.getOrDefault(false)
            }
            val msg = if (ok) getString(R.string.cli_world_backup_done, outFile.name)
            else getString(R.string.cli_world_backup_failed)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmDelete(world: World) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.cli_world_delete_title, world.name))
            .setMessage(R.string.cli_world_delete_msg)
            .setPositiveButton(R.string.cli_world_delete) { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) { world.dir.deleteRecursively() }
                    reload()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private inner class WorldAdapter(
        private val onBackup: (World) -> Unit,
        private val onDelete: (World) -> Unit
    ) : RecyclerView.Adapter<WorldAdapter.VH>() {
        private val items = mutableListOf<World>()
        private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        fun submit(list: List<World>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.world_item_name)
            val meta: TextView = v.findViewById(R.id.world_item_meta)
            val backup: View = v.findViewById(R.id.world_item_backup)
            val delete: View = v.findViewById(R.id.world_item_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.view_world_item, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val w = items[position]
            holder.name.text = w.name
            holder.meta.text = getString(
                R.string.cli_world_item_meta,
                String.format(Locale.US, "%.1f", w.sizeMb),
                fmt.format(Date(w.lastPlayed))
            )
            holder.backup.setOnClickListener { onBackup(w) }
            holder.delete.setOnClickListener { onDelete(w) }
        }
    }
}

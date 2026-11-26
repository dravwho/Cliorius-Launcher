/*
 * Cliorius Launcher — Mod Manager terintegrasi
 * - Daftar mod terpasang di instance aktif (diparse ModParser: Fabric/Quilt/Forge)
 * - Toggle mod (disable = rename .jar -> .disabled, dibaca Minecraft)
 * - Hapus mod
 * - Cari & unduh mod dari Modrinth API (katalog publik, mirror resmi)
 */
package net.kdt.pojavlaunch.firefly.fragments

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
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
import net.kdt.pojavlaunch.firefly.version.PgwVersionRepository
import net.kdt.pojavlaunch.firefly.version.VersionIsolation
import net.kdt.pojavlaunch.firefly.version.ModrinthApiCatalog
import net.kdt.pojavlaunch.firefly.version.ModrinthApiVersion
import com.movtery.feature.mod.parser.ModParser
import com.movtery.feature.mod.parser.ModInfo
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ModManagerFragment : Fragment(R.layout.fragment_mod_manager) {
    companion object {
        const val TAG = "ModManagerFragment"
    }

    private lateinit var adapter: ModsAdapter
    private lateinit var catalogAdapter: CatalogAdapter
    private lateinit var list: RecyclerView
    private lateinit var loading: ProgressBar
    private lateinit var empty: TextView
    private lateinit var versionLabel: TextView
    private var searchJob: kotlinx.coroutines.Job? = null

    private fun modsDir(): File {
        val home = File(Tools.DIR_GAME_HOME)
        val current = PgwVersionRepository.current()
        val dir = VersionIsolation.resolveGameDirectory(
            home, null, current?.id,
            net.kdt.pojavlaunch.firefly.prefs.LauncherPreferences.PREF_VERSION_ISOLATION
        )
        return File(dir, "mods").apply { mkdirs() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list = view.findViewById(R.id.mods_list)
        loading = view.findViewById(R.id.mods_loading)
        empty = view.findViewById(R.id.mods_empty)
        versionLabel = view.findViewById(R.id.mods_version_label)
        adapter = ModsAdapter(
            onToggle = { info, enabled -> toggleMod(info, enabled) },
            onDelete = { info -> confirmDelete(info) }
        )
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        catalogAdapter = CatalogAdapter { entry -> installFromCatalog(entry) }

        view.findViewById<ImageButton>(R.id.mods_refresh).setOnClickListener { reload() }
        view.findViewById<com.kdt.mcgui.MineButton>(R.id.mods_browse).setOnClickListener { browseOnline() }
        view.findViewById<EditText>(R.id.mods_search).addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(t: android.text.Editable?) { filter(t?.toString()) }
            override fun beforeTextChanged(c: CharSequence?, s: Int, b: Int, a: Int) {}
            override fun onTextChanged(c: CharSequence?, s: Int, b: Int, a: Int) {}
        })
        reload()
    }

    private fun reload() {
        loading.visibility = View.VISIBLE
        empty.visibility = View.GONE
        lifecycleScope.launch {
            val current = withContext(Dispatchers.IO) { PgwVersionRepository.current() }
            versionLabel.text = getString(R.string.cli_mods_version_label, current?.id ?: "—")
            val mods = parseModsBlocking()
            loading.visibility = View.GONE
            adapter.submit(mods)
            empty.visibility = if (mods.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun filter(query: String?) {
        val q = query?.trim() ?: ""
        lifecycleScope.launch(Dispatchers.IO) {
            val mods = parseModsBlocking()
            val filtered = if (q.isEmpty()) mods else mods.filter {
                it.name.contains(q, true) || it.id.contains(q, true)
            }
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                adapter.submit(filtered)
                empty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun toggleMod(info: ModInfo, enabled: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            val file = info.file
            val target = if (enabled) {
                File(file.parentFile, file.name.removeSuffix(".disabled"))
            } else {
                File(file.parentFile, file.name + ".disabled")
            }
            file.renameTo(target)
        }
    }

    private fun confirmDelete(info: ModInfo) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.cli_mods_delete_title, info.name.ifBlank { info.id }))
            .setMessage(R.string.cli_mods_delete_msg)
            .setPositiveButton(R.string.cli_mods_delete) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    info.file.delete()
                    withContext(kotlinx.coroutines.Dispatchers.Main) { reload() }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ===== Katalog online (Modrinth) =====
    private fun browseOnline() {
        val dialogView = layoutInflater.inflate(R.layout.fragment_mod_catalog, null, false)
        val dialogList = dialogView.findViewById<RecyclerView>(R.id.catalog_list)
        val dialogSearch = dialogView.findViewById<EditText>(R.id.catalog_search)
        val dialogLoading = dialogView.findViewById<ProgressBar>(R.id.catalog_loading)
        val dialogEmpty = dialogView.findViewById<TextView>(R.id.catalog_empty)
        dialogList.layoutManager = LinearLayoutManager(requireContext())
        dialogList.adapter = catalogAdapter

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.cli_mods_browse_title)
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        fun search(query: String) {
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                dialogLoading.visibility = View.VISIBLE
                dialogEmpty.visibility = View.GONE
                val results = withContext(Dispatchers.IO) { runCatching { searchModrinth(query) }.getOrElse { emptyList() } }
                dialogLoading.visibility = View.GONE
                catalogAdapter.submit(results)
                dialogEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
            }
        }
        dialogSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(t: android.text.Editable?) { search(t?.toString() ?: "") }
            override fun beforeTextChanged(c: CharSequence?, s: Int, b: Int, a: Int) {}
            override fun onTextChanged(c: CharSequence?, s: Int, b: Int, a: Int) {}
        })
        dialog.setOnShowListener { search("") }
        dialog.show()
    }

    private fun searchModrinth(query: String): List<CatalogEntryUi> {
        val mcVersion = PgwVersionRepository.current()?.id?.let { id ->
            // Ambil versi MC dasar dari id (mis. "1.21.4-Fabric" -> "1.21.4")
            Regex("^\\d+\\.\\d+(\\.\\d+)?").find(id)?.value
        } ?: ""
        val url = "https://api.modrinth.com/v2/search?limit=40" +
            "&index=relevance" +
            (if (query.isNotBlank()) "&query=" + java.net.URLEncoder.encode(query, "UTF-8") else "") +
            (if (mcVersion.isNotBlank()) "&facets=[[\"versions:$mcVersion\"],[\"project_type:mod\"]]" else "")
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "ClioriusLauncher/1.0")
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        val body = conn.inputStream.bufferedReader().readText()
        val root = com.google.gson.JsonParser.parseString(body).asJsonObject
        val hits = root.getAsJsonArray("hits")
        return hits.mapNotNull { el ->
            val o = el.asJsonObject
            val slug = o.get("slug")?.asString ?: return@mapNotNull null
            CatalogEntryUi(
                slug = slug,
                title = o.get("title")?.asString ?: slug,
                description = o.get("description")?.asString ?: "",
                downloads = o.get("downloads")?.asLong ?: 0L,
                iconUrl = o.get("icon_url")?.takeIf { !it.isJsonNull }?.asString
            )
        }
    }

    private fun installFromCatalog(entry: CatalogEntryUi) {
        Toast.makeText(requireContext(), getString(R.string.cli_mods_downloading, entry.title), Toast.LENGTH_SHORT).show()
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching { downloadLatestMod(entry.slug) }
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                result.onSuccess { file ->
                    Toast.makeText(requireContext(),
                        getString(R.string.cli_mods_installed, file.name), Toast.LENGTH_LONG).show()
                    reload()
                }.onFailure { error ->
                    Tools.showError(requireContext(), error)
                }
            }
        }
    }

    private fun downloadLatestMod(slug: String): File {
        val mcVersion = PgwVersionRepository.current()?.id?.let { id ->
            Regex("^\\d+\\.\\d+(\\.\\d+)?").find(id)?.value
        } ?: ""
        val conn = URL("https://api.modrinth.com/v2/project/$slug/version").openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "ClioriusLauncher/1.0")
        val versions = com.google.gson.JsonParser.parseString(conn.inputStream.bufferedReader().readText()).asJsonArray
        val chosen = versions.firstOrNull { el ->
            val o = el.asJsonObject
            val gameVersions = o.get("game_versions")?.asJsonArray?.map { it.asString }.orEmpty()
            mcVersion.isBlank() || mcVersion in gameVersions
        } ?: versions.firstOrNull() ?: throw java.io.IOException("No release found for $slug")
        val files = chosen.asJsonObject.getAsJsonArray("files")
        val file = files.firstOrNull { it.asJsonObject.get("primary")?.asBoolean == true }?.asJsonObject
            ?: files.firstOrNull()?.asJsonObject ?: throw java.io.IOException("No file in release")
        val url = file.get("url").asString
        val fileName = file.get("filename").asString
        val dest = File(modsDir(), fileName)
        Tools.downloadFile(url, dest.absolutePath)
        return dest
    }


    /** Parse mod secara blocking dengan listener sinkron (ModParser memakai Task async internal). */
    private suspend fun parseModsBlocking(): List<ModInfo> = withContext(Dispatchers.IO) {
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            ModParser().parseAllMods(modsDir(), object : com.movtery.feature.mod.parser.ModParserListener {
                override fun onProgress(recentlyParsedModInfo: ModInfo, totalFileCount: Int) {}
                override fun onParseEnded(modInfoList: List<ModInfo>) {
                    if (cont.isActive) cont.resume(modInfoList) {}
                }
            })
        }
    }

    data class CatalogEntryUi(
        val slug: String,
        val title: String,
        val description: String,
        val downloads: Long,
        val iconUrl: String?
    )
}

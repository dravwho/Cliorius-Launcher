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
import android.widget.ImageView
import android.graphics.BitmapFactory
import java.util.concurrent.ConcurrentHashMap
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
import net.kdt.pojavlaunch.firefly.utils.ModHealth
import com.movtery.feature.mod.parser.ModParser
import com.movtery.feature.mod.parser.ModInfo
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ModManagerFragment : Fragment(R.layout.fragment_mod_manager) {
    companion object {
        const val TAG = "ModManagerFragment"
        // In-memory icon cache: slug -> bitmap (Modrinth CDN icons)
        val iconCache = ConcurrentHashMap<String, android.graphics.Bitmap?>()
    }

    private lateinit var adapter: ModsAdapter
    private lateinit var catalogAdapter: CatalogAdapter
    private lateinit var list: RecyclerView
    private lateinit var loading: ProgressBar
    private lateinit var empty: TextView
    private lateinit var versionLabel: TextView
    private var searchJob: kotlinx.coroutines.Job? = null

    /** Tipe konten aktif: mod / shader / resourcepack / datapack */
    private var contentType: String = "mod"

    private fun contentFolder(): String = when (contentType) {
        "shader" -> "shaderpacks"
        "resourcepack" -> "resourcepacks"
        "datapack" -> "datapacks"
        else -> "mods"
    }

    private fun modsDir(): File {
        val home = File(Tools.DIR_GAME_HOME)
        val current = PgwVersionRepository.current()
        val dir = VersionIsolation.resolveGameDirectory(
            home, null, current?.id,
            net.kdt.pojavlaunch.firefly.prefs.LauncherPreferences.PREF_VERSION_ISOLATION
        )
        return File(dir, contentFolder()).apply { mkdirs() }
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

        val tabs = mapOf(
            "mod" to view.findViewById<TextView>(R.id.tab_mod),
            "shader" to view.findViewById<TextView>(R.id.tab_shader),
            "resourcepack" to view.findViewById<TextView>(R.id.tab_resourcepack),
            "datapack" to view.findViewById<TextView>(R.id.tab_datapack)
        )
        tabs.forEach { (type, tv) ->
            tv.setOnClickListener {
                if (contentType == type) return@setOnClickListener
                contentType = type
                tabs.forEach { (t, v) ->
                    val active = t == type
                    v.setBackgroundResource(if (active) R.drawable.cli_tab_active else R.drawable.cli_tab_inactive)
                    v.setTextColor(if (active) requireContext().getColor(R.color.accent_on_accent)
                                   else requireContext().getColor(R.color.secondary_text))
                    v.setTypeface(null, if (active) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                }
                reload()
            }
        }

        view.findViewById<ImageButton>(R.id.mods_refresh).setOnClickListener { reload() }
        view.findViewById<com.kdt.mcgui.MineButton>(R.id.mods_browse).setOnClickListener { browseOnline() }
        view.findViewById<EditText>(R.id.mods_search).addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(t: android.text.Editable?) { filter(t?.toString()) }
            override fun beforeTextChanged(c: CharSequence?, s: Int, b: Int, a: Int) {}
            override fun onTextChanged(c: CharSequence?, s: Int, b: Int, a: Int) {}
        })
        view.findViewById<View>(R.id.mods_check_updates)?.setOnClickListener { checkUpdates() }
        view.findViewById<View>(R.id.mods_health)?.setOnClickListener { showHealth() }
        reload()
    }

    /** Cliorius v9: resolve every installed jar against Modrinth and offer updates. */
    private fun checkUpdates() {
        val ctx = requireContext()
        Toast.makeText(ctx, R.string.cli_mods_check_updates, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val mcVersion = currentMinecraftVersion()
            val mods = withContext(Dispatchers.IO) { parseModsBlocking() }
            val updates = withContext(Dispatchers.IO) {
                mods.mapNotNull { mod -> runCatching { ModHealth.checkUpdate(mod, mcVersion) }.getOrNull() }
            }
            if (updates.isEmpty()) {
                Toast.makeText(ctx, R.string.cli_mods_no_updates, Toast.LENGTH_LONG).show()
                return@launch
            }
            val labels = updates.map { "${it.modName}: ${it.installedVersion} -> ${it.latestVersion}" }
            AlertDialog.Builder(ctx)
                .setTitle(getString(R.string.cli_mods_updates_found, updates.size))
                .setItems(labels.toTypedArray()) { _, _ -> }
                .setPositiveButton(R.string.cli_mods_update_all) { _, _ -> applyUpdates(updates) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun applyUpdates(updates: List<ModHealth.UpdateInfo>) {
        val ctx = requireContext()
        lifecycleScope.launch {
            var done = 0
            withContext(Dispatchers.IO) {
                updates.forEach { u ->
                    runCatching {
                        val dest = File(modsDir(), u.fileName)
                        Tools.downloadFile(u.downloadUrl, dest.absolutePath)
                        done++
                    }
                }
            }
            Toast.makeText(ctx, getString(R.string.cli_mods_updated, "$done"), Toast.LENGTH_LONG).show()
            reload()
        }
    }

    /** Cliorius v9: duplicate ids, wrong loader, missing dependencies. */
    private fun showHealth() {
        val ctx = requireContext()
        lifecycleScope.launch {
            val mods = withContext(Dispatchers.IO) { parseModsBlocking() }
            val loader = detectLoader()
            val conflicts = withContext(Dispatchers.IO) { ModHealth.detectConflicts(mods, loader) }
            val title = if (conflicts.isEmpty())
                getString(R.string.cli_mods_no_conflicts)
            else getString(R.string.cli_mods_conflicts, conflicts.size)
            val body = if (conflicts.isEmpty()) "" else conflicts.joinToString("\n\n") {
                val tag = if (it.severity == ModHealth.Severity.ERROR) "[!]" else "[~]"
                "$tag ${it.title}\n${it.detail}"
            }
            AlertDialog.Builder(ctx)
                .setTitle(title)
                .setMessage(body.ifBlank { getString(R.string.cli_mods_no_conflicts) })
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun currentMinecraftVersion(): String =
        PgwVersionRepository.current()?.id?.let { id ->
            Regex("^\\d+\\.\\d+(\\.\\d+)?").find(id)?.value
        } ?: ""

    private fun detectLoader(): String {
        val id = PgwVersionRepository.current()?.id?.lowercase() ?: return "vanilla"
        return when {
            id.contains("quilt") -> "quilt"
            id.contains("fabric") -> "fabric"
            id.contains("neoforge") -> "neoforge"
            id.contains("forge") -> "forge"
            else -> "vanilla"
        }
    }

    private fun reload() {
        loading.visibility = View.VISIBLE
        empty.visibility = View.GONE
        lifecycleScope.launch {
            val current = withContext(Dispatchers.IO) { PgwVersionRepository.current() }
            versionLabel.text = getString(R.string.cli_mods_version_label, current?.id ?: "—")
            val mods = if (contentType == "mod") parseModsBlocking() else listFilesAsModInfo()
            loading.visibility = View.GONE
            adapter.submit(mods)
            empty.visibility = if (mods.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun filter(query: String?) {
        val q = query?.trim() ?: ""
        lifecycleScope.launch(Dispatchers.IO) {
            val mods = if (contentType == "mod") parseModsBlocking() else listFilesAsModInfo()
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
                if (file.name.endsWith(".disabled", ignoreCase = true)) file
                else File(file.parentFile, file.name + ".disabled")
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
            (if (mcVersion.isNotBlank()) "&facets=[[\"versions:$mcVersion\"],[\"project_type:$contentType\"]]"
             else "&facets=[[\"project_type:$contentType\"]]")
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
        // Cliorius: no confirmation dialog — click "Unduh" = download immediately
        Toast.makeText(requireContext(), getString(R.string.cli_mods_downloading, entry.title), Toast.LENGTH_SHORT).show()
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching { downloadWithDependencies(entry.slug) }
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                result.onSuccess { files ->
                    val main = files.firstOrNull()?.name ?: entry.title
                    val extra = files.size - 1
                    val msg = if (extra > 0)
                        getString(R.string.cli_mods_installed_deps, main, extra)
                    else getString(R.string.cli_mods_installed, main)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    reload()
                }.onFailure { error ->
                    Tools.showError(requireContext(), error)
                }
            }
        }
    }

    /**
     * Cliorius v8: unduh mod + seluruh dependensi wajibnya secara rekursif.
     * Modrinth menyediakan field `dependencies` berisi project_id yang dibutuhkan.
     * Tanpa ini banyak mod gagal jalan (Fabric API, Cloth Config, dsb. tidak terpasang).
     */
    private fun downloadWithDependencies(rootSlug: String): List<File> {
        val installed = mutableListOf<File>()
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(rootSlug)
        while (queue.isNotEmpty()) {
            val slug = queue.removeFirst()
            if (!visited.add(slug)) continue
            val result = downloadLatestMod(slug) ?: continue
            installed.add(result.file)
            result.dependencyProjectIds.forEach { depId ->
                val depSlug = projectIdToSlug(depId) ?: return@forEach
                if (depSlug !in visited) queue.add(depSlug)
            }
        }
        if (installed.isEmpty()) throw java.io.IOException("No release found for $rootSlug")
        return installed
    }

    private data class DownloadResult(val file: File, val dependencyProjectIds: List<String>)

    /** Cache project_id -> slug agar tidak query berulang. */
    private val projectSlugCache = ConcurrentHashMap<String, String>()

    private fun projectIdToSlug(projectId: String): String? {
        projectSlugCache[projectId]?.let { return it }
        return runCatching {
            val conn = URL("https://api.modrinth.com/v2/project/$projectId").openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "ClioriusLauncher/1.0")
            conn.connectTimeout = 8000; conn.readTimeout = 10000
            val obj = com.google.gson.JsonParser.parseString(
                conn.inputStream.bufferedReader().readText()
            ).asJsonObject
            val slug = obj.get("slug")?.asString
            if (slug != null) projectSlugCache[projectId] = slug
            slug
        }.getOrNull()
    }

    private fun downloadLatestMod(slug: String): DownloadResult? {
        val mcVersion = PgwVersionRepository.current()?.id?.let { id ->
            Regex("^\\d+\\.\\d+(\\.\\d+)?").find(id)?.value
        } ?: ""
        val conn = URL("https://api.modrinth.com/v2/project/$slug/version").openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "ClioriusLauncher/1.0")
        conn.connectTimeout = 10000; conn.readTimeout = 15000
        val versions = com.google.gson.JsonParser.parseString(conn.inputStream.bufferedReader().readText()).asJsonArray
        val chosen = versions.firstOrNull { el ->
            val o = el.asJsonObject
            val gameVersions = o.get("game_versions")?.asJsonArray?.map { it.asString }.orEmpty()
            mcVersion.isBlank() || mcVersion in gameVersions
        } ?: versions.firstOrNull() ?: return null
        val chosenObj = chosen.asJsonObject
        val files = chosenObj.getAsJsonArray("files")
        val file = files.firstOrNull { it.asJsonObject.get("primary")?.asBoolean == true }?.asJsonObject
            ?: files.firstOrNull()?.asJsonObject ?: return null
        val url = file.get("url").asString
        val fileName = file.get("filename").asString
        val dest = File(modsDir(), fileName)
        if (!dest.exists()) Tools.downloadFile(url, dest.absolutePath)

        // Kumpulkan dependensi wajib (dependency_type == "required")
        val deps = mutableListOf<String>()
        chosenObj.getAsJsonArray("dependencies")?.forEach { d ->
            val o = d.asJsonObject
            val type = o.get("dependency_type")?.asString
            val pid = o.get("project_id")?.asString
            if (type == "required" && pid != null) deps.add(pid)
        }
        return DownloadResult(dest, deps)
    }


    /** Fetch a mod icon bitmap from Modrinth CDN (cached). Null = no icon. */
    fun loadIcon(slug: String, onLoaded: (android.graphics.Bitmap?) -> Unit) {
        iconCache[slug]?.let { onLoaded(it); return }
        lifecycleScope.launch(Dispatchers.IO) {
            val bmp = runCatching {
                val entry = searchModrinth(slug).firstOrNull()
                val url = entry?.iconUrl ?: return@runCatching null
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "ClioriusLauncher/1.0")
                conn.connectTimeout = 8000; conn.readTimeout = 8000
                conn.inputStream.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
            iconCache[slug] = bmp
            withContext(Dispatchers.Main) { onLoaded(bmp) }
        }
    }

    /** Untuk shader/resourcepack/datapack: daftar file langsung tanpa parser mod. */
    private fun listFilesAsModInfo(): List<ModInfo> {
        val dir = modsDir()
        val files = dir.listFiles()?.filter { it.isFile } ?: emptyList()
        return files.map { f ->
            val display = f.name.removeSuffix(".disabled")
            ModInfo(display, "", display.substringBeforeLast('.'), "", emptyArray()).apply {
                this.file = f
            }
        }.sortedBy { it.name.lowercase() }
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

/*
 * Cliorius Launcher — mod health: update checker + conflict detection
 *
 * Update checker: Modrinth exposes /v2/version_file/{sha1} which resolves an
 * installed jar back to its project. Comparing that project's newest compatible
 * version against the installed one tells us whether an update exists.
 *
 * Conflict detection: catches the three failure modes that actually break a
 * launch — duplicate mod ids, mods built for a different loader, and mods whose
 * required dependencies are missing.
 */
package net.kdt.pojavlaunch.firefly.utils

import com.google.gson.JsonParser
import com.movtery.feature.mod.parser.ModInfo
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ModHealth {

    private const val API = "https://api.modrinth.com/v2"
    private const val UA = "ClioriusLauncher/9.0"

    // ---------- Update checker ----------

    data class UpdateInfo(
        val modName: String,
        val installedVersion: String,
        val latestVersion: String,
        val downloadUrl: String,
        val fileName: String
    )

    /**
     * Resolves an installed jar to its Modrinth project via SHA-1 and returns an
     * [UpdateInfo] when a newer compatible version exists, otherwise null.
     */
    fun checkUpdate(mod: ModInfo, minecraftVersion: String): UpdateInfo? {
        val file = mod.file ?: return null
        val sha1 = ModpackImporter.sha1Of(file) ?: return null
        val versionJson = getJson("$API/version_file/$sha1?algorithm=sha1") ?: return null
        val versionObj = runCatching { versionJson.asJsonObject }.getOrNull() ?: return null
        val projectId = versionObj.get("project_id")?.takeIf { !it.isJsonNull }?.asString ?: return null
        val installedVersion = versionObj.get("version_number")
            ?.takeIf { !it.isJsonNull }?.asString ?: mod.version

        val listJson = getJson("$API/project/$projectId/version") ?: return null
        val versions = runCatching { listJson.asJsonArray }.getOrNull() ?: return null

        val compatible = versions.mapNotNull { el ->
            runCatching { el.asJsonObject }.getOrNull()
        }.filter { obj ->
            val gameVersions = obj.getAsJsonArray("game_versions")
                ?.mapNotNull { runCatching { it.asString }.getOrNull() }.orEmpty()
            minecraftVersion.isBlank() || minecraftVersion in gameVersions
        }
        val newest = compatible.firstOrNull() ?: return null
        val newestVersion = newest.get("version_number")
            ?.takeIf { !it.isJsonNull }?.asString ?: return null
        if (newestVersion == installedVersion) return null

        val files = newest.getAsJsonArray("files") ?: return null
        val primary = files.mapNotNull { runCatching { it.asJsonObject }.getOrNull() }
            .firstOrNull { it.get("primary")?.asBoolean == true }
            ?: files.mapNotNull { runCatching { it.asJsonObject }.getOrNull() }.firstOrNull()
            ?: return null
        val url = primary.get("url")?.takeIf { !it.isJsonNull }?.asString ?: return null
        val fileName = primary.get("filename")?.takeIf { !it.isJsonNull }?.asString ?: return null

        return UpdateInfo(
            modName = mod.name.ifBlank { mod.id },
            installedVersion = installedVersion,
            latestVersion = newestVersion,
            downloadUrl = url,
            fileName = fileName
        )
    }

    // ---------- Conflict detection ----------

    enum class Severity { ERROR, WARNING }

    data class Conflict(
        val severity: Severity,
        val title: String,
        val detail: String
    )

    /**
     * @param mods parsed installed mods
     * @param instanceLoader "fabric" | "quilt" | "forge" | "neoforge" | "vanilla"
     */
    fun detectConflicts(mods: List<ModInfo>, instanceLoader: String): List<Conflict> {
        val out = mutableListOf<Conflict>()

        // 1. Duplicate mod ids (two versions of the same mod installed).
        mods.groupBy { it.id.lowercase() }
            .filter { it.key.isNotBlank() && it.value.size > 1 }
            .forEach { (id, dupes) ->
                out += Conflict(
                    Severity.ERROR,
                    "Duplicate mod: $id",
                    "${dupes.size} files provide the same mod id — Minecraft will fail to load. " +
                        "Keep one: " + dupes.joinToString(", ") { it.file?.name ?: "?" }
                )
            }

        // 2. Mods built for a different loader.
        if (instanceLoader != "vanilla") {
            mods.forEach { mod ->
                val name = mod.file?.name?.lowercase() ?: return@forEach
                val looksForge = name.contains("forge") && !name.contains("fabric")
                val looksFabric = name.contains("fabric")
                if (instanceLoader.startsWith("fabric") || instanceLoader == "quilt") {
                    if (looksForge) {
                        out += Conflict(
                            Severity.ERROR,
                            "Wrong loader: ${mod.name.ifBlank { mod.id }}",
                            "This looks like a Forge mod but the instance uses $instanceLoader."
                        )
                    }
                } else if (instanceLoader == "forge" || instanceLoader == "neoforge") {
                    if (looksFabric) {
                        out += Conflict(
                            Severity.ERROR,
                            "Wrong loader: ${mod.name.ifBlank { mod.id }}",
                            "This looks like a Fabric mod but the instance uses $instanceLoader."
                        )
                    }
                }
            }
        }

        // 3. Missing required dependencies (Fabric API is the classic case).
        val ids = mods.map { it.id.lowercase() }.toSet()
        val needsFabricApi = mods.any { mod ->
            val n = mod.file?.name?.lowercase() ?: ""
            n.contains("sodium") || n.contains("iris") || n.contains("indium") ||
                n.contains("lithium") || n.contains("reeses")
        }
        if (needsFabricApi && "fabric-api".let { it !in ids } && "fabric" !in ids) {
            out += Conflict(
                Severity.WARNING,
                "Fabric API may be missing",
                "Installed mods usually require Fabric API. Install it from the Mod Manager."
            )
        }

        return out
    }

    private fun getJson(url: String) = runCatching {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", UA)
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        if (conn.responseCode != 200) return@runCatching null
        JsonParser.parseString(conn.inputStream.bufferedReader().readText())
    }.getOrNull()
}

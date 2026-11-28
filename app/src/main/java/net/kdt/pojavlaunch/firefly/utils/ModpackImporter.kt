/*
 * Cliorius Launcher — Modrinth .mrpack importer
 *
 * Implements the official Modrinth modpack format:
 *   https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack
 *
 * A .mrpack is a ZIP containing modrinth.index.json at the root plus optional
 * overrides/ and client-overrides/ folders. Every entry in `files` is downloaded
 * into the instance directory at its relative `path`, then overrides are copied.
 *
 * Security: `path` is rejected when it contains "..", starts with "/" or "\",
 * or starts with a drive letter — as required by the specification.
 */
package net.kdt.pojavlaunch.firefly.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipFile

object ModpackImporter {

    data class Result(
        val name: String,
        val minecraftVersion: String,
        val loader: String,
        val filesInstalled: Int,
        val filesTotal: Int,
        val overridesCopied: Int
    )

    data class Progress(val current: Int, val total: Int, val label: String)

    /** Rejects absolute paths, drive letters and any ".." segment. */
    fun isSafePath(path: String): Boolean {
        if (path.isBlank()) return false
        if (path.startsWith("/") || path.startsWith("\\")) return false
        if (path.length >= 2 && path[1] == ':') return false
        return path.split('/', '\\').none { it == ".." }
    }

    fun import(
        mrpack: File,
        instanceDir: File,
        onProgress: (Progress) -> Unit = {}
    ): Result {
        if (!instanceDir.exists()) instanceDir.mkdirs()
        ZipFile(mrpack).use { zip ->
            val indexEntry = zip.getEntry("modrinth.index.json")
                ?: throw IOException("modrinth.index.json not found — not a valid .mrpack")
            val indexText = zip.getInputStream(indexEntry)
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JsonParser.parseString(indexText).asJsonObject

            val name = root.get("name")?.takeIf { !it.isJsonNull }?.asString
                ?: mrpack.nameWithoutExtension
            val deps: JsonObject? = root.getAsJsonObject("dependencies")
            val mcVersion = deps?.get("minecraft")?.takeIf { !it.isJsonNull }?.asString ?: ""
            val loader = when {
                deps?.has("fabric-loader") == true -> "fabric"
                deps?.has("quilt-loader") == true -> "quilt"
                deps?.has("neoforge") == true -> "neoforge"
                deps?.has("forge") == true -> "forge"
                else -> "vanilla"
            }

            val files: JsonArray = root.getAsJsonArray("files") ?: JsonArray()
            val total = files.size()
            var installed = 0

            files.forEachIndexed { index, element ->
                val obj = element.asJsonObject
                val path = obj.get("path")?.takeIf { !it.isJsonNull }?.asString
                    ?: return@forEachIndexed
                if (!isSafePath(path)) return@forEachIndexed

                // Skip files that are explicitly not for the client.
                val env = obj.getAsJsonObject("env")
                if (env?.get("client")?.takeIf { !it.isJsonNull }?.asString == "unsupported") {
                    return@forEachIndexed
                }

                val dest = File(instanceDir, path)
                val expectedSha1 = obj.getAsJsonObject("hashes")
                    ?.get("sha1")?.takeIf { !it.isJsonNull }?.asString

                // Already present and matching -> nothing to do.
                if (dest.isFile && expectedSha1 != null &&
                    sha1Of(dest)?.equals(expectedSha1, ignoreCase = true) == true
                ) {
                    installed++
                    onProgress(Progress(index + 1, total, path))
                    return@forEachIndexed
                }

                onProgress(Progress(index + 1, total, path))
                val downloads = obj.getAsJsonArray("downloads") ?: JsonArray()
                var ok = false
                for (d in downloads) {
                    val url = runCatching { d.asString }.getOrNull() ?: continue
                    if (runCatching { downloadTo(url, dest) }.isSuccess) {
                        ok = true
                        break
                    }
                }
                if (ok) installed++
            }

            var overridesCopied = 0
            for (folder in listOf("overrides", "client-overrides")) {
                overridesCopied += copyFolder(zip, folder, instanceDir)
            }

            return Result(name, mcVersion, loader, installed, total, overridesCopied)
        }
    }

    private fun copyFolder(zip: ZipFile, folder: String, target: File): Int {
        var copied = 0
        val prefix = "$folder/"
        for (entry in zip.entries()) {
            if (!entry.name.startsWith(prefix) || entry.isDirectory) continue
            val rel = entry.name.removePrefix(prefix)
            if (!isSafePath(rel)) continue
            val dest = File(target, rel)
            dest.parentFile?.mkdirs()
            runCatching {
                zip.getInputStream(entry).use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                copied++
            }
        }
        return copied
    }

    private fun downloadTo(url: String, dest: File): Boolean {
        var current = url
        var hops = 0
        while (hops < 5) {
            val conn = URL(current).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "ClioriusLauncher/9.0")
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = false
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location") ?: return false
                current = if (location.startsWith("http")) location
                else URL(URL(current), location).toString()
                conn.disconnect()
                hops++
                continue
            }
            if (code != 200) {
                conn.disconnect()
                return false
            }
            dest.parentFile?.mkdirs()
            conn.inputStream.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            conn.disconnect()
            return true
        }
        return false
    }

    fun sha1Of(file: File): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()
}

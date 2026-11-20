package com.japaneixxx.freya.data

import android.content.Context
import androidx.core.content.FileProvider
import com.japaneixxx.freya.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class UpdateRelease(
    val version: String,
    val downloadUrl: String,
    val fileName: String,
    val beta: Boolean = false,
    /** SHA-256 esperado do APK (hex), quando a Release informa; null = sem como verificar. */
    val sha256: String? = null,
    /** Arquivo "<apk>.sha256" da Release, usado quando o GitHub não informa o digest do asset. */
    val sha256Url: String? = null
)

object Updater {
    private const val latestReleaseUrl =
        "https://api.github.com/repos/Japaneixxx/FreyaVault/releases/latest"
    private const val releasesUrl =
        "https://api.github.com/repos/Japaneixxx/FreyaVault/releases?per_page=30"
    private val client = OkHttpClient()

    fun isBeta(version: String): Boolean = Versions.isBeta(version)

    /**
     * Verifica se há versão nova. Automático (`includeBeta = false`) usa só a Release estável mais recente;
     * a verificação manual (`includeBeta = true`) também considera as versões beta.
     */
    suspend fun check(includeBeta: Boolean = false): UpdateRelease? = withContext(Dispatchers.IO) {
        if (includeBeta) checkIncludingBeta() else checkStable()
    }

    private fun get(url: String): okhttp3.Response = client.newCall(
        Request.Builder().url(url).header("Accept", "application/vnd.github+json").build()
    ).execute()

    // /releases/latest ignora pré-lançamentos, então só traz versões estáveis.
    private fun checkStable(): UpdateRelease? = get(latestReleaseUrl).use { response ->
        if (response.code == 404) return null // repositório ainda sem Release publicada
        if (!response.isSuccessful) error("GitHub retornou HTTP ${response.code}")
        parseRelease(JSONObject(response.body?.string().orEmpty()))
    }

    private fun checkIncludingBeta(): UpdateRelease? = get(releasesUrl).use { response ->
        if (response.code == 404) return null
        if (!response.isSuccessful) error("GitHub retornou HTTP ${response.code}")
        val list = JSONArray(response.body?.string().orEmpty())
        (0 until list.length())
            .map { list.getJSONObject(it) }
            .filter { !it.optBoolean("draft") }
            .mapNotNull { parseRelease(it) }
            .fold(null as UpdateRelease?) { best, r ->
                if (best == null || isNewer(r.version, best.version)) r else best
            }
    }

    /** Devolve a Release só se ela tiver APK e for mais nova que a versão instalada. */
    private fun parseRelease(json: JSONObject): UpdateRelease? {
        val version = json.optString("tag_name").removePrefix("v")
        val apk = json.optJSONArray("assets")?.let { assets ->
            (0 until assets.length())
                .map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
        } ?: return null
        val url = apk.optString("browser_download_url")
        val name = apk.optString("name")
        if (version.isBlank() || url.isBlank() || name.isBlank()) return null
        if (!isNewer(version, BuildConfig.VERSION_NAME)) return null
        // O GitHub devolve "digest": "sha256:<hex>" nos assets; senão procura o arquivo .sha256 publicado junto.
        val digest = apk.optString("digest").removePrefix("sha256:").takeIf { it.length == 64 }?.lowercase()
        val shaUrl = json.optJSONArray("assets")?.let { assets ->
            (0 until assets.length()).map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name").equals("$name.sha256", ignoreCase = true) }
                ?.optString("browser_download_url")?.takeIf { it.isNotBlank() }
        }
        return UpdateRelease(version, url, name, beta = json.optBoolean("prerelease") || isBeta(version),
            sha256 = digest, sha256Url = shaUrl)
    }

    /** Baixa o APK, confere o SHA-256 (quando a Release informa) e apaga APKs antigos do cache. */
    suspend fun download(context: Context, release: UpdateRelease): android.net.Uri =
        withContext(Dispatchers.IO) {
            context.cacheDir.listFiles { f -> f.name.endsWith(".apk", ignoreCase = true) }?.forEach { it.delete() }
            val destination = File(context.cacheDir, release.fileName)
            val request = Request.Builder().url(release.downloadUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Não foi possível baixar a atualização (HTTP ${response.code})")
                response.body?.byteStream()?.use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                } ?: error("GitHub não retornou o APK")
            }
            val expected = release.sha256 ?: release.sha256Url?.let { fetchExpectedSha(it) }
            if (expected != null) {
                val actual = sha256Of(destination)
                if (!actual.equals(expected, ignoreCase = true)) {
                    destination.delete()
                    error("O APK baixado não confere com o hash publicado (SHA-256). Atualização cancelada por segurança.")
                }
            }
            FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", destination)
        }

    private fun fetchExpectedSha(url: String): String? = runCatching {
        client.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) null
            else r.body?.string().orEmpty().trim().split(Regex("\\s+")).firstOrNull()?.takeIf { it.length == 64 }
        }
    }.getOrNull()

    private fun sha256Of(file: File): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun isNewer(candidate: String, current: String): Boolean = Versions.isNewer(candidate, current)
}

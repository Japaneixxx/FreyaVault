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
    val beta: Boolean = false
)

object Updater {
    private const val latestReleaseUrl =
        "https://api.github.com/repos/Japaneixxx/FreyaVault/releases/latest"
    private const val releasesUrl =
        "https://api.github.com/repos/Japaneixxx/FreyaVault/releases?per_page=30"
    private val client = OkHttpClient()

    /**
     * Versão beta: o último dígito do Bug é 1, 3 ou 5 (ex.: 1.8.41, 1.8.43, 1.8.45).
     * A exceção é x.x.5 (Bug exatamente 5), que é estável.
     */
    fun isBeta(version: String): Boolean {
        val bug = version.split("-", limit = 2).first().split(".").getOrNull(2)?.toIntOrNull() ?: return false
        return bug != 5 && bug % 10 in setOf(1, 3, 5)
    }

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
        return UpdateRelease(version, url, name, beta = json.optBoolean("prerelease") || isBeta(version))
    }

    suspend fun download(context: Context, release: UpdateRelease): android.net.Uri =
        withContext(Dispatchers.IO) {
            val destination = File(context.cacheDir, release.fileName)
            val request = Request.Builder().url(release.downloadUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Não foi possível baixar a atualização (HTTP ${response.code})")
                response.body?.byteStream()?.use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                } ?: error("GitHub não retornou o APK")
            }
            FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", destination)
        }

    private fun isNewer(candidate: String, current: String): Boolean {
        fun parts(value: String) = value.split("-", limit = 2).first()
            .split(".").map { it.toIntOrNull() ?: 0 }
        val newParts = parts(candidate)
        val currentParts = parts(current)
        for (i in 0 until maxOf(newParts.size, currentParts.size)) {
            val newer = newParts.getOrElse(i) { 0 }
            val existing = currentParts.getOrElse(i) { 0 }
            if (newer != existing) return newer > existing
        }
        return false
    }
}

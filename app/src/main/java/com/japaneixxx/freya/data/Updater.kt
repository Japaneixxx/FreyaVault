package com.japaneixxx.freya.data

import android.content.Context
import androidx.core.content.FileProvider
import com.japaneixxx.freya.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

data class UpdateRelease(
    val version: String,
    val downloadUrl: String,
    val fileName: String
)

object Updater {
    private const val latestReleaseUrl =
        "https://api.github.com/repos/Japaneixxx/FreyaVault/releases/latest"
    private val client = OkHttpClient()

    suspend fun check(): UpdateRelease? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(latestReleaseUrl)
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(request).execute().use { response ->
            if (response.code == 404) return@withContext null // repositório ainda sem Release publicada
            if (!response.isSuccessful) error("GitHub retornou HTTP ${response.code}")
            val json = JSONObject(response.body?.string().orEmpty())
            val version = json.optString("tag_name").removePrefix("v")
            val apk = json.optJSONArray("assets")?.let { assets ->
                (0 until assets.length())
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
            } ?: return@withContext null
            val url = apk.optString("browser_download_url")
            val name = apk.optString("name")
            if (version.isBlank() || url.isBlank() || name.isBlank()) null
            else if (isNewer(version, BuildConfig.VERSION_NAME)) UpdateRelease(version, url, name)
            else null
        }
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

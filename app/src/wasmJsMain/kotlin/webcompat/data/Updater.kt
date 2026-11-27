package com.japaneixxx.freya.data

import android.content.Context
import android.net.Uri

data class UpdateRelease(
    val version: String,
    val downloadUrl: String,
    val fileName: String,
    val beta: Boolean = false,
    val sha256: String? = null,
    val sha256Url: String? = null
)

/** Atualização por APK é exclusiva do Android. */
object Updater {
    fun isBeta(version: String): Boolean = Versions.isBeta(version)

    suspend fun check(includeBeta: Boolean = false): UpdateRelease? = null

    suspend fun download(context: Context, release: UpdateRelease): Uri =
        throw UnsupportedOperationException("Atualizações por APK não estão disponíveis na versão web.")
}

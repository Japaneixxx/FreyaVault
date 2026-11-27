package com.japaneixxx.freya.web

import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/** Ponte entre os ActivityResultContracts e o sistema de arquivos do navegador. */
object WebFiles {
    private val outputs = HashMap<String, PendingOutput>()
    private val inputs = HashMap<String, ByteArray>()
    private var counter = 0

    fun createOutput(fileName: String, mime: String): Uri {
        val key = "freya-save-${++counter}"
        outputs[key] = PendingOutput(fileName, mime)
        return Uri.parse(key)
    }

    fun output(uri: Uri): OutputStream? = outputs[uri.toString()]

    fun createInput(bytes: ByteArray): Uri {
        val key = "freya-open-${++counter}"
        inputs[key] = bytes
        return Uri.parse(key)
    }

    fun input(uri: Uri): InputStream? = inputs[uri.toString()]?.let { ByteArrayInputStream(it) }

    class PendingOutput(private val fileName: String, private val mime: String) : OutputStream() {
        private val buffer = ByteArrayOutputStream()
        override fun write(bytes: ByteArray) { buffer.write(bytes) }
        override fun close() {
            WebBridge.download(fileName, mime, buffer.bytes)
        }
    }
}

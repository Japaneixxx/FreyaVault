@file:OptIn(kotlin.ExperimentalStdlibApi::class)

package java.io

/** Streams mínimos para os fluxos de exportação/importação de arquivos. */
abstract class OutputStream : AutoCloseable {
    abstract fun write(bytes: ByteArray)
    open fun write(byte: Int) = write(byteArrayOf(byte.toByte()))
    override fun close() {}
}

abstract class InputStream : AutoCloseable {
    abstract fun readBytes(): ByteArray
    override fun close() {}
}

class ByteArrayOutputStream : OutputStream() {
    private val data = ArrayList<Byte>()
    val bytes: ByteArray get() = data.toByteArray()
    override fun write(bytes: ByteArray) { data.addAll(bytes.asIterable()) }
}

class ByteArrayInputStream(private val bytes: ByteArray) : InputStream() {
    override fun readBytes(): ByteArray = bytes
}

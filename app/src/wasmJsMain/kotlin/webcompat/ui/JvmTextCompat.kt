package com.japaneixxx.freya.ui

class WebCharset(val name: String)

object Charsets {
    val UTF_8 = WebCharset("UTF-8")
}

fun String.toByteArray(charset: WebCharset): ByteArray = encodeToByteArray()
fun ByteArray.toString(charset: WebCharset): String = decodeToString()

fun <K : Comparable<K>, V> Map<K, V>.toSortedMap(): Map<K, V> =
    entries.sortedBy { it.key }.associate { it.key to it.value }

package org.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

open class JSONException(message: String? = null) : Exception(message)

private val compactJson = Json { ignoreUnknownKeys = true }
private val prettyJson = Json { ignoreUnknownKeys = true; prettyPrint = true; prettyPrintIndent = "  " }

class JSONObject(private val map: LinkedHashMap<String, Any?> = LinkedHashMap()) {

    constructor(json: String) : this(parseObject(json))

    constructor(source: Map<*, *>) : this(LinkedHashMap<String, Any?>().apply {
        source.forEach { (k, v) -> put(k.toString(), v) }
    })

    fun put(key: String, value: Any?): JSONObject {
        map[key] = value
        return this
    }

    fun has(key: String): Boolean = map.containsKey(key)
    fun isNull(key: String): Boolean = !map.containsKey(key) || map[key] == null
    fun length(): Int = map.size
    fun keys(): Iterator<String> = map.keys.iterator()
    fun remove(key: String): Any? = map.remove(key)

    fun opt(key: String): Any? = map[key]
    fun get(key: String): Any? = map[key] ?: throw JSONException("JSONObject[\"$key\"] not found.")

    fun optString(key: String): String = optString(key, "")
    fun optString(key: String, fallback: String): String = when (val v = map[key]) {
        null -> fallback
        is String -> v
        is JsonPrimitive -> v.content
        else -> v.toString()
    }

    fun getString(key: String): String = optString(key)

    fun optInt(key: String): Int = optInt(key, 0)
    fun optInt(key: String, fallback: Int): Int = number(key, fallback).toInt()
    fun getInt(key: String): Int = optInt(key)

    fun optLong(key: String): Long = optLong(key, 0L)
    fun optLong(key: String, fallback: Long): Long = number(key, fallback).toLong()
    fun getLong(key: String): Long = optLong(key)

    fun optDouble(key: String): Double = optDouble(key, Double.NaN)
    fun optDouble(key: String, fallback: Double): Double = number(key, fallback).toDouble()
    fun getDouble(key: String): Double = optDouble(key, 0.0)

    fun optBoolean(key: String): Boolean = optBoolean(key, false)
    fun optBoolean(key: String, fallback: Boolean): Boolean = when (val v = map[key]) {
        null -> fallback
        is Boolean -> v
        is String -> v.equals("true", ignoreCase = true)
        else -> fallback
    }
    fun getBoolean(key: String): Boolean = optBoolean(key)

    fun optJSONObject(key: String): JSONObject? = map[key] as? JSONObject
    fun getJSONObject(key: String): JSONObject = optJSONObject(key) ?: throw JSONException("JSONObject[\"$key\"] is not an object.")
    fun optJSONArray(key: String): JSONArray? = map[key] as? JSONArray
    fun getJSONArray(key: String): JSONArray = optJSONArray(key) ?: throw JSONException("JSONObject[\"$key\"] is not an array.")

    private fun number(key: String, fallback: Number): Number = when (val v = map[key]) {
        is Number -> v
        is String -> v.toDoubleOrNull() ?: fallback
        is JsonPrimitive -> v.doubleOrNull ?: fallback
        else -> fallback
    }

    fun toJsonElement(): JsonElement = JsonObject(map.mapValues { toElement(it.value) })

    override fun toString(): String = compactJson.encodeToString(JsonElement.serializer(), toJsonElement())
    fun toString(indentSpaces: Int): String = prettyJson.encodeToString(JsonElement.serializer(), toJsonElement())

    companion object {
        fun stringify(value: Any?): String = compactJson.encodeToString(JsonElement.serializer(), toElement(value))
    }
}

class JSONArray(private val values: MutableList<Any?> = mutableListOf()) {

    constructor(json: String) : this(parseArray(json))

    fun put(value: Any?): JSONArray {
        values.add(value)
        return this
    }

    fun put(index: Int, value: Any?) {
        while (values.size <= index) values.add(null)
        values[index] = value
    }

    fun length(): Int = values.size
    fun isNull(index: Int): Boolean = index !in values.indices || values[index] == null

    fun opt(index: Int): Any? = values.getOrNull(index)
    fun get(index: Int): Any? = values.getOrNull(index) ?: throw JSONException("JSONArray[$index] not found.")

    fun optString(index: Int): String = optString(index, "")
    fun optString(index: Int, fallback: String): String = when (val v = values.getOrNull(index)) {
        null -> fallback
        is String -> v
        else -> v.toString()
    }
    fun getString(index: Int): String = optString(index)

    fun optInt(index: Int): Int = optInt(index, 0)
    fun optInt(index: Int, fallback: Int): Int = number(index, fallback).toInt()
    fun getInt(index: Int): Int = optInt(index)

    fun optLong(index: Int): Long = optLong(index, 0L)
    fun optLong(index: Int, fallback: Long): Long = number(index, fallback).toLong()
    fun getLong(index: Int): Long = optLong(index)

    fun optDouble(index: Int): Double = optDouble(index, Double.NaN)
    fun optDouble(index: Int, fallback: Double): Double = number(index, fallback).toDouble()
    fun getDouble(index: Int): Double = optDouble(index, 0.0)

    fun optBoolean(index: Int): Boolean = optBoolean(index, false)
    fun optBoolean(index: Int, fallback: Boolean): Boolean = values.getOrNull(index) as? Boolean ?: fallback
    fun getBoolean(index: Int): Boolean = optBoolean(index)

    fun optJSONObject(index: Int): JSONObject? = values.getOrNull(index) as? JSONObject
    fun getJSONObject(index: Int): JSONObject = optJSONObject(index) ?: throw JSONException("JSONArray[$index] is not an object.")
    fun optJSONArray(index: Int): JSONArray? = values.getOrNull(index) as? JSONArray
    fun getJSONArray(index: Int): JSONArray = optJSONArray(index) ?: throw JSONException("JSONArray[$index] is not an array.")

    private fun number(index: Int, fallback: Number): Number = when (val v = values.getOrNull(index)) {
        is Number -> v
        is String -> v.toDoubleOrNull() ?: fallback
        is JsonPrimitive -> v.doubleOrNull ?: fallback
        else -> fallback
    }

    fun toJsonElement(): JsonElement = JsonArray(values.map { toElement(it) })

    override fun toString(): String = compactJson.encodeToString(JsonElement.serializer(), toJsonElement())
}

private fun toElement(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is JsonElement -> value
    is String -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is Number -> {
        val d = value.toDouble()
        if (value is Int || value is Long || value is Short || value is Byte) JsonPrimitive(value.toString().toLong())
        else if (d % 1.0 == 0.0 && d in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()) JsonPrimitive(d.toLong())
        else JsonPrimitive(d)
    }
    is JSONObject -> value.toJsonElement()
    is JSONArray -> value.toJsonElement()
    is Map<*, *> -> JSONObject(value).toJsonElement()
    is Iterable<*> -> JSONArray().also { a -> value.forEach { a.put(it) } }.toJsonElement()
    else -> JsonPrimitive(value.toString())
}

private fun parseObject(json: String): LinkedHashMap<String, Any?> {
    val element = compactJson.parseToJsonElement(json)
    require(element is JsonObject) { "JSON não é um objeto" }
    return LinkedHashMap<String, Any?>().also { m ->
        element.forEach { (k, v) -> m[k] = fromElement(v) }
    }
}

private fun parseArray(json: String): MutableList<Any?> {
    val element = compactJson.parseToJsonElement(json)
    require(element is JsonArray) { "JSON não é um array" }
    return element.map { fromElement(it) }.toMutableList()
}

private fun fromElement(element: JsonElement): Any? = when (element) {
    JsonNull -> null
    is JsonObject -> JSONObject(LinkedHashMap<String, Any?>().also { m ->
        element.forEach { (k, v) -> m[k] = fromElement(v) }
    })
    is JsonArray -> JSONArray(element.map { fromElement(it) }.toMutableList())
    is JsonPrimitive -> when {
        element.isString -> element.content
        element.booleanOrNull != null && (element.content == "true" || element.content == "false") -> element.booleanOrNull
        element.longOrNull != null && !element.content.contains('.') && !element.content.contains('e') && !element.content.contains('E') -> element.longOrNull
        element.doubleOrNull != null -> element.doubleOrNull
        else -> element.content
    }
}

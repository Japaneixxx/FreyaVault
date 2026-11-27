package com.japaneixxx.freya.data

import com.japaneixxx.freya.web.WebBridge
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key, "")

fun JSONObject.dbl(key: String): Double? =
    if (!has(key) || isNull(key)) null else optDouble(key).takeUnless { it.isNaN() }

private fun JSONArray?.objs(): List<JSONObject> =
    if (this == null) emptyList() else List(length()) { getJSONObject(it) }

class PluggyApi(private val clientId: String, private val clientSecret: String) {
    private val base = "/api/pluggy"
    private var apiKey: String = ""

    companion object {
        private const val MAX_RETRIES = 3
    }

    class PluggyHttpException(val code: Int, message: String) : RuntimeException(message) {
        val retryable: Boolean get() = code == 429 || code in 500..599
    }

    private suspend fun callOnce(
        path: String,
        method: String = "GET",
        body: String? = null
    ): JSONObject {
        val headers = HashMap<String, String>()
        headers["Accept"] = "application/json"
        if (apiKey.isNotBlank()) headers["X-API-KEY"] = apiKey
        if (body != null) headers["Content-Type"] = "application/json"
        val response = WebBridge.fetchText(base + path, method, headers, body)
        if (!response.ok) {
            val msg = runCatching { JSONObject(response.body).str("message") }.getOrDefault("")
            val hint = when (response.status) {
                401, 403 -> " (confira Client ID / Client Secret)"
                404 -> " (confira o Item ID)"
                else -> ""
            }
            throw PluggyHttpException(
                response.status,
                "Pluggy ${response.status}: ${msg.ifBlank { response.body.take(120) }}$hint"
            )
        }
        return JSONObject(response.body.ifBlank { "{}" })
    }

    /** Até 3 novas tentativas em 429, 5xx e falhas de rede. */
    private suspend fun call(path: String, method: String = "GET", body: String? = null): JSONObject {
        var attempt = 0
        while (true) {
            try {
                return callOnce(path, method, body)
            } catch (e: PluggyHttpException) {
                if (!e.retryable || attempt >= MAX_RETRIES) throw e
            } catch (e: Exception) {
                if (attempt >= MAX_RETRIES) throw e
            }
            delay(1000L shl attempt)
            attempt++
        }
    }

    suspend fun authenticate() {
        val body = JSONObject().put("clientId", clientId).put("clientSecret", clientSecret).toString()
        val res = call("/auth", "POST", body)
        apiKey = res.getString("apiKey")
    }

    private suspend fun get(path: String): JSONObject = call(path)

    suspend fun item(itemId: String): JSONObject = get("/items/$itemId")

    suspend fun accounts(itemId: String): List<JSONObject> =
        get("/accounts?itemId=$itemId").optJSONArray("results").objs()

    suspend fun investments(itemId: String): List<JSONObject> {
        val all = mutableListOf<JSONObject>()
        var page = 1
        while (page <= 20) {
            val res = get("/investments?itemId=$itemId&pageSize=500&page=$page")
            all += res.optJSONArray("results").objs()
            if (page >= res.optInt("totalPages", 1)) break
            page++
        }
        return all
    }

    suspend fun transactions(accountId: String, from: String): List<JSONObject> {
        val all = mutableListOf<JSONObject>()
        var query: String? = "?accountId=$accountId&dateFrom=${from}T00:00:00.000Z"
        var guard = 0
        while (query != null && guard++ < 200) {
            val res = get("/v2/transactions$query")
            all += res.optJSONArray("results").objs()
            query = if (res.isNull("next")) null else res.optString("next").ifBlank { null }
        }
        return all
    }
}

package app.meufin.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key, "")

private fun JSONArray?.objs(): List<JSONObject> =
    if (this == null) emptyList() else List(length()) { getJSONObject(it) }

class PluggyApi(private val clientId: String, private val clientSecret: String) {
    private val base = "https://api.pluggy.ai"
    private val jsonType = "application/json".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()
    private var apiKey: String = ""

    private fun call(req: Request): JSONObject {
        http.newCall(req).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (!r.isSuccessful) {
                val msg = runCatching { JSONObject(body).str("message") }.getOrDefault("")
                val hint = when (r.code) {
                    401, 403 -> " (confira Client ID / Client Secret)"
                    404 -> " (confira o Item ID)"
                    else -> ""
                }
                throw RuntimeException("Pluggy ${r.code}: ${msg.ifBlank { body.take(120) }}$hint")
            }
            return JSONObject(body)
        }
    }

    /** A API key do Pluggy dura 2h; autenticamos a cada sincronização. */
    fun authenticate() {
        val body = JSONObject().put("clientId", clientId).put("clientSecret", clientSecret)
            .toString().toRequestBody(jsonType)
        val res = call(Request.Builder().url("$base/auth").post(body).build())
        apiKey = res.getString("apiKey")
    }

    private fun get(path: String): JSONObject =
        call(Request.Builder().url("$base$path").header("X-API-KEY", apiKey).get().build())

    fun item(itemId: String): JSONObject = get("/items/$itemId")

    fun accounts(itemId: String): List<JSONObject> =
        get("/accounts?itemId=$itemId").optJSONArray("results").objs()

    /** GET /v2/transactions (paginação por cursor). O /transactions antigo retorna 410. */
    fun transactions(accountId: String, from: String): List<JSONObject> {
        val all = mutableListOf<JSONObject>()
        var query: String? = "?accountId=$accountId&dateFrom=${from}T00:00:00.000Z"
        var guard = 0
        while (query != null && guard++ < 200) {
            val res = get("/v2/transactions$query")
            all += res.optJSONArray("results").objs()
            // "next" já vem pronto ("?accountId=...&after=..."): é só anexar como veio.
            query = if (res.isNull("next")) null else res.optString("next").ifBlank { null }
        }
        return all
    }
}

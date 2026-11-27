package com.japaneixxx.freya.web

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Ponte mínima com o navegador, sem dependência de kotlinx-browser. */
@JsFun("() => Date.now()")
private external fun jsNow(): Double

@JsFun("() => { const d = new Date(); const p = n => String(n).padStart(2, '0'); return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()); }")
private external fun jsTodayIso(): String

@JsFun("() => new Date().getTimezoneOffset()")
private external fun jsTimezoneOffset(): Double

@JsFun("(key) => window.localStorage.getItem(key)")
private external fun jsStorageGet(key: String): String?

@JsFun("(key, value) => { window.localStorage.setItem(key, value); }")
private external fun jsStorageSet(key: String, value: String)

@JsFun("(key) => { window.localStorage.removeItem(key); }")
private external fun jsStorageRemove(key: String)

@JsFun("(url) => { window.open(url, '_blank', 'noopener'); }")
private external fun jsOpenUrl(url: String)

@JsFun("(name, mime, b64) => { try { const bin = atob(b64); const arr = new Uint8Array(bin.length); for (let i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i); const blob = new Blob([arr], { type: mime }); const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = name; document.body.appendChild(a); a.click(); a.remove(); setTimeout(function () { URL.revokeObjectURL(url); }, 2000); } catch (e) { console.error(e); } }")
private external fun jsDownload(name: String, mime: String, b64: String)

@JsFun("(accept, cb) => { const input = document.createElement('input'); input.type = 'file'; input.accept = accept || '*/*'; input.onchange = function () { const f = input.files && input.files[0]; if (!f) { cb(null); return; } const r = new FileReader(); r.onload = function () { cb(String(r.result || '')); }; r.onerror = function () { cb(null); }; r.readAsText(f); }; input.click(); }")
private external fun jsOpenFile(accept: String, cb: (String?) -> Unit)

@JsFun("(text) => { try { if (navigator.share) { navigator.share({ text: text }).catch(function () {}); } else if (navigator.clipboard) { navigator.clipboard.writeText(text).then(function () { alert('Resumo copiado para a área de transferência.'); }).catch(function () {}); } else { alert(text); } } catch (e) { alert(text); } }")
private external fun jsShare(text: String)

@JsFun("() => (typeof crypto !== 'undefined' && crypto.randomUUID) ? crypto.randomUUID() : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) { const r = Math.random() * 16 | 0; const v = c === 'x' ? r : (r & 0x3 | 0x8); return v.toString(16); })")
private external fun jsUuid(): String

@JsFun("(s) => Date.parse(s)")
private external fun jsParseDate(s: String): Double

@JsFun("(a, b) => Math.pow(a, b)")
private external fun jsPow(a: Double, b: Double): Double

/** fetch com Promise; o corpo devolvido é JSON com status e texto. */
@JsFun("(url, method, headersJson, body, ok, fail) => fetch(url, { method: method, headers: JSON.parse(headersJson), body: body || undefined }).then(function (r) { return r.text().then(function (t) { ok(JSON.stringify({ status: r.status, ok: r.ok, body: t })); }); }).catch(function (e) { fail(String(e)); })")
private external fun jsFetch(
    url: String,
    method: String,
    headersJson: String,
    body: String?,
    ok: (String) -> Unit,
    fail: (String) -> Unit
)

object WebBridge {
    fun nowMillis(): Long = jsNow().toLong()
    fun todayIso(): String = jsTodayIso()
    /** Minutos a oeste de UTC (como getTimezoneOffset). */
    fun timezoneOffsetMinutes(): Int = jsTimezoneOffset().toInt()

    fun storageGet(key: String): String? = runCatching { jsStorageGet(key) }.getOrNull()
    fun storageSet(key: String, value: String) { runCatching { jsStorageSet(key, value) } }
    fun storageRemove(key: String) { runCatching { jsStorageRemove(key) } }

    fun openUrl(url: String) { jsOpenUrl(url) }
    fun share(text: String) { jsShare(text) }
    fun randomUuid(): String = jsUuid()

    fun download(fileName: String, mime: String, bytes: ByteArray) =
        jsDownload(fileName, mime, Base64Codec.encode(bytes))

    fun openFile(accept: String, callback: (String?) -> Unit) = jsOpenFile(accept, callback)

    fun parseDateMillis(text: String): Long = jsParseDate(text).toLong()
    fun pow(base: Double, exponent: Double): Double = jsPow(base, exponent)

    suspend fun fetchText(url: String, method: String, headers: Map<String, String>, body: String?): WebResponse =
        suspendCancellableCoroutine { cont: CancellableContinuation<WebResponse> ->
            val headersJson = buildString {
                append('{')
                headers.entries.forEachIndexed { i, (k, v) ->
                    if (i > 0) append(',')
                    append('"').append(k).append("\":\"").append(v.escapeJson()).append('"')
                }
                append('}')
            }
            try {
                jsFetch(
                    url, method, headersJson, body,
                    { raw -> cont.resume(parseResponse(raw)) },
                    { error -> cont.resumeWithException(RuntimeException(error)) }
                )
            } catch (e: Throwable) {
                cont.resumeWithException(e)
            }
        }

    private fun parseResponse(raw: String): WebResponse {
        val o = org.json.JSONObject(raw)
        return WebResponse(o.optInt("status"), o.optString("body"))
    }
}

data class WebResponse(val status: Int, val body: String) {
    val ok: Boolean get() = status in 200..299
}

private fun String.escapeJson(): String = buildString {
    for (ch in this) when (ch) {
        '"' -> append("\\\"")
        '\\' -> append("\\\\")
        '\n' -> append("\\n")
        '\r' -> append("\\r")
        '\t' -> append("\\t")
        else -> append(ch)
    }
}

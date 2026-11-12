package app.meufin.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/** Taxas de mercado em % ao ano, com a data a que se referem (formato dd/MM/aaaa, como o Banco Central informa). */
data class MarketRates(val cdi: Double, val cdiDate: String, val ipca: Double, val ipcaDate: String)

/**
 * Consulta a API pública de séries do Banco Central (SGS), sem chave nem servidor próprio:
 * série 4389 = CDI anualizado (base 252) e série 13522 = IPCA acumulado em 12 meses.
 */
object MarketRatesApi {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun latest(series: Int): Pair<Double, String> {
        val url = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.$series/dados/ultimos/1?formato=json"
        http.newCall(Request.Builder().url(url).get().build()).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw RuntimeException("Banco Central ${r.code}: não foi possível buscar a série $series")
            val arr = JSONArray(body)
            if (arr.length() == 0) throw RuntimeException("Banco Central sem dados para a série $series")
            val o = arr.getJSONObject(arr.length() - 1)
            val value = o.optString("valor").replace(",", ".").toDoubleOrNull()
                ?: throw RuntimeException("Resposta inesperada do Banco Central")
            return value to o.optString("data")
        }
    }

    /** Bloqueante: chame de um dispatcher de IO. Lança exceção se qualquer série falhar. */
    fun fetch(): MarketRates {
        val (cdi, cdiDate) = latest(4389)
        val (ipca, ipcaDate) = latest(13522)
        return MarketRates(cdi, cdiDate, ipca, ipcaDate)
    }
}

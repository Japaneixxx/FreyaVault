package com.japaneixxx.freya.data

import com.japaneixxx.freya.web.WebBridge
import org.json.JSONArray

data class MarketRates(val cdi: Double, val cdiDate: String, val ipca: Double, val ipcaDate: String)

object MarketRatesApi {
    private suspend fun latest(series: Int): Pair<Double, String> {
        val url = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.$series/dados/ultimos/1?formato=json"
        val response = WebBridge.fetchText(url, "GET", emptyMap(), null)
        if (!response.ok) throw RuntimeException("Banco Central ${response.status}: não foi possível buscar a série $series")
        val arr = JSONArray(response.body)
        if (arr.length() == 0) throw RuntimeException("Banco Central sem dados para a série $series")
        val o = arr.getJSONObject(arr.length() - 1)
        val value = o.optString("valor").replace(",", ".").toDoubleOrNull()
            ?: throw RuntimeException("Resposta inesperada do Banco Central")
        return value to o.optString("data")
    }

    suspend fun fetch(): MarketRates {
        val (cdi, cdiDate) = latest(4389)
        val (ipca, ipcaDate) = latest(13522)
        return MarketRates(cdi, cdiDate, ipca, ipcaDate)
    }
}

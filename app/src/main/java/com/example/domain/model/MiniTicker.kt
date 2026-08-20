package com.example.domain.model

import org.json.JSONArray

/** Radar satırı: tüm piyasa taraması için 24s miniTicker özeti. */
data class MiniTickerRow(
    val symbol: String,
    val last: Double,
    val changePct: Double,
    val quoteVol: Double
)

object MiniTickerParser {

    /**
     * `!miniTicker@arr` ham JSON dizisini parse eder.
     * `P` (yüzde değişim) yoksa `(c - o) / o × 100` ile hesaplanır.
     */
    fun parseArray(json: String): List<MiniTickerRow> {
        return try {
            val arr = JSONArray(json)
            val out = ArrayList<MiniTickerRow>()
            for (i in 0 until arr.length()) {
                val t = arr.optJSONObject(i) ?: continue
                val symbol = t.optString("s", "")
                if (symbol.isEmpty()) continue
                val last = t.optString("c", "0").toDoubleOrNull() ?: 0.0
                var pct = t.optString("P", "").toDoubleOrNull()
                if (pct == null) {
                    val open = t.optString("o", "0").toDoubleOrNull() ?: 0.0
                    pct = if (open > 0) (last - open) / open * 100.0 else 0.0
                }
                val quoteVol = t.optString("q", "0").toDoubleOrNull() ?: 0.0
                out.add(MiniTickerRow(symbol, last, pct, quoteVol))
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }
}

package com.example.domain.model

import org.json.JSONObject

/** Binance 24 saatlik ticker özeti (fapi `ticker/24hr`). */
data class Ticker24h(
    val symbol: String,
    val lastPrice: Double,
    val openPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val volume: Double,          // base asset hacmi
    val quoteVolume: Double,     // USDT cinsinden hacim
    val changePct: Double
)

object Ticker24hParser {

    fun parse(json: String): Ticker24h? {
        return try {
            val o = JSONObject(json)
            val symbol = o.optString("symbol", "")
            val last = o.optString("lastPrice", "").toDoubleOrNull() ?: return null
            if (symbol.isEmpty()) return null
            val open = o.optString("openPrice", "0").toDoubleOrNull() ?: 0.0
            val high = o.optString("highPrice", "0").toDoubleOrNull() ?: 0.0
            val low = o.optString("lowPrice", "0").toDoubleOrNull() ?: 0.0
            val vol = o.optString("volume", "0").toDoubleOrNull() ?: 0.0
            val quoteVol = o.optString("quoteVolume", "0").toDoubleOrNull() ?: 0.0
            val pct = o.optString("priceChangePercent", "").toDoubleOrNull()
                ?: if (open > 0) (last - open) / open * 100.0 else 0.0
            Ticker24h(symbol, last, open, high, low, vol, quoteVol, pct)
        } catch (_: Exception) {
            null
        }
    }
}

package com.example.domain.model

import org.json.JSONObject

/** Binance USD-M futures premiumIndex cevabından funding rate özeti. */
data class FundingSnap(
    val symbol: String,
    val lastFundingRate: Double,   // ondalık (0.0001 = %0.01)
    val nextFundingTime: Long = 0L
)

object FundingParser {

    fun parse(json: String): FundingSnap? {
        return try {
            val o = JSONObject(json)
            val symbol = o.optString("symbol", "")
            val rate = o.optString("lastFundingRate", "").toDoubleOrNull()
            if (symbol.isEmpty() || rate == null || !rate.isFinite()) return null
            FundingSnap(symbol, rate, o.optLong("nextFundingTime", 0L))
        } catch (_: Exception) {
            null
        }
    }
}

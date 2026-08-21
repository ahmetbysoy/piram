package com.example.data.remote.rest

import com.example.domain.model.FundingParser
import com.example.domain.model.FundingSnap
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Binance USD-M futures `premiumIndex` REST istemcisi (funding rate, public). */
class FundingRateClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    fun fetch(symbol: String): FundingSnap? {
        return try {
            val url = "https://fapi.binance.com/fapi/v1/premiumIndex?symbol=${symbol.uppercase().trim()}"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                FundingParser.parse(body)
            }
        } catch (_: Exception) {
            null
        }
    }
}

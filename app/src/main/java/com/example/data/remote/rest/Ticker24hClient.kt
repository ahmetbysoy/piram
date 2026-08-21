package com.example.data.remote.rest

import com.example.domain.model.Ticker24h
import com.example.domain.model.Ticker24hParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Binance USD-M futures `ticker/24hr` REST istemcisi (public). */
class Ticker24hClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    fun fetch(symbol: String): Ticker24h? {
        return try {
            val url = "https://fapi.binance.com/fapi/v1/ticker/24hr?symbol=${symbol.uppercase().trim()}"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                Ticker24hParser.parse(body)
            }
        } catch (_: Exception) {
            null
        }
    }
}

package com.example.data.remote.rest

import com.example.domain.model.OiSnap
import com.example.domain.model.OpenInterestParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Binance USD-M futures `openInterest` REST istemcisi (public, key yok). */
class OpenInterestClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    fun fetch(symbol: String): OiSnap? {
        return try {
            val url = "https://fapi.binance.com/fapi/v1/openInterest?symbol=${symbol.uppercase().trim()}"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                OpenInterestParser.parse(body)
            }
        } catch (_: Exception) {
            null
        }
    }
}

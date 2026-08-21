package com.example.data.remote.rest

import com.example.domain.model.SymbolMeta
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Binance spot `exchangeInfo` istemcisi — sembol listesi + tickSize/stepSize. */
class ExchangeInfoClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    fun fetch(): List<SymbolMeta>? {
        return try {
            val request = Request.Builder()
                .url("https://api.binance.com/api/v3/exchangeInfo")
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                parse(body)
            }
        } catch (_: Exception) {
            null
        }
    }

    /** exchangeInfo JSON → USDT/USDC, TRADING spot çiftleri listesi. */
    fun parse(json: String): List<SymbolMeta> {
        return try {
            val root = JSONObject(json)
            val symbols = root.optJSONArray("symbols") ?: return emptyList()
            val out = ArrayList<SymbolMeta>()
            for (i in 0 until symbols.length()) {
                val s = symbols.optJSONObject(i) ?: continue
                if (s.optString("status") != "TRADING") continue
                val symbol = s.optString("symbol", "")
                val quote = s.optString("quoteAsset", "")
                if (quote != "USDT" && quote != "USDC") continue
                val base = s.optString("baseAsset", symbol)
                val filters = s.optJSONArray("filters")
                var tickSize = "0.01"
                var stepSize = "0.001"
                if (filters != null) {
                    for (j in 0 until filters.length()) {
                        val f = filters.optJSONObject(j) ?: continue
                        when (f.optString("filterType")) {
                            "PRICE_FILTER" -> tickSize = f.optString("tickSize", "0.01")
                            "LOT_SIZE" -> stepSize = f.optString("stepSize", "0.001")
                        }
                    }
                }
                out.add(SymbolMeta(symbol, base, tickSize, stepSize))
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }
}

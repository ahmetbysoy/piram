package com.example.domain.model

import org.json.JSONObject
import java.util.Locale

/** Binance USD-M futures açık pozisyon (openInterest) REST cevabı. */
data class OiSnap(
    val symbol: String,
    val oi: Double,   // kontrat adedi
    val at: Long = System.currentTimeMillis()
)

/**
 * OI durum makinesi — "OI yoksa yalan yok".
 * `bekliyor` → ilk cevap gelmedi; `ok` → taze; `yok` → hiç gelmedi (CORS/ağ);
 * `eski` → daha önce vardı ama son sorgu başarısız.
 */
enum class OiState { BEKLIYOR, OK, YOK, ESKI }

object OpenInterestParser {

    fun parse(json: String): OiSnap? {
        return try {
            val o = JSONObject(json)
            val symbol = o.optString("symbol", "")
            val oi = o.optString("openInterest", "").toDoubleOrNull()
            if (symbol.isEmpty() || oi == null || !oi.isFinite()) return null
            OiSnap(symbol, oi)
        } catch (_: Exception) {
            null
        }
    }

    /** Ekran USDT gösterir: kontrat × fiyat. */
    fun oiToUsdt(contracts: Double?, price: Double): Double? {
        if (contracts == null || !contracts.isFinite() || !price.isFinite() || price <= 0) return null
        return contracts * price
    }
}

/** OI USDT değerini kısa yazar: 1.2B / 345.6M / 12.3K. */
fun formatOi(n: Double): String {
    return when {
        n >= 1_000_000_000.0 -> String.format(Locale.US, "%.2fB", n / 1_000_000_000.0)
        n >= 1_000_000.0 -> String.format(Locale.US, "%.1fM", n / 1_000_000.0)
        n >= 1_000.0 -> String.format(Locale.US, "%.1fK", n / 1_000.0)
        else -> String.format(Locale.US, "%.0f", n)
    }
}

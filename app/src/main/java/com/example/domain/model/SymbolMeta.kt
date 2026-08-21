package com.example.domain.model

/**
 * Sembol meta bilgisi (exchangeInfo'dan): fiyat hanesi (tickSize) ve lot adımı (stepSize).
 */
data class SymbolMeta(
    val symbol: String,
    val base: String,
    val tickSize: String,
    val stepSize: String
) {
    /** tickSize'tan ondalık hane sayısı: "0.01" → 2, "0.00000001" → 8. */
    fun tickDecimals(): Int {
        val t = tickSize.trim()
        val dot = t.indexOf('.')
        if (dot < 0) return 0
        return t.length - dot - 1
    }
}

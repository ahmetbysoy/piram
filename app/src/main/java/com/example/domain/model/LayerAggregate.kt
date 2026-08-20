package com.example.domain.model

/**
 * Bir piramit katmanının ekran/snapshot görünümü.
 * Tüm hacim alanları USDT **notional** (fiyat × adet) cinsindendir — adet sayılmaz.
 */
data class LayerAggregate(
    val layerIndex: Int,
    val minNotional: Double,
    val maxNotional: Double,
    val notional: Double,        // o anki toplam notional (USDT)
    val displayNotional: Double, // lerp ile yumuşatılmış görsel değer
    val buyNotional: Double,
    val sellNotional: Double,
    val orderCount: Int,
    val buyRatio: Float,
    val isWhaleTier: Boolean,
    val label: String
)

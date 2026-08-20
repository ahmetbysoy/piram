package com.example.domain.model

/**
 * Binance USD-M futures likidasyon (forceOrder) olayı.
 * `side`: zorla kapatılan emrin tarafı — SELL = long likidasyon, BUY = short likidasyon.
 */
data class Liquidation(
    val symbol: String,
    val side: OrderSide,
    val price: Double,
    val quantity: Double,
    val notional: Double,
    val timestamp: Long
)

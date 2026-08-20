package com.example.domain.model

enum class OrderSide {
    BUY,
    SELL
}

data class Order(
    val id: String,
    val side: OrderSide,
    val volume: Double,
    val price: Double,
    val timestamp: Long,
    val value: Double = volume * price,
    val exchange: String = "Binance",
    val layerIndex: Int = 0,
    val isWhale: Boolean = false
)

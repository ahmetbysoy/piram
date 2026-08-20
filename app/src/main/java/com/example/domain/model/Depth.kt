package com.example.domain.model

data class DepthLevel(
    val price: Double,
    val volume: Double,
    val total: Double = price * volume
)

data class Depth(
    val bids: List<DepthLevel>,
    val asks: List<DepthLevel>,
    val exchange: String,
    val timestamp: Long,
    val midPrice: Double = if (bids.isNotEmpty() && asks.isNotEmpty()) (bids.first().price + asks.first().price) / 2.0 else 0.0,
    val spread: Double = if (bids.isNotEmpty() && asks.isNotEmpty()) (asks.first().price - bids.first().price) else 0.0,
    val imbalance: Double = run {
        val totalBid = bids.take(10).sumOf { it.volume }
        val totalAsk = asks.take(10).sumOf { it.volume }
        if (totalBid + totalAsk > 0) (totalBid - totalAsk) / (totalBid + totalAsk) else 0.0
    }
)

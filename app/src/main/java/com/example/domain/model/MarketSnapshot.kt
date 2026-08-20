package com.example.domain.model

data class MarketSnapshot(
    val symbol: String,
    val currentPrice: Double,
    val high24h: Double = 0.0,
    val low24h: Double = 0.0,
    val volume24h: Double = 0.0,
    val priceChange24h: Double = 0.0,
    val trades: List<Order> = emptyList(),
    val recentPrices: List<Double> = emptyList(),
    val recentVolumes: List<Double> = emptyList(),
    val depth: Depth? = null,
    val bursts: List<BurstCluster> = emptyList(),
    val orderFlowImbalance: Double = 0.0,
    val buyVolume1m: Double = 0.0,
    val sellVolume1m: Double = 0.0,
    val vwap: Double = 0.0,
    val exchangePrices: Map<String, Double> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

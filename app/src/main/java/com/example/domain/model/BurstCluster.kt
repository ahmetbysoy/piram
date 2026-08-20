package com.example.domain.model

data class BurstCluster(
    val id: String,
    val side: OrderSide,
    val totalValue: Double,
    val totalVolume: Double,
    val orderCount: Int,
    val startTime: Long,
    val endTime: Long,
    val avgPrice: Double,
    val intensityScore: Double, // Z-Score velocity metric
    val exchange: String = "Multi"
)

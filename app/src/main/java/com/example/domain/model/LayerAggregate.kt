package com.example.domain.model

data class LayerAggregate(
    val layerIndex: Int,
    val minVolume: Double,
    val maxVolume: Double,
    val currentVolume: Double,
    val displayVolume: Double,
    val buyVolume: Double,
    val sellVolume: Double,
    val orderCount: Int,
    val buyRatio: Float,
    val isWhaleTier: Boolean,
    val label: String
)

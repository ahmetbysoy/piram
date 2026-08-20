package com.example.domain.model

data class ConsensusResult(
    val overallSignal: SignalType,
    val buyScore: Double, // 0.0 to 100.0
    val sellScore: Double, // 0.0 to 100.0
    val neutralScore: Double, // 0.0 to 100.0
    val confidence: Double, // 0.0 to 1.0
    val consensusStrength: Double, // -100 to +100
    val activeStrategiesCount: Int,
    val bullishCount: Int,
    val bearishCount: Int,
    val neutralCount: Int,
    val topBullishStrategy: String? = null,
    val topBearishStrategy: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

package com.example.domain.model

enum class SignalType {
    STRONG_BUY,
    BUY,
    NEUTRAL,
    SELL,
    STRONG_SELL
}

data class StrategyResult(
    val strategyId: String,
    val name: String,
    val signal: SignalType,
    val confidence: Double, // 0.0 to 1.0
    val score: Double, // -1.0 (strong sell) to +1.0 (strong buy)
    val reasoning: String,
    val metrics: Map<String, Double> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

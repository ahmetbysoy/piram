package com.example.domain.engine.strategy

import com.example.domain.model.MarketSnapshot
import com.example.domain.model.StrategyResult

interface Strategy {
    val id: String
    val name: String
    val description: String
    val category: StrategyCategory
    fun execute(data: MarketSnapshot): StrategyResult
}

enum class StrategyCategory(val label: String) {
    TREND("Trend"),
    MOMENTUM("Momentum"),
    MICROSTRUCTURE("Order Flow"),
    VOLATILITY("Volatility"),
    ARBITRAGE("Arbitrage")
}

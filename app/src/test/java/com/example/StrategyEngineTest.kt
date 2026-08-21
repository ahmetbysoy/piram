package com.example

import com.example.domain.engine.strategy.StrategyEngine
import com.example.domain.model.MarketSnapshot
import com.example.domain.model.Order
import com.example.domain.model.OrderSide
import com.example.domain.model.SignalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrategyEngineTest {

    @Test
    fun testAllStrategiesExecution() {
        val engine = StrategyEngine()
        assertEquals(30, engine.strategies.size)

        val trades = listOf(
            Order("1", OrderSide.BUY, 1.0, 60000.0, System.currentTimeMillis()),
            Order("2", OrderSide.BUY, 2.5, 60100.0, System.currentTimeMillis()),
            Order("3", OrderSide.BUY, 4.0, 60200.0, System.currentTimeMillis())
        )
        val prices = listOf(59800.0, 59900.0, 60000.0, 60100.0, 60200.0)

        val snapshot = MarketSnapshot(
            symbol = "BTCUSDT",
            currentPrice = 60200.0,
            trades = trades,
            recentPrices = prices,
            recentVolumes = listOf(1.0, 2.5, 4.0),
            depth = null,
            bursts = emptyList(),
            orderFlowImbalance = 0.8,
            vwap = 60133.3,
            timestamp = System.currentTimeMillis()
        )

        val (results, consensus) = engine.executeAll(snapshot)
        assertEquals(30, results.size)
        assertNotNull(consensus)
        assertTrue(consensus.activeStrategiesCount == 30)
        assertTrue(consensus.buyScore >= 0.0 && consensus.buyScore <= 100.0)
    }
}

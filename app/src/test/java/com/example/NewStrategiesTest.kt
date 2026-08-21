package com.example

import com.example.domain.engine.strategy.LiquidationCascadeStrategy
import com.example.domain.engine.strategy.RoundNumberMagnetStrategy
import com.example.domain.engine.strategy.WhaleFootprintStrategy
import com.example.domain.model.MarketSnapshot
import com.example.domain.model.Order
import com.example.domain.model.OrderSide
import com.example.domain.model.SignalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhaleFootprintStrategyTest {

    private fun order(id: String, side: OrderSide, volume: Double, price: Double, whale: Boolean) =
        Order(id, side, volume, price, System.currentTimeMillis(), isWhale = whale)

    private fun snapshot(trades: List<Order>) = MarketSnapshot(
        symbol = "BTCUSDT",
        currentPrice = 60_000.0,
        trades = trades,
        recentPrices = listOf(59_000.0, 60_000.0, 61_000.0)
    )

    @Test
    fun `yetersiz ornek notr`() {
        val r = WhaleFootprintStrategy().execute(snapshot(emptyList()))
        assertEquals(SignalType.NEUTRAL, r.signal)
    }

    @Test
    fun `whale yok notr`() {
        val trades = (1..25).map { order("$it", OrderSide.BUY, 0.01, 60_000.0, whale = false) }
        val r = WhaleFootprintStrategy().execute(snapshot(trades))
        assertEquals(SignalType.NEUTRAL, r.signal)
    }

    @Test
    fun `whale alis baskin buy`() {
        val trades = (1..25).map { order("r$it", OrderSide.BUY, 0.01, 60_000.0, whale = false) } +
            listOf(
                order("w1", OrderSide.BUY, 5.0, 60_100.0, whale = true),
                order("w2", OrderSide.BUY, 6.0, 60_200.0, whale = true)
            )
        val r = WhaleFootprintStrategy().execute(snapshot(trades))
        assertTrue(r.score > 0)
        assertTrue(r.reasoning.contains("x avg"))
    }

    @Test
    fun `whale satis baskin sell`() {
        val trades = (1..25).map { order("r$it", OrderSide.SELL, 0.01, 60_000.0, whale = false) } +
            listOf(
                order("w1", OrderSide.SELL, 5.0, 59_900.0, whale = true),
                order("w2", OrderSide.SELL, 6.0, 59_800.0, whale = true)
            )
        val r = WhaleFootprintStrategy().execute(snapshot(trades))
        assertTrue(r.score < 0)
    }
}

class RoundNumberMagnetStrategyTest {

    @Test
    fun `roundLevel buyukluge gore`() {
        assertEquals(60_000.0, RoundNumberMagnetStrategy.roundLevel(60_200.0), 1e-9)
        assertEquals(1_200.0, RoundNumberMagnetStrategy.roundLevel(1_234.0), 1e-9)
        assertEquals(120.0, RoundNumberMagnetStrategy.roundLevel(123.0), 1e-9)
        assertEquals(12.0, RoundNumberMagnetStrategy.roundLevel(12.3), 1e-9)
    }

    @Test
    fun `yakin seviye yon uretir`() {
        val snap = MarketSnapshot(symbol = "BTCUSDT", currentPrice = 60_150.0, recentPrices = listOf(60_000.0))
        val r = RoundNumberMagnetStrategy().execute(snap)
        // 60_150 > 60_000 → yukarıda → aşağı çekim (SELL)
        assertTrue(r.score < 0)
        assertEquals(60_000.0, r.metrics["round"]!!, 1e-9)
    }

    @Test
    fun `uzak seviye notr`() {
        val snap = MarketSnapshot(symbol = "BTCUSDT", currentPrice = 60_450.0, recentPrices = listOf(60_000.0))
        val r = RoundNumberMagnetStrategy().execute(snap)
        assertEquals(SignalType.NEUTRAL, r.signal)
    }
}

class LiquidationCascadeStrategyTest {

    @Test
    fun `kaskad yoksa notr`() {
        val snap = MarketSnapshot(symbol = "BTCUSDT", currentPrice = 60_000.0, recentPrices = listOf(60_000.0))
        val r = LiquidationCascadeStrategy().execute(snap)
        assertEquals(SignalType.NEUTRAL, r.signal)
    }

    @Test
    fun `likidasyon + dusus sell`() {
        val snap = MarketSnapshot(
            symbol = "BTCUSDT",
            currentPrice = 59_800.0,
            recentPrices = listOf(60_000.0, 59_950.0, 59_900.0, 59_850.0, 59_800.0),
            liquidationNotional60s = 2_000_000.0,
            liquidationCount60s = 12
        )
        val r = LiquidationCascadeStrategy().execute(snap)
        assertTrue(r.score < 0)
        assertEquals(12.0, r.metrics["liqCount"]!!, 1e-9)
    }
}

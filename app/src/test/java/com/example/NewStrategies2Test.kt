package com.example

import com.example.domain.engine.ConsensusVolatility
import com.example.domain.engine.strategy.FibonacciConfluenceStrategy
import com.example.domain.engine.strategy.OIDivergenceStrategy
import com.example.domain.engine.strategy.TapeReadingSpeedStrategy
import com.example.domain.engine.strategy.VwapReversionStrategy
import com.example.domain.engine.strategy.WickRejectionStrategy
import com.example.domain.model.MarketSnapshot
import com.example.domain.model.Order
import com.example.domain.model.OrderSide
import com.example.domain.model.SignalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WickRejectionStrategyTest {

    private fun snap(prices: List<Double>) = MarketSnapshot(symbol = "BTCUSDT", currentPrice = prices.last(), recentPrices = prices)

    @Test
    fun `yetersiz ornek notr`() {
        assertEquals(SignalType.NEUTRAL, WickRejectionStrategy().execute(snap(listOf(1.0, 2.0))).signal)
    }

    @Test
    fun `alt wick baskin buy`() {
        // open yuksek, close yuksek ama low cok asagida (uzun alt wick)
        val prices = listOf(100.0, 101.0, 100.5, 101.2, 99.0, 101.0, 100.8, 101.1, 100.9, 101.0)
        val r = WickRejectionStrategy().execute(snap(prices))
        assertTrue(r.score > 0)
    }

    @Test
    fun `ust wick baskin sell`() {
        // open dusuk, close dusuk ama high cok yukarida (uzun ust wick)
        val prices = listOf(100.0, 99.0, 100.5, 99.2, 103.0, 99.0, 100.2, 99.1, 100.3, 99.0)
        val r = WickRejectionStrategy().execute(snap(prices))
        assertTrue(r.score < 0)
    }
}

class OIDivergenceStrategyTest {

    private fun snap(changePct: Double, oiDelta: Double) = MarketSnapshot(
        symbol = "BTCUSDT",
        currentPrice = 100.0,
        recentPrices = listOf(100.0, 100.0 + changePct),
        oiDelta = oiDelta
    )

    @Test
    fun `oi yok notr`() {
        val snap = MarketSnapshot(symbol = "BTCUSDT", currentPrice = 100.0, recentPrices = listOf(100.0))
        assertEquals(SignalType.NEUTRAL, OIDivergenceStrategy().execute(snap).signal)
    }

    @Test
    fun `fiyat yukari oi artis yeni long buy`() {
        assertTrue(OIDivergenceStrategy().execute(snap(0.5, 100.0)).score > 0)
    }

    @Test
    fun `fiyat asagi oi artis yeni short sell`() {
        assertTrue(OIDivergenceStrategy().execute(snap(-0.5, 100.0)).score < 0)
    }

    @Test
    fun `fiyat yukari oi dusus zayif`() {
        val r = OIDivergenceStrategy().execute(snap(0.5, -100.0))
        assertTrue(r.score > 0)
        assertTrue(r.score < 0.55) // short cover daha zayif
    }
}

class TapeReadingSpeedStrategyTest {

    private fun order(id: String, value: Double) = Order(id, OrderSide.BUY, 1.0, 100.0, System.currentTimeMillis(), value = value)

    private fun snap(trades: List<Order>, ofi: Double) = MarketSnapshot(
        symbol = "BTCUSDT", currentPrice = 100.0, trades = trades, orderFlowImbalance = ofi,
        recentPrices = listOf(100.0, 101.0, 102.0)
    )

    @Test
    fun `yetersiz ornek notr`() {
        assertEquals(SignalType.NEUTRAL, TapeReadingSpeedStrategy().execute(snap(emptyList(), 0.0)).signal)
    }

    @Test
    fun `buyuk alicilar hizlaniyor buy`() {
        val trades = (1..10).map { order("a$it", 100.0) } + (11..20).map { order("b$it", 500.0) }
        val r = TapeReadingSpeedStrategy().execute(snap(trades, 0.5))
        assertTrue(r.score > 0)
    }

    @Test
    fun `buyuk saticilar hizlaniyor sell`() {
        val trades = (1..10).map { order("a$it", 100.0) } + (11..20).map { order("b$it", 500.0) }
        val r = TapeReadingSpeedStrategy().execute(snap(trades, -0.5))
        assertTrue(r.score < 0)
    }
}

class VwapReversionStrategyTest {

    @Test
    fun `vwap yok notr`() {
        val snap = MarketSnapshot(symbol = "BTCUSDT", currentPrice = 0.0, recentPrices = emptyList())
        assertEquals(SignalType.NEUTRAL, VwapReversionStrategy().execute(snap).signal)
    }

    @Test
    fun `asiri yukari sapma sell`() {
        // 20 fiyat 100 civarinda, vwap 100, fiyat 101.5 → dev 1.5%, std kucuk → z yuksek
        val prices = (1..20).map { 100.0 + (it % 2) * 0.05 }
        val snap = MarketSnapshot(symbol = "BTCUSDT", currentPrice = 101.5, recentPrices = prices, vwap = 100.0)
        assertTrue(VwapReversionStrategy().execute(snap).score < 0)
    }

    @Test
    fun `asiri asagi sapma buy`() {
        val prices = (1..20).map { 100.0 + (it % 2) * 0.05 }
        val snap = MarketSnapshot(symbol = "BTCUSDT", currentPrice = 98.5, recentPrices = prices, vwap = 100.0)
        assertTrue(VwapReversionStrategy().execute(snap).score > 0)
    }
}

class FibonacciConfluenceStrategyTest {

    @Test
    fun `yetersiz ornek notr`() {
        val snap = MarketSnapshot(symbol = "BTCUSDT", currentPrice = 100.0, recentPrices = listOf(1.0))
        assertEquals(SignalType.NEUTRAL, FibonacciConfluenceStrategy().execute(snap).signal)
    }

    @Test
    fun `seviyeye yakin sinyal uretir`() {
        val prices = (1..30).map { 100.0 + it * 1.0 } // 101..130, swing high 130 low 101
        // 0.236 retracement: 130 - 29*0.236 = 123.156 → fiyat tam üstünde
        val snap = MarketSnapshot(symbol = "BTCUSDT", currentPrice = 123.16, recentPrices = prices)
        val r = FibonacciConfluenceStrategy().execute(snap)
        assertTrue(r.signal != SignalType.NEUTRAL)
    }

    @Test
    fun `seviyeden uzak notr`() {
        val prices = (1..30).map { 100.0 + it * 1.0 }
        val snap = MarketSnapshot(symbol = "BTCUSDT", currentPrice = 108.0, recentPrices = prices)
        assertEquals(SignalType.NEUTRAL, FibonacciConfluenceStrategy().execute(snap).signal)
    }
}

class ConsensusVolatilityTest {

    @Test
    fun `az ornek stabil degil`() {
        val b = ConsensusVolatility.band(listOf(10.0))
        assertTrue(!b.isUnstable)
    }

    @Test
    fun `dar bant stabil`() {
        val b = ConsensusVolatility.band(listOf(10.0, 12.0, 11.0, 10.0, 12.0))
        assertTrue(!b.isUnstable)
    }

    @Test
    fun `genis bant kararsiz`() {
        val b = ConsensusVolatility.band(listOf(50.0, -40.0, 30.0, -30.0, 40.0, -50.0))
        assertTrue(b.stdDev > 25.0)
        assertTrue(b.isUnstable)
    }
}

package com.example

import com.example.domain.engine.strategy.StrategyPerformanceTracker
import org.junit.Assert.assertEquals
import org.junit.Test

class StrategyPerformanceTrackerTest {

    @Test
    fun `soguk baslangicta agirlik notr`() {
        val t = StrategyPerformanceTracker(clock = { 0L })
        assertEquals(1.0, t.weight("s1"), 1e-9)
    }

    @Test
    fun `yeterli ornek yoksa agirlik yine notr`() {
        val t = StrategyPerformanceTracker(minSamples = 10, clock = { 0L })
        // 5 dogru isabet, 5'ten az ornek -> ceza yok
        t.record("s1", bullish = true, price = 100.0, at = 0L)
        t.record("s1", bullish = true, price = 100.0, at = 5001L)
        t.resolve(price = 110.0, now = 70_000L)
        assertEquals(1.0, t.weight("s1"), 1e-9)
    }

    @Test
    fun `iyi strateji agirligi artirir`() {
        val t = StrategyPerformanceTracker(minSamples = 3, recordIntervalMs = 1L, clock = { 0L })
        t.record("good", bullish = true, price = 100.0, at = 0L)
        t.record("good", bullish = true, price = 100.0, at = 2L)
        t.record("good", bullish = true, price = 100.0, at = 4L)
        // 3/3 isabet: fiyat yukseldi
        t.resolve(price = 110.0, now = 70_000L)
        assertEquals(3, t.resolvedCount("good"))
        assertEquals(3, t.hitCount("good"))
        assertEquals(1.0, t.winRate("good"), 1e-9)
        // 0.5 + 1.0 = 1.5 -> cap 1.2
        assertEquals(1.2, t.weight("good"), 1e-9)
    }

    @Test
    fun `kotu strateji agirligi dusurur`() {
        val t = StrategyPerformanceTracker(minSamples = 3, recordIntervalMs = 1L, clock = { 0L })
        t.record("bad", bullish = true, price = 100.0, at = 0L)
        t.record("bad", bullish = true, price = 100.0, at = 2L)
        t.record("bad", bullish = true, price = 100.0, at = 4L)
        // 0/3 isabet: fiyat dustu
        t.resolve(price = 90.0, now = 70_000L)
        assertEquals(0, t.hitCount("bad"))
        // 0.5 + 0.0 = 0.5
        assertEquals(0.5, t.weight("bad"), 1e-9)
    }

    @Test
    fun `bearish tahmin de dogru degerlendirilir`() {
        val t = StrategyPerformanceTracker(minSamples = 2, recordIntervalMs = 1L, clock = { 0L })
        t.record("short", bullish = false, price = 100.0, at = 0L)
        t.record("short", bullish = false, price = 100.0, at = 2L)
        t.resolve(price = 90.0, now = 70_000L) // dustu -> bearish isabet
        assertEquals(2, t.hitCount("short"))
    }

    @Test
    fun `throttle ayni pencerede tek kayit`() {
        val t = StrategyPerformanceTracker(recordIntervalMs = 5000L, minSamples = 1, clock = { 0L })
        t.record("s", bullish = true, price = 100.0, at = 0L)
        t.record("s", bullish = true, price = 100.0, at = 1000L) // throttle'li, atlanir
        t.record("s", bullish = true, price = 100.0, at = 2000L) // atlanir
        t.resolve(price = 110.0, now = 70_000L)
        assertEquals(1, t.resolvedCount("s"))
    }

    @Test
    fun `shouldRecord skor esigi`() {
        val t = StrategyPerformanceTracker()
        assertEquals(true, t.shouldRecord(0.2))
        assertEquals(false, t.shouldRecord(0.05))
        assertEquals(true, t.shouldRecord(-0.3))
    }

    @Test
    fun `clear sifirlar`() {
        val t = StrategyPerformanceTracker(minSamples = 1, recordIntervalMs = 1L, clock = { 0L })
        t.record("s", bullish = true, price = 100.0, at = 0L)
        t.resolve(price = 110.0, now = 70_000L)
        t.clear()
        assertEquals(0, t.resolvedCount("s"))
        assertEquals(1.0, t.weight("s"), 1e-9)
    }
}

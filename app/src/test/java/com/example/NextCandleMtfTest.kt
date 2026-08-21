package com.example

import com.example.domain.engine.MultiTimeframeConsensus
import com.example.domain.engine.NextCandleGame
import com.example.domain.model.SignalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiTimeframeConsensusTest {

    @Test
    fun `notr sinyaller null`() {
        assertNull(MultiTimeframeConsensus.compare(SignalType.NEUTRAL, SignalType.BUY))
        assertNull(MultiTimeframeConsensus.compare(SignalType.BUY, SignalType.NEUTRAL))
    }

    @Test
    fun `ayni yon teyit`() {
        assertTrue(MultiTimeframeConsensus.compare(SignalType.BUY, SignalType.STRONG_BUY)!!.contains("ALIŞ teyitli"))
        assertTrue(MultiTimeframeConsensus.compare(SignalType.SELL, SignalType.SELL)!!.contains("SATIŞ teyitli"))
    }

    @Test
    fun `kisa satis uzun alis karisik`() {
        val r = MultiTimeframeConsensus.compare(SignalType.SELL, SignalType.BUY)!!
        assertTrue(r.contains("karışık"))
    }

    @Test
    fun `kisa alis uzun satis dip toplama`() {
        val r = MultiTimeframeConsensus.compare(SignalType.BUY, SignalType.SELL)!!
        assertTrue(r.contains("dip toplama"))
    }
}

class NextCandleGameTest {

    @Test
    fun `zayif sinyal tahmin yok`() {
        val g = NextCandleGame(clock = { 0L })
        assertNull(g.predict(5.0, 100.0, 0L))
    }

    @Test
    fun `guclu sinyal tahmin uretir`() {
        val g = NextCandleGame(clock = { 0L })
        assertEquals(true, g.predict(40.0, 100.0, 0L))
        assertEquals(true, g.pendingBullish())
        // bekleyen varken yeni tahmin yok
        assertNull(g.predict(40.0, 100.0, 1000L))
    }

    @Test
    fun `dogru tahmin isabet`() {
        var now = 0L
        val g = NextCandleGame(clock = { now })
        g.predict(40.0, 100.0, 0L) // bullish
        now = 61_000L
        g.resolve(110.0, now) // yükseldi → win
        assertTrue(g.chip().startsWith("🎯 1/1"))
    }

    @Test
    fun `yanlis tahmin isabetsiz`() {
        var now = 0L
        val g = NextCandleGame(clock = { now })
        g.predict(-40.0, 100.0, 0L) // bearish
        now = 61_000L
        g.resolve(110.0, now) // yükseldi → loss
        assertTrue(g.chip().contains("🎯 0/1"))
    }

    @Test
    fun `seri gosterimi`() {
        var now = 0L
        val g = NextCandleGame(clock = { now })
        // 2 doğru tahmin
        g.predict(40.0, 100.0, 0L)
        now = 61_000L
        g.resolve(110.0, now)
        g.predict(40.0, 110.0, now)
        now = 121_000L
        g.resolve(120.0, now)
        assertTrue(g.chip().contains("🔥2"))
    }

    @Test
    fun `bekleyen tahmin chip gosterir`() {
        val g = NextCandleGame(clock = { 0L })
        g.predict(40.0, 100.0, 0L)
        assertTrue(g.chip().contains("bekliyor"))
        assertFalse(g.chip().contains("/"))
    }

    @Test
    fun `clear sifirlar`() {
        var now = 0L
        val g = NextCandleGame(clock = { now })
        g.predict(40.0, 100.0, 0L)
        now = 61_000L
        g.resolve(110.0, now)
        g.clear()
        assertEquals("", g.chip())
    }
}

package com.example

import com.example.domain.engine.LiquidationTracker
import com.example.domain.engine.MarketPersonality
import com.example.domain.engine.RektMeter
import com.example.domain.engine.WhaleRetailBoard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MarketPersonalityTest {

    @Test
    fun `buyuk balina ve salvo agresif`() {
        val (label, emoji) = MarketPersonality.evaluate(
            whalePct = 70.0, burstCount = 2, changePct = 0.5, ofi = 0.3
        )
        assertEquals("AGRESİF", label)
        assertEquals("🦈", emoji)
    }

    @Test
    fun `buyuk para ama fiyat kimildamiyor sinsilik`() {
        val (label, _) = MarketPersonality.evaluate(
            whalePct = 70.0, burstCount = 0, changePct = 0.02, ofi = 0.0
        )
        assertEquals("SİNSİ", label)
    }

    @Test
    fun `sakin piyasa`() {
        val (label, emoji) = MarketPersonality.evaluate(
            whalePct = 20.0, burstCount = 0, changePct = 0.01, ofi = 0.0
        )
        assertEquals("SAKİN", label)
        assertEquals("😴", emoji)
    }

    @Test
    fun `uc ve uzeri salvo cilgin`() {
        val (label, _) = MarketPersonality.evaluate(
            whalePct = 40.0, burstCount = 3, changePct = 1.0, ofi = 0.5
        )
        assertEquals("ÇILGIN", label)
    }

    @Test
    fun `varsayilan kararsiz`() {
        val (label, emoji) = MarketPersonality.evaluate(
            whalePct = 40.0, burstCount = 0, changePct = 0.5, ofi = 0.0
        )
        assertEquals("KARARSIZ", label)
        assertEquals("🎲", emoji)
    }
}

class RektMeterTest {

    @Test
    fun `seviye esikleri`() {
        assertEquals(0, RektMeter.level(0.0))
        assertEquals(0, RektMeter.level(49_999.0))
        assertEquals(1, RektMeter.level(50_000.0))
        assertEquals(3, RektMeter.level(1_000_000.0))
        assertEquals(5, RektMeter.level(10_000_000.0))
    }

    @Test
    fun `emoji ve label`() {
        assertEquals("", RektMeter.emoji(0))
        assertEquals("🔥", RektMeter.emoji(3))
        assertEquals("🔥🔥🔥", RektMeter.emoji(5))
        assertEquals("REKT", RektMeter.label(5))
        assertEquals("", RektMeter.label(2))
    }
}

class LiquidationTrackerTest {

    @Test
    fun `60sn pencere toplami`() {
        var now = 0L
        val t = LiquidationTracker(clock = { now })
        t.record(100.0, 0L)
        t.record(200.0, 10_000L)
        assertEquals(300.0, t.sum(now), 1e-9)
        assertEquals(2, t.count(now))
    }

    @Test
    fun `60sn sonrasi budanir`() {
        var now = 0L
        val t = LiquidationTracker(clock = { now })
        t.record(100.0, 0L)
        now = 61_000L
        assertEquals(0.0, t.sum(now), 1e-9)
        assertEquals(0, t.count(now))
    }

    @Test
    fun `negatif yok sayilir`() {
        val t = LiquidationTracker(clock = { 0L })
        t.record(-5.0, 0L)
        t.record(0.0, 0L)
        assertEquals(0, t.count(0L))
    }
}

class WhaleRetailBoardTest {

    @Test
    fun `bos veri null`() {
        assertNull(WhaleRetailBoard.evaluate(0.0, 0.0))
    }

    @Test
    fun `balina agirlikli`() {
        val b = WhaleRetailBoard.evaluate(800.0, 200.0)
        assertNotNull(b)
        assertEquals(0.8, b!!.whalePct, 1e-9)
        assertEquals("🐋 BALİNA", b.winner)
        assertEquals("3-0", b.score)
    }

    @Test
    fun `perakende agirlikli`() {
        val b = WhaleRetailBoard.evaluate(200.0, 800.0)!!
        assertEquals(0.2, b.whalePct, 1e-9)
        assertEquals("🐟 PERAKENDE", b.winner)
        assertEquals("0-3", b.score)
    }

    @Test
    fun `dengede`() {
        val b = WhaleRetailBoard.evaluate(500.0, 500.0)!!
        assertEquals(0.5, b.whalePct, 1e-9)
        assertEquals("⚖️ DENGEDE", b.winner)
        assertEquals("1-1", b.score)
    }
}

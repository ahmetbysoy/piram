package com.example

import com.example.domain.engine.MarketMood
import com.example.domain.engine.StoryGenerator
import com.example.domain.engine.StreakStats
import com.example.domain.model.JournalRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketMoodTest {

    @Test
    fun `emoji esikleri`() {
        assertEquals("🚀", MarketMood.emoji(50.0))
        assertEquals("🐂", MarketMood.emoji(20.0))
        assertEquals("😐", MarketMood.emoji(0.0))
        assertEquals("🐻", MarketMood.emoji(-20.0))
        assertEquals("😱", MarketMood.emoji(-50.0))
    }

    @Test
    fun `label esikleri`() {
        assertEquals("FOMO", MarketMood.label(45.0))
        assertEquals("Boğa", MarketMood.label(15.0))
        assertEquals("Kararsız", MarketMood.label(0.0))
        assertEquals("Ayı", MarketMood.label(-15.0))
        assertEquals("Panik", MarketMood.label(-45.0))
    }
}

class StreakStatsTest {

    private fun row(kind: String, price: Double, later15: Double?) =
        JournalRow(kind = kind, price = price, at = 0L, later15 = later15)

    @Test
    fun `bos liste sifir`() {
        val s = StreakStats.fromJournal(emptyList())
        assertEquals(0, s.current)
        assertEquals(0, s.best)
        assertEquals(0, s.total)
        assertEquals(0.0, s.winRate, 1e-9)
    }

    @Test
    fun `later15 olmayanlar sayilmaz`() {
        val s = StreakStats.fromJournal(listOf(row("TOPLAMA", 100.0, null)))
        assertEquals(0, s.total)
    }

    @Test
    fun `uc dogru seri`() {
        // yeniden eskiye: 3 toplama, hepsi yukselmis
        val rows = listOf(
            row("TOPLAMA", 100.0, 105.0),
            row("TOPLAMA", 100.0, 104.0),
            row("TOPLAMA", 100.0, 103.0)
        )
        val s = StreakStats.fromJournal(rows)
        assertEquals(3, s.current)
        assertEquals(3, s.best)
        assertEquals(3, s.total)
        assertEquals(3, s.wins)
    }

    @Test
    fun `seri kayip ile kirilir`() {
        // kronolojik: win, loss, win → aktif seri 1
        val rows = listOf(
            row("TOPLAMA", 100.0, 105.0), // en yeni: win
            row("TOPLAMA", 100.0, 95.0),  // loss
            row("TOPLAMA", 100.0, 103.0)  // en eski: win
        )
        val s = StreakStats.fromJournal(rows)
        assertEquals(1, s.current)
        assertEquals(1, s.best)
        assertEquals(3, s.total)
        assertEquals(2, s.wins)
    }

    @Test
    fun `bosaltma yon degerlendirmesi`() {
        // bosaltma: fiyat duserse isabet
        val s = StreakStats.fromJournal(listOf(row("BOSALTMA", 200.0, 190.0)))
        assertEquals(1, s.wins)
        val s2 = StreakStats.fromJournal(listOf(row("BOSALTMA", 200.0, 210.0)))
        assertEquals(0, s2.wins)
    }

    @Test
    fun `winRate`() {
        val rows = listOf(
            row("TOPLAMA", 100.0, 105.0),
            row("TOPLAMA", 100.0, 95.0)
        )
        val s = StreakStats.fromJournal(rows)
        assertEquals(0.5, s.winRate, 1e-9)
    }
}

class StoryGeneratorTest {

    @Test
    fun `kurumsal agirlikta alis akisi`() {
        val s = StoryGenerator.generate(
            whaleNotional = 800.0,
            retailNotional = 200.0,
            ofi = 0.3,
            burstCount = 2,
            currentPrice = 101.0,
            vwap = 100.0
        )
        assertTrue(s.startsWith("Kanka özet: "))
        assertTrue(s.contains("kurumsal ağırlıkta"))
        assertTrue(s.contains("ALIŞ yönlü"))
        assertTrue(s.contains("2 salvo aktif"))
        assertTrue(s.contains("VWAP üstünde"))
    }

    @Test
    fun `perakende agirlikta satis akisi`() {
        val s = StoryGenerator.generate(
            whaleNotional = 100.0,
            retailNotional = 900.0,
            ofi = -0.4,
            burstCount = 0,
            currentPrice = 99.0,
            vwap = 100.0
        )
        assertTrue(s.contains("perakende ağırlıkta"))
        assertTrue(s.contains("SATIŞ yönlü"))
        assertTrue(s.contains("VWAP altında"))
    }

    @Test
    fun `dengeli ve vwap yok`() {
        val s = StoryGenerator.generate(0.0, 0.0, 0.0, 0, 0.0, 0.0)
        assertTrue(s.contains("dengeli"))
        // vwap 0 → "üstünde/altında" yok, nokta ile biter
        assertTrue(!s.contains("VWAP"))
    }
}

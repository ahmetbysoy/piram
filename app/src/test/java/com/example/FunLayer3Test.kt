package com.example

import com.example.domain.engine.CalmBeforeStorm
import com.example.domain.engine.PainScore
import com.example.domain.engine.PersonalityHistory
import com.example.domain.model.Depth
import com.example.domain.model.DepthLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PainScoreTest {

    @Test
    fun `sell likidasyon long acisi`() {
        val r = PainScore.evaluate(sellLiqNotional = 500_000.0, buyLiqNotional = 50_000.0)
        assertNotNull(r)
        assertTrue(r!!.contains("long'lar"))
    }

    @Test
    fun `buy likidasyon short acisi`() {
        val r = PainScore.evaluate(sellLiqNotional = 50_000.0, buyLiqNotional = 500_000.0)
        assertNotNull(r)
        assertTrue(r!!.contains("short'lar"))
    }

    @Test
    fun `esik alti null`() {
        assertNull(PainScore.evaluate(10_000.0, 5_000.0))
    }
}

class CalmBeforeStormTest {

    @Test
    fun `sikisma + dengesizlik rozet`() {
        // dar fiyat aralığı (sıkışma)
        val prices = (1..20).map { 100.0 + it * 0.001 }
        val depth = Depth(
            bids = listOf(DepthLevel(100.0, 50.0)),
            asks = listOf(DepthLevel(101.0, 5.0)),
            exchange = "X",
            timestamp = 0L
        )
        val r = CalmBeforeStorm.evaluate(prices, depth)
        assertEquals("🌪️ Fırtına öncesi sessizlik", r)
    }

    @Test
    fun `yeterli veri yoksa null`() {
        assertNull(CalmBeforeStorm.evaluate(listOf(1.0, 2.0), null))
        assertNull(CalmBeforeStorm.evaluate(emptyList(), null))
    }

    @Test
    fun `genis aralikta rozet yok`() {
        // Uzun dönem sakin, kısa dönem ani hareket → volRatio yüksek → sıkışma yok
        val prices = (1..14).map { 100.0 } + listOf(90.0, 110.0, 90.0, 110.0, 90.0, 110.0)
        val depth = Depth(
            bids = listOf(DepthLevel(100.0, 50.0)),
            asks = listOf(DepthLevel(101.0, 5.0)),
            exchange = "X",
            timestamp = 0L
        )
        assertNull(CalmBeforeStorm.evaluate(prices, depth))
    }
}

class PersonalityHistoryTest {

    @Test
    fun `ozet en sik etiket`() {
        var now = 0L
        val h = PersonalityHistory(clock = { now })
        h.record("ÇILGIN", 0L)
        h.record("SAKİN", 1000L)
        h.record("ÇILGIN", 2000L)
        now = 3000L
        assertEquals("📅 ÇILGIN×2", h.summaryChip(now))
    }

    @Test
    fun `bos gecmis bos ozet`() {
        val h = PersonalityHistory(clock = { 0L })
        assertEquals("", h.summaryChip(0L))
    }

    @Test
    fun `24 saat sonrasi budanir`() {
        var now = 0L
        val h = PersonalityHistory(clock = { now })
        h.record("AGRESİF", 0L)
        now = 25 * 3_600_000L
        assertEquals("", h.summaryChip(now))
    }
}

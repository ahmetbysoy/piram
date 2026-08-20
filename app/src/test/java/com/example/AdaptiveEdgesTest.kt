package com.example

import com.example.domain.engine.AdaptiveEdges
import com.example.domain.engine.SignalConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveEdgesTest {

    @Test
    fun `yeterli ornek yoksa null`() {
        assertNull(AdaptiveEdges.adaptiveRange(listOf(100.0, 200.0)))
    }

    @Test
    fun `percentile dogru calisir`() {
        assertEquals(200.0, AdaptiveEdges.percentile(listOf(100.0, 200.0, 300.0), 0.5), 0.0)
    }

    @Test
    fun `coin dagilimina gore aralik uyarlanir`() {
        val notionals = (1..100).map { 1000.0 + it * 10.0 } // 1010 .. 2000
        val range = AdaptiveEdges.adaptiveRange(notionals)
        assertNotNull(range)
        assertTrue(range!!.first >= SignalConfig.MIN_NOTIONAL)
        assertTrue(range.second > range.first)
    }

    @Test
    fun `kucuk coinlerde alt sinir korunur`() {
        val notionals = (1..60).map { it * 0.5 } // 0.5 .. 30 USDT — hepsi alt sınırın altında
        val range = AdaptiveEdges.adaptiveRange(notionals)
        assertNotNull(range)
        assertEquals(SignalConfig.MIN_NOTIONAL, range!!.first, 0.0)
    }
}

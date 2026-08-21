package com.example

import com.example.domain.engine.BookProfile
import com.example.domain.model.Depth
import com.example.domain.model.DepthLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookProfileTest {

    private fun depth(bids: List<Pair<Double, Double>>, asks: List<Pair<Double, Double>>): Depth =
        Depth(
            bids = bids.map { DepthLevel(it.first, it.second) },
            asks = asks.map { DepthLevel(it.first, it.second) },
            exchange = "X",
            timestamp = 0L
        )

    @Test
    fun `bos depth null`() {
        assertNull(BookProfile.compute(null))
        assertNull(BookProfile.compute(Depth(emptyList(), emptyList(), "X", 0L)))
    }

    @Test
    fun `dengeli kitap`() {
        val d = depth(
            bids = listOf(100.0 to 5.0, 99.0 to 5.0),
            asks = listOf(101.0 to 5.0, 102.0 to 5.0)
        )
        val p = BookProfile.compute(d)!!
        assertEquals(10.0, p.bidTotal, 1e-9)
        assertEquals(10.0, p.askTotal, 1e-9)
        assertEquals(0.0, p.imbalance, 1e-9)
        assertEquals(100.0, p.bestBid, 1e-9)
        assertEquals(101.0, p.bestAsk, 1e-9)
    }

    @Test
    fun `bid agirlikli kitap pozitif dengesizlik`() {
        val d = depth(
            bids = listOf(100.0 to 9.0),
            asks = listOf(101.0 to 1.0)
        )
        val p = BookProfile.compute(d)!!
        assertTrue(p.imbalance > 0)
        assertEquals(0.8, p.imbalance, 1e-9)
    }

    @Test
    fun `duvar fiyatlari en kalin seviye`() {
        val d = depth(
            bids = listOf(100.0 to 1.0, 98.0 to 50.0),
            asks = listOf(101.0 to 30.0, 105.0 to 1.0)
        )
        val p = BookProfile.compute(d)!!
        assertEquals(98.0, p.bidWallPrice!!, 1e-9)
        assertEquals(101.0, p.askWallPrice!!, 1e-9)
    }

    @Test
    fun `spread degeri tasinir`() {
        val d = depth(listOf(100.0 to 1.0), listOf(101.5 to 1.0))
        val p = BookProfile.compute(d)
        assertNotNull(p)
        assertEquals(1.5, p!!.spread, 1e-9)
    }
}

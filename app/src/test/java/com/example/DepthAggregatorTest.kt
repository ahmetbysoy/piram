package com.example

import com.example.domain.engine.DepthAggregator
import com.example.domain.model.Depth
import com.example.domain.model.DepthLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DepthAggregatorTest {

    private fun depth(
        bids: List<Pair<Double, Double>>,
        asks: List<Pair<Double, Double>>,
        exchange: String = "X"
    ): Depth = Depth(
        bids = bids.map { DepthLevel(it.first, it.second) },
        asks = asks.map { DepthLevel(it.first, it.second) },
        exchange = exchange,
        timestamp = 0L
    )

    @Test
    fun `empty list returns null`() {
        assertNull(DepthAggregator.aggregate(emptyList()))
    }

    @Test
    fun `single venue is sorted best first`() {
        val d = depth(
            bids = listOf(100.0 to 1.0, 102.0 to 2.0),
            asks = listOf(104.0 to 3.0, 103.0 to 1.0)
        )
        val agg = DepthAggregator.aggregate(listOf(d))
        assertNotNull(agg)
        assertEquals(DepthAggregator.AGGREGATED_LABEL, agg!!.exchange)
        assertEquals(102.0, agg.bids.first().price, 0.0)
        assertEquals(103.0, agg.asks.first().price, 0.0)
    }

    @Test
    fun `merges multiple venues and computes mid and spread`() {
        val a = depth(listOf(99.0 to 1.0), listOf(101.0 to 1.0), "A")
        val b = depth(listOf(100.0 to 2.0), listOf(102.0 to 2.0), "B")
        val agg = DepthAggregator.aggregate(listOf(a, b))!!

        assertEquals(100.0, agg.bids.first().price, 0.0) // best bid across venues
        assertEquals(101.0, agg.asks.first().price, 0.0) // best ask across venues
        assertEquals(2, agg.bids.size)
        assertEquals(2, agg.asks.size)
        assertTrue(agg.spread > 0)
        assertTrue(agg.midPrice > 0)
    }

    @Test
    fun `ignores venues with an empty side`() {
        val empty = Depth(emptyList(), emptyList(), "E", 0L)
        val ok = depth(listOf(100.0 to 1.0), listOf(101.0 to 1.0), "OK")
        val agg = DepthAggregator.aggregate(listOf(empty, ok))!!
        assertEquals(1, agg.bids.size)
        assertEquals(1, agg.asks.size)
    }

    @Test
    fun `caps each side at MAX_LEVELS`() {
        val manyBids = (0 until 100).map { (200.0 - it) to 1.0 }
        val manyAsks = (0 until 100).map { (200.0 + it) to 1.0 }
        val agg = DepthAggregator.aggregate(listOf(depth(manyBids, manyAsks)))!!
        assertEquals(DepthAggregator.MAX_LEVELS, agg.bids.size)
        assertEquals(DepthAggregator.MAX_LEVELS, agg.asks.size)
    }
}

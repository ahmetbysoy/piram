package com.example

import com.example.domain.engine.AbsorptionIndex
import com.example.domain.engine.IcebergDetector
import com.example.domain.model.Depth
import com.example.domain.model.DepthLevel
import com.example.domain.model.Order
import com.example.domain.model.OrderSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AbsorptionIndexTest {

    @Test
    fun `az ornek sifir`() {
        assertEquals(0.0, AbsorptionIndex.compute(emptyList(), emptyList()), 1e-9)
    }

    @Test
    fun `yuksek hacim dar aralik yuksek skor`() {
        val trades = (1..30).map {
            Order("$it", OrderSide.BUY, 10.0, 100.0 + (it % 3) * 0.1, System.currentTimeMillis())
        }
        val prices = (1..30).map { 100.0 + (it % 3) * 0.1 }
        val score = AbsorptionIndex.compute(trades, prices)
        assertTrue(score > 0)
    }

    @Test
    fun `genis aralik dusuk skor`() {
        val trades = (1..10).map { Order("$it", OrderSide.BUY, 1.0, 100.0, System.currentTimeMillis()) }
        val prices = (1..20).map { 100.0 + it * 5.0 } // geniş aralık
        val score = AbsorptionIndex.compute(trades, prices)
        assertTrue(score < 0.5)
    }
}

class IcebergDetectorTest {

    private fun depth(bids: List<Pair<Double, Double>>, asks: List<Pair<Double, Double>>) = Depth(
        bids = bids.map { DepthLevel(it.first, it.second) },
        asks = asks.map { DepthLevel(it.first, it.second) },
        exchange = "X",
        timestamp = 0L
    )

    @Test
    fun `tekrar dolan duvar iceberg tespiti`() {
        val d = IcebergDetector(refillThreshold = 3)
        // 1: zirve 1000
        d.detect(depth(listOf(100.0 to 1000.0), emptyList()))
        // 2: düştü (200 < 500)
        d.detect(depth(listOf(100.0 to 200.0), emptyList()))
        // 3: toparlandı (900 >= 800) → refill 1
        d.detect(depth(listOf(100.0 to 900.0), emptyList()))
        // 4-5: aynı döngü
        d.detect(depth(listOf(100.0 to 200.0), emptyList()))
        d.detect(depth(listOf(100.0 to 900.0), emptyList()))
        // 6-7: üçüncü refill
        d.detect(depth(listOf(100.0 to 200.0), emptyList()))
        val hits = d.detect(depth(listOf(100.0 to 900.0), emptyList()))
        assertTrue(hits.isNotEmpty())
        assertEquals("BID", hits.first().side)
        assertEquals(100.0, hits.first().price, 1e-9)
    }

    @Test
    fun `bos depth bos liste`() {
        val d = IcebergDetector()
        assertEquals(0, d.detect(null).size)
        assertEquals(0, d.detect(Depth(emptyList(), emptyList(), "X", 0L)).size)
    }

    @Test
    fun `dolu ama kucuk dalgalanma iceberg degil`() {
        val d = IcebergDetector(refillThreshold = 3)
        repeat(6) {
            d.detect(depth(listOf(100.0 to 900.0), emptyList()))
        }
        assertEquals(0, d.detect(depth(listOf(100.0 to 950.0), emptyList())).size)
    }
}

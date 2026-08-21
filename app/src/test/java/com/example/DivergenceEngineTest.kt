package com.example

import com.example.domain.engine.DivergenceEngine
import com.example.domain.engine.DivergenceKind
import com.example.domain.model.LayerAggregate
import org.junit.Assert.assertEquals
import org.junit.Test

class DivergenceEngineTest {

    private fun layer(idx: Int, buy: Double, sell: Double): LayerAggregate {
        val total = buy + sell
        val ratio = if (total > 0) (buy / total).toFloat() else 0.5f
        return LayerAggregate(
            layerIndex = idx,
            minNotional = 0.0,
            maxNotional = 0.0,
            notional = total,
            displayNotional = total,
            buyNotional = buy,
            sellNotional = sell,
            orderCount = 1,
            buyRatio = ratio,
            isWhaleTier = idx >= 6,
            label = "L$idx"
        )
    }

    @Test
    fun `toplama - büyükler alış küçükler satış fiyat düşüyor`() {
        val layers = listOf(
            layer(0, 0.0, 500.0),
            layer(1, 0.0, 400.0),
            layer(2, 0.0, 300.0),
            layer(3, 0.0, 0.0),
            layer(4, 0.0, 0.0),
            layer(5, 0.0, 0.0),
            layer(6, 800.0, 0.0),
            layer(7, 1200.0, 0.0)
        )
        val sig = DivergenceEngine.evaluate(layers, priceChangePct = -1.0, oiDelta = null)
        assertEquals(DivergenceKind.TOPLAMA, sig.kind)
        assertEquals(true, sig.yazi.contains("toplama"))
    }

    @Test
    fun `bosaltma - küçükler kovalıyor büyükler satış fiyat yükseliyor`() {
        val layers = listOf(
            layer(0, 500.0, 0.0),
            layer(1, 400.0, 0.0),
            layer(2, 300.0, 0.0),
            layer(3, 0.0, 0.0),
            layer(4, 0.0, 0.0),
            layer(5, 0.0, 0.0),
            layer(6, 0.0, 800.0),
            layer(7, 0.0, 1200.0)
        )
        val sig = DivergenceEngine.evaluate(layers, priceChangePct = +1.0, oiDelta = null)
        assertEquals(DivergenceKind.BOSALTMA, sig.kind)
        assertEquals(true, sig.yazi.contains("boşaltma"))
    }

    @Test
    fun `hacim eşiğin altındaysa sinyal yok`() {
        val layers = listOf(
            layer(0, 1.0, 0.0),
            layer(6, 0.0, 1.0)
        )
        val sig = DivergenceEngine.evaluate(layers, priceChangePct = -5.0, oiDelta = null)
        assertEquals(DivergenceKind.YOK, sig.kind)
    }

    @Test
    fun `numLayers degisince indeksler turetilir (10 katman)`() {
        // 10 katman: whale 8-9, retail 0-3 (bottomTo = 10*2/5 = 4)
        val layers = listOf(
            layer(0, 0.0, 500.0),
            layer(1, 0.0, 400.0),
            layer(2, 0.0, 300.0),
            layer(3, 0.0, 200.0),
            layer(4, 0.0, 0.0),
            layer(5, 0.0, 0.0),
            layer(6, 0.0, 0.0),
            layer(7, 0.0, 0.0),
            layer(8, 800.0, 0.0),
            layer(9, 1200.0, 0.0)
        )
        val sig = DivergenceEngine.evaluate(layers, priceChangePct = -1.0, oiDelta = null, numLayers = 10)
        assertEquals(DivergenceKind.TOPLAMA, sig.kind)
    }
}

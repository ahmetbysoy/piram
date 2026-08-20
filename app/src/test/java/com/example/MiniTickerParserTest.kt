package com.example

import com.example.domain.model.MiniTickerParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MiniTickerParserTest {

    @Test
    fun `miniTicker dizisi parse edilir`() {
        val json = """
            [
              {"e":"24hrMiniTicker","s":"BTCUSDT","c":"65000.00","o":"63000.00","q":"1500000000"},
              {"e":"24hrMiniTicker","s":"ETHUSDT","c":"3200.00","o":"3400.00","q":"900000000"}
            ]
        """.trimIndent()
        val rows = MiniTickerParser.parseArray(json)
        assertEquals(2, rows.size)
        assertEquals("BTCUSDT", rows[0].symbol)
        assertEquals(65000.0, rows[0].last, 1e-9)
        // P yok → (c-o)/o*100
        assertEquals((65000.0 - 63000.0) / 63000.0 * 100.0, rows[0].changePct, 1e-6)
        assertEquals(1_500_000_000.0, rows[0].quoteVol, 1e-9)
        // ETH: düşüş
        assertTrue(rows[1].changePct < 0)
    }

    @Test
    fun `P alani varsa direkt kullanilir`() {
        val json = """[{"s":"SOLUSDT","c":"150.0","o":"140.0","P":"7.14"}]"""
        val rows = MiniTickerParser.parseArray(json)
        assertEquals(1, rows.size)
        assertEquals(7.14, rows[0].changePct, 1e-6)
    }

    @Test
    fun `bozuk JSON bos liste`() {
        assertEquals(0, MiniTickerParser.parseArray("garbage").size)
    }

    @Test
    fun `sembol olmayan satirlar atlanir`() {
        val json = """[{"c":"100.0","o":"99.0"},{"s":"BTCUSDT","c":"100.0","o":"99.0"}]"""
        val rows = MiniTickerParser.parseArray(json)
        assertEquals(1, rows.size)
        assertEquals("BTCUSDT", rows[0].symbol)
    }
}

package com.example

import com.example.domain.model.Ticker24hParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Ticker24hParserTest {

    @Test
    fun `gecerli JSON parse`() {
        val json = """{"symbol":"BTCUSDT","priceChange":"1234.5","priceChangePercent":"2.100","weightedAvgPrice":"59999.0","lastPrice":"60200.00","lastQty":"1.2","openPrice":"58963.5","highPrice":"61123.4","lowPrice":"58000.1","volume":"45000.0","quoteVolume":"2710000000.0"}"""
        val t = Ticker24hParser.parse(json)
        assertNotNull(t)
        assertEquals("BTCUSDT", t!!.symbol)
        assertEquals(60200.0, t.lastPrice, 1e-9)
        assertEquals(61123.4, t.highPrice, 1e-9)
        assertEquals(58000.1, t.lowPrice, 1e-9)
        assertEquals(2.1, t.changePct, 1e-9)
        assertEquals(2_710_000_000.0, t.quoteVolume, 1e-9)
    }

    @Test
    fun `pct yoksa open dan hesaplanir`() {
        val json = """{"symbol":"BTCUSDT","lastPrice":"105.0","openPrice":"100.0","highPrice":"110.0","lowPrice":"95.0","volume":"1.0","quoteVolume":"100.0"}"""
        val t = Ticker24hParser.parse(json)!!
        assertEquals(5.0, t.changePct, 1e-9)
    }

    @Test
    fun `bozuk JSON null`() {
        assertNull(Ticker24hParser.parse("garbage"))
    }

    @Test
    fun `eksik lastPrice null`() {
        assertNull(Ticker24hParser.parse("""{"symbol":"BTCUSDT"}"""))
    }

    @Test
    fun `negatif degisim`() {
        val json = """{"symbol":"ETHUSDT","lastPrice":"90.0","openPrice":"100.0","highPrice":"110.0","lowPrice":"85.0","volume":"1.0","quoteVolume":"90.0"}"""
        val t = Ticker24hParser.parse(json)!!
        assertTrue(t.changePct < 0)
        assertEquals(-10.0, t.changePct, 1e-9)
    }
}

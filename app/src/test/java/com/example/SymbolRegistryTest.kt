package com.example

import com.example.domain.SymbolRegistry
import com.example.domain.model.SymbolMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolRegistryTest {

    @Test
    fun `tohum liste ilk basta devrede`() {
        val reg = SymbolRegistry()
        assertTrue(reg.symbols().isNotEmpty())
        assertNotNull(reg.get("BTCUSDT"))
    }

    @Test
    fun `resolve ETH ve BTC kisa isimleri`() {
        val reg = SymbolRegistry()
        assertEquals("ETHUSDT", reg.resolve("ETH"))
        assertEquals("BTCUSDT", reg.resolve("BTC"))
        assertEquals("BTCUSDT", reg.resolve("btcusdt"))
    }

    @Test
    fun `resolve bilinmeyen sembol USDT ekler`() {
        val reg = SymbolRegistry()
        assertEquals("FOOUSDT", reg.resolve("FOO"))
        // usdt bitiyorsa aynen
        assertEquals("FOOUSDT", reg.resolve("FOOUSDT"))
    }

    @Test
    fun `tickDecimals dogru hesaplanir`() {
        val reg = SymbolRegistry()
        assertEquals(2, reg.tickDecimals("BTCUSDT"))
        assertEquals(8, reg.tickDecimals("PEPEUSDT"))
        assertNull(reg.tickDecimals("BILINMEYENXXX"))
    }

    @Test
    fun `ingest tohum listeyi ezer ve arama calisir`() {
        val reg = SymbolRegistry()
        reg.ingest(listOf(
            SymbolMeta("BTCUSDT", "BTC", "0.01", "0.00001"),
            SymbolMeta("ETHUSDT", "ETH", "0.01", "0.0001"),
            SymbolMeta("ZZZUSDT", "ZZZ", "0.001", "0.1")
        ))
        assertTrue(reg.loaded)
        // seed'de olmayan eklendi
        assertNotNull(reg.get("ZZZUSDT"))
        // search rank: exact/baslangic
        val res = reg.search("ETH")
        assertEquals("ETHUSDT", res.first().symbol)
        // seed'deki PEPE artık yok (ezildi)
        assertNull(reg.get("PEPEUSDT"))
    }

    @Test
    fun `bos sorguda populerler doner`() {
        val reg = SymbolRegistry()
        val res = reg.search("")
        assertTrue(res.isNotEmpty())
        assertTrue(res.any { it.symbol.startsWith("BTC") })
    }

    @Test
    fun `symbolMeta tickDecimals`() {
        assertEquals(0, SymbolMeta("X", "X", "1", "1").tickDecimals())
        assertEquals(4, SymbolMeta("X", "X", "0.0001", "1").tickDecimals())
    }
}

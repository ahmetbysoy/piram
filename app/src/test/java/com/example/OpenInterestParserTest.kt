package com.example

import com.example.domain.model.OiState
import com.example.domain.model.OpenInterestParser
import com.example.domain.model.formatOi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OpenInterestParserTest {

    @Test
    fun `gecerli JSON parse edilir`() {
        val snap = OpenInterestParser.parse("""{"openInterest":"78556.472","symbol":"BTCUSDT","time":1724000000000}""")
        assertNotNull(snap)
        assertEquals("BTCUSDT", snap!!.symbol)
        assertEquals(78556.472, snap.oi, 1e-6)
    }

    @Test
    fun `bozuk JSON null`() {
        assertNull(OpenInterestParser.parse("not json at all"))
    }

    @Test
    fun `eksik alan null`() {
        assertNull(OpenInterestParser.parse("""{"symbol":"BTCUSDT"}"""))
    }

    @Test
    fun `kontrat carpi fiyat USDT`() {
        assertEquals(2000.0, OpenInterestParser.oiToUsdt(100.0, 20.0)!!, 1e-9)
        assertNull(OpenInterestParser.oiToUsdt(null, 20.0))
        assertNull(OpenInterestParser.oiToUsdt(100.0, 0.0))
    }

    @Test
    fun `oi formati`() {
        assertEquals("1.20B", formatOi(1_200_000_000.0))
        assertEquals("345.6M", formatOi(345_600_000.0))
        assertEquals("12.3K", formatOi(12_300.0))
    }

    @Test
    fun `oi state baslangici bekliyor`() {
        assertEquals(OiState.BEKLIYOR, OiState.valueOf("BEKLIYOR"))
    }
}

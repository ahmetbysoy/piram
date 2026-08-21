package com.example

import com.example.domain.engine.strategy.ExchangeLeadLagStrategy
import com.example.domain.engine.strategy.FundingRateSqueezeStrategy
import com.example.domain.model.FundingParser
import com.example.domain.model.MarketSnapshot
import com.example.domain.model.SignalType
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
class FundingParserTest {

    @Test
    fun `premiumIndex parse`() {
        val json = """{"symbol":"BTCUSDT","markPrice":"60000.0","lastFundingRate":"0.00010000","nextFundingTime":1724000000000}"""
        val snap = FundingParser.parse(json)
        assertNotNull(snap)
        assertEquals("BTCUSDT", snap!!.symbol)
        assertEquals(0.0001, snap.lastFundingRate, 1e-12)
    }

    @Test
    fun `bozuk JSON null`() {
        assertNull(FundingParser.parse("garbage"))
    }

    @Test
    fun `eksik alan null`() {
        assertNull(FundingParser.parse("""{"symbol":"BTCUSDT"}"""))
    }
}

class FundingRateSqueezeStrategyTest {

    private fun snapshot(funding: Double?, oiDelta: Double?) = MarketSnapshot(
        symbol = "BTCUSDT", currentPrice = 60_000.0, fundingRate = funding, oiDelta = oiDelta
    )

    @Test
    fun `funding yoksa notr`() {
        val r = FundingRateSqueezeStrategy().execute(snapshot(null, null))
        assertEquals(SignalType.NEUTRAL, r.signal)
    }

    @Test
    fun `normal funding notr`() {
        val r = FundingRateSqueezeStrategy().execute(snapshot(0.00001, 100.0))
        assertEquals(SignalType.NEUTRAL, r.signal)
    }

    @Test
    fun `pozitif funding + OI artisi long squeeze sell`() {
        val r = FundingRateSqueezeStrategy().execute(snapshot(0.0008, 500.0))
        assertTrue(r.score < 0)
        assertTrue(r.reasoning.contains("long squeeze"))
    }

    @Test
    fun `negatif funding + OI artisi short squeeze buy`() {
        val r = FundingRateSqueezeStrategy().execute(snapshot(-0.0008, 500.0))
        assertTrue(r.score > 0)
        assertTrue(r.reasoning.contains("short squeeze"))
    }

    @Test
    fun `asiri funding ama OI dusuyor notr`() {
        val r = FundingRateSqueezeStrategy().execute(snapshot(0.0008, -500.0))
        assertEquals(SignalType.NEUTRAL, r.signal)
    }
}

class ExchangeLeadLagStrategyTest {

    @Test
    fun `yetersiz veri notr`() {
        val snap = MarketSnapshot(symbol = "BTCUSDT", currentPrice = 60_000.0)
        assertEquals(SignalType.NEUTRAL, ExchangeLeadLagStrategy().execute(snap).signal)
    }

    @Test
    fun `lider yukari onculuk buy`() {
        val snap = MarketSnapshot(
            symbol = "BTCUSDT",
            currentPrice = 60_000.0,
            exchangePrices = mapOf("Binance" to 60_200.0, "Bybit" to 60_000.0, "OKX" to 60_000.0),
            venueTimes = mapOf("Binance" to 1000L, "Bybit" to 900L, "OKX" to 800L)
        )
        val r = ExchangeLeadLagStrategy().execute(snap)
        assertTrue(r.score > 0)
        assertTrue(r.reasoning.contains("Binance"))
    }

    @Test
    fun `lider asagi onculuk sell`() {
        val snap = MarketSnapshot(
            symbol = "BTCUSDT",
            currentPrice = 60_000.0,
            exchangePrices = mapOf("Binance" to 59_800.0, "Bybit" to 60_000.0),
            venueTimes = mapOf("Binance" to 1000L, "Bybit" to 900L)
        )
        val r = ExchangeLeadLagStrategy().execute(snap)
        assertTrue(r.score < 0)
    }

    @Test
    fun `esit fiyat notr`() {
        val snap = MarketSnapshot(
            symbol = "BTCUSDT",
            currentPrice = 60_000.0,
            exchangePrices = mapOf("Binance" to 60_000.0, "Bybit" to 60_000.0),
            venueTimes = mapOf("Binance" to 1000L, "Bybit" to 900L)
        )
        val r = ExchangeLeadLagStrategy().execute(snap)
        assertEquals(SignalType.NEUTRAL, r.signal)
    }
}

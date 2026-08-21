package com.example

import com.example.domain.engine.strategy.SignalExplainer
import com.example.domain.engine.strategy.StrategyCorrelation
import com.example.domain.model.SignalType
import com.example.domain.model.StrategyResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalExplainerTest {

    private fun result(signal: SignalType, confidence: Double, score: Double, reason: String) = StrategyResult(
        strategyId = "s", name = "S", signal = signal, confidence = confidence, score = score, reasoning = reason
    )

    @Test
    fun `alis aciklamasi`() {
        val s = SignalExplainer.explain(result(SignalType.BUY, 0.65, 0.4, "EMA9 > EMA21"))
        assertTrue(s.startsWith("ALIŞ (%65)"))
        assertTrue(s.contains("EMA9 > EMA21"))
    }

    @Test
    fun `guclu satis aciklamasi`() {
        val s = SignalExplainer.explain(result(SignalType.STRONG_SELL, 0.9, -0.8, "LIQ SWEEP"))
        assertTrue(s.startsWith("güçlü SATIŞ (%90)"))
    }

    @Test
    fun `notr aciklamasi`() {
        val s = SignalExplainer.explain(result(SignalType.NEUTRAL, 0.5, 0.0, "Standby"))
        assertTrue(s.startsWith("NÖTR (%50)"))
    }
}

class StrategyCorrelationTest {

    private fun historyOf(vararg pairs: Pair<String, List<Int>>): Map<String, List<Int>> =
        pairs.toMap()

    @Test
    fun `kisa gecmis ceza yok`() {
        val h = historyOf("a" to listOf(1, -1, 1))
        assertEquals(1.0, StrategyCorrelation.penalty(h, "a"), 1e-9)
    }

    @Test
    fun `bilinmeyen id ceza yok`() {
        val h = historyOf("a" to (1..20).map { 1 })
        assertEquals(1.0, StrategyCorrelation.penalty(h, "x"), 1e-9)
    }

    @Test
    fun `tam uyumlu ikiliye ceza`() {
        val votes = (1..20).map { if (it % 2 == 0) 1 else -1 }
        val h = historyOf("a" to votes, "b" to votes)
        assertEquals(StrategyCorrelation.PENALTY, StrategyCorrelation.penalty(h, "a"), 1e-9)
        assertEquals(StrategyCorrelation.PENALTY, StrategyCorrelation.penalty(h, "b"), 1e-9)
    }

    @Test
    fun `zit stratejiler ceza yok`() {
        val a = (1..20).map { 1 }
        val b = (1..20).map { -1 }
        val h = historyOf("a" to a, "b" to b)
        assertEquals(1.0, StrategyCorrelation.penalty(h, "a"), 1e-9)
    }

    @Test
    fun `notr oylar eslesme sayilmaz`() {
        val a = listOf(1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0)
        val b = listOf(1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0)
        val h = historyOf("a" to a, "b" to b)
        // 10 non-neutral oy, hepsi uyumlu → ceza
        assertEquals(StrategyCorrelation.PENALTY, StrategyCorrelation.penalty(h, "a"), 1e-9)
    }

    @Test
    fun `esik alti uyum ceza yok`() {
        val a = (1..20).map { 1 }
        // b: 20 oyun 17'si aynı (%85), 3'ü zıt
        val b = listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -1, -1, -1)
        val h = historyOf("a" to a, "b" to b)
        // %85 tam eşik üstü değil (>0.85 gerekli) → ceza yok
        assertEquals(1.0, StrategyCorrelation.penalty(h, "a"), 1e-9)
    }
}

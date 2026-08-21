package com.example.domain.engine.strategy

import com.example.domain.model.SignalType
import com.example.domain.model.StrategyResult

/**
 * #9 explainSignal — bir strateji sonucunu insan diline çevirir:
 * yön + güven + teknik gerekçe tek satırda ("ALIŞ (%65) · EMA9: ..., VWAP: ...").
 * "Kara kutu" sinyalini açıklanabilir yapar. Saf Kotlin, test edilebilir.
 */
object SignalExplainer {

    fun explain(result: StrategyResult): String {
        val yon = when (result.signal) {
            SignalType.STRONG_BUY -> "güçlü ALIŞ"
            SignalType.BUY -> "ALIŞ"
            SignalType.STRONG_SELL -> "güçlü SATIŞ"
            SignalType.SELL -> "SATIŞ"
            SignalType.NEUTRAL -> "NÖTR"
        }
        val conf = (result.confidence * 100).toInt()
        return "$yon (%$conf) · ${result.reasoning}"
    }
}

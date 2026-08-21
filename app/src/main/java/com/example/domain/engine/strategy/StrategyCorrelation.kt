package com.example.domain.engine.strategy

/**
 * #8 StrategyCorrelationMatrix (pratik hali) — strateji oy geçmişinden benzerlik tespiti.
 *
 * İki strateji aynı yönü sürekli birlikte oyluyorsa (uyum oranı > [threshold]) bunlar
 * birbirine fazla benziyor demektir; [PENALTY] ile ağırlığı hafif düşürülür (redundancy).
 * Nötr oylar (0) eşleşme hesabına katılmaz. Saf Kotlin, test edilebilir.
 */
object StrategyCorrelation {

    const val PENALTY = 0.7

    /**
     * [history]: strategyId → kronolojik yön oy listesi (1 = alış, -1 = satış, 0 = nötr).
     * Dönen değer çarpan olarak ağırlığa uygulanır (1.0 = ceza yok).
     */
    fun penalty(
        history: Map<String, List<Int>>,
        id: String,
        minSamples: Int = 10,
        threshold: Double = 0.85
    ): Double {
        val mine = history[id] ?: return 1.0
        if (mine.size < minSamples) return 1.0

        var maxAgree = 0.0
        for ((otherId, other) in history) {
            if (otherId == id || other.size < minSamples) continue
            val n = minOf(mine.size, other.size)
            var agree = 0
            var cnt = 0
            for (i in 0 until n) {
                val a = mine[mine.size - n + i]
                val b = other[other.size - n + i]
                if (a != 0 && b != 0) {   // nötr oylar sayılmaz
                    cnt++
                    if (a == b) agree++
                }
            }
            if (cnt >= minSamples) {
                val rate = agree.toDouble() / cnt
                if (rate > maxAgree) maxAgree = rate
            }
        }
        return if (maxAgree > threshold) PENALTY else 1.0
    }
}

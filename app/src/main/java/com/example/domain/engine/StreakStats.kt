package com.example.domain.engine

import com.example.domain.model.JournalRow

/**
 * #15 StreakTracker — sinyal günlüğünden art arda doğru tahmin serisi.
 * `later15` dolu satırlar sonuçlanmış sayılır:
 * TOPLAMA → fiyat yükselmeli, BOŞALTMA → fiyat düşmeli.
 * Saf fonksiyon (state'siz), test edilebilir.
 */
data class StreakSummary(
    val current: Int,   // şu anki aktif seri
    val best: Int,      // en uzun seri
    val total: Int,     // sonuçlanmış toplam sinyal
    val wins: Int
) {
    val winRate: Double get() = if (total > 0) wins.toDouble() / total else 0.0
}

object StreakStats {

    fun fromJournal(rows: List<JournalRow>): StreakSummary {
        val resolved = rows.filter { it.later15 != null }
        var total = 0
        var wins = 0
        var current = 0
        var best = 0

        // rows en-yeniden eskiye; seri için kronolojik (eskiden yeniye) geç
        for (row in resolved.asReversed()) {
            val later = row.later15!!
            val win = if (row.kind == "TOPLAMA") later >= row.price else later < row.price
            total++
            if (win) {
                wins++
                current++
                if (current > best) best = current
            } else {
                current = 0
            }
        }
        return StreakSummary(current, best, total, wins)
    }
}

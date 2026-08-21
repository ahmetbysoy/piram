package com.example.domain.engine

/**
 * #11 MarketMoodEmoji — konsensüs gücüne göre piyasa ruh hali (emoji + etiket).
 * Saf Kotlin, test edilebilir.
 */
object MarketMood {

    fun emoji(strength: Double): String = when {
        strength >= 45.0 -> "🚀"
        strength >= 15.0 -> "🐂"
        strength <= -45.0 -> "😱"
        strength <= -15.0 -> "🐻"
        else -> "😐"
    }

    fun label(strength: Double): String = when {
        strength >= 45.0 -> "FOMO"
        strength >= 15.0 -> "Boğa"
        strength <= -45.0 -> "Panik"
        strength <= -15.0 -> "Ayı"
        else -> "Kararsız"
    }
}

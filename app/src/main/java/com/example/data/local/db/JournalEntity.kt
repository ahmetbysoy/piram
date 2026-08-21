package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Toplama/boşaltma sinyal günlüğü (piramit `signalJournal` portu).
 * `later5/15/60`: sinyalden 5/15/60 dakika sonraki fiyat (isabet hesabı).
 */
@Entity(tableName = "journal")
data class JournalEntity(
    @PrimaryKey
    val id: Long,          // sinyal anı (ms)
    val symbol: String,
    val kind: String,      // TOPLAMA / BOSALTMA
    val price: Double,
    val at: Long,
    val later5: Double? = null,
    val later15: Double? = null,
    val later60: Double? = null
)

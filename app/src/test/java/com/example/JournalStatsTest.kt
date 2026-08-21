package com.example

import com.example.domain.model.JournalRow
import com.example.domain.model.journalHitRate
import org.junit.Assert.assertEquals
import org.junit.Test

class JournalStatsTest {

    @Test
    fun `later15 yoksa sayilmaz`() {
        val rows = listOf(
            JournalRow(kind = "TOPLAMA", price = 100.0, at = 1L)
        )
        assertEquals(0 to 0, journalHitRate(rows))
    }

    @Test
    fun `toplama yukselince isabet`() {
        val rows = listOf(
            JournalRow(kind = "TOPLAMA", price = 100.0, at = 1L, later15 = 105.0),
            JournalRow(kind = "BOSALTMA", price = 200.0, at = 2L, later15 = 190.0)
        )
        assertEquals(2 to 2, journalHitRate(rows))
    }

    @Test
    fun `yanlis yon isabetsiz`() {
        val rows = listOf(
            JournalRow(kind = "TOPLAMA", price = 100.0, at = 1L, later15 = 95.0),
            JournalRow(kind = "BOSALTMA", price = 200.0, at = 2L, later15 = 210.0)
        )
        assertEquals(2 to 0, journalHitRate(rows))
    }

    @Test
    fun `karisik`() {
        val rows = listOf(
            JournalRow(kind = "TOPLAMA", price = 100.0, at = 1L, later15 = 105.0), // ok
            JournalRow(kind = "TOPLAMA", price = 100.0, at = 2L, later15 = 95.0),  // yanlis
            JournalRow(kind = "BOSALTMA", price = 200.0, at = 3L)                  // later yok
        )
        assertEquals(2 to 1, journalHitRate(rows))
    }
}

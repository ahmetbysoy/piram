package com.example.presentation.screens.pyramid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.BorderDark
import com.example.core.theme.BuyGreen
import com.example.core.theme.CardDark
import com.example.core.theme.SellRed
import com.example.core.theme.TextMuted
import com.example.core.theme.TextPrimary
import com.example.core.theme.WhaleGold
import com.example.core.util.MathUtils
import com.example.domain.model.JournalRow
import com.example.domain.model.journalHitRate

/**
 * Sinyal günlüğü: toplama/boşaltma kayıtları + later15 isabet oranı.
 * Her zaman 3 satırlık sabit yükseklik tutar (eksik satırlar placeholder) —
 * yeni sinyal gelince layout asla zıplamaz.
 */
@Composable
fun SignalJournalCard(
    journal: List<JournalRow>,
    modifier: Modifier = Modifier
) {
    val (n, ok) = journalHitRate(journal)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("signal_journal_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SİNYAL GÜNLÜĞÜ",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = TextMuted
            )
            if (n > 0) {
                Text(
                    text = "İsabet: $ok/$n (${"%.0f".format(ok.toDouble() / n * 100)}%)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = WhaleGold
                )
            }
        }

        val rows = journal.take(3)
        rows.forEach { row ->
            val isToplama = row.kind == "TOPLAMA"
            val color = if (isToplama) BuyGreen else SellRed
            val later = row.later15
            val laterStr = if (later != null) {
                val hit = if (isToplama) later >= row.price else later < row.price
                val arrow = if (hit) "✓" else "✗"
                "$arrow ${MathUtils.formatPrice(later)}"
            } else {
                "bekliyor"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .height(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = if (isToplama) "TOPLAMA" else "BOŞALTMA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "@ ${MathUtils.formatPrice(row.price)}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
                Text(
                    text = "15dk: $laterStr",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }
        }

        // Sabit 3 satır: eksikleri placeholder ile doldur (kart yüksekliği asla değişmez)
        repeat((3 - rows.size).coerceAtLeast(0)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .height(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "—",
                    fontSize = 10.sp,
                    color = TextMuted.copy(alpha = 0.3f)
                )
            }
        }
    }
}

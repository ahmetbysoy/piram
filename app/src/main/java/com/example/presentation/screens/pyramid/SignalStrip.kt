package com.example.presentation.screens.pyramid

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.CardDark
import com.example.core.theme.NeonCyan
import com.example.core.theme.SellRed
import com.example.core.theme.SurfaceDark
import com.example.core.theme.TextMuted
import com.example.core.theme.WhaleGold

/** Sinyal çipi: metin + renk. */
private data class Chip(val text: String, val color: Color)

/**
 * Sinyal Şeridi — MoodStrip'in yeniden tasarımı.
 * 6 bilgi sinyalini ÖNCELİK sırasına göre teke indirir:
 * calmStorm > pain > mtf > personality > streak > nextCandle.
 * En kritik olan tek chip olarak gösterilir; diğerleri "+N daha" ile açılır.
 */
@Composable
fun SignalStrip(
    calmStormYazi: String = "",
    painYazi: String = "",
    mtfYazi: String = "",
    personalityChip: String = "",
    personalitySummary: String = "",
    streakCurrent: Int = 0,
    nextCandleChip: String = "",
    quiet: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val chips = buildList {
        if (calmStormYazi.isNotBlank()) add(Chip(calmStormYazi, TextMuted))
        if (painYazi.isNotBlank()) add(Chip(painYazi, SellRed))
        if (mtfYazi.isNotBlank()) add(Chip(mtfYazi, WhaleGold))
        if (personalityChip.isNotBlank()) add(Chip(personalityChip, TextMuted))
        if (personalitySummary.isNotBlank()) add(Chip(personalitySummary, TextMuted))
        if (streakCurrent >= 2) add(Chip("🔥 $streakCurrent seri", WhaleGold))
        if (nextCandleChip.isNotBlank()) add(Chip(nextCandleChip, NeonCyan))
    }
    val allChips = if (chips.isEmpty() && quiet) listOf(Chip("🌫️ Sessizlik", TextMuted)) else chips

    if (allChips.isEmpty()) return

    val primary = allChips.first()
    val extra = allChips.drop(1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(200))
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("signal_strip")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChipBadge(primary)
            if (extra.isNotEmpty()) {
                Text(
                    text = if (expanded) "gizle" else "+${extra.size} daha",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceDark)
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("signal_strip_more")
                )
            }
        }

        if (expanded && extra.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                extra.forEach { chip -> ChipBadge(chip) }
            }
        }
    }
}

@Composable
private fun ChipBadge(chip: Chip) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceDark)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = chip.text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = chip.color
        )
    }
}

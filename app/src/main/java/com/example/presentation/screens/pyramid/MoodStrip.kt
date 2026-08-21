package com.example.presentation.screens.pyramid

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.BuyGreen
import com.example.core.theme.CardDark
import com.example.core.theme.SellRed
import com.example.core.theme.SurfaceDark
import com.example.core.theme.TextMuted
import com.example.core.theme.TextSecondary
import com.example.core.theme.WhaleGold
import com.example.domain.engine.MarketMood
import com.example.domain.engine.MarketPersonality
import com.example.domain.engine.StoryGenerator
import com.example.domain.engine.StreakStats
import com.example.domain.model.JournalRow

/**
 * Ruh hali şeridi (#11 + #13 + #15): konsensüs emojisi, seri sayacı ve
 * "kanka özeti" cümlesi tek kompakt kartta. Sabit yükseklik — layout zıplamaz.
 */
@Composable
fun MoodStrip(
    consensusStrength: Double,
    journal: List<JournalRow>,
    whaleNotional: Double,
    retailNotional: Double,
    burstCount: Int,
    orderFlowImbalance: Double,
    currentPrice: Double,
    vwap: Double,
    changePct: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val emoji = MarketMood.emoji(consensusStrength)
    val label = MarketMood.label(consensusStrength)
    val moodColor = when {
        consensusStrength >= 15.0 -> BuyGreen
        consensusStrength <= -15.0 -> SellRed
        else -> TextSecondary
    }
    val streak = StreakStats.fromJournal(journal)
    val story = StoryGenerator.generate(
        whaleNotional = whaleNotional,
        retailNotional = retailNotional,
        ofi = orderFlowImbalance,
        burstCount = burstCount,
        currentPrice = currentPrice,
        vwap = vwap
    )
    val quiet = (whaleNotional + retailNotional) < 500.0 && burstCount == 0
    val total = whaleNotional + retailNotional
    val whalePct = if (total > 0) whaleNotional / total * 100.0 else 0.0
    val personality = MarketPersonality.evaluate(
        whalePct = whalePct,
        burstCount = burstCount,
        changePct = changePct,
        ofi = orderFlowImbalance
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("mood_strip")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = emoji, fontSize = 16.sp)
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = moodColor,
                letterSpacing = 0.5.sp
            )

            if (streak.current >= 2) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceDark)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🔥 ${streak.current} seri",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = WhaleGold
                    )
                }
            }

            // Kişilik etiketi (#20)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${personality.second} ${personality.first}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }

            if (quiet) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceDark)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🌫️ Fırtına öncesi sessizlik",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }
            }
        }

        Text(
            text = story,
            fontSize = 11.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .testTag("mood_story")
        )
    }
}

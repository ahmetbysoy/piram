package com.example.presentation.screens.pyramid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.example.core.theme.BuyGreen
import com.example.core.theme.CardDark
import com.example.core.theme.SellRed
import com.example.core.theme.TextMuted
import com.example.core.theme.TextPrimary
import com.example.core.theme.WhaleGold
import com.example.core.util.MathUtils
import com.example.domain.engine.WhaleRetailBoard

/**
 * #19 WhaleVsRetailScoreboard — kurumsal vs perakende skor tablosu (spor skoru gibi).
 * Sabit yükseklik: kurumsal/perakende her zaman görünür, layout zıplamaz.
 */
@Composable
fun ScoreboardBar(
    whaleNotional: Double,
    retailNotional: Double,
    modifier: Modifier = Modifier
) {
    val board = WhaleRetailBoard.evaluate(whaleNotional, retailNotional)
    val whaleBar = board?.whalePct?.toFloat()?.coerceIn(0.02f, 0.98f) ?: 0.5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardDark)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("scoreboard_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🐋 ${MathUtils.formatUsd(whaleNotional)}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = WhaleGold
            )
            Text(
                text = board?.score ?: "0-0",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "🐟 ${MathUtils.formatUsd(retailNotional)}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
        }

        // Tug-of-war çubuğu (yeşil = kurumsal, kırmızı = perakende)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(whaleBar)
                    .height(6.dp)
                    .background(BuyGreen)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f - whaleBar)
                    .height(6.dp)
                    .background(SellRed)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "KURUMSAL ${"%.0f".format((board?.whalePct ?: 0.5) * 100)}%",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = BuyGreen
            )
            Text(
                text = board?.winner ?: "⚖️",
                fontSize = 8.sp,
                color = TextMuted
            )
            Text(
                text = "PERAKENDE ${"%.0f".format((1 - (board?.whalePct ?: 0.5)) * 100)}%",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = SellRed
            )
        }
    }
}

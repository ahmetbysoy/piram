package com.example.presentation.screens.pyramid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.core.theme.BorderDark
import com.example.core.theme.BuyGreen
import com.example.core.theme.CardDark
import com.example.core.theme.NeonCyan
import com.example.core.theme.SellRed
import com.example.core.theme.TextMuted
import com.example.core.theme.TextSecondary
import com.example.core.util.MathUtils

/**
 * 24 saatlik ticker istatistikleri: High / Low / Change% / Volume.
 * Sabit yükseklik — layout zıplamaz. Veri yoksa placeholder.
 */
@Composable
fun TickerStatsCard(
    high24h: Double,
    low24h: Double,
    change24h: Double,
    volume24h: Double,
    priceDecimals: Int = -1,
    modifier: Modifier = Modifier
) {
    val hasData = high24h > 0 && low24h > 0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("ticker_stats_card"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Stat("24H HIGH", if (hasData) MathUtils.formatPrice(high24h, priceDecimals) else "—", NeonCyan)
        Stat("24H LOW", if (hasData) MathUtils.formatPrice(low24h, priceDecimals) else "—", TextSecondary)
        Stat(
            "24H Δ",
            if (hasData) "${if (change24h > 0) "+" else ""}${"%.2f".format(change24h)}%" else "—",
            when {
                !hasData -> TextSecondary
                change24h >= 0 -> BuyGreen
                else -> SellRed
            }
        )
        Stat("24H HACİM", if (volume24h > 0) MathUtils.formatVolume(volume24h) else "—", TextSecondary)
    }
}

@Composable
private fun Stat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = TextMuted
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

package com.example.presentation.screens.pyramid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.BorderDark
import com.example.core.theme.BuyGreen
import com.example.core.theme.CardDark
import com.example.core.theme.NeonCyan
import com.example.core.theme.NeonPink
import com.example.core.theme.PinkPastel
import com.example.core.theme.PurplePastel
import com.example.core.theme.SellRed
import com.example.core.theme.SurfaceDark
import com.example.core.theme.TextMuted
import com.example.core.theme.TextPrimary
import com.example.core.theme.TextSecondary
import com.example.core.theme.WhaleGold
import com.example.core.util.MathUtils
import com.example.domain.model.Depth

@Composable
fun MicrostructureStatsCard(
    orderFlowImbalance: Double,
    vwap: Double,
    depth: Depth?,
    whaleVolume: Double,
    retailVolume: Double,
    burstCount: Int,
    modifier: Modifier = Modifier
) {
    val ofiPct = orderFlowImbalance * 100.0
    val ofiColor = if (ofiPct > 10.0) BuyGreen else if (ofiPct < -10.0) SellRed else TextSecondary

    val spreadBps = if (depth != null && depth.midPrice > 0) (depth.spread / depth.midPrice) * 10000.0 else 0.0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(10.dp)
            .testTag("microstructure_stats_card"),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Metric 1: Order Flow Imbalance
        StatColumn(
            label = "OFI DELTA",
            value = "${if (ofiPct > 0) "+" else ""}${"%.1f".format(ofiPct)}%",
            valueColor = ofiColor,
            subLabel = if (ofiPct > 15) "BUY AGGRESSION" else if (ofiPct < -15) "SELL AGGRESSION" else "BALANCED"
        )

        // Metric 2: VWAP
        StatColumn(
            label = "VWAP",
            value = MathUtils.formatPrice(vwap),
            valueColor = NeonCyan,
            subLabel = "VOL-WEIGHTED"
        )

        // Metric 3: Whale vs Retail
        val totalRatio = whaleVolume + retailVolume
        val whalePct = if (totalRatio > 0) (whaleVolume / totalRatio) * 100.0 else 0.0
        StatColumn(
            label = "INSTITUTIONAL",
            value = "${"%.0f".format(whalePct)}%",
            valueColor = WhaleGold,
            subLabel = "${MathUtils.formatVolume(whaleVolume)} VOL"
        )

        // Metric 4: Spread / Bursts
        StatColumn(
            label = "SPREAD",
            value = "${"%.1f".format(spreadBps)} bps",
            valueColor = PurplePastel,
            subLabel = "$burstCount BURSTS"
        )
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    valueColor: Color,
    subLabel: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = subLabel,
            fontSize = 8.sp,
            color = TextMuted
        )
    }
}

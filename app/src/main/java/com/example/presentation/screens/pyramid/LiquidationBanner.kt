package com.example.presentation.screens.pyramid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.Icon
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
import com.example.core.theme.SellRed
import com.example.core.theme.SurfaceDark
import com.example.core.theme.TextPrimary
import com.example.core.util.MathUtils
import com.example.domain.model.Liquidation
import com.example.domain.model.OrderSide

/**
 * Son likidasyon (forceOrder) olayını gösteren tek satırlık bant.
 */
@Composable
fun LiquidationBanner(
    liquidation: Liquidation?,
    modifier: Modifier = Modifier
) {
    if (liquidation == null) return

    val isSell = liquidation.side == OrderSide.SELL
    val color = if (isSell) SellRed else BuyGreen
    val label = if (isSell) "LONG LİKİDASYON" else "SHORT LİKİDASYON"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark.copy(alpha = 0.6f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("liquidation_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ElectricBolt,
            contentDescription = "Likidasyon",
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp
        )
        Text(
            text = "${MathUtils.formatVolume(liquidation.quantity)} @ ${MathUtils.formatPrice(liquidation.price)}",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = MathUtils.formatUsd(liquidation.notional),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

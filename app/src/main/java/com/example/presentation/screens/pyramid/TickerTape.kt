package com.example.presentation.screens.pyramid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.BgDark
import com.example.core.theme.BorderDark
import com.example.core.theme.BuyGreen
import com.example.core.theme.NeonPink
import com.example.core.theme.PinkPastel
import com.example.core.theme.PurpleDark
import com.example.core.theme.PurplePastel
import com.example.core.theme.SellRed
import com.example.core.theme.SurfaceDark
import com.example.core.theme.TextMuted
import com.example.core.theme.TextPrimary
import com.example.core.theme.TextSecondary
import com.example.core.theme.WhaleGold
import com.example.core.util.MathUtils
import com.example.domain.model.Order
import com.example.domain.model.OrderSide

@Composable
fun TickerTape(
    whaleOrders: List<Order>,
    onOrderClick: (Order) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = "Tape",
                    tint = NeonPink,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "WHALE FLOW STREAM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPink,
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                text = "${whaleOrders.size} LARGE PRINTS",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
        }

        if (whaleOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Scanning real-time L2 order flow for institutional volume...",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .testTag("ticker_tape_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(whaleOrders, key = { it.id }) { order ->
                    TickerItem(order = order, onClick = { onOrderClick(order) })
                }
            }
        }
    }
}

@Composable
private fun TickerItem(
    order: Order,
    onClick: () -> Unit
) {
    val isBuy = order.side == OrderSide.BUY
    val sideColor = if (isBuy) BuyGreen else SellRed

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(1.dp, sideColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (order.isWhale) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Whale",
                tint = WhaleGold,
                modifier = Modifier.size(12.dp)
            )
        }

        Text(
            text = if (isBuy) "BUY" else "SELL",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = sideColor
        )

        Text(
            text = MathUtils.formatVolume(order.volume),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "@ ${MathUtils.formatPrice(order.price)}",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary
        )

        Text(
            text = MathUtils.formatUsd(order.value),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = WhaleGold
        )

        Text(
            text = order.exchange,
            fontSize = 9.sp,
            color = TextMuted
        )
    }
}

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.BorderDark
import com.example.core.theme.BuyGreen
import com.example.core.theme.CardDark
import com.example.core.theme.LayerColors
import com.example.core.theme.PurplePastel
import com.example.core.theme.SellRed
import com.example.core.theme.TextMuted
import com.example.core.theme.TextPrimary
import com.example.core.theme.TextSecondary
import com.example.core.theme.WhaleGold
import com.example.core.util.MathUtils
import com.example.domain.model.LayerAggregate

@Composable
fun TooltipOverlay(
    layer: LayerAggregate,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val layerColor = LayerColors.getOrElse(layer.layerIndex % LayerColors.size) { PurplePastel }
    val totalNotional = layer.buyNotional + layer.sellNotional
    val buyPct = if (totalNotional > 0) (layer.buyNotional / totalNotional) * 100.0 else 50.0
    val sellPct = 100.0 - buyPct

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.5.dp, layerColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("layer_tooltip_dialog")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(layerColor)
                    )
                    Text(
                        text = layer.label.uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (layer.isWhaleTier) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(WhaleGold.copy(alpha = 0.2f))
                                .border(1.dp, WhaleGold, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "INSTITUTIONAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhaleGold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notional breakdown (USDT)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Layer Notional", fontSize = 11.sp, color = TextMuted)
                    Text(
                        MathUtils.formatUsd(layer.notional),
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Trade Count", fontSize = 11.sp, color = TextMuted)
                    Text(
                        "${layer.orderCount} orders",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Buy vs Sell Delta bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "BUY: ${\"%.1f\".format(buyPct)}% (${MathUtils.formatUsd(layer.buyNotional)})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BuyGreen
                    )
                    Text(
                        "SELL: ${\"%.1f\".format(sellPct)}% (${MathUtils.formatUsd(layer.sellNotional)})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SellRed
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (buyPct / 100.0).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = BuyGreen,
                    trackColor = SellRed
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Threshold: ${MathUtils.formatUsd(layer.minNotional)} - ${MathUtils.formatUsd(layer.maxNotional)}",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Text(
                    text = "Dynamic Decay Active",
                    fontSize = 10.sp,
                    color = PurplePastel
                )
            }
        }
    }
}

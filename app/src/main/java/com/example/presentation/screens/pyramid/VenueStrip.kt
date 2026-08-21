package com.example.presentation.screens.pyramid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
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
import com.example.core.theme.WhaleGold
import com.example.core.util.MathUtils
import com.example.domain.model.Depth
import com.example.domain.model.OiState
import com.example.domain.model.formatOi

/**
 * Compact cross-venue market data strip.
 *
 * Surfaces the newly aggregated multi-venue signals:
 *  - cross-exchange price spread (statistical arbitrage spread),
 *  - rolling 1-minute buy/sell flow,
 *  - how many venues currently have a live order book,
 *  - the latest trade price on each venue (colored vs the mean).
 */
@Composable
fun VenueStrip(
    venuePrices: Map<String, Double>,
    venueDepths: List<Depth>,
    buyVolume1m: Double,
    sellVolume1m: Double,
    timeframeBuyNotional: Double,
    timeframeSellNotional: Double,
    timeframe: String,
    oiUsdt: Double?,
    oiDelta: Double?,
    oiState: OiState,
    priceDecimals: Int = -1,
    modifier: Modifier = Modifier
) {
    val prices = venuePrices.values.filter { it > 0 }
    val mean = if (prices.isNotEmpty()) prices.average() else 0.0
    val minPrice = prices.minOrNull() ?: 0.0
    val maxPrice = prices.maxOrNull() ?: 0.0
    val spreadBps = if (mean > 0) (maxPrice - minPrice) / mean * 10000.0 else 0.0

    val totalFlow = buyVolume1m + sellVolume1m
    val flowPct = if (totalFlow > 0) (buyVolume1m - sellVolume1m) / totalFlow * 100.0 else 0.0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("venue_strip"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StripCell(
            label = "X-VENUE",
            value = "${"%.1f".format(spreadBps)} bps",
            valueColor = if (spreadBps > 15.0) WhaleGold else NeonCyan
        )

        StripCell(
            label = "1M FLOW",
            value = "${if (flowPct > 0) "+" else ""}${"%.1f".format(flowPct)}% ${if (flowPct >= 0) "BUY" else "SELL"}",
            valueColor = when {
                flowPct > 1.0 -> BuyGreen
                flowPct < -1.0 -> SellRed
                else -> TextSecondary
            }
        )

        StripCell(
            label = "BOOKS",
            value = "${venueDepths.size}/${venuePrices.size.coerceAtLeast(1)}",
            valueColor = NeonCyan
        )

        // OI (açık pozisyon) — "OI yoksa yalan yok"
        val oiValue = when {
            oiState == OiState.BEKLIYOR -> "…"
            oiUsdt == null -> "yok"
            else -> {
                val arrow = when {
                    oiDelta == null -> ""
                    oiDelta > 0 -> " ↑"
                    oiDelta < 0 -> " ↓"
                    else -> ""
                }
                formatOi(oiUsdt) + arrow + if (oiState == OiState.ESKI) " (eski)" else ""
            }
        }
        StripCell(
            label = "OI",
            value = oiValue,
            valueColor = when (oiState) {
                OiState.OK -> NeonCyan
                OiState.ESKI -> WhaleGold
                else -> TextMuted
            }
        )

        // Seçili timeframe için pencere neti (USDT)
        val winNet = timeframeBuyNotional - timeframeSellNotional
        val winLabel = when (timeframe) {
            "1M" -> "1DK"
            "5M" -> "5DK"
            "15M" -> "15DK"
            "ALL" -> "AÇILIŞ"
            else -> timeframe
        }
        StripCell(
            label = winLabel,
            value = "${if (winNet > 0) "+" else ""}${MathUtils.formatUsd(kotlin.math.abs(winNet))}",
            valueColor = when {
                winNet > 0 -> BuyGreen
                winNet < 0 -> SellRed
                else -> TextSecondary
            }
        )

        // Per-venue last trade price chips
        venuePrices.entries.sortedBy { it.value }.forEach { (exchange, price) ->
            val color = when {
                mean <= 0 -> TextSecondary
                price >= mean -> BuyGreen
                else -> SellRed
            }
            StripCell(
                label = VENUE_CODES[exchange] ?: exchange.take(3).uppercase(),
                value = MathUtils.formatPrice(price, priceDecimals),
                valueColor = color
            )
        }
    }
}

@Composable
private fun StripCell(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
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
            color = valueColor
        )
    }
}

private val VENUE_CODES = mapOf(
    "Binance" to "BIN",
    "Bybit" to "BYB",
    "OKX" to "OKX",
    "Kraken" to "KRA",
    "KuCoin" to "KUC"
)

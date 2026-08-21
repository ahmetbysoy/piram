package com.example.presentation.screens.pyramid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.core.theme.CardDark
import com.example.core.theme.NeonCyan
import com.example.core.theme.SellRed
import com.example.core.theme.TextMuted
import com.example.core.theme.TextPrimary
import com.example.core.theme.TextSecondary
import com.example.core.theme.WhaleGold
import com.example.core.util.MathUtils
import com.example.domain.engine.WhaleRetailBoard
import com.example.domain.model.Depth
import com.example.domain.model.OiState
import com.example.domain.model.formatOi

/**
 * Piyasa şeridi — ScoreboardBar (kurumsal vs perakende) ile VenueStrip
 * (çoklu borsa verisi) tek kartta birleşti.
 */
@Composable
fun MarketStrip(
    whaleNotional: Double,
    retailNotional: Double,
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
    val board = WhaleRetailBoard.evaluate(whaleNotional, retailNotional)
    val whaleBar = board?.whalePct?.toFloat()?.coerceIn(0.02f, 0.98f) ?: 0.5f

    val prices = venuePrices.values.filter { it > 0 }
    val mean = if (prices.isNotEmpty()) prices.average() else 0.0
    val minPrice = prices.minOrNull() ?: 0.0
    val maxPrice = prices.maxOrNull() ?: 0.0
    val spreadBps = if (mean > 0) (maxPrice - minPrice) / mean * 10000.0 else 0.0

    val totalFlow = buyVolume1m + sellVolume1m
    val flowPct = if (totalFlow > 0) (buyVolume1m - sellVolume1m) / totalFlow * 100.0 else 0.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("market_strip")
    ) {
        // Kurumsal vs perakende skor + tug-of-war
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🐋 ${MathUtils.formatUsd(whaleNotional)}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = WhaleGold
            )
            Text(
                text = board?.score ?: "0-0",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "🐟 ${MathUtils.formatUsd(retailNotional)}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(2.dp))
        ) {
            Box(Modifier.fillMaxWidth(whaleBar).height(5.dp).background(BuyGreen))
            Box(Modifier.fillMaxWidth(1f - whaleBar).height(5.dp).background(SellRed))
        }

        // Venue hücreleri (yatay kaydırma + fade)
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("market_strip_cells"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StripCell("X-VENUE", "${"%.1f".format(spreadBps)} bps", if (spreadBps > 15.0) WhaleGold else NeonCyan)
                StripCell(
                    "1M FLOW",
                    "${if (flowPct > 0) "+" else ""}${"%.1f".format(flowPct)}% ${if (flowPct >= 0) "BUY" else "SELL"}",
                    when {
                        flowPct > 1.0 -> BuyGreen
                        flowPct < -1.0 -> SellRed
                        else -> TextSecondary
                    }
                )
                StripCell("BOOKS", "${venueDepths.size}/${venuePrices.size.coerceAtLeast(1)}", NeonCyan)

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
                    "OI", oiValue,
                    when (oiState) {
                        OiState.OK -> NeonCyan
                        OiState.ESKI -> WhaleGold
                        else -> TextMuted
                    }
                )

                val winNet = timeframeBuyNotional - timeframeSellNotional
                val winLabel = when (timeframe) {
                    "1M" -> "1DK"; "5M" -> "5DK"; "15M" -> "15DK"; "ALL" -> "AÇILIŞ"; else -> timeframe
                }
                StripCell(
                    winLabel,
                    "${if (winNet > 0) "+" else ""}${MathUtils.formatUsd(kotlin.math.abs(winNet))}",
                    when {
                        winNet > 0 -> BuyGreen
                        winNet < 0 -> SellRed
                        else -> TextSecondary
                    }
                )

                venuePrices.entries.sortedBy { it.value }.forEach { (exchange, price) ->
                    val color = when {
                        mean <= 0 -> TextSecondary
                        price >= mean -> BuyGreen
                        else -> SellRed
                    }
                    StripCell(
                        VENUE_CODES[exchange] ?: exchange.take(3).uppercase(),
                        MathUtils.formatPrice(price, priceDecimals),
                        color
                    )
                }
            }

            // Sağ kenar fade: kaydırılabilir ipucu
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(24.dp)
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, BgDark)))
            )
        }
    }
}

@Composable
private fun StripCell(label: String, value: String, valueColor: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(com.example.core.theme.SurfaceDark)
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
    "Binance" to "BIN", "Bybit" to "BYB", "OKX" to "OKX", "Kraken" to "KRA", "KuCoin" to "KUC"
)

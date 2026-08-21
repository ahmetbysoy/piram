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
import androidx.compose.foundation.layout.width
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
import com.example.core.theme.SellRed
import com.example.core.theme.TextMuted
import com.example.core.theme.TextPrimary
import com.example.core.util.MathUtils
import com.example.domain.engine.BookProfile
import com.example.domain.model.Depth
import com.example.domain.model.DepthLevel

/**
 * Kompakt derinlik ısı haritası: ilk 5 bid/ask seviyesini aynalı barlarla çizer,
 * kitap dengesizliğini ve duvar fiyatlarını gösterir.
 */
@Composable
fun DepthHeatmap(
    depth: Depth?,
    priceDecimals: Int = -1,
    modifier: Modifier = Modifier
) {
    val d = depth ?: return
    val profile = BookProfile.compute(d) ?: return

    val bids = d.bids.take(5)
    val asks = d.asks.take(5)
    val maxVol = maxOf(
        bids.maxOfOrNull { it.volume } ?: 0.0,
        asks.maxOfOrNull { it.volume } ?: 0.0,
        0.0001
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("depth_heatmap")
    ) {
        // Header: imbalance
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "DERİNLİK",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = TextMuted
            )
            val imbPct = profile.imbalance * 100.0
            Text(
                text = "Dengesizlik: ${if (imbPct > 0) "+" else ""}${"%.0f".format(imbPct)}% ${if (imbPct >= 0) "ALIŞ" else "SATIŞ"}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = when {
                    imbPct > 3 -> BuyGreen
                    imbPct < -3 -> SellRed
                    else -> TextPrimary
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Aynalı merdiven: asks (sol, en iyi içeride) + bids (sağ)
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                asks.reversed().forEach { level ->
                    AskLevelBar(level, maxVol, priceDecimals)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                bids.forEach { level ->
                    BidLevelBar(level, maxVol, priceDecimals)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Duvar fiyatları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = profile.askWallPrice?.let { "Ask duvarı: ${MathUtils.formatPrice(it, priceDecimals)}" } ?: "—",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = SellRed
            )
            Text(
                text = profile.bidWallPrice?.let { "Bid duvarı: ${MathUtils.formatPrice(it, priceDecimals)}" } ?: "—",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = BuyGreen
            )
        }
    }
}

@Composable
private fun AskLevelBar(level: DepthLevel, maxVol: Double, priceDecimals: Int) {
    val frac = (level.volume / maxVol).toFloat().coerceIn(0.02f, 1.0f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = MathUtils.formatPrice(level.price, priceDecimals),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .width((frac * 60).dp)
                .height(8.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SellRed.copy(alpha = 0.55f))
        )
    }
}

@Composable
private fun BidLevelBar(level: DepthLevel, maxVol: Double, priceDecimals: Int) {
    val frac = (level.volume / maxVol).toFloat().coerceIn(0.02f, 1.0f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width((frac * 60).dp)
                .height(8.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(BuyGreen.copy(alpha = 0.55f))
        )
        Text(
            text = MathUtils.formatPrice(level.price, priceDecimals),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted,
            modifier = Modifier.weight(1f)
        )
    }
}

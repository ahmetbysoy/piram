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
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.BgDark
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
import com.example.domain.model.ConsensusResult
import com.example.domain.model.SignalType
import com.example.presentation.components.PulseBar

@Composable
fun ConsensusHeader(
    consensus: ConsensusResult,
    currentPrice: Double,
    symbol: String,
    priceDecimals: Int = -1,
    consensusUnstable: Boolean = false,
    moodEmoji: String = "",
    narrative: String = "",
    modifier: Modifier = Modifier
) {
    val (badgeBg, badgeBorder, badgeText, signalLabel) = when (consensus.overallSignal) {
        SignalType.STRONG_BUY -> Quad(BuyGreen.copy(alpha = 0.2f), BuyGreen, BuyGreen, "🚀 STRONG BUY")
        SignalType.BUY -> Quad(BuyGreen.copy(alpha = 0.12f), BuyGreen.copy(alpha = 0.6f), BuyGreen, "BUY")
        SignalType.STRONG_SELL -> Quad(SellRed.copy(alpha = 0.2f), SellRed, SellRed, "🐻 STRONG SELL")
        SignalType.SELL -> Quad(SellRed.copy(alpha = 0.12f), SellRed.copy(alpha = 0.6f), SellRed, "SELL")
        SignalType.NEUTRAL -> Quad(SurfaceDark, BorderDark, TextSecondary, "NEUTRAL")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("consensus_header_card")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = symbol,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PurplePastel.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "HFT ${consensus.activeStrategiesCount}-STRAT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurplePastel
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (currentPrice > 0) "$${com.example.core.util.MathUtils.formatPrice(currentPrice, priceDecimals)}" else "Streaming Live...",
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (consensus.overallSignal == SignalType.BUY || consensus.overallSignal == SignalType.STRONG_BUY) BuyGreen else if (consensus.overallSignal == SignalType.SELL || consensus.overallSignal == SignalType.STRONG_SELL) SellRed else TextPrimary
                    )
                }

                // Consensus Signal Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeBg)
                        .border(1.5.dp, badgeBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("consensus_signal_badge")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (moodEmoji.isNotBlank()) {
                            Text(
                                text = moodEmoji,
                                fontSize = 22.sp
                            )
                        }
                        Text(
                            text = signalLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Score: ${if (consensus.consensusStrength > 0) "+" else ""}${"%.0f".format(consensus.consensusStrength)}",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = badgeText.copy(alpha = 0.8f)
                        )
                        if (consensus.conflict) {
                            Text(
                                text = "⚠️ KARIŞIK",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeText.copy(alpha = 0.9f)
                            )
                        }
                        if (consensusUnstable) {
                            Text(
                                text = "🎢 KARARSIZ",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeText.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Consensus strength bar & strategy votes
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "BULLISH: ${consensus.bullishCount}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BuyGreen
                )
                Text(
                    text = "NEUTRAL: ${consensus.neutralCount}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
                Text(
                    text = "BEARISH: ${consensus.bearishCount}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SellRed
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { (consensus.buyScore / 100.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = BuyGreen,
                trackColor = SurfaceDark // boş kalan kısım NEUTRAL (eskiden SellRed'di)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Güven ısı çubuğu (#14) — nabız animasyonlu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "GÜVEN",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = TextMuted
                )
                PulseBar(
                    progress = consensus.confidence.toFloat(),
                    color = when {
                        consensus.overallSignal == SignalType.BUY || consensus.overallSignal == SignalType.STRONG_BUY -> BuyGreen
                        consensus.overallSignal == SignalType.SELL || consensus.overallSignal == SignalType.STRONG_SELL -> SellRed
                        else -> PurplePastel
                    },
                    height = 6.dp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${"%.0f".format(consensus.confidence * 100)}%",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Top strategy hints — sabit yükseklik + tek satır (kart yüksekliği zıplamaz)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (consensus.topBullishStrategy != null) "Top Bull: ${consensus.topBullishStrategy}" else "Confidence: ${"%.0f".format(consensus.confidence * 100)}%",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (consensus.topBearishStrategy != null) "Top Bear: ${consensus.topBearishStrategy}" else "${consensus.activeStrategiesCount}/20 Active",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Tek anlatı motoru: divergence ?: kanka özeti (FlowNarrative + StoryGenerator birleşti)
            if (narrative.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = narrative,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_narrative")
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

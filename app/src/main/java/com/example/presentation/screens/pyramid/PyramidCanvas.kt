package com.example.presentation.screens.pyramid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.BorderDark
import com.example.core.theme.BuyGreen
import com.example.core.theme.LayerColors
import com.example.core.theme.NeonCyan
import com.example.core.theme.NeonPink
import com.example.core.theme.PinkPastel
import com.example.core.theme.PurplePastel
import com.example.core.theme.SellRed
import com.example.core.theme.SurfaceDark
import com.example.core.theme.TextMuted
import com.example.core.theme.TextPrimary
import com.example.core.theme.WhaleGold
import com.example.core.util.MathUtils
import com.example.domain.model.LayerAggregate
import kotlin.math.max
import kotlin.math.sqrt

@Composable
fun PyramidCanvas(
    layers: List<LayerAggregate>,
    selectedLayerIndex: Int?,
    onLayerSelected: (LayerAggregate?) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("pyramid_canvas")
                .pointerInput(layers) {
                    detectTapGestures { offset ->
                        if (layers.isEmpty()) return@detectTapGestures
                        val h = size.height
                        val layerHeight = h / layers.size
                        val tappedIndex = ((h - offset.y) / layerHeight).toInt().coerceIn(0, layers.size - 1)
                        if (tappedIndex in layers.indices) {
                            if (selectedLayerIndex == tappedIndex) {
                                onLayerSelected(null)
                            } else {
                                onLayerSelected(layers[tappedIndex])
                            }
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val numLayers = layers.size
            if (numLayers == 0) return@Canvas

            val layerHeight = (height / numLayers).coerceAtLeast(16f)
            val padding = 6f
            val usableHeight = layerHeight - padding
            val centerX = width / 2f

            // Draw center axis guide line
            drawLine(
                color = BorderDark.copy(alpha = 0.7f),
                start = Offset(centerX, 0f),
                end = Offset(centerX, height),
                strokeWidth = 1.5f
            )

            // Calculate max volume for sqrt normalization
            val maxVol = max(0.001, layers.maxOfOrNull { it.displayVolume } ?: 1.0)
            val sqrtMaxVol = sqrt(maxVol)

            // Draw each layer from top (Apex/Whale) to bottom (Base/Retail)
            // layerIndex 0 is Retail at bottom, (numLayers - 1) is Whale at top
            for (layer in layers) {
                val idx = layer.layerIndex
                // Vertical position: highest index at top
                val yTop = height - (idx + 1) * layerHeight + (padding / 2f)
                val isSelected = selectedLayerIndex == idx

                val baseColor = LayerColors.getOrElse(idx % LayerColors.size) { PurplePastel }
                val layerSqrt = sqrt(max(0.0, layer.displayVolume))
                val volumeFraction = (layerSqrt / sqrtMaxVol).toFloat().coerceIn(0.04f, 1.0f)

                val maxHalfWidth = (width * 0.44f)
                val barWidth = maxHalfWidth * volumeFraction

                val buyRatio = layer.buyRatio.coerceIn(0f, 1f)
                val sellRatio = (1f - buyRatio).coerceIn(0f, 1f)

                val buyBarWidth = barWidth * buyRatio
                val sellBarWidth = barWidth * sellRatio

                // Draw Buy Bar (Right side of Center)
                val buyGradient = Brush.horizontalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = if (isSelected) 0.95f else 0.75f),
                        BuyGreen.copy(alpha = if (isSelected) 1.0f else 0.85f)
                    ),
                    startX = centerX,
                    endX = centerX + buyBarWidth
                )

                drawRoundRect(
                    brush = buyGradient,
                    topLeft = Offset(centerX + 2f, yTop),
                    size = Size(buyBarWidth.coerceAtLeast(4f), usableHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Draw Sell Bar (Left side of Center)
                val sellGradient = Brush.horizontalGradient(
                    colors = listOf(
                        SellRed.copy(alpha = if (isSelected) 1.0f else 0.85f),
                        baseColor.copy(alpha = if (isSelected) 0.95f else 0.75f)
                    ),
                    startX = centerX - sellBarWidth,
                    endX = centerX
                )

                drawRoundRect(
                    brush = sellGradient,
                    topLeft = Offset(centerX - sellBarWidth - 2f, yTop),
                    size = Size(sellBarWidth.coerceAtLeast(4f), usableHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Selection highlight border
                if (isSelected) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(centerX - sellBarWidth - 3f, yTop - 1f),
                        size = Size(sellBarWidth + buyBarWidth + 6f, usableHeight + 2f),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Draw Whale Tier Badge Glow at Apex
                if (layer.isWhaleTier) {
                    drawCircle(
                        color = WhaleGold,
                        radius = 3.dp.toPx(),
                        center = Offset(centerX, yTop + usableHeight / 2f)
                    )
                }

                // Draw Layer Label (Left Edge)
                val labelStyle = TextStyle(
                    color = if (isSelected) TextPrimary else TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                val labelLayout = textMeasurer.measure(layer.label, labelStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = layer.label,
                    topLeft = Offset(12f, yTop + (usableHeight - labelLayout.size.height) / 2f),
                    style = labelStyle
                )

                // Draw Volume text (Right Edge)
                val volStr = MathUtils.formatVolume(layer.currentVolume)
                val volStyle = TextStyle(
                    color = if (layer.isWhaleTier) WhaleGold else TextPrimary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                val volLayout = textMeasurer.measure(volStr, volStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = volStr,
                    topLeft = Offset(width - volLayout.size.width - 12f, yTop + (usableHeight - volLayout.size.height) / 2f),
                    style = volStyle
                )
            }
        }
    }
}

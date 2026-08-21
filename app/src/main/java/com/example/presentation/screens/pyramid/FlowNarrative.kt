package com.example.presentation.screens.pyramid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.BuyGreen
import com.example.core.theme.SellRed
import com.example.core.theme.TextSecondary
import com.example.domain.engine.DivergenceKind

/**
 * Toplama / boşaltma anlatı cümlesi (tek satır).
 * Sabit yükseklik rezerve eder: içerik gelip gittiğinde layout zıplamaz.
 */
@Composable
fun FlowNarrative(
    divergenceYazi: String,
    divergenceKind: DivergenceKind,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        if (divergenceYazi.isNotBlank()) {
            val color = when (divergenceKind) {
                DivergenceKind.TOPLAMA -> BuyGreen
                DivergenceKind.BOSALTMA -> SellRed
                else -> TextSecondary
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flow_narrative"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = divergenceYazi,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = color
                )
            }
        }
    }
}

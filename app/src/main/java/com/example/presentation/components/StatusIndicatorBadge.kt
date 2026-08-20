package com.example.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.BorderDark
import com.example.core.theme.BuyGreen
import com.example.core.theme.NeonYellow
import com.example.core.theme.SellRed
import com.example.core.theme.SurfaceDark
import com.example.core.theme.TextPrimary
import com.example.domain.model.ConnectionState
import com.example.domain.model.ExchangeStatus

@Composable
fun StatusIndicatorBadge(
    statuses: List<ExchangeStatus>,
    modifier: Modifier = Modifier
) {
    val activeCount = statuses.count { it.state is ConnectionState.Connected }
    val isAnyConnecting = statuses.any { it.state is ConnectionState.Connecting || it.state is ConnectionState.Reconnecting }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val (statusColor, statusText) = when {
        activeCount > 0 -> BuyGreen to "LIVE WS ($activeCount/${statuses.size})"
        isAnyConnecting -> NeonYellow to "CONNECTING"
        else -> SellRed to "DISCONNECTED"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("status_indicator_badge"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .alpha(if (activeCount > 0) pulseAlpha else 1f)
                .background(statusColor)
        )

        Text(
            text = statusText,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            letterSpacing = 0.5.sp
        )
    }
}

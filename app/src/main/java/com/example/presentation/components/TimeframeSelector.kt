package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.BgDark
import com.example.core.theme.PinkPastel
import com.example.core.theme.PurplePastel
import com.example.core.theme.SurfaceDark
import com.example.core.theme.TextMuted
import com.example.core.theme.TextPrimary

@Composable
fun TimeframeSelector(
    selectedTimeframe: String,
    onSelectTimeframe: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeframes = listOf("1M", "5M", "15M", "ALL")

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        timeframes.forEach { tf ->
            val isSelected = tf == selectedTimeframe
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) PurplePastel else Color.Transparent)
                    .clickable { onSelectTimeframe(tf) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("timeframe_$tf")
            ) {
                Text(
                    text = tf,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) BgDark else TextMuted
                )
            }
        }
    }
}

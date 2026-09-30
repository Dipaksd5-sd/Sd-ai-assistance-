package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.GeminiConnectionState
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricSky
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.NeonCyan

@Composable
fun ConnectionIndicator(
    state: GeminiConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_pulse"
    )

    val (color, text) = when (state) {
        GeminiConnectionState.CONNECTED -> GlowGreen to "Gemini Live Online"
        GeminiConnectionState.CONNECTING -> ElectricSky to "Connecting Live API…"
        GeminiConnectionState.RECONNECTING -> NeonCyan to "Reconnecting…"
        GeminiConnectionState.ERROR -> AccentCoral to "Connection Issue"
        GeminiConnectionState.DISCONNECTED -> Color.Gray to "Offline"
    }

    Box(
        modifier = modifier
            .testTag("connection_indicator")
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.85f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(if (state == GeminiConnectionState.CONNECTED || state == GeminiConnectionState.CONNECTING) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    color = color
                )
            )
        }
    }
}

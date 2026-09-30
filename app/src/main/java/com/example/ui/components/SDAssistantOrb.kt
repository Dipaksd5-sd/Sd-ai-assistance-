package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.AssistantState
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricSky
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OrbErrorCore
import com.example.ui.theme.OrbIdleCore
import com.example.ui.theme.OrbSpeakingCore
import com.example.ui.theme.OrbThinkingCore
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SDAssistantOrb(
    state: AssistantState,
    micAmplitude: Float,
    playbackAmplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_loops")

    // Slow breathing animation for IDLE
    val idleBreathing by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_breathing"
    )

    // Rotation animation for THINKING state
    val thinkingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "thinking_rotation"
    )

    // Pulse for glow ring
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    // Dynamic scale driven by audio volume
    val targetAudioScale = when (state) {
        AssistantState.LISTENING -> 1f + (micAmplitude * 0.45f).coerceIn(0f, 0.45f)
        AssistantState.SPEAKING -> 1f + (playbackAmplitude * 0.5f).coerceIn(0f, 0.5f)
        AssistantState.THINKING -> 1f
        AssistantState.IDLE -> idleBreathing
        AssistantState.ERROR -> 0.95f
        AssistantState.DISCONNECTED -> 0.9f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetAudioScale,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "animated_orb_scale"
    )

    // Color gradient based on state
    val (coreColor, outerColor, ringColor) = when (state) {
        AssistantState.IDLE -> Triple(OrbIdleCore, CyberViolet, ElectricSky)
        AssistantState.LISTENING -> Triple(NeonCyan, ElectricSky, NeonCyan)
        AssistantState.THINKING -> Triple(OrbThinkingCore, CyberViolet, NeonCyan)
        AssistantState.SPEAKING -> Triple(OrbSpeakingCore, NeonCyan, ElectricSky)
        AssistantState.ERROR -> Triple(OrbErrorCore, AccentCoral, AccentCoral)
        AssistantState.DISCONNECTED -> Triple(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF475569))
    }

    Box(
        modifier = modifier
            .testTag("sd_assistant_orb")
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val baseRadius = (this.size.minDimension / 2) * 0.65f
            val currentRadius = baseRadius * animatedScale

            // 1. Ambient outer glow circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        outerColor.copy(alpha = 0.35f * glowPulse),
                        coreColor.copy(alpha = 0.12f * glowPulse),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 1.55f
                ),
                radius = currentRadius * 1.55f,
                center = center
            )

            // 2. Outer acoustic wave rings (expand when LISTENING or SPEAKING)
            val ringCount = 3
            for (i in 1..ringCount) {
                val ringMultiplier = 1f + (i * 0.16f)
                val dynamicAlpha = when (state) {
                    AssistantState.LISTENING -> ((micAmplitude * 1.2f) - (i * 0.2f)).coerceIn(0.15f, 0.8f)
                    AssistantState.SPEAKING -> ((playbackAmplitude * 1.3f) - (i * 0.18f)).coerceIn(0.2f, 0.85f)
                    AssistantState.THINKING -> 0.25f
                    else -> 0.12f
                }

                drawCircle(
                    color = ringColor.copy(alpha = dynamicAlpha),
                    radius = currentRadius * ringMultiplier,
                    center = center,
                    style = Stroke(width = (2.2f - (i * 0.4f)).coerceAtLeast(1f).dp.toPx())
                )
            }

            // 3. Thinking orbiting photon arcs
            if (state == AssistantState.THINKING) {
                drawThinkingArcs(center, currentRadius * 1.22f, thinkingRotation, ringColor)
            }

            // 4. Main Core Sphere Orb (Rich 3D gradient)
            val lightOffset = Offset(center.x - (currentRadius * 0.35f), center.y - (currentRadius * 0.35f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        coreColor,
                        outerColor,
                        Color(0xFF030712)
                    ),
                    center = lightOffset,
                    radius = currentRadius * 1.1f
                ),
                radius = currentRadius,
                center = center
            )

            // 5. Inner Core Specular Highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        Color.White.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = lightOffset,
                    radius = currentRadius * 0.45f
                ),
                radius = currentRadius * 0.35f,
                center = lightOffset
            )
        }
    }
}

private fun DrawScope.drawThinkingArcs(
    center: Offset,
    radius: Float,
    rotationDeg: Float,
    color: Color
) {
    val radians = Math.toRadians(rotationDeg.toDouble())
    val arcCount = 4
    for (i in 0 until arcCount) {
        val angle = radians + (i * (Math.PI / 2))
        val x = (center.x + radius * cos(angle)).toFloat()
        val y = (center.y + radius * sin(angle)).toFloat()
        drawCircle(
            color = color.copy(alpha = 0.75f),
            radius = 4.dp.toPx(),
            center = Offset(x, y)
        )
    }
}

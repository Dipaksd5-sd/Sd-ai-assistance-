package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AssistantEngine
import com.example.domain.model.AssistantState
import com.example.domain.model.DeviceActionResult
import com.example.ui.components.ConnectionIndicator
import com.example.ui.components.SDAssistantOrb
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DeepCosmic
import com.example.ui.theme.ElectricSky
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    assistantEngine: AssistantEngine,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assistantState by assistantEngine.assistantState.collectAsState()
    val connectionState by assistantEngine.connectionState.collectAsState()
    val statusText by assistantEngine.statusText.collectAsState()
    val micAmplitude by assistantEngine.micAmplitude.collectAsState()
    val playbackAmplitude by assistantEngine.playbackAmplitude.collectAsState()
    val preferences by assistantEngine.preferences.collectAsState()

    var lastActionFeedback by remember { mutableStateOf<DeviceActionResult?>(null) }

    LaunchedEffect(Unit) {
        assistantEngine.lastActionResult.collect { result ->
            lastActionFeedback = result
            delay(4500)
            if (lastActionFeedback == result) {
                lastActionFeedback = null
            }
        }
    }

    Scaffold(
        containerColor = DeepCosmic,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DeepCosmic,
                            Color(0xFF090E1B),
                            DeepCosmic
                        )
                    )
                )
        ) {
            // Main Voice Assistant Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Top Bar: App Identity + Connection Indicator + Settings Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SD",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                color = NeonCyan
                            )
                        )
                        Text(
                            text = "VOICE ASSISTANT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                letterSpacing = 1.5.sp,
                                color = TextSecondary
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ConnectionIndicator(
                            state = connectionState,
                            onClick = { assistantEngine.startAssistant() }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .testTag("open_settings_button")
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = ElectricSky,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Central Section: Animated SD Orb & Dynamic State
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Assistant State Badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 18.dp)
                    ) {
                        Text(
                            text = when (assistantState) {
                                AssistantState.IDLE -> "IDLE • SAY 'SD' OR TAP"
                                AssistantState.LISTENING -> "LISTENING TO YOU…"
                                AssistantState.THINKING -> "SD IS PROCESSING…"
                                AssistantState.SPEAKING -> "SD IS SPEAKING • TAP TO STOP"
                                AssistantState.ERROR -> "SYSTEM ERROR"
                                AssistantState.DISCONNECTED -> "OFFLINE"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = when (assistantState) {
                                    AssistantState.LISTENING -> NeonCyan
                                    AssistantState.SPEAKING -> ElectricSky
                                    AssistantState.THINKING -> NeonCyan
                                    AssistantState.ERROR -> Color(0xFFEF4444)
                                    else -> TextSecondary
                                },
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    // Central Animated Orb (Responds to Mic and SD Voice, Clickable)
                    SDAssistantOrb(
                        state = assistantState,
                        micAmplitude = micAmplitude,
                        playbackAmplitude = playbackAmplitude,
                        onClick = { assistantEngine.toggleListening() },
                        size = 270.dp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Conversational Subtitle / Status Display
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .testTag("status_text_display")
                    )

                    // Action Execution Feedback banner
                    AnimatedVisibility(
                        visible = lastActionFeedback != null,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        lastActionFeedback?.let { feedback ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = DarkSurfaceVariant.copy(alpha = 0.95f)
                                ),
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .fillMaxWidth(0.85f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (feedback.success) NeonCyan else Color(0xFFEF4444))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = feedback.spokenFeedback ?: feedback.message,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Bottom Area: Voice suggestions carousel & Quick Controls
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Suggested commands discovery carousel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val suggestions = listOf(
                            "Battery kitni hai?" to "check battery",
                            "YouTube kholo" to "open youtube",
                            "Turn on flashlight" to "turn on flashlight",
                            "Take a photo" to "take a photo",
                            "What time is it?" to "what time is it",
                            "Open Settings" to "open settings"
                        )
                        suggestions.forEach { (label, command) ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = DarkSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        assistantEngine.sendDirectTextCommand(command)
                                    }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    ),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Push-to-Talk / Interrupt Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when (assistantState) {
                                AssistantState.LISTENING -> NeonCyan
                                AssistantState.SPEAKING -> ElectricSky
                                else -> DarkSurfaceVariant
                            },
                            modifier = Modifier
                                .testTag("primary_mic_button")
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable {
                                    assistantEngine.toggleListening()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (assistantState) {
                                        AssistantState.SPEAKING -> Icons.Default.Stop
                                        AssistantState.LISTENING -> Icons.Default.Mic
                                        else -> Icons.Default.MicOff
                                    },
                                    contentDescription = "Voice Control",
                                    tint = when (assistantState) {
                                        AssistantState.LISTENING,
                                        AssistantState.SPEAKING -> DeepCosmic
                                        else -> TextPrimary
                                    },
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (preferences.isWakeWordEnabled) "Wake word: 'SD' is active" else "Wake word OFF • Tap mic",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
    }
}

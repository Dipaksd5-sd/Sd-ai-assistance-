package com.example.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AssistantEngine
import com.example.permissions.PermissionManager
import com.example.service.SDAssistantService
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DeepCosmic
import com.example.ui.theme.ElectricSky
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    assistantEngine: AssistantEngine,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences by assistantEngine.preferences.collectAsState()

    var customApiKeyInput by remember(preferences.customApiKey) {
        mutableStateOf(preferences.customApiKey)
    }

    // Permission check state
    var hasMicPermission by remember {
        mutableStateOf(PermissionManager.hasRecordAudioPermission(context))
    }
    var hasCameraPermission by remember {
        mutableStateOf(PermissionManager.hasCameraPermission(context))
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            Toast.makeText(context, "Microphone permission granted!", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            Toast.makeText(context, "Camera permission granted!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SD Settings", color = TextPrimary) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepCosmic
                )
            )
        },
        containerColor = DeepCosmic,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Voice & Wake Word Section
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "VOICE & INTERACTION",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Wake word toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wake Word ('SD')", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Activate SD automatically by saying 'SD'",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = preferences.isWakeWordEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { assistantEngine.memoryDataSource.setWakeWordEnabled(enabled) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepCosmic,
                                checkedTrackColor = NeonCyan
                            ),
                            modifier = Modifier.testTag("wake_word_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Background Assistant toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Background Assistant", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Keep SD listening in background via foreground service",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = preferences.isBackgroundServiceEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    assistantEngine.memoryDataSource.setBackgroundServiceEnabled(enabled)
                                    if (enabled) {
                                        SDAssistantService.startService(context)
                                    } else {
                                        SDAssistantService.stopService(context)
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepCosmic,
                                checkedTrackColor = NeonCyan
                            ),
                            modifier = Modifier.testTag("bg_service_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gemini Voice selection
                    Text("SD Voice Tone", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val voices = listOf("Puck", "Charon", "Aoede", "Fenrir", "Kore")
                        voices.forEach { voice ->
                            val isSelected = preferences.preferredVoice == voice
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    scope.launch { assistantEngine.memoryDataSource.setPreferredVoice(voice) }
                                },
                                label = { Text(voice, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = DeepCosmic,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextPrimary
                                )
                            )
                        }
                    }
                }
            }

            // 2. Hardware Permissions Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "DEVICE PERMISSIONS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = ElectricSky,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Mic permission item
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (hasMicPermission) GlowGreen else AccentCoral,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Microphone", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (hasMicPermission) "Active & Granted" else "Required for voice interaction",
                                    color = if (hasMicPermission) GlowGreen else AccentCoral,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        if (!hasMicPermission) {
                            Button(
                                onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DeepCosmic),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("request_mic_permission_btn")
                            ) {
                                Text("Grant", fontSize = 12.sp)
                            }
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = GlowGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Camera permission item
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (hasCameraPermission) GlowGreen else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Camera", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (hasCameraPermission) "Granted for photo capture" else "Optional for taking photos",
                                    color = if (hasCameraPermission) GlowGreen else TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        if (!hasCameraPermission) {
                            Button(
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = TextPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("request_camera_permission_btn")
                            ) {
                                Text("Grant", fontSize = 12.sp)
                            }
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = GlowGreen)
                        }
                    }
                }
            }

            // 3. API Key & Connection Testing Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "GEMINI LIVE API CONNECTION",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        "The assistant automatically connects using your AI Studio Secrets key. You can also provide a manual override key below.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customApiKeyInput,
                        onValueChange = { customApiKeyInput = it },
                        label = { Text("Custom API Key (Optional)", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_api_key_input"),
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    assistantEngine.memoryDataSource.setCustomApiKey(customApiKeyInput)
                                    assistantEngine.startAssistant()
                                    Toast.makeText(context, "API Key updated. Reconnecting SD...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = DeepCosmic
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_api_key_btn")
                        ) {
                            Text("Save Key")
                        }

                        OutlinedButton(
                            onClick = {
                                assistantEngine.startAssistant()
                                Toast.makeText(context, "Testing Gemini Live connection...", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_connection_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = ElectricSky)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Live", color = ElectricSky)
                        }
                    }
                }
            }

            // 4. Memory Management Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ASSISTANT MEMORY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = AccentCoral,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "SD stores only your assistant preferences locally on this device. You can reset all memory anytime.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                assistantEngine.memoryDataSource.clearMemory()
                                customApiKeyInput = ""
                                Toast.makeText(context, "Memory cleared. SD reset to defaults.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCoral.copy(alpha = 0.2f),
                            contentColor = AccentCoral
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_memory_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Memory")
                    }
                }
            }

            // 5. About SD Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ElectricSky)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("About SD Assistant", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "SD Assistant v1.0\nNative Android AI Voice Assistant powered by Gemini Live API, real-time bidirectional PCM audio, instant interruption, and native device action system.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

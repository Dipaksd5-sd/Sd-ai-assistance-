package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.domain.AssistantEngine
import com.example.permissions.PermissionManager
import com.example.service.SDAssistantService
import com.example.ui.MainScreen
import com.example.ui.SettingsScreen
import com.example.ui.theme.SDAssistantTheme

enum class Screen {
    MAIN,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var assistantEngine: AssistantEngine

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (micGranted) {
            assistantEngine.startAssistant()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        assistantEngine = AssistantEngine.getInstance(this)

        // Request necessary permissions on first launch
        val permissionsToRequest = mutableListOf<String>()
        if (!PermissionManager.hasRecordAudioPermission(this)) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (!PermissionManager.hasNotificationPermission(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            assistantEngine.startAssistant()
        }

        setContent {
            SDAssistantTheme {
                var currentScreen by remember { mutableStateOf(Screen.MAIN) }

                Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                    when (screen) {
                        Screen.MAIN -> {
                            MainScreen(
                                assistantEngine = assistantEngine,
                                onOpenSettings = { currentScreen = Screen.SETTINGS },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Screen.SETTINGS -> {
                            SettingsScreen(
                                assistantEngine = assistantEngine,
                                onBack = { currentScreen = Screen.MAIN },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (PermissionManager.hasRecordAudioPermission(this)) {
            assistantEngine.startAssistant()
        }
    }

    override fun onStop() {
        super.onStop()
        // If background service is NOT enabled, release mic/audio to conserve resources
        if (!assistantEngine.preferences.value.isBackgroundServiceEnabled) {
            assistantEngine.stopListening()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!assistantEngine.preferences.value.isBackgroundServiceEnabled) {
            assistantEngine.release()
        }
    }
}

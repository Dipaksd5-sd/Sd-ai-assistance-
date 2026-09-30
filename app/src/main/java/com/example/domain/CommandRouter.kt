package com.example.domain

import android.util.Log
import com.example.domain.model.DeviceActionResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Locale

class CommandRouter(private val deviceActionManager: DeviceActionManager) {

    companion object {
        private const val TAG = "COMMAND_ROUTER"
    }

    private val _lastActionResult = MutableSharedFlow<DeviceActionResult>(replay = 1)
    val lastActionResult: SharedFlow<DeviceActionResult> = _lastActionResult.asSharedFlow()

    /**
     * Executes an approved Gemini Live tool function call.
     */
    suspend fun executeTool(name: String, args: Map<String, Any?>): Map<String, Any?> {
        Log.d(TAG, "Routing Gemini function: $name with args: $args")

        val result: DeviceActionResult = try {
            when (name) {
                "openApp" -> {
                    val appName = args["appName"]?.toString() ?: ""
                    deviceActionManager.openApp(appName)
                }
                "searchYouTube" -> {
                    val query = args["query"]?.toString() ?: ""
                    deviceActionManager.searchYouTube(query)
                }
                "openUrl" -> {
                    val url = args["url"]?.toString() ?: ""
                    deviceActionManager.openUrl(url)
                }
                "getBatteryStatus" -> {
                    deviceActionManager.getBatteryStatus()
                }
                "toggleFlashlight" -> {
                    val state = args["state"]?.toString() ?: "TOGGLE"
                    deviceActionManager.toggleFlashlight(state)
                }
                "openCamera" -> {
                    deviceActionManager.openCameraApp()
                }
                "takePhoto" -> {
                    deviceActionManager.takePhotoWithCameraX()
                }
                "getCurrentTime" -> {
                    deviceActionManager.getCurrentTime()
                }
                "openSettings" -> {
                    val settingType = args["settingType"]?.toString()
                    deviceActionManager.openSystemSettings(settingType)
                }
                else -> {
                    DeviceActionResult.error("Unsupported function: $name")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception routing function: $name", e)
            DeviceActionResult.error("Execution failed: ${e.message}")
        }

        _lastActionResult.emit(result)

        return mapOf(
            "status" to if (result.success) "success" else "error",
            "message" to result.message,
            "spokenFeedback" to (result.spokenFeedback ?: result.message),
            "data" to result.data
        )
    }

    /**
     * Local direct fallback parser for quick actions.
     */
    suspend fun handleDirectCommand(command: String): DeviceActionResult {
        val query = command.trim().lowercase(Locale.ROOT)
        val result = when {
            query.contains("youtube") && query.contains("search") -> {
                val searchPart = query.substringAfter("search").trim()
                deviceActionManager.searchYouTube(searchPart.ifEmpty { "AI news" })
            }
            query.contains("youtube") -> deviceActionManager.openApp("youtube")
            query.contains("chrome") -> deviceActionManager.openApp("chrome")
            query.contains("battery") -> deviceActionManager.getBatteryStatus()
            query.contains("flashlight on") || query.contains("torch on") -> deviceActionManager.toggleFlashlight("ON")
            query.contains("flashlight off") || query.contains("torch off") -> deviceActionManager.toggleFlashlight("OFF")
            query.contains("flashlight") || query.contains("torch") -> deviceActionManager.toggleFlashlight("TOGGLE")
            query.contains("photo") || query.contains("picture") -> deviceActionManager.takePhotoWithCameraX()
            query.contains("camera") -> deviceActionManager.openCameraApp()
            query.contains("time") || query.contains("date") -> deviceActionManager.getCurrentTime()
            query.contains("settings") -> deviceActionManager.openSystemSettings("system")
            else -> deviceActionManager.openApp(query)
        }
        _lastActionResult.emit(result)
        return result
    }
}

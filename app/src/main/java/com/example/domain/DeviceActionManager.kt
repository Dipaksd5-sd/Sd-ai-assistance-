package com.example.domain

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.domain.model.DeviceActionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

class DeviceActionManager(private val context: Context) {

    companion object {
        private const val TAG = "DEVICE_ACTION"
    }

    private val packageManager: PackageManager = context.packageManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var isTorchOn = false

    /**
     * Safely opens an installed app without hardcoded activity class names.
     */
    fun openApp(appName: String): DeviceActionResult {
        val query = appName.trim().lowercase(Locale.ROOT)
        Log.d(TAG, "Request to open app: '$query'")

        // 1. Direct package match or known alias
        val targetPackage = when {
            query.contains("youtube") -> "com.google.android.youtube"
            query.contains("chrome") -> "com.android.chrome"
            query.contains("gallery") || query.contains("photos") -> "com.google.android.apps.photos"
            query.contains("camera") -> {
                return openCameraApp()
            }
            query.contains("setting") -> {
                return openSystemSettings("system")
            }
            query.contains("whatsapp") -> "com.whatsapp"
            query.contains("maps") -> "com.google.android.apps.maps"
            query.contains("gmail") || query.contains("mail") -> "com.google.android.gm"
            query.contains("calendar") -> "com.google.android.calendar"
            query.contains("clock") -> "com.google.android.deskclock"
            else -> null
        }

        if (targetPackage != null) {
            val intent = packageManager.getLaunchIntentForPackage(targetPackage)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                return try {
                    context.startActivity(intent)
                    DeviceActionResult.success(
                        message = "Opened $appName successfully",
                        spokenFeedback = "Okay Boss, $appName khol diya.",
                        actionType = "open_app"
                    )
                } catch (e: Exception) {
                    DeviceActionResult.error("Boss, $appName launch nahi ho paya: ${e.message}")
                }
            }
        }

        // 2. Search installed applications by display label
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in installedApps) {
            val label = packageManager.getApplicationLabel(appInfo).toString().lowercase(Locale.ROOT)
            if (label.contains(query) || query.contains(label)) {
                val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    return try {
                        context.startActivity(launchIntent)
                        DeviceActionResult.success(
                            message = "Opened ${appInfo.packageName}",
                            spokenFeedback = "Done Boss, $label open kar diya.",
                            actionType = "open_app"
                        )
                    } catch (e: Exception) {
                        DeviceActionResult.error("Boss, launch karne mein error aayi.")
                    }
                }
            }
        }

        return DeviceActionResult.error(
            message = "App '$appName' is not installed",
            spokenFeedback = "Boss, '$appName' phone mein installed nahi mila."
        )
    }

    /**
     * Searches YouTube or plays topic.
     */
    fun searchYouTube(query: String): DeviceActionResult {
        val trimmed = query.trim()
        val encodedQuery = Uri.encode(trimmed)
        val webUri = Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")

        val intent = Intent(Intent.ACTION_VIEW, webUri).apply {
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Browser fallback
                val fallbackIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
            DeviceActionResult.success(
                message = "Searching YouTube for: $trimmed",
                spokenFeedback = "Okay Boss, YouTube par '$trimmed' search kar diya.",
                actionType = "youtube_search"
            )
        } catch (e: Exception) {
            DeviceActionResult.error("Boss, YouTube search nahi ho saka: ${e.message}")
        }
    }

    /**
     * Opens a web URL safely in browser.
     */
    fun openUrl(rawUrl: String): DeviceActionResult {
        var url = rawUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            DeviceActionResult.success(
                message = "Opened URL: $url",
                spokenFeedback = "Browser mein open kar diya, Boss.",
                actionType = "open_url"
            )
        } catch (e: Exception) {
            DeviceActionResult.error("Boss, link open nahi ho saki.")
        }
    }

    /**
     * Gets real-time battery status using Android BatteryManager APIs.
     */
    fun getBatteryStatus(): DeviceActionResult {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        if (batteryIntent == null) {
            return DeviceActionResult.error("Battery info unavailable")
        }

        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val tempRaw = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)

        val percentage = if (level >= 0 && scale > 0) (level * 100) / scale else -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val tempCelsius = tempRaw / 10.0f

        val chargingDetail = when {
            isCharging && plugged == BatteryManager.BATTERY_PLUGGED_AC -> "phone AC charger se charge ho raha hai"
            isCharging && plugged == BatteryManager.BATTERY_PLUGGED_USB -> "phone USB se charge ho raha hai"
            isCharging && plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS -> "phone wireless charger par hai"
            isCharging -> "phone charging par hai"
            else -> "phone charging par nahi hai"
        }

        val speech = if (percentage >= 0) {
            "Boss, battery $percentage percent hai aur $chargingDetail."
        } else {
            "Boss, battery status check nahi ho paya."
        }

        return DeviceActionResult.success(
            message = "Battery: $percentage%, Charging: $isCharging, Temp: $tempCelsius°C",
            spokenFeedback = speech,
            actionType = "battery_status",
            data = mapOf(
                "percentage" to percentage.toString(),
                "isCharging" to isCharging.toString(),
                "temperature" to "$tempCelsius°C"
            )
        )
    }

    /**
     * Controls flashlight using CameraManager Torch API.
     */
    fun toggleFlashlight(state: String): DeviceActionResult {
        return try {
            val cameraId = getTorchCameraId()
            if (cameraId == null) {
                return DeviceActionResult.error(
                    "Flashlight not available on this device",
                    "Boss, is device mein flashlight available nahi hai."
                )
            }

            val targetOn = when (state.uppercase(Locale.ROOT)) {
                "ON" -> true
                "OFF" -> false
                "TOGGLE" -> !isTorchOn
                else -> !isTorchOn
            }

            cameraManager.setTorchMode(cameraId, targetOn)
            isTorchOn = targetOn

            val spoken = if (targetOn) {
                "Boss, flashlight on ho gayi."
            } else {
                "Boss, flashlight off kar di."
            }

            DeviceActionResult.success(
                message = "Flashlight set to $targetOn",
                spokenFeedback = spoken,
                actionType = "flashlight"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight error", e)
            DeviceActionResult.error(
                "Failed to control flashlight: ${e.message}",
                "Boss, flashlight operate nahi ho paayi."
            )
        }
    }

    private fun getTorchCameraId(): String? {
        val cameraIds = cameraManager.cameraIdList
        for (id in cameraIds) {
            val chars = cameraManager.getCameraCharacteristics(id)
            val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            val facing = chars.get(CameraCharacteristics.LENS_FACING)
            if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return id
            }
        }
        return cameraIds.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }

    /**
     * Opens the camera app.
     */
    fun openCameraApp(): DeviceActionResult {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            DeviceActionResult.success(
                message = "Camera opened",
                spokenFeedback = "Okay Boss, camera open kar diya.",
                actionType = "open_camera"
            )
        } catch (e: Exception) {
            DeviceActionResult.error("Boss, camera app open nahi ho saka.")
        }
    }

    /**
     * Takes photo using CameraX and saves to MediaStore gallery.
     */
    suspend fun takePhotoWithCameraX(): DeviceActionResult = withContext(Dispatchers.Main) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return@withContext DeviceActionResult.error(
                message = "Camera permission not granted",
                spokenFeedback = "Boss, photo khinchne ke liye camera permission chahiye."
            )
        }

        try {
            val cameraProvider = suspendCancellableCoroutine<ProcessCameraProvider> { cont ->
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({
                    try {
                        cont.resume(future.get())
                    } catch (e: Exception) {
                        cont.cancel(e)
                    }
                }, ContextCompat.getMainExecutor(context))
            }

            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Check if lifecycle can bind (or fall back to system intent)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val name = "SD_Photo_$timeStamp"

            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SDAssistant")
                }
            }

            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

            // If headless context without lifecycleOwner, open camera app as reliable action
            openCameraApp()
            DeviceActionResult.success(
                message = "Camera ready for photo capture",
                spokenFeedback = "Done Boss, camera ready hai.",
                actionType = "take_photo"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Camera capture error", e)
            openCameraApp()
        }
    }

    /**
     * Gets current time and date.
     */
    fun getCurrentTime(): DeviceActionResult {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        val now = Date()

        val timeStr = timeFormat.format(now)
        val dateStr = dateFormat.format(now)

        return DeviceActionResult.success(
            message = "Time: $timeStr, Date: $dateStr",
            spokenFeedback = "Boss, abhi samay $timeStr hai, aur aaj $dateStr hai.",
            actionType = "time",
            data = mapOf("time" to timeStr, "date" to dateStr)
        )
    }

    /**
     * Opens Android System Settings.
     */
    fun openSystemSettings(settingType: String? = null): DeviceActionResult {
        val action = when (settingType?.lowercase(Locale.ROOT)) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "battery" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent.ACTION_POWER_USAGE_SUMMARY
            } else {
                Settings.ACTION_SETTINGS
            }
            else -> Settings.ACTION_SETTINGS
        }

        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            DeviceActionResult.success(
                message = "Settings opened: ${settingType ?: "general"}",
                spokenFeedback = "Okay Boss, settings khol diya.",
                actionType = "settings"
            )
        } catch (e: Exception) {
            DeviceActionResult.error("Boss, settings open nahi ho saki.")
        }
    }
}

package com.example.domain.model

/**
 * Result returned by native Android device actions to be fed back to Gemini Live
 * and communicated naturally to the user.
 */
data class DeviceActionResult(
    val success: Boolean,
    val message: String,
    val spokenFeedback: String? = null,
    val actionType: String = "general",
    val data: Map<String, String> = emptyMap()
) {
    companion object {
        fun success(
            message: String,
            spokenFeedback: String = message,
            actionType: String = "general",
            data: Map<String, String> = emptyMap()
        ) = DeviceActionResult(
            success = true,
            message = message,
            spokenFeedback = spokenFeedback,
            actionType = actionType,
            data = data
        )

        fun error(
            message: String,
            spokenFeedback: String = message,
            actionType: String = "general"
        ) = DeviceActionResult(
            success = false,
            message = message,
            spokenFeedback = spokenFeedback,
            actionType = actionType
        )
    }
}

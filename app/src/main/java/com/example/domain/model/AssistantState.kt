package com.example.domain.model

/**
 * High-level state of the SD voice assistant.
 */
enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR,
    DISCONNECTED
}

/**
 * Real-time connection status with Gemini Live WebSocket server.
 */
enum class GeminiConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

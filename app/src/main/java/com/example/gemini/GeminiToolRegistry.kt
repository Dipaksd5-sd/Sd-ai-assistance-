package com.example.gemini

import org.json.JSONArray
import org.json.JSONObject

object GeminiToolRegistry {

    fun getToolDeclarationsJson(): JSONArray {
        val toolsArray = JSONArray()
        val toolObject = JSONObject()
        val funcArray = JSONArray()

        // 1. openApp
        funcArray.put(JSONObject().apply {
            put("name", "openApp")
            put("description", "Opens an installed application on the user's Android phone such as YouTube, Chrome, Camera, Gallery, Settings, WhatsApp, etc.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("appName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The name of the app to launch (e.g. 'youtube', 'chrome', 'camera', 'gallery', 'settings')")
                    })
                })
                put("required", JSONArray().apply { put("appName") })
            })
        })

        // 2. searchYouTube
        funcArray.put(JSONObject().apply {
            put("name", "searchYouTube")
            put("description", "Searches for videos, songs, or topics on YouTube and opens the YouTube results.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The search query keywords for YouTube (e.g. 'AI news', 'techno song', 'latest tech review')")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })

        // 3. openUrl
        funcArray.put(JSONObject().apply {
            put("name", "openUrl")
            put("description", "Opens a website URL in the phone's web browser.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("url", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The web URL to open, e.g. 'https://www.google.com'")
                    })
                })
                put("required", JSONArray().apply { put("url") })
            })
        })

        // 4. getBatteryStatus
        funcArray.put(JSONObject().apply {
            put("name", "getBatteryStatus")
            put("description", "Checks current phone battery percentage, charging state (plugged in or not), and battery health.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 5. toggleFlashlight
        funcArray.put(JSONObject().apply {
            put("name", "toggleFlashlight")
            put("description", "Controls the phone flashlight / torch.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("state", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "'ON', 'OFF', or 'TOGGLE'")
                    })
                })
                put("required", JSONArray().apply { put("state") })
            })
        })

        // 6. openCamera
        funcArray.put(JSONObject().apply {
            put("name", "openCamera")
            put("description", "Opens the phone camera application.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 7. takePhoto
        funcArray.put(JSONObject().apply {
            put("name", "takePhoto")
            put("description", "Takes a photo instantly using native CameraX and stores it in the device photo gallery.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 8. getCurrentTime
        funcArray.put(JSONObject().apply {
            put("name", "getCurrentTime")
            put("description", "Gets the current time, day, and date from the phone.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 9. openSettings
        funcArray.put(JSONObject().apply {
            put("name", "openSettings")
            put("description", "Opens Android system settings or a specific settings panel (e.g. 'wifi', 'bluetooth', 'display', 'sound', 'battery', 'system').")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("settingType", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Specific settings target: 'system', 'wifi', 'bluetooth', 'battery', 'display', 'sound'")
                    })
                })
            })
        })

        toolObject.put("functionDeclarations", funcArray)
        toolsArray.put(toolObject)
        return toolsArray
    }
}

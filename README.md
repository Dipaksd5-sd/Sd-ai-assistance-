# SD Assistant — Native Android Voice Assistant

SD Assistant is a production-oriented, native Android voice assistant powered by the **Gemini Live API**, Kotlin Coroutines, and Jetpack Compose. It runs directly on your Android phone without external bridges, Termux, or localhost servers.

---

## 🚀 Features

- **Gemini Live Bidirectional Audio:** Direct WebSocket streaming with 16 kHz PCM microphone capture and 24 kHz speech playback (`gemini-2.5-flash-native-audio-preview-12-2025`).
- **Instant Interruption:** Immediate cancellation and audio track flush when user speech is detected while SD is speaking.
- **Native Android Device Controls:**
  - YouTube launch & search
  - Flashlight control (Torch API)
  - Battery telemetry (percentage, charging state, temperature)
  - Camera & photo capture via CameraX
  - System settings and date/time queries
- **Futuristic Jetpack Compose UI:** Animated multi-ring glowing orb responding in real time to microphone volume and assistant speech.
- **Foreground Service:** Continuous background voice operation with Android 14/15 microphone foreground service compliance.

---

## 🛠️ Automated CI/CD (GitHub Actions)

This repository includes a pre-configured GitHub Actions workflow (`.github/workflows/build-and-release-debug-apk.yml`) to automatically compile the debug APK and publish it as a GitHub Release.

### Workflow Triggers

1. **Push to `main` or `master`:** Automatically builds the debug APK, runs verification, and publishes a new release.
2. **Version Tag Push (`v*`):** Pushing any tag like `git tag v1.0.0 && git push origin v1.0.0` triggers an automated release tagged with `v1.0.0`.
3. **Manual Trigger (`workflow_dispatch`):** Go to **Actions** → **Build and Publish Debug APK** → **Run workflow** to build anytime with custom release titles and tags.

### Setting Up API Key in GitHub Secrets (Optional)

1. Open your GitHub Repository.
2. Navigate to **Settings** → **Secrets and variables** → **Actions**.
3. Click **New repository secret**.
4. Set Name: `GEMINI_API_KEY` and Value: your Gemini API key.
5. If omitted, the build will use the placeholder from `.env.example`, and you can enter your custom API key directly in the SD Assistant Settings screen inside the app.

### Where to Find the APK

- **GitHub Releases:** Navigate to the **Releases** tab on GitHub to download `SD-Assistant-debug.apk`.
- **Workflow Artifacts:** In any completed Actions run, download `SD-Assistant-debug-apk` from the **Artifacts** section.

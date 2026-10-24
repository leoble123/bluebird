# Bluebird

An Android messaging app with an iMessage-inspired design and a distinct Android character.

**Current milestone:** UI-first, powered entirely by mock data. Bluebird does **not** send or receive real
messages (no SMS, no iMessage). The connection status is always shown as simulated.

## Status

| Area | State |
| --- | --- |
| Project foundation, navigation shell, light/dark theme | Done (Prompt 1) |
| Design system | Not started |
| Inbox | Placeholder list wired to mock data |
| Conversation, Contacts, Settings | Placeholder screens |
| Local persistence (Room) | Not started |
| Real messaging transport | Out of scope for this milestone |

## Tech stack

Kotlin · Jetpack Compose · Material 3 (customised) · MVVM · Coroutines/Flow · Navigation Compose

| Tool | Version |
| --- | --- |
| Android Gradle Plugin | 9.4.1 (built-in Kotlin) |
| Gradle | 9.6.0 (via wrapper) |
| Kotlin | 2.4.21 |
| Compose BOM | 2026.09.00 |
| JDK | 17 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |

Application ID: `com.bluebird.messaging`

## Project layout

A single `app` module, split by package:

```
app/src/main/java/com/bluebird/messaging/
  core/model          shared data models (Contact, Conversation, Message)
  core/provider       MessagingProvider interface + MockMessagingProvider
  core/designsystem   theme and shared UI components
  feature/inbox       conversation list
  feature/conversation  message timeline and composer
  feature/contacts    contact browsing
  feature/settings    preferences, diagnostics, connection status
  navigation          app navigation graph
```

**Why one module for now:** most feature areas are still placeholders, and separate Gradle modules would be
empty shells that slow the build. The package boundaries mirror the planned modules, so `core` and each `feature`
can be promoted to its own module later (most likely when real transport work begins) without reorganising code.

Screens never talk to a messaging network directly. They depend on `MessagingProvider`; today that is
`MockMessagingProvider`, wired in `AppContainer.kt`. A real provider can replace it there.

## Building

### Prerequisites

- **JDK 17** (Microsoft OpenJDK, Temurin, or the JDK bundled with Android Studio)
- **Android SDK** with `platforms;android-37.0`, `build-tools;36.1.0`, and `platform-tools`
- Either set `ANDROID_HOME` to the SDK folder, or create `local.properties` in the project root containing
  `sdk.dir=<path to SDK>`

Android Studio installs all of this for you. On this Windows machine the setup script `tools/setup-android-sdk.ps1`
installed the SDK into `%LOCALAPPDATA%\Android\Sdk`.

### Windows (PowerShell)

```powershell
.\gradlew.bat assembleDebug          # build the debug APK
.\gradlew.bat testDebugUnitTest      # run unit tests
```

### macOS (Terminal)

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

On macOS, if Android Studio is installed, the SDK is usually at `~/Library/Android/sdk`.

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Installing on a device

1. On the phone or tablet, enable **Developer options** (tap *Build number* 7 times in *About phone*), then turn on
   **USB debugging**.
2. Connect it by USB and accept the debugging prompt.
3. Run `adb install -r app/build/outputs/apk/debug/app-debug.apk` (or `.\gradlew.bat installDebug`).

You can also copy the APK to the device and open it, after allowing installs from that source.

## Known limitations

- Everything shown is sample data; nothing persists across app restarts yet.
- Screens other than the inbox are placeholders.
- No emulator or physical device testing has been done yet.

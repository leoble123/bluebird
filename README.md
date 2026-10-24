# Bluebird

An Android messaging app with an iMessage-inspired design and a distinct Android character.

**Current milestone:** UI-first, powered entirely by mock data. Bluebird does **not** send or receive real
messages (no SMS, no iMessage). The connection status is always shown as simulated.

## Status

| Area | State |
| --- | --- |
| Project foundation, navigation shell, light/dark theme | Done (Prompt 1) |
| Design system ("icy frost") | Done (Prompt 2); browse it in Settings → Design system |
| Inbox | Interim list using design-system rows, wired to mock data |
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

## Design system

Lives in `core/designsystem`. Screens read everything through `BluebirdTheme.colors`, `.typography`, `.motion`,
`.layout` and the tokens in `Tokens.kt`.

- **Identity:** cobalt primary, apricot accent used sparingly, cool glacier neutrals (light) and abyssal ice layers
  (dark). The system font with an iOS-like type rhythm. A custom rounded-stroke icon set (`BluebirdIcons`).
- **Icy frost** (`Frost.kt`): hazy translucent surfaces with a soft top glow, two faint refraction streaks, a
  delicate rim light, and a pre-generated grain texture. All static brushes cached per size: no blur, no runtime
  shaders, no animation. Used only on chrome: search, composer, frosted top bars, menus, selection, reactions.
  `FrostSettings(enabled = false)` turns every frost surface solid.
- **Signature details:** the "ice facet" (a tapered outer corner on the last bubble of a group), deterministic
  two-tone monogram avatars, a spring "settle" on the send button, gentle press-compression on bubbles and buttons,
  large emoji-only messages, and a small ice glint on empty states.
- **Performance rules:** message bubbles are flat solid colors; animated values are read in the draw/layer phase;
  typing uses `TextFieldState` so keystrokes don't recompose screens; all motion collapses to instant when system
  animations are off.
- **Adaptive:** `BluebirdLayout` provides gutters, readable widths, list-pane width and bubble width caps for
  compact, medium and expanded windows.
- **Future calling:** tokens already include call accept/decline colors and hero avatar sizes; phone and video
  glyphs exist in the icon set.

### Screenshot tests

`.\gradlew.bat recordRoborazziDebug` renders the design system on the JVM (Robolectric + Roborazzi, no emulator)
in light and dark, at phone and tablet widths, into `app/build/outputs/roborazzi/`.

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
- Conversation, Contacts and Settings are still placeholders.
- No emulator or physical device testing has been done yet. Visuals have been checked only through JVM
  screenshot renders, which do not measure real-device frame timing.

# Bluebird

An Android messaging app with an iMessage-inspired design and a distinct Android character.

**Current milestone:** UI-first, powered entirely by mock data. Bluebird does **not** send or receive real
messages (no SMS, no iMessage). The connection status is always shown as simulated.

## Status

| Area | State |
| --- | --- |
| Project foundation, navigation shell, light/dark theme | Done (Prompt 1) |
| Design system (iOS-style frosted glass) | Done (Prompt 2); browse it in Settings → Design system |
| Inbox | Done (Prompt 3): search, pinned grid, swipe and long-press actions, delete with undo, new message |
| Conversation | Done (Prompt 4): timeline, replies, reactions, selection, copy/delete with undo, drafts |
| Contacts, Settings | Placeholder screens |
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

- **Identity:** cobalt primary, apricot accent used sparingly, iOS-style neutrals (white and grouped gray in light,
  black and graphite in dark). The system font with an iOS-like type rhythm. A custom rounded-stroke icon set (`BluebirdIcons`).
- **Frosted glass** (`Glass.kt`): real iOS-style backdrop blur via the Haze library. Floating chrome (top bars,
  the composer, menus) blurs the content scrolling beneath it under a translucent white or graphite tint. GPU blur
  on Android 12+; a translucent fill on Android 11 and below, or when `GlassSettings(blur = false)` is set.
  Never used on list rows or message bubbles.
- **Signature details:** the facet (a tapered outer corner on the last bubble of a group), deterministic two-tone
  monogram avatars, a spring "settle" on the send button, gentle press-compression on bubbles and buttons, and large
  emoji-only messages.
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

## Inbox

`feature/inbox`. All logic lives in `InboxViewModel`; the screen only renders pre-formatted `InboxItem`s.

- Large "Messages" title that scrolls away under a glass top bar, with a "Simulated" label.
- Search across contact names, handles and message text; matches are highlighted, and content matches show the
  matching message.
- Pinned conversations as an avatar grid at the top (in the order they were pinned).
- Swipe right to mark read/unread, swipe left to delete (with a 4-second undo). The same actions, plus pin/unpin,
  are in the long-press menu and exposed to TalkBack as accessibility actions.
- Opening a conversation marks it read. "New message" opens a contact picker that reuses an existing thread or
  starts a new one.

## Conversation

`feature/conversation`. `buildTimeline` (pure, unit-tested) turns messages into rows; `ConversationViewModel` owns
all state; the screen only renders.

- Bottom-anchored timeline: new messages appear above the composer and follow you if you're at the bottom, but
  never yank you out of older history. A jump-to-latest button appears when scrolled up.
- Bubble grouping for messages from the same sender within 5 minutes, and time separators after gaps of more
  than an hour or a new day ("Today 9:41 AM", "Yesterday …", "Tuesday …", "Sat, Oct 12 at …").
- Delivery status under your latest message only; "Not delivered" on any failed message. Tap a bubble to see its
  time.
- Long-press menu: tapbacks (❤️ 👍 👎 😂 ‼️ ❓), Reply, Copy, Select, Delete. Replies quote the original inside the
  bubble and above the composer. Selection mode supports multi-copy and multi-delete. Deletes have a 4-second undo.
- Group conversations show sender names and avatars once per run.
- Drafts are kept per conversation when you leave and come back (in memory until Prompt 5).
- Header shows the avatar and name; tapping it opens details, where calling is shown as unavailable and the
  conversation is clearly labelled simulated. The attachment button lists photo and camera options as "coming in a
  later update".

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

- The inbox is fully functional against mock data, but changes (pins, read state, deletions, new conversations) are
  kept in memory only and reset when the app restarts, until local persistence arrives in Prompt 5.
- Replies are simulated: after you send a message it goes Sent → Delivered → Read, the mock contact shows
  "typing", then answers with a canned reply.
- Attachments (photos, camera) and emoji picking are not implemented yet.
- Conversation, Contacts and Settings are still placeholders.
- No emulator or physical device testing has been done yet. Visuals have been checked only through JVM
  screenshot renders, which do not measure real-device frame timing.

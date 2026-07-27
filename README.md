# Flip to Mute

Flip to Mute is an Android app concept for controlling incoming-call sound by placing a phone face down.

## Status

**Foundation setup.** Incoming-call handling and face-down detection are not implemented yet.

## Technology stack

- Kotlin
- Jetpack Compose with Material 3
- Simple MVVM foundation
- Navigation Compose
- Kotlin coroutines
- Preferences DataStore
- Gradle Kotlin DSL with a Gradle Version Catalog
- Minimum Android version: API 26 (Android 8.0)

## Clone and run

1. Clone the repository.
2. Open the project in a current stable version of Android Studio.
3. Allow Android Studio to sync the Gradle project and install the requested SDK components.
4. Select an emulator or physical device running API 26 or newer.
5. Run the `app` configuration.

The current app launches a temporary home screen that communicates the incomplete setup state. It does not request permissions or change call, sensor, or audio behavior.

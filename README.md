
# ThumbStudio

A mobile-first Android thumbnail editor for Roblox creators.

## Current features

- 16:9 thumbnail canvas
- Import an image from your phone
- Editable title with a bold outline
- Export thumbnails as 1280 × 720 PNG images
- Automatic debug APK builds with GitHub Actions

## Build the APK

1. Open the **Actions** tab.
2. Select **ThumbStudio Android Build**.
3. Open the latest successful workflow run.
4. Under **Artifacts**, download `ThumbStudio-debug-APK`.
5. Extract the ZIP and install `app-debug.apk` on your Android device.

This is an early development build, not a Play Store release.

## Planned features

- Editable 3D scenes and character models
- More text styles, layers, and effects
- Project saving
- Optional AI-assisted scene creation

## Development

- Kotlin
- Android Gradle Plugin 8.7.3
- Gradle 8.9
- Java 17

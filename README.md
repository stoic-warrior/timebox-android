# Timebox Android

Native Android timeboxing prototype built with Kotlin + Jetpack Compose.

## Current UX

- 00:00-24:00 vertical timeline
- Date navigation with previous/next day
- Blocks show title and start/end time
- Long-press empty space to create a 30-minute block
- Long-press a block and drag to move it
- Drag the bottom handle to resize
- 5-minute snapping
- Blocks cannot overlap or leave the day range
- Double-tap a block to edit its title or delete it
- Data is stored locally per date with SharedPreferences

## Open

Open this folder in Android Studio:

`outputs/timebox-android`

Then run the `app` configuration on an emulator or Android device.

## Build

```bash
./gradlew assembleDebug
```

Debug APK:

`app/build/outputs/apk/debug/app-debug.apk`

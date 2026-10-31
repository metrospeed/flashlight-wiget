<img src="assets/icon.svg" width="64" height="64" alt="">

# flashlight wiget

Created in [T3 Code](https://t3.codes).

A minimal Android app with a 1×1 home screen widget that toggles the flashlight.
The widget turns yellow while the light is on. Requires Android 8.0+ and a phone with a camera flash.

## Build

Needs JDK 17+ and the Android SDK (`sdk.dir` in `local.properties`, or `ANDROID_HOME`).

```sh
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```

Install with `adb install app/build/outputs/apk/release/app-release.apk`, or copy the APK to the phone and open it.
The release build is signed with the debug key so it can be sideloaded as-is.

## Use

Long-press the home screen → **Widgets** → drag **Flashlight** onto it (or tap **Add widget to home screen** in the app).
Tap the widget to toggle the light. While it's on, a "Flashlight is on" notification is shown; tapping it also turns the light off.

## How it works

- `FlashlightWidget` — the widget; its tap starts `FlashlightService`.
- `FlashlightService` — turns the torch on/off with `CameraManager.setTorchMode` (no camera permission needed).
  It runs as a foreground service while the light is on, because Android turns the torch off
  if the process that enabled it is killed. It also tracks changes from the quick settings tile.
- `MainActivity` — a big toggle button plus instructions.

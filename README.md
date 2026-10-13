# Magnetic Gesture Actions

An offline Android application for triggering phone actions using any permanent
magnet and the phone's built-in magnetometer (`TYPE_MAGNETIC_FIELD`). Works on
any Android phone that exposes a public magnetometer.

## Current implementation

- Kotlin, Jetpack Compose Material 3, MVVM-compatible sensor/domain separation.
- Public `SensorManager` magnetometer via `TYPE_MAGNETIC_FIELD`.
- Baseline, adaptive tracking, hysteresis-style approach/presence/removal state,
  cooldown, and synthetic-testable detector.
- Deterministic single/double/triple tap segmentation API.
- User-started foreground service with a persistent monitoring notification.
- Unit tests for magnitude and magnetic approach/removal.

## Build

Open this directory in Android Studio, or use the Gradle wrapper after generating
it with `gradle wrapper`:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

The project uses compile SDK 36, minimum SDK 26, and target SDK 35. A local
Android SDK is required; `local.properties` is machine-specific and ignored.

## Test procedure

1. Enable Developer options and USB debugging on your Android device.
2. Install `app/build/outputs/apk/debug/app-debug.apk` from Android Studio or
   `adb install`.
3. Open the dashboard and confirm a magnetometer is listed.
4. Keep the phone stationary, observe the baseline, and move a magnet
   toward and away from the back in several positions.
5. Record whether readings change and whether a full removal event is reported.
6. Repeat with a case installed. Detection distance is not universal: it
   depends on magnet strength, orientation, sensor location, and case thickness.

Short rapid gestures can be missed because sensor sampling rate and magnetic
geometry vary. Do not rely on this application for safety-critical actions.

## Connected-device verification

On 2026-10-09, a USB-debugging Samsung SM-A356E (Android 16) was connected.
The device exposed 35 sensors, including `AK09918C Magnetometer` from Asahi
Kasei Microdevices. Live magnetometer readings were successfully received by
this app. The magnetometer is the sole sensor used for gesture detection.

## How it works

Bring any permanent magnet close to the phone and pull it away — that counts
as one tap. The app groups taps by timing into single, double, or triple
patterns. Each pattern can be mapped independently to a notification,
flashlight, media play/pause, URL, or installed app. Settings are saved
locally and applied by the foreground service, including while another app
is visible.

Calibrate with the magnet away from the phone. Recalibrate if the magnet,
case, or phone orientation changes. The pattern window is 700 ms — a
single-tap action waits for that window so it can still distinguish it from a
double or triple tap. Flashlight hardware activates immediately after the
pattern is finalised.
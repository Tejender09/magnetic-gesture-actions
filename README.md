# Magnetic Secret Actions

An offline Android application for triggering phone actions with a magnetic
finger-ring using the phone's built-in magnetometer (TYPE_MAGNETIC_FIELD).
The primary target is Samsung Galaxy A35 5G (SM-A356E), but the app works
on any Android phone that exposes a public magnetometer.

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

## A35 test procedure

1. Enable Developer options and USB debugging on the SM-A356E.
2. Install `app/build/outputs/apk/debug/app-debug.apk` from Android Studio or
   `adb install`.
3. Open the dashboard and confirm a magnetometer is listed.
4. Keep the phone stationary, observe the baseline, and move a small magnet
   toward and away from the back in several positions.
5. Record whether readings change and whether a full removal event is reported.
6. Repeat with the case installed. Detection distance is not universal: it
   depends on magnet strength, orientation, sensor location, and case thickness.

Short rapid taps can be missed because sensor sampling rate and magnetic
geometry vary. Do not rely on this application for safety-critical actions.
## Connected-device verification

On 2026-10-09, a USB-debugging Samsung SM-A356E (Android 16) was connected.
The device exposed 35 sensors, including `AK09918C Magnetometer` from Asahi
Kasei Microdevices. Live magnetometer readings were successfully received by
this app. The magnetometer is the sole sensor used for gesture detection.

## Magnetic finger-ring mode

The intended input is a small permanent magnet mounted in or worn as a
finger-ring. Start monitoring explicitly and keep the ring away from the phone
while calibrating. After calibration, each approach-and-removal cycle is
treated as one tap; timing groups taps into single, double, or triple patterns.
Each pattern can be mapped independently to a notification, flashlight,
media play/pause, URL, or installed app. URL and app-package values, detector
sensitivity, and the pattern timeout are editable in the ring configuration
panel and are saved locally. The selected action is executed by the foreground
service, including while another app is visible. App actions require the
package name of an installed launchable app.

Ring magnets differ in polarity, size, orientation, and distance from the
phone. Calibrate again if the ring, case, phone orientation, or mounting
position changes. Android still requires a visible foreground-service
notification for continuous monitoring; this app does not claim silent
always-on operation after force-stop or reboot.

The ring pattern window is optimized to 700 ms. A single-tap action waits for
that window so the app can still distinguish it from a double or triple tap.
This is the main intentional latency in action execution; flashlight hardware
activation itself occurs immediately after the pattern is finalized.

# Location Changer (ਲੋਕੇਸ਼ਨ ਬਦਲੋ)

A deliberately simple Android app that changes your phone's location to a spot you pick on a map,
for every other app, and puts it back with one button. Built for a non-technical user: big
buttons, high contrast, English **and** Punjabi (Gurmukhi), no jargon, no ads, no accounts, no
backend.

The daily user never opens Developer options. The app turns Developer options on only if it has
to, and always turns it back off — so banking/UPI apps that complain about Developer options stay
happy.

- Package: `com.gurnoor.locationchanger`
- Tech: Kotlin, Jetpack Compose, Material 3, single-activity MVVM, MapLibre + OpenFreeMap tiles,
  Nominatim search, DataStore.
- minSdk 26, target/compileSdk 37.

---

## 1. Build and install

Requirements: JDK 17 and the Android SDK (platform 37). Point `local.properties` at your SDK:

```
sdk.dir=/Users/you/Library/Android/sdk
```

Build the debug APK:

```bash
./gradlew assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`. Install it on the phone (USB
debugging on):

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Run the unit tests:

```bash
./gradlew testDebugUnitTest
```

---

## 2. One-time ADB setup (do this once per phone)

Normal apps cannot mock location unless they are the selected "mock location app". Grant that once
with the phone connected and USB debugging on:

```bash
adb shell pm grant com.gurnoor.locationchanger android.permission.WRITE_SECURE_SETTINGS
adb shell appops set com.gurnoor.locationchanger android:mock_location allow
```

Open the app. If it still shows the English "Setup needed" screen, tap **Check again**. Once both
rows show ✓, that screen never appears again and the daily user only ever sees the simple home
screen.

> These two commands are also shown, copyable, inside the app's setup screen.

---

## 3. Verify it's working

1. In the app, pick a place (search a city/village, or tap the map) so a pin appears.
2. Tap the green **Change location here**. The status card turns green.
3. Open **Google Maps**. The blue dot should jump to the spot you chose (give it a few seconds).
4. Back in the app, tap the red **Stop – go back to real location**. Google Maps returns to your
   real position after a few seconds.

---

## 4. Samsung (One UI) — keep it running

So One UI doesn't stop the background service:

- The app asks once to **ignore battery optimization** — allow it.
- Also add the app to the never-sleeping list: **Settings → Battery → Background usage limits →
  Never sleeping apps → add Location Changer.**

---

## 5. Signed release APK

1. Create a keystore once:

   ```bash
   keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias locationchanger
   ```

2. Add the signing config to `app/build.gradle.kts` (keep secrets out of git — read them from
   `local.properties` or environment variables):

   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release.jks")
               storePassword = System.getenv("KEYSTORE_PASSWORD")
               keyAlias = System.getenv("KEY_ALIAS") ?: "locationchanger"
               keyPassword = System.getenv("KEY_PASSWORD")
           }
       }
       buildTypes {
           getByName("release") { signingConfig = signingConfigs.getByName("release") }
       }
   }
   ```

3. Build:

   ```bash
   ./gradlew assembleRelease
   ```

   The signed APK is at `app/build/outputs/apk/release/app-release.apk`.

---

## 6. Known limitations

- **Some apps detect mock locations directly** (banking, some games, ride apps) and may notice or
  refuse to use the faked position. This is unavoidable from a normal app.
- **A factory reset or a reinstall clears the ADB grant** — run the two commands in section 2
  again.
- The map and place search need internet. Saved places still work offline (you just won't see
  tiles or be able to search for new places).
- **Satellite view** (on by default, 🗺/🛰 button to switch) uses Esri World Imagery, which is free
  with no key but is meant for non-commercial / personal use with credit shown (the app shows it).
  In rural Punjab it is sharp to house level at zoom 18; beyond that the app stretches the zoom-18
  photo, because Esri has no closer imagery there. For a fully-licensed source, swap in Google Maps
  or MapTiler (both need an API key).
- **Developer options switch** in the app turns off Developer options *and* USB debugging together
  (SBI YONO checks USB debugging). Don't use the switch inside the Settings app to turn Developer
  options off — on Samsung that also wipes the mock-location permission and you'd need the
  `appops` command again.
- Mock location is only delivered to apps while the phone's own **Location** toggle is on; the app
  tells the user and offers a button to the location settings if it's off.

---

## How the "no Developer options" part works

On most modern phones, once the `mock_location` app-op is allowed (section 2), the app can mock
location **without Developer options being on at all**. So in practice the app records
*"works without Developer options: yes"* on the first run and never touches that setting again.

The app still keeps a safety strategy in case a device needs it:

1. On Start, try to mock **without** enabling Developer options.
2. Only if that fails, enable `Settings.Global.DEVELOPMENT_SETTINGS_ENABLED` and retry, remembering
   that *the app* turned it on.
3. On Stop, turn Developer options back off — but only if the app was the one that turned it on.
4. On boot (and on app start when nothing is being mocked), if the app had left Developer options
   on, turn it back off and clear the flag — so a crash or reboot never leaves the phone with
   Developer options on.

It never touches `ADB_ENABLED` or any other secure setting.

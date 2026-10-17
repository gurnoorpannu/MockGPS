# Location Changer — User Guide / ਵਰਤੋਂ ਗਾਈਡ

A simple guide for using the app. Part A is the one-time setup (for the son).
Part B is the everyday guide (for the daily user), in English and Punjabi.

---

## Part A — One-time setup (do this once)

You only do this once, with the phone connected to a computer by USB.

1. On the phone, turn on **USB debugging** (Settings → About phone → tap "Build
   number" 7 times → go back → Developer options → USB debugging).
2. Connect the phone to the computer and run these two commands:

   ```bash
   adb install -r app-debug.apk
   adb shell pm grant com.gurnoor.locationchanger android.permission.WRITE_SECURE_SETTINGS
   adb shell appops set com.gurnoor.locationchanger android:mock_location allow
   ```

3. Open the app. If it shows a black-and-white "Setup needed" screen, tap
   **Check again**. Once it passes, that screen never shows again.
4. On Samsung, add the app to **Settings → Battery → Background usage limits →
   Never sleeping apps**, and allow the battery prompt when the app asks.

After this, hand the phone back. The daily user never needs any of the above.

---

## Part B — Everyday use

### English

**The screen**
- A big map fills the screen, with a red pin in the middle.
- A search box is on top. A green button is at the bottom.

**To change your location**
1. Find the place you want:
   - **Move the map** with your finger until the red pin sits on the place, **or**
   - **Type a place name** in the search box and tap it in the list.
2. The name of the place shows at the bottom.
3. Tap the green **Change** button.
4. Done — your phone now shows that place to all other apps. Open Google Maps to
   see the blue dot there.

**To go back to your real location**
1. Tap the red **Stop** button.
2. Wait a few seconds. Your phone is back to your real place.

**Other buttons**
- **＋ / −** on the right: make the map bigger or smaller.
- **◎** on the right: jump back to where you really are.
- **Save**: remember the current place with a name.
- **Saved**: open your saved places and tap one to use it again.
- **English / ਪੰਜਾਬੀ** button on top: switch the language any time.

---

### ਪੰਜਾਬੀ

**ਸਕ੍ਰੀਨ**
- ਪੂਰੀ ਸਕ੍ਰੀਨ ਤੇ ਵੱਡਾ ਨਕਸ਼ਾ ਹੁੰਦਾ ਹੈ, ਵਿਚਕਾਰ ਲਾਲ ਪਿੰਨ।
- ਉੱਪਰ ਲੱਭਣ ਵਾਲਾ ਡੱਬਾ, ਹੇਠਾਂ ਹਰਾ ਬਟਨ।

**ਲੋਕੇਸ਼ਨ ਬਦਲਣ ਲਈ**
1. ਜਿਹੜੀ ਜਗ੍ਹਾ ਚਾਹੀਦੀ ਹੈ, ਉਹ ਲੱਭੋ:
   - **ਨਕਸ਼ਾ ਉਂਗਲ ਨਾਲ ਹਿਲਾਓ** ਤਾਂ ਜੋ ਲਾਲ ਪਿੰਨ ਉਸ ਜਗ੍ਹਾ ਤੇ ਆ ਜਾਵੇ, **ਜਾਂ**
   - **ਜਗ੍ਹਾ ਦਾ ਨਾਂ ਲਿਖੋ** ਅਤੇ ਲਿਸਟ ਵਿੱਚੋਂ ਉਸ ਤੇ ਛੋਹੋ।
2. ਜਗ੍ਹਾ ਦਾ ਨਾਂ ਹੇਠਾਂ ਦਿਖੇਗਾ।
3. ਹਰਾ **ਬਦਲੋ** ਬਟਨ ਦਬਾਓ।
4. ਹੋ ਗਿਆ — ਹੁਣ ਤੁਹਾਡਾ ਫ਼ੋਨ ਬਾਕੀ ਸਾਰੀਆਂ ਐਪਾਂ ਨੂੰ ਇਹੀ ਜਗ੍ਹਾ ਦੱਸੇਗਾ। ਵੇਖਣ ਲਈ
   ਗੂਗਲ ਮੈਪ ਖੋਲ੍ਹੋ।

**ਅਸਲੀ ਲੋਕੇਸ਼ਨ ਤੇ ਵਾਪਸ ਜਾਣ ਲਈ**
1. ਲਾਲ **ਬੰਦ** ਬਟਨ ਦਬਾਓ।
2. ਕੁਝ ਸਕਿੰਟ ਰੁਕੋ। ਤੁਹਾਡਾ ਫ਼ੋਨ ਅਸਲੀ ਜਗ੍ਹਾ ਤੇ ਵਾਪਸ ਆ ਜਾਵੇਗਾ।

**ਬਾਕੀ ਬਟਨ**
- ਸੱਜੇ ਪਾਸੇ **＋ / −**: ਨਕਸ਼ਾ ਵੱਡਾ ਜਾਂ ਛੋਟਾ ਕਰੋ।
- ਸੱਜੇ ਪਾਸੇ **◎**: ਆਪਣੀ ਅਸਲੀ ਜਗ੍ਹਾ ਤੇ ਵਾਪਸ ਜਾਓ।
- **ਸੰਭਾਲੋ**: ਮੌਜੂਦਾ ਜਗ੍ਹਾ ਨੂੰ ਨਾਂ ਦੇ ਕੇ ਯਾਦ ਰੱਖੋ।
- **ਸੰਭਾਲੀਆਂ**: ਸੰਭਾਲੀਆਂ ਜਗ੍ਹਾਵਾਂ ਖੋਲ੍ਹੋ ਅਤੇ ਕਿਸੇ ਇੱਕ ਤੇ ਛੋਹ ਕੇ ਦੁਬਾਰਾ ਵਰਤੋ।
- ਉੱਪਰ **English / ਪੰਜਾਬੀ** ਬਟਨ: ਕਦੇ ਵੀ ਭਾਸ਼ਾ ਬਦਲੋ।

---

## If something doesn't work / ਜੇ ਕੁਝ ਕੰਮ ਨਾ ਕਰੇ

- **The map is blank** → check internet. The map needs internet to load.
  (ਨਕਸ਼ਾ ਖਾਲੀ ਹੈ → ਇੰਟਰਨੈੱਟ ਚੈੱਕ ਕਰੋ।)
- **"Location is turned off"** → tap the button it shows to open settings and turn
  location on. (ਲੋਕੇਸ਼ਨ ਬੰਦ ਹੈ → ਬਟਨ ਦਬਾ ਕੇ ਲੋਕੇਸ਼ਨ ਚਾਲੂ ਕਰੋ।)
- **A banking app complains** → some apps can detect a changed location. Tap the
  red **Stop** button before using them.
  (ਬੈਂਕ ਐਪ ਸ਼ਿਕਾਇਤ ਕਰੇ → ਲਾਲ **ਬੰਦ** ਬਟਨ ਦਬਾ ਕੇ ਪਹਿਲਾਂ ਅਸਲੀ ਲੋਕੇਸ਼ਨ ਤੇ ਆਓ।)

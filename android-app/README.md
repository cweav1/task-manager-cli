# Bourbon Journal Android app

A Jetpack Compose implementation of the Bourbon Tasting Journal UI optimized for Android and ready to ship to the Google Play Console. Tastings are stored locally with `DataStore` so the experience stays private on-device.

## Features
- Quick-add tasting form with flavor chips and demo fill shortcut.
- On-device persistence using `DataStore`; entries stay private to your phone.
- Sort and filter panel to search by name/distillery, filter by flavor or minimum score, and switch between date, score, name, or price ordering.
- Inline stats for total entries, average score, favorite pour, and most common flavor note.

## Project layout
- `app/src/main/java/com/example/bourbonjournal/MainActivity.kt` – Single-activity Compose UI for creating, browsing, and deleting tastings.
- `app/src/main/res` – Dark theme resources, icon vector, and strings.
- `app/build.gradle.kts` – Android application module using Compose Material 3 and `DataStore`.

## Running the app
1. Open the `android-app` folder in Android Studio (Giraffe or newer).
2. When prompted, let Android Studio download the required SDKs.
3. Use **Build > Build Bundle(s) / APK(s) > Build APK(s)** to produce an installable binary, or **Generate Signed Bundle / APK** to create a release artifact for Play Console.

### Run on the Android Emulator
1. In Android Studio, open **Device Manager** → **Create Virtual Device…** and pick a Pixel profile.
2. Choose a Google Play system image that matches the app’s `targetSdk` (34) and finish creating the AVD.
3. With the AVD selected, click **Run** ▶️ (or `Shift` + `F10`) to deploy `app` to the emulator.
4. You can also run from the terminal: `./gradlew installDebug` followed by `adb shell am start -n com.example.bourbonjournal/.MainActivity` while the emulator is running.

### Command line
If you prefer the CLI, ensure you have JDK 17+ and Gradle available, then from the `android-app` directory run:
```bash
gradle wrapper --gradle-version 8.7   # regenerate wrapper JAR if needed
./gradlew assembleDebug
```

> Note: The Gradle wrapper JAR is not bundled in this repository because of the offline environment used to generate this patch. Running `gradle wrapper` once will download the necessary files and make `./gradlew` fully self-contained.

## Build an installable APK for your own device
The fastest path is to create a signed `release` APK and sideload it.

### 1) Generate a signing key (one-time)
```bash
keytool -genkeypair \
  -alias bourbonKey \
  -keyalg RSA -keysize 2048 -validity 3650 \
  -keystore release-keystore.jks
```

### 2) Create `keystore.properties` next to `settings.gradle.kts`
```
storeFile=release-keystore.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=bourbonKey
keyPassword=YOUR_KEY_PASSWORD
```
`app/build.gradle.kts` will pick this up automatically; without it, the release build will fall back to the debug signing config.

### 3) Build a release APK
```bash
./gradlew assembleRelease
# APK output: app/build/outputs/apk/release/app-release.apk
```

### 4) Install on a real device (USB or Wi‑Fi ADB)
- Enable **Developer options** and **USB debugging** on the device.
- Connect the device and verify with `adb devices`.
- Install the APK: `adb install -r app/build/outputs/apk/release/app-release.apk`.

You can use the debug build instead by replacing `assembleRelease` with `assembleDebug` and installing `app/build/outputs/apk/debug/app-debug.apk`.

## Download the project as a single ZIP
If you just need the Android project files (without the Git history), create a zip from the repo root:

```bash
# From /workspace/task-manager-cli
zip -r bourbon-journal-android.zip android-app \
  -x "*/build/*" "*/.gradle/*" "*/.idea/*"
```

This produces `bourbon-journal-android.zip` containing the entire `android-app` folder while skipping Gradle build outputs, caches, and IDE metadata. Transfer the ZIP to your device or share it as needed.

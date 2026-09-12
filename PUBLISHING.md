# NanoAlarm — publishing smoke test notes

This app exists to smoke-test the path from a built Android app to a live
Google Play Console production release. It intentionally does nothing.

- Package: `org.righteffort.nanoalarm`
- compileSdk / targetSdk / minSdk: 37 (Android 17), matching a Pixel 7a on
  current OS. This is set high on purpose per your instruction; it means
  the app will refuse to install on anything older than Android 17.
- UI: Kotlin + Jetpack Compose, single Activity, one screen with banner text.

Everything below that requires your Google identity, a long-lived signing
key, or Play Console interaction is called out as **YOU DO THIS**.

## 1. Generate the production (upload) keystore — YOU DO THIS

I did not generate this. Run it yourself so only you ever hold the
passwords:

```
mkdir -p ~/keys
chmod 700 keys
keytool -genkeypair -v \
  -keystore ~/keys/nanoalarm-release.p12 \
  -alias nanoalarm \
  -keyalg RSA -keysize 2048 -validity 10000
chmod 600 ~/keys/nanoalarm-release.p12
```

- Put the `.p12` file **outside** the git repo (e.g. `~/keys/`), never inside
  `nanoalarm/`. `.gitignore` already blocks `*.p12`/`*.keystore` as a
  backstop, but don't rely on that.
- Only one password is prompted for (PKCS12 format, keytool's default).
  Use it for both `storePassword` and `keyPassword` in
  `keystore.properties` below. Store it somewhere durable (password
  manager) — if you lose this key
  later you cannot update the app under the same listing without going
  through Play's key-loss recovery process.
- 10000 days validity (~27 years) is the standard recommendation so it
  outlives the app.

Then copy `keystore.properties.example` to `keystore.properties` (at the
repo root; it's gitignored) and fill in the real path and passwords:

```
cp keystore.properties.example keystore.properties
chmod 600 keystore.properties
$EDITOR keystore.properties
```

Once that file exists, `./gradlew assembleRelease` / `bundleRelease` will
automatically sign with it. Without it, release builds still build but are
unsigned (fine for local testing of the build config, not for Play upload
of a "signed" APK — though note Play accepts an upload-key-signed AAB and
handles final signing itself if you enable Play App Signing, which is the
default and recommended for new apps).

## 2. Build the artifacts

From the repo root:

```
./gradlew assembleDebug && ./gradlew assembleRelease && ./gradlew bundleRelease
```

## 3. Install a build on your phone to test — YOU DO THIS (adb)

1. On your Pixel: Settings → About phone → tap "Build number" 7 times to
   enable Developer options, then Settings → System → Developer options →
   enable "USB debugging".
2. Connect the phone via USB and accept the "Allow USB debugging?" prompt
   on the phone.
3. Confirm the device is visible:
   ```
   adb devices
   ```
4. Install a debug build directly:
   ```
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
5. To test the **signed release APK** instead (recommended before
   uploading to Play, to confirm the release/minified build config also
   installs and runs, not just debug):
   ```
   ./gradlew assembleRelease   # after keystore.properties is set up
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```
   Note: you can't have both debug and release variants installed under
   the same applicationId at once with different signatures — if install
   fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, run
   `adb uninstall org.righteffort.nanoalarm` first.
6. The AAB itself (`app-release.aab`) is not directly installable via adb —
   it's a publishing format Google Play splits into device-specific APKs.
   Testing the signed release APK in step 5 already exercises the same
   signing config and release build type, so it's a reasonable stand-in.
   If you want to test the *exact* AAB output locally before uploading
   (optional, extra confidence, not required to publish), use Google's
   `bundletool`. It ships as a plain jar, not a shell command:
   ```
   curl -sL -o bundletool.jar https://github.com/google/bundletool/releases/download/1.18.3/bundletool-all-1.18.3.jar
   java -jar bundletool.jar build-apks \
     --bundle=app/build/outputs/bundle/release/app-release.aab \
     --output=/tmp/nanoalarm.apks --ks=<your.p12> --ks-key-alias=nanoalarm \
     --local-testing
   java -jar bundletool.jar install-apks --apks=/tmp/nanoalarm.apks
   ```
   (I can download the jar and script this for you if you want it — just
   ask. It's optional; testing the release APK per step 5 is usually
   enough confidence before a Play upload.)

## Notes on why targetSdk/minSdk are 37

Google Play requires new app submissions to target API 36 (Android 16) or
higher as of August 31, 2026. We're one better at 37 (Android 17) since
that's what your Pixel is actually running and you said "Android 17+,
set high unless it would block publishing" — 37 does not block publishing
(it's above the floor). If you ever want to support older devices, lower
`minSdk` in `app/build.gradle.kts`; `targetSdk`/`compileSdk` should stay at
or above whatever Play's current floor is.

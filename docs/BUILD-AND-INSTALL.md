# Muse Android — Build & Install

**Honest preface:** the machine this code was written on has **no Android toolchain** (no JDK, Gradle, or SDK) and **no connected phone**, so the APK has **not** been built or installed from here. Use one of the two free paths below. The first needs nothing installed locally.

---

## Path A (recommended, zero local setup): build in GitHub Actions, download the APK

1. Push this branch to GitHub (the workflow is at `.github/workflows/android.yml`):
   ```bash
   git push -u origin claude/demon-muse-android-app-290695
   ```
2. On GitHub, open the **Actions** tab → the **Android CI** run for your branch.
3. When it finishes green, open the run and download the **`muse-debug-apk`** artifact (a zip containing `app-debug.apk`).
4. Copy the APK to your phone and tap it to install. You'll need to allow "install unknown apps" for your file manager/browser once (Android Settings → Apps → Special access → Install unknown apps).

The same run also produces a **`unit-test-report`** artifact (the security/logic test results) whether or not the build succeeds.

> The APK is a **debug** build: debuggable, `applicationId` `com.muse.companion.debug`, and unsigned for release. That's expected — it installs fine by sideloading.

---

## Path B (if you want an emulator or on-device debugging): Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (free).
2. `File ▸ Open…` and select the **`android/`** folder (not the repo root).
3. Let Gradle sync (this also generates the Gradle wrapper jar, which is not committed).
4. `Build ▸ Build App Bundle(s) / APK(s) ▸ Build APK(s)`, or press Run with a device/emulator selected.

---

## Path C (command line, if you already have the SDK)

Requires JDK 17+, the Android SDK, and Gradle 8.9 (or let the wrapper fetch it).

```bash
cd android
gradle wrapper --gradle-version 8.9   # one-time: creates ./gradlew + wrapper jar
./gradlew testDebugUnitTest            # run the unit tests
./gradlew assembleDebug                # produces app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## What "works" after install (and what doesn't yet)

See `docs/09-feature-status.md` for the exact, honest status of every feature. In short, Milestone 1 gives you a running app with the dark UI, three-layer memory, settings/personality, the security screen (audit log + kill switch), and an **offline fallback** engine — **no real language model yet** (that's Milestone 2). Nothing claims to work that hasn't been built.

---

## Reproducibility

The build is pinned: AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21, and a Gradle **version catalog** (`android/gradle/libs.versions.toml`) that fixes every dependency version. The CI workflow is the canonical, reproducible recipe.

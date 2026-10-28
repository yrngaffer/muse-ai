# Muse — Hardware & Environment Compatibility Report

**Date:** 2026-10-28
**Author:** Senior Android/AI/security engineering pass (automated)
**Scope:** What can and cannot be built for the Muse Android app in the *current development environment*, with zero budget.

---

## 1. Development machine (where the code is written)

Inspected directly via the shell. Facts, not assumptions:

| Tool | Result | Implication |
|------|--------|-------------|
| OS | Windows 11 Pro (build 26300), MSYS2/MinGW shell | Dev host only — not the target |
| Node.js | v24.16.0 ✅ | Existing web Muse builds/runs here |
| npm | 11.13.0 ✅ | — |
| Python | 3.14.5 ✅ | Available for tooling/scripts |
| Git | 2.54.0 ✅ | Version control OK |
| **Java / JDK** | ❌ **not installed** (`java`, `javac` not found) | **Cannot compile Android/Kotlin locally** |
| **Gradle** | ❌ **not installed** | **Cannot run Android builds locally** |
| **Android SDK** | ❌ **not installed** (`adb`, `sdkmanager` absent; `ANDROID_HOME`/`ANDROID_SDK_ROOT` empty) | **Cannot build an APK locally** |
| Physical Android device | ❌ not attached to this host | **Cannot install or run on-device here** |

### Hard conclusion (stated honestly)

> **This machine cannot build, sign, install, or run an Android APK, and cannot run on-device features (LLM inference, microphone, TTS, accessibility).** No claim in this project says otherwise. Any "tested" status in the feature matrix refers only to logic that runs on a plain JVM (unit tests), not to an APK on a phone.

Installing a full Android toolchain locally is possible but costs multi-GB downloads and a lot of setup — and still can't test on-device behavior without your phone. So we take a better zero-cost route.

---

## 2. The zero-cost build path that actually works

The repository already has a GitHub remote (`github.com/lilgafferz-ai/muse-ai`). **GitHub Actions builds Android APKs for free** (2,000 free minutes/month on private repos, unlimited on public) on Ubuntu runners that ship with JDK + Android SDK.

So the pipeline is:

```
You push code  ──▶  GitHub Actions (free Linux runner)
                      ├─ installs JDK 17 + Android SDK
                      ├─ runs unit tests  (security/logic — real pass/fail)
                      ├─ assembleDebug    (produces muse-debug.apk)
                      └─ uploads the APK as a downloadable artifact
                           │
You download the APK  ◀──┘   →  sideload onto your phone
```

This satisfies every constraint in the master prompt:
- **Zero cost** — no paid CI, no servers, no new hardware.
- **Reproducible** — the exact build steps live in `.github/workflows/android.yml`.
- **Honest** — the APK is built by a real toolchain and either succeeds (artifact appears) or fails (red run). Nothing is faked.

**Alternative (also free):** install [Android Studio](https://developer.android.com/studio) on your own machine, `File ▸ Open` the `android/` folder, let it sync, then `Build ▸ Build APK(s)`. Use this if you want to run on an emulator or debug on a connected phone. See `docs/BUILD-AND-INSTALL.md`.

---

## 3. Target device (your phone) — detected at runtime, not here

We **cannot** detect your phone's RAM / CPU / storage from this dev box. That is not a cop-out — it is correct: the app itself inspects the device at runtime and recommends a model. The relevant signals and where the app reads them:

| Signal | Android API |
|--------|-------------|
| Total RAM | `ActivityManager.getMemoryInfo().totalMem` |
| Low-RAM device flag | `ActivityManager.isLowRamDevice()` |
| CPU ABIs | `Build.SUPPORTED_ABIS` (expect `arm64-v8a` on any modern phone) |
| Free storage | `StatFs` on the app's files dir |
| Android version | `Build.VERSION.SDK_INT` |

The app ships a **Model Manager** screen that reads these and recommends/validates a model before download (see §4 and `docs/02-architecture-and-model-selection.md`).

**Minimum supported Android:** API 26 (Android 8.0). Chosen because it covers ~95%+ of active devices while giving modern `BiometricPrompt`, `JobScheduler`, foreground-service, and Keystore APIs. Local on-device LLM realistically wants a 64-bit (`arm64-v8a`) device with **≥4 GB RAM**; below that, Muse runs in offline rule-based mode or via an opt-in remote model.

---

## 4. Local-inference feasibility (summary — full detail in doc 02)

| Option | Form | On-device? | Cost | Verdict for Muse |
|--------|------|-----------|------|------------------|
| **MediaPipe LLM Inference** (`tasks-genai`) | Maven dep, runs Gemma/Phi on-device | ✅ | Free | **Primary on-device engine** (M2) — maintained by Google, no native build |
| **llama.cpp (JNI/NDK)** | GGUF models, compiled `.so` | ✅ | Free | Alternative engine behind same interface; heavier to integrate |
| **Remote (OpenRouter / your own Ollama on LAN)** | HTTP | ❌ (leaves phone) | Free tier / your server | **Opt-in, disabled by default**; reuses existing web-Muse concept |
| ~~Ollama native on Android~~ | — | — | — | Not a native Android runtime; excluded |

Muse uses a **pluggable `InferenceEngine` interface** so the engine can be swapped without touching the rest of the app. Milestone 1 ships the interface + an offline deterministic engine so the app runs with *no model at all*; the real on-device LLM engine lands in Milestone 2 with CI verification.

---

## 5. Feature feasibility in THIS environment

"Buildable" = compiles in CI. "Device-testable here" = can be verified without your phone.

| Capability | Buildable (CI) | Verifiable here | Notes |
|-----------|:---:|:---:|-------|
| Compose UI, navigation, theming | ✅ | partial (Robolectric) | — |
| Room memory + CRUD + delete | ✅ | ✅ (Robolectric, in-memory DB) | 3-layer memory logic unit-tested |
| Keystore-backed encryption | ✅ | ⚠️ instrumented only | Keystore needs a device/emulator |
| Permission broker + tool schemas | ✅ | ✅ (pure JVM) | Core security — fully unit-tested |
| SSRF / URL allow-list guard | ✅ | ✅ (pure JVM) | Fully unit-tested |
| Path-traversal guard | ✅ | ✅ (pure JVM) | Fully unit-tested |
| Prompt-injection boundary | ✅ | ✅ (pure JVM) | Fully unit-tested |
| On-device LLM inference | ✅ (M2) | ❌ | Needs device + model file |
| Voice (STT/TTS) | ✅ | ❌ | Android framework APIs; needs device |
| App launch / open URL / share intents | ✅ | ❌ | Needs device to see effect |
| Accessibility automation | ✅ | ❌ | Needs device + user to enable service |
| Web search / fetch (online tools) | ✅ | ⚠️ guard-only | Network call needs connectivity; guard is unit-tested |

---

## 6. What this means for the roadmap

- **Milestone 1 (this pass):** full Gradle project + Compose prototype + 3-layer memory + the complete security core (broker, guards, audit log, injection boundary) **with real unit tests**, plus free CI that builds the APK. This is the prompt's "first working milestone" and Phases 1–3.
- **Milestone 2:** on-device LLM engine (MediaPipe), voice, model manager download/verify — each verified against CI before being called done.
- **Milestone 3:** device automation (accessibility), online tools hardening, SAF file ops, export/import.

Nothing is marked "done" until CI (and, for device features, you on a real phone) confirms it. Status for every feature is tracked in `docs/09-feature-status.md`.

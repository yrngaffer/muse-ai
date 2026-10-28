# Muse for Android (native)

A private, local-first AI companion for Android — Kotlin + Jetpack Compose, built around a
zero-trust security core. This is the native port of the web "Muse AI Companion" in the repo
root, rebuilt with a permission broker, encrypted memory, and an auditable tool system.

> **Status:** Milestone 1 (prototype + security core). The APK is built for free by GitHub
> Actions — this project was authored on a machine with no Android toolchain. See
> [`../docs/BUILD-AND-INSTALL.md`](../docs/BUILD-AND-INSTALL.md) and
> [`../docs/09-feature-status.md`](../docs/09-feature-status.md) for the honest state of things.

## Build

Nothing to install locally — push and let CI build it, or open `android/` in Android Studio.
Full instructions: [`../docs/BUILD-AND-INSTALL.md`](../docs/BUILD-AND-INSTALL.md).

## Module layout

```
android/
├─ app/
│  ├─ build.gradle.kts
│  └─ src/
│     ├─ main/java/com/muse/companion/
│     │  ├─ MuseApplication.kt, MainActivity.kt
│     │  ├─ di/            # hand-wired AppContainer (no Hilt in M1)
│     │  ├─ core/          # Clock, Outcome, redacting logger
│     │  ├─ security/      # PermissionBroker, ToolSpec, guards (SSRF/path/injection), crypto, audit
│     │  ├─ data/          # Room memory (3 layers) + audit store + DataStore settings
│     │  ├─ ai/            # InferenceEngine (+ offline engine), device caps, model catalog, context builder
│     │  ├─ tools/         # Tool interface, parser, registry, intent tools, web fetch
│     │  ├─ agent/         # Orchestrator (bounded agent loop)
│     │  ├─ voice/         # STT/TTS controller
│     │  └─ ui/            # Compose screens: chat, memory, settings, security
│     └─ test/java/…       # JVM unit tests (security + logic) — run in CI
├─ gradle/libs.versions.toml   # pinned dependency versions
└─ settings.gradle.kts
```

## The one security rule to remember

The AI only ever produces **text**. Every proposed tool call is independently validated by the
`PermissionBroker` against a code-defined `ToolSpec` (schema, scope, permission tier, guards) and
the user's settings. The model cannot grant itself access, and no web page / file / tool output can
either. This is enforced in code and in tests (`OrchestratorTest`, `PermissionBrokerTest`), not by prompting.

## Docs

All deliverable docs live in [`../docs/`](../docs/): compatibility report, architecture & model
selection, security architecture, threat model, permissions guide, feature status, memory
backup/deletion, disabling automation, and the Windows roadmap.

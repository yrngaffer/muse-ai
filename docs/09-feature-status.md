# Muse — Feature Status

**As of this commit, the first CI run has NOT completed.** So: the APK build and the unit-test
pass/fail are **UNVERIFIED** until the GitHub Actions run is green. Below, **"unit-tested"** means a
JVM test exists that CI will execute — not that it has already passed here. No on-device behavior
has been verified, because there is no phone attached to the build machine.

Status legend (from the master prompt):
- ✅ **Implemented & tested** — code + a test that CI runs (pass confirmed once CI is green)
- 🟦 **Implemented, not yet tested** — code exists; needs a device or CI to verify
- 🟨 **Partially implemented** — interface/skeleton present; full behavior is a later milestone
- 🧱 **Blocked by OS restriction** — Android policy limits this; documented alternative provided
- ⬜ **Not implemented** — planned, not started

| # | Capability | Status | Notes |
|---|-----------|:---:|-------|
| 1 | Native Android UI (Compose, dark theme, nav) | 🟦 | Compiles in CI; visual/behavior needs a device |
| 2 | Chat screen with streaming responses | 🟦 | Streaming path exercised by the offline engine |
| 3 | Offline fallback engine (no model) | ✅ | `OfflineRuleEngine`; deterministic |
| 4 | **On-device LLM inference** | 🟨 | `InferenceEngine` interface complete; concrete MediaPipe/llama.cpp engine = **M2** |
| 5 | Remote model engine (opt-in) | ⬜ | Interface ready; OpenRouter/LAN-Ollama adapter = M2, disabled by default |
| 6 | 3-layer memory (permanent/episodic/adaptive) | ✅ | Room schema + repository; `MemoryRepositoryTest` (Robolectric) |
| 7 | Memory encryption at rest (sensitive) | 🟦 | `AesGcmSecretBox` crypto **unit-tested**; Keystore key provisioning needs a device |
| 8 | Memory UI (inspect/add/search/delete/clear) | 🟦 | Screen implemented; needs device to interact |
| 9 | Memory export/import (encrypted) | ⬜ | Format specified (docs/07); code = M2 |
| 10 | Persistent settings + personality | 🟦 | DataStore; defaults carry the Muse persona |
| 11 | **Permission broker (zero-trust gate)** | ✅ | `PermissionBroker`; `PermissionBrokerTest` incl. "reject even if AI requests it" |
| 12 | Tool schema validation (strict args) | ✅ | Covered by `PermissionBrokerTest` |
| 13 | SSRF / URL allow-list guard | ✅ | `UrlGuard`; `UrlGuardTest` (incl. DNS-rebinding) |
| 14 | Path-traversal guard | ✅ | `PathGuard`; `PathGuardTest` |
| 15 | Prompt-injection boundary (data fencing) | ✅ | `PromptInjectionSanitizer`; `PromptInjectionTest` |
| 16 | Append-only audit log | 🟦 | Room-backed; insert/read only; shown on Security screen |
| 17 | Kill switch (disable all automation) | 🟦 | Policy + broker enforcement; test covers broker side |
| 18 | Emergency stop (cancel generation/queue) | 🟦 | Cancels the coroutine job; UI wired |
| 19 | Secret-redacting logger | ✅ | `MuseLog.scrub`; `MuseLogTest` |
| 20 | Tool: open URL (intent) | 🟦 | Code done; needs device to see effect |
| 21 | Tool: share text (intent) | 🟦 | Code done; needs device |
| 22 | Tool: launch app | 🧱 | Limited by Android 11+ package visibility; fails cleanly on hidden apps |
| 23 | Tool: web fetch (online) | 🟦 | Guarded by `UrlGuard`; off by default; network path needs connectivity |
| 24 | Bounded agent loop (one tool round) | ✅ | `Orchestrator`; `OrchestratorTest` proves deny + non-execution |
| 25 | Device capability detection + model recommendation | ✅ | `DeviceCapabilities` + `ModelCatalog`; `ModelCatalogTest` |
| 26 | Voice input (push-to-talk, STT) | 🟦 | `VoiceController` + recognizer intent; needs device |
| 27 | Spoken responses (TTS) | 🟦 | `VoiceController.speak`; needs device |
| 28 | Wake phrase / power-button launch | ⬜ | Investigated (docs/05, docs/11); not implemented — device/OEM dependent |
| 29 | Accessibility-based automation | ⬜ | Design only; M3, explicit opt-in required |
| 30 | SAF file browse/read/write | ⬜ | `PathGuard` ready; SAF integration = M3 |
| 31 | Sandboxed code execution | ⬜ | **Intentionally disabled** — no genuine on-device isolation yet (see docs/03, docs/08) |
| 32 | Free CI build of APK | 🟦 | Workflow written; **pending first run** |
| 33 | Instrumented (on-device) tests | ⬜ | M2, needs an emulator/device in CI |

## Honest summary

- **Solid and tested now (logic):** the security core — broker, guards, injection boundary, crypto
  transform, audit, orchestrator deny-path, model recommendation, parser, redaction. These run on a
  plain JVM in CI.
- **Built but needs a device/CI to verify:** the whole UI, memory UI, voice, intent tools, Keystore
  key provisioning, and the APK build itself.
- **Deliberately not done yet (and not faked):** real on-device LLM, remote engine, export/import,
  accessibility automation, SAF, and sandboxed code execution. Sandboxed execution stays **off** until
  there is real isolation — per the master prompt, we disable it rather than pretend a normal process
  is a sandbox.

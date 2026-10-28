# Muse — Architecture & Model Selection

**Date:** 2026-10-28

This document records the design decisions, why they were made, and the concrete model recommendation logic. It is the companion to the compatibility report (`docs/01`).

---

## 1. Design goals (from the master prompt, condensed)

1. **Local-first & private** — runs offline; nothing leaves the phone unless you explicitly say so.
2. **Zero-trust** — the AI model never gets raw filesystem/shell/network/accessibility power. Every action goes through a permission broker.
3. **Modular** — model, memory, UI, tools, and security are separate, swappable components.
4. **Honest** — no faked capabilities, no silent remote substitution.
5. **Portable memory** — a future Windows client can reuse the same memory format.

---

## 2. Component map

```
┌──────────────────────────────────────────────────────────────────┐
│                        UI  (Jetpack Compose)                        │
│   Chat · Memory · Settings · Security/Audit · Model Manager         │
└───────────────┬────────────────────────────────────────────────────┘
                │ ViewModels (state only — no privileged calls)
┌───────────────▼────────────────────────────────────────────────────┐
│                        Orchestrator  (Agent loop)                    │
│   builds context → calls engine → parses tool requests               │
└───────┬───────────────────────┬───────────────────────┬────────────┘
        │                       │                        │
┌───────▼────────┐   ┌──────────▼─────────┐   ┌──────────▼───────────┐
│ InferenceEngine │   │   PermissionBroker  │   │     MemoryStore       │
│  (pluggable)    │   │  (zero-trust gate)  │   │  Room + Keystore AEAD │
│ Echo│MediaPipe│ │   │  ┌───────────────┐  │   │  permanent/episodic/  │
│ llama.cpp│Remote│   │  │ guards:        │  │   │  adaptive layers      │
└────────────────┘   │  │  UrlGuard(SSRF)│  │   └───────────────────────┘
                     │  │  PathGuard     │  │
   AI output is      │  │  Schema check  │  │   ┌───────────────────────┐
   DATA, never a     │  │  Confirm gate  │  │   │     AuditLog (append)  │
   privileged call ──┼─▶│  Audit hook    │──┼──▶│  every tool call +     │
                     │  └───────────────┘  │   │  allow/deny + outcome  │
                     └──────────┬──────────┘   └───────────────────────┘
                                │ only allowed, scoped, validated calls
                     ┌──────────▼──────────┐
                     │        Tools         │
                     │ launchApp · openUrl  │
                     │ share · webFetch*    │
                     │ (*online, opt-in)    │
                     └─────────────────────┘
```

**The critical security invariant:** the model produces *text*. The orchestrator extracts *proposed* tool calls from that text and hands each one to the **PermissionBroker**, which independently validates the tool name, argument schema, resource scope, and permission level — and asks you to confirm anything sensitive. The model's "opinion" that a call is fine carries no weight. This is enforced in code and in tests, not by prompting.

---

## 3. Why these technologies

| Concern | Choice | Why |
|--------|--------|-----|
| UI | Kotlin + Jetpack Compose + Material 3 | Prompt requirement; modern, less boilerplate |
| DI | Manual `AppContainer` (M1) | De-risks the first CI build (no Dagger/Hilt codegen); Hilt is an easy later swap |
| Structured data | Room (SQLite) | Prompt requirement; typed DAOs, migration support, testable with Robolectric |
| Encryption keys | Android Keystore directly (`KeyGenParameterSpec`, AES-256-GCM) | Keys never leave hardware-backed Keystore; authenticated encryption; no extra dependency |
| Settings | DataStore (Preferences) | Modern replacement for SharedPreferences; coroutine-friendly |
| Tool arguments | `kotlinx.serialization` + explicit schemas | Strict, structured args — a core injection defense |
| Network | OkHttp, every request via `UrlGuard` | Mature; SSRF guard is centralized |
| On-device LLM | MediaPipe `tasks-genai` (M2) behind `InferenceEngine` | Maintained by Google, no native build step, runs Gemma/Phi |
| Tests | JUnit4 + Robolectric + MockK + Truth | Security logic runs on plain JVM in CI |

### Why manual DI instead of Hilt (for now)
With no local compiler, the first build's correctness can only be verified in CI. Dagger/Hilt add an annotation-processing surface (KSP/kapt + version matching) that is a common first-build failure point. A hand-written `AppContainer` is ~40 lines, trivially correct, and keeps components just as swappable. Hilt can be introduced later as a pure refactor once the build is green. Room's KSP is kept because codegen there is essential and well understood.

### Why field-level encryption (M1) before full SQLCipher DB encryption
The Android Keystore gives a hardware-backed AES-256-GCM key with no extra dependency (`AesGcmSecretBox` + `KeystoreKeyProvider`). We encrypt *sensitive memory content* with it immediately (the crypto transform is unit-tested on the JVM with an in-memory key; key provisioning is device-bound). Full database-at-rest encryption (SQLCipher) adds a native `.so` dependency and build risk; it is scheduled for a later verified milestone. Both are "modern authenticated encryption"; this sequencing ships real protection now without jeopardizing the first build.

---

## 4. Three-layer memory (data model)

One Room table `memories` with a `layer` discriminator keeps the schema simple and portable (good for the future Windows client). Layers:

- **PERMANENT** — explicitly approved, stable facts (name, preferences, goals). High retention, requires confirmation to store.
- **EPISODIC** — conversation summaries / events, with timestamp, topic, source, confidence, relevance. Subject to retention limits.
- **ADAPTIVE** — learned corrections & preferences (preferred formats, disliked behaviors), with a reinforcement counter.

Each row carries: `id`, `layer`, `content` (encrypted when sensitive), `topic`, `source`, `confidence`, `verified` (user-confirmed vs. assumption vs. model-suggested), `sensitivity`, `createdAt`, `updatedAt`, `lastAccessedAt`, `reinforcement`. Deleting a memory removes it from the DB; because we do not maintain a separate vector index in M1, "remove from search index" is inherently satisfied (search is a DB query). The schema is **versioned** (`schemaVersion`) and the export format carries it for migration.

---

## 5. Model selection logic (what the app recommends on *your* phone)

The `ModelCatalog` + `DeviceCapabilities` compute a recommendation at runtime. The table below is the policy the code implements:

| Detected total RAM | Recommended default | Context | Notes |
|--------------------|---------------------|---------|-------|
| < 3 GB or `isLowRamDevice` | **No on-device model** → offline rule-based or opt-in remote | — | On-device LLM not advisable |
| 3–4 GB | **Gemma-3 1B (int4)** ~0.9 GB | 2k | Smallest viable chat model |
| 5–6 GB | **Llama-3.2 1B (int4)** or **Gemma-3 1B** | 4k | Comfortable |
| 7–8 GB | **Gemma-2 2B (int4)** / **Qwen2.5 1.5B** | 4k | Good quality/speed balance |
| > 8 GB | **Phi-3.5-mini (int4)** / **Qwen2.5 3B (int4)** | 4k–8k | Best quality that stays local |

Guards the code enforces before any download:
- ABI must include `arm64-v8a`.
- Free storage must exceed model size × 1.5 (download + unpack headroom).
- A model whose estimated runtime RAM exceeds ~55% of total RAM is blocked with an explanation.
- Every catalog entry carries **license** + **provenance (source URL + SHA-256)**; downloads are integrity-checked before load, and a model that fails verification is deleted, never loaded.

All model licenses used will be open-weight and redistributable (Gemma terms, Apache-2.0, MIT, Llama community license as applicable) — recorded per entry in the catalog.

---

## 6. Online / remote policy (privacy)

- Online tools and the remote engine are **disabled by default**.
- Enabling the remote engine shows exactly what will leave the phone and to which host.
- Arbitrary URL fetches are blocked from reaching `localhost`, RFC-1918 private ranges, link-local, and cloud metadata IPs (`169.254.169.254` etc.) — enforced in `UrlGuard` **after DNS resolution** to defeat DNS-rebinding/SSRF.
- Private memory, local files, and credentials are never attached to outbound requests unless you explicitly authorize that specific action.

---

## 7. Portability to a future Windows client

- Memory is a plain, versioned schema (no Android-only types in the row model).
- Export format: a single JSON document `{ schemaVersion, exportedAt, memories[], personality }`, written through Keystore-backed encryption (`.muse` file). The same JSON can be read by a Windows client.
- Sync is **not** implemented and remains disabled until a future authenticated, device-paired channel is built. See `docs/10-windows-roadmap.md`.

# Android Permissions Guide

> Milestone 1 (M1) · last reviewed 2026-10-28. This guide describes **only what the code in `android/` actually does today**. Anything not yet built is labelled by milestone (M2/M3). The app was authored on a machine with **no Android toolchain and no phone**, so no on-device permission prompt has been observed here — the prompts described below are what the code is wired to trigger once the CI-built APK runs on a device (see [docs/BUILD-AND-INSTALL.md](BUILD-AND-INSTALL.md)).

Muse declares exactly **four** Android permissions in [`android/app/src/main/AndroidManifest.xml`](../android/app/src/main/AndroidManifest.xml). That list is deliberately minimal — fewer permissions means a smaller attack surface and less for the zero-trust [`PermissionBroker`](../android/app/src/main/java/com/muse/companion/security/PermissionBroker.kt) to have to defend. Nothing here is requested "just in case."

## At a glance

| Permission | Protection level | Prompted? | Needed for | If denied / absent |
|---|---|---|---|---|
| `INTERNET` | normal (install-time) | No prompt — auto-granted | `web_fetch` tool, remote engine (M2) | Online features stay off; offline chat is unaffected |
| `ACCESS_NETWORK_STATE` | normal (install-time) | No prompt — auto-granted | Detecting whether you are online | App assumes offline |
| `RECORD_AUDIO` | dangerous (runtime) | Yes, when you tap the mic | Push-to-talk voice input | Voice input unavailable; typing still works |
| `POST_NOTIFICATIONS` | dangerous (runtime, API 33+) | **Not yet** (declared, unused in M1) | Planned background/status notifications (M2) | Nothing today; notifications would simply not show |

"Install-time" permissions (Android's *normal* protection level) are granted automatically when the APK is installed and have no user-facing prompt; the user cannot individually revoke them from the standard per-permission screen. "Runtime" permissions (*dangerous* level) trigger a system dialog the first time the app needs them and can be revoked any time in **Settings ▸ Apps ▸ Muse ▸ Permissions**. See [docs/01-compatibility-report.md](01-compatibility-report.md) for how these behave across the supported Android versions.

### Which M1 tools touch which permission

The four tools registered in M1 lean on surprisingly little. Only `web_fetch` performs its own network I/O; the other three delegate to another app via an Android `Intent`, so they do not consume Muse's `INTERNET` permission at all.

| Tool | Class | Uses `INTERNET`? | Notes |
|---|---|:---:|---|
| `open_url` | `OpenUrlTool.kt` | No | Hands an `https` URL to the browser via `ACTION_VIEW`; URL still screened by `UrlGuard` first |
| `share_text` | `ShareTextTool.kt` | No | Opens the system share sheet; you pick the target app |
| `launch_app` | `LaunchAppTool.kt` | No | Resolves a launcher intent; limited by package visibility (see below) |
| `web_fetch` | `WebFetchTool.kt` | **Yes** | Gated by `onlineToolsEnabled` **and** `UrlGuard`; off by default |

---

## 1. `INTERNET`

**What it is for.** Lets Muse open outbound network connections. In M1 the only code path that uses it is the `web_fetch` tool ([`WebFetchTool.kt`](../android/app/src/main/java/com/muse/companion/tools/impl/WebFetchTool.kt)), which fetches the readable text of a public `https` page. In M2 the opt-in remote inference engine will use it too (see [docs/02-architecture-and-model-selection.md](02-architecture-and-model-selection.md)).

**When it is requested.** Install-time. There is no runtime dialog — Android grants `INTERNET` silently at install. **This does not mean Muse talks to the network by default.** Two independent gates stand in front of every network call:

- The user's security policy. `SecurityPolicy.onlineToolsEnabled` defaults to **`false`** ([`SecurityPolicy.kt`](../android/app/src/main/java/com/muse/companion/security/SecurityPolicy.kt)). The broker rejects any tool whose spec sets `requiresInternet = true` while online tools are off, returning *"online tools are disabled."* So `web_fetch` is inert until you explicitly enable it in Settings.
- `UrlGuard`. Even when online tools are on, every URL is screened by [`UrlGuard.kt`](../android/app/src/main/java/com/muse/companion/security/guards/UrlGuard.kt) for SSRF / DNS-rebinding before the request is allowed (`https` only, no private/loopback/link-local/cloud-metadata targets, redirects not followed).

**If denied / absent.** `INTERNET` cannot be denied at runtime (it is install-time). The practical "off switch" is the `onlineToolsEnabled` toggle: with it off — the default — the offline engine and all local features work exactly the same, and no bytes leave the device. Pure offline mode needs neither network permission at runtime.

## 2. `ACCESS_NETWORK_STATE`

**What it is for.** Lets Muse *ask the OS* whether there is a validated internet connection, without opening a socket. Used by [`ConnectivityMonitor.kt`](../android/app/src/main/java/com/muse/companion/ai/ConnectivityMonitor.kt), which checks `NET_CAPABILITY_INTERNET` **and** `NET_CAPABILITY_VALIDATED` on the active network. The chat top bar shows this as the "Online · *engine*" / "Offline · *engine*" status line, and it is used to gate online UI.

**When it is requested.** Install-time, no prompt.

**If denied / absent.** Not user-deniable at runtime. If it were unavailable, `ConnectivityMonitor.isOnline()` returns `false` and Muse simply behaves as if offline — a safe default. It never drives a network call on its own; it only informs the UI and gating.

## 3. `RECORD_AUDIO`

**What it is for.** Microphone access for **push-to-talk voice input** (speech-to-text). Muse does not do always-on listening (see §6). Spoken *output* (text-to-speech) in [`VoiceController.kt`](../android/app/src/main/java/com/muse/companion/voice/VoiceController.kt) needs no permission; only capturing your voice does.

**When it is requested.** At **runtime**, and only when you tap the microphone button in the chat input bar. The flow lives in [`ChatScreen.kt`](../android/app/src/main/java/com/muse/companion/ui/chat/ChatScreen.kt):

1. The mic button calls `micPerm.launch(Manifest.permission.RECORD_AUDIO)` — a standard `ActivityResultContracts.RequestPermission()` dialog.
2. On grant, Muse launches the **system** speech-recognizer UI via `VoiceController.recognitionIntent()`, which sets `EXTRA_PREFER_OFFLINE = true` so on-device recognition is used when the device supports it.
3. The recognized text is handed back to the chat view model.

So the prompt appears lazily, tied to an explicit user gesture — never at launch.

**If denied.** The permission callback receives `granted = false` and simply does **not** launch the recognizer; nothing crashes and no partial capture happens. You can still type normally, and you can grant it later from the mic button or Settings. Voice input/output are implemented but **not yet verified on a physical device** — status 🟦 in [docs/09-feature-status.md](09-feature-status.md) (#26, #27).

## 4. `POST_NOTIFICATIONS`

**Honest status: declared, not yet used.** The permission is listed in the manifest and the manifest comment says it is "requested at runtime (API 33+)," but **as of M1 no code requests it and Muse posts no notifications.** There is no `NotificationManager` use, no notification channel, and no foreground service anywhere in `android/`. It is reserved for planned status/background notifications (for long-running tool or inference work) and will be wired up in M2.

**What it will be for.** On Android 13 (API 33) and newer, posting any notification requires this runtime permission. When notifications are added, Muse will request it at runtime the first time it wants to show one — consistent with how `RECORD_AUDIO` is handled.

**When it is requested.** Today: never (no call site exists). Planned (M2): at runtime, the first time a notification would be shown. On Android 12L and below the permission has no effect — notifications post without it.

**If denied.** Today nothing changes, because nothing uses it. Once notifications exist, denial means they are suppressed by the OS; all other functionality continues normally. Muse must never depend on a notification being delivered.

---

## What Muse deliberately does **not** request

### `QUERY_ALL_PACKAGES`

Muse intentionally does **not** declare `QUERY_ALL_PACKAGES`. It is Play-Store-restricted (apps must justify it) and privacy-hostile, because it reveals the full list of everything you have installed. The manifest calls this omission out explicitly.

**The resulting limitation — `launch_app`.** On Android 11+ (API 30), *package visibility* hides other apps from yours by default. Because Muse does not ask to see all packages, the `launch_app` tool ([`LaunchAppTool.kt`](../android/app/src/main/java/com/muse/companion/tools/impl/LaunchAppTool.kt)) can only start apps whose launcher intent the OS lets it resolve via `packageManager.getLaunchIntentForPackage(pkg)`. For a package that is hidden or not installed, that call returns `null` and the tool **fails cleanly** with:

> App '*pkg*' not found or not visible (Android package visibility).

No crash, no silent enumeration of your apps. This is a conscious trade-off: narrower launch capability in exchange for not demanding a sensitive, trust-eroding permission. If a specific set of target apps is ever needed, the correct fix is a scoped `<queries>` element naming just those packages — **not** `QUERY_ALL_PACKAGES`. Status 🧱 ("blocked by OS restriction, documented alternative") in [docs/09-feature-status.md](09-feature-status.md) #22. The threat-model rationale for keeping the permission set minimal is in [docs/03-threat-model.md](03-threat-model.md).

---

## Planned: Accessibility Service (M3) — not implemented

An Android **Accessibility Service** is the usual mechanism for cross-app automation (reading on-screen content, tapping, scrolling in *other* apps). Muse does **not** ship one. There is no `AccessibilityService` subclass and no accessibility declaration anywhere in the code — it is design-only, slated for **M3** (status ⬜ in [docs/09-feature-status.md](09-feature-status.md) #29).

**Honest scope, if/when it lands.** An accessibility service is one of the most powerful things an Android app can hold: it can observe the content of windows across apps and perform gestures on the user's behalf. Because of that power:

- **It requires explicit user activation in system Settings** (Settings ▸ Accessibility ▸ Muse ▸ On), behind an OS warning screen. An app **cannot** turn it on for itself — the user must flip a system toggle. There is no programmatic self-grant.
- Even with the service enabled at the OS level, it would **not** bypass Muse's own gate: every automated action would still be a tool call that the [`PermissionBroker`](../android/app/src/main/java/com/muse/companion/security/PermissionBroker.kt) validates against a code-defined [`ToolSpec`](../android/app/src/main/java/com/muse/companion/security/ToolSpec.kt), subject to the kill switch and per-tool enablement.
- It would be opt-in, disclosed, and revocable — never a default, and always covered by the global "disable all automation" control described in [docs/05-disable-all-automation.md](05-disable-all-automation.md).

Until M3, treat any claim that Muse can "control other apps" as false.

---

## Activation: power button, wake phrase, default assistant

The master prompt asks whether Muse can be summoned by the power button, a wake phrase ("Hey Muse"), or by being the device's default assistant. **Honest answer: none of this is implemented, and it is fundamentally device/OEM dependent.**

Why it is not built:

- **Power-button / gesture launch** is controlled by the OEM and the system, not by ordinary apps. A normal app cannot reliably bind itself to the power button; the behavior differs by manufacturer and Android version, and much of it is simply not exposed to third-party apps.
- **Default digital-assistant role** (`ROLE_ASSISTANT`) can route some hardware gestures to an app, but only after the **user** sets Muse as the default assist app in system Settings, and even then the long-press/gesture mapping is inconsistent across devices. It is not something the app can assume or self-assign.
- **Always-on wake phrase** needs continuous hotword listening, which on most devices means system/OEM hotword integration (plus a real battery and privacy cost). Muse does no always-on listening by design.

**Realistic alternatives (future options, not yet built).** These are the launch paths a normal app *can* own without special OEM cooperation:

- **Quick Settings tile** — a `TileService` giving a one-tap entry from the notification shade. (future)
- **Home-screen shortcut / launcher icon** — already present; Muse is a launchable activity (`MainActivity`). (available today)
- **Notification action** — a persistent "Talk to Muse" action, which would depend on `POST_NOTIFICATIONS` (§4) and a service. (future, M2+)
- **Push-to-talk mic button** — the in-app microphone described in §3. (available today, pending device verification)

Status for wake-phrase/power-button launch is ⬜ "investigated, not implemented — device/OEM dependent" in [docs/09-feature-status.md](09-feature-status.md) #28. Roadmap context is in [docs/10-windows-roadmap.md](10-windows-roadmap.md).

---

## What a normal Android app **cannot** do

Muse is an ordinary sandboxed Android app, and it is worth being blunt about the hard limits the OS enforces no matter what Muse's own code wants:

- **It cannot silently read other apps' private data.** Each app's internal storage is private to that app; scoped storage and package visibility (see above) wall off other apps' files and even the list of what is installed. Muse has no code — and no permission — to reach into another app's data.
- **It cannot bypass the lock screen.** A normal app cannot unlock the device, read secure lock-screen content it is not shown, or defeat the keyguard. Nothing in Muse attempts this.
- **It cannot grant itself permissions.** Dangerous permissions require a runtime prompt the **user** answers; special access like an Accessibility Service requires the **user** to enable it on a system Settings screen behind a warning. An app (and certainly the language model inside it) can request, but never self-approve.

That last point is the crux of Muse's security posture, and it holds at *two* layers. At the OS layer, Android decides what Muse may touch. At the app layer, the [`PermissionBroker`](../android/app/src/main/java/com/muse/companion/security/PermissionBroker.kt) independently validates **every** proposed tool call against code-defined specs and the user's policy — it trusts the tool table and your settings, never the model's claim that a call is safe. External/tool content cannot register a tool, widen a scope, or grant a permission. **The model cannot grant itself access to anything.** Full detail is in [docs/08-security-architecture.md](08-security-architecture.md).

---

## Reviewing or revoking a permission on your device

Because Muse holds so few permissions, the system permission screen is short and easy to audit:

- **See what is granted:** Settings ▸ Apps ▸ Muse ▸ Permissions. In M1 only **Microphone** appears as a toggleable (dangerous) permission; Notifications shows once notification code lands (M2). `INTERNET` and `ACCESS_NETWORK_STATE` are install-time and do not appear as per-permission toggles.
- **Revoke the microphone:** turn **Microphone** off there, or deny the prompt. Voice input then stops working and typing continues; Muse re-prompts next time you tap the mic.
- **Turn off network use without touching the OS:** disable online tools in Muse's own Settings (`onlineToolsEnabled`), which the broker enforces regardless of the OS-level `INTERNET` grant. The global kill switch in [docs/05-disable-all-automation.md](05-disable-all-automation.md) stops all tool execution at once.

None of these actions require reinstalling, and none can be undone silently by the app — re-granting always goes back through you.

---

## See also

- [docs/01-compatibility-report.md](01-compatibility-report.md) — supported Android versions and API-level behavior (e.g. `POST_NOTIFICATIONS` from API 33).
- [docs/02-architecture-and-model-selection.md](02-architecture-and-model-selection.md) — offline vs. on-device vs. opt-in remote engines.
- [docs/03-threat-model.md](03-threat-model.md) — why the permission set is kept minimal.
- [docs/05-disable-all-automation.md](05-disable-all-automation.md) — kill switch and turning everything off.
- [docs/08-security-architecture.md](08-security-architecture.md) — the permission broker, guards, and risk tiers.
- [docs/09-feature-status.md](09-feature-status.md) — implemented / planned status of every capability named here.
- [docs/10-windows-roadmap.md](10-windows-roadmap.md) — milestone roadmap (M2/M3).
- [docs/BUILD-AND-INSTALL.md](BUILD-AND-INSTALL.md) — building the APK in CI and sideloading it (where the runtime prompts first appear).

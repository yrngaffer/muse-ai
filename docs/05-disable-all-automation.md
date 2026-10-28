# Disabling All Automation

_Operator guide · as of 2026-10-28 · Milestone 1 (M1)_

This is the "how do I make Muse stop doing things" page. It lists every off switch Muse ships,
from the lightest (stop this one turn) to the heaviest (remove the app), and for each one states
**exactly** what it stops and — just as important — what it does **not** stop.

Everything below describes code that exists and runs today. Where a control touches a feature that
is not built yet, that is called out as **planned (M2)** or **planned (M3)** rather than implied to
work.

## The guarantee these switches rest on

Before the switches, the thing that makes them trustworthy:

- **Muse never initiates actions on its own.** A turn only begins when you send a message
  (`ui/chat/ChatViewModel.kt` → `send()`), or tap Allow on a confirmation you were shown. There is
  no background loop, no scheduler, no "it decided to do something while you were away."
- **The agent loop is deliberately bounded.** `agent/Orchestrator.kt` runs exactly one generation
  pass, then checks any tool calls the model proposed, then at most one follow-up pass that sees the
  tool output as fenced **data** (`security/guards/PromptInjectionSanitizer.kt`). It does not loop
  autonomously — the class comment calls this out, and `OrchestratorTest` pins it.
- **The model cannot grant itself access.** Every proposed tool call is independently validated by
  `security/PermissionBroker.kt` against a code-defined tool table (`security/ToolSpec.kt`) and your
  settings (`security/SecurityPolicy.kt`). The broker trusts the spec table and your policy — never
  the model's claim that a call is "safe." External or tool-returned content cannot register a tool,
  widen a scope, or flip a switch.

So the controls below are enforced **in code (and unit tests), not by prompting.** Turning a switch
off is a policy change the broker reads on the next turn; it is not a request the model can ignore.

## Quick reference

| # | Control | Where | What it stops | Persists? |
|---|---------|-------|---------------|:--------:|
| 1 | Emergency stop | Chat screen (■ button) | The current turn + its not-yet-run tool calls | No — this turn only |
| 2 | Per-tool disable | Security screen | One named tool, on every future turn | Yes |
| 3 | Kill switch | Security screen | **All** tool calls via the broker | Yes |
| 4 | Online tools + remote engine off | Settings (default) | Muse's own network use | Yes (default state) |
| 5 | Full offline operation | #4 + airplane mode / disable intent tools | Any data leaving the phone | While offline |
| 6 | Uninstall | Android system | Everything, permanently | Permanent |

---

## 1. Emergency stop — cancel the turn in flight

**Control:** the ■ (Stop) button in the chat top bar. It appears only while a turn is running or a
confirmation is waiting (`ui/chat/ChatScreen.kt`, gated on `isGenerating || pendingConfirmation`),
and calls `ChatViewModel.emergencyStop()`.

**What it does:** cancels the coroutine `job` that is driving the current turn, sets
`isGenerating = false`, clears any pending confirmation dialog, and appends
`"🛑 Stopped. Queued actions cancelled."`

**What it stops:**
- The in-flight generation (token streaming stops immediately — the engine's `generate` flow is
  cancellable, see `ai/InferenceEngine.kt`).
- Any tool calls from this turn that **had not run yet.** Tool calls execute sequentially inside the
  same cancelled `job` in `Orchestrator.respond`, so cancelling the job stops the remaining ones.
- A confirmation prompt that was waiting for your Allow/Cancel — it is dismissed (same as Cancel).

**What it does NOT stop:**
- A tool that **already executed** this turn. If Muse already opened a URL or opened the share
  sheet, those side effects have happened — stop cannot undo them.
- Anything in future turns. It is a per-turn cancel, not a setting. Your **next** message runs
  normally with all tools still enabled.
- Chat itself, memory, or voice.

Use this when a single response is going somewhere you do not want. For a durable "off," use #2 or #3.

---

## 2. Per-tool disable switches — turn off one capability

**Control:** Security screen → **Tool permissions**. Each registered tool has a toggle
(`ui/security/SecurityScreen.kt`). Turning one off calls
`SettingsRepository.setToolDisabled(id, disabled = true)`, which adds the tool's id to the
`disabledToolIds` set persisted in DataStore (`data/settings/SettingsRepository.kt`).

The tools listed are the four registered in `di/AppContainer.kt`:

| Tool id | What it does | Reaches network? |
|---------|--------------|:----------------:|
| `open_url` | Opens an https URL in your browser (intent) | No (hands off to the browser) |
| `share_text` | Opens the Android share sheet | No (hands off to another app) |
| `launch_app` | Launches an installed app by package id | No |
| `web_fetch` | Fetches a public https page's text | **Yes** (gated by #4) |

**What it stops:** the broker denies that specific tool on every future turn. The check is
`SecurityPolicy.isToolEnabled(id)` → `PermissionBroker.validate` returns
`Deny("tool '<id>' is disabled in settings")`, surfaced in chat as `⛔ <id> denied`. The setting
persists across app restarts.

**What it does NOT stop:**
- The other tools, chat/generation, memory, or voice.
- The current in-flight turn — a disable takes effect on the **next** turn, because the broker reads
  a fresh policy snapshot per message (`ChatViewModel.send()` reads `securityPolicy.first()`). To
  stop the current turn too, also hit emergency stop (#1).

Use this to permanently remove one capability (e.g. switch off `web_fetch` and `launch_app` but
keep `open_url`).

---

## 3. Kill switch — deny every tool call

**Control:** Security screen → the banner at the top, **"Kill switch — disable all automation"**
(`ui/security/SecurityScreen.kt`). Its own subtitle states the behavior: *"When on, the broker
denies every tool call. Chat still works."* The toggle calls `SettingsRepository.setKillSwitch(true)`,
setting `SecurityPolicy.killSwitchEngaged = true`.

**What it does:** `killSwitchEngaged` is the **first** check in `PermissionBroker.validate` — before
the tool is even looked up:

```
if (policy.killSwitchEngaged) return deny(toolId, "kill switch engaged — all automation is stopped")
```

So every proposed tool call is denied, regardless of which tool, its arguments, or your per-tool
switches. `SecurityPolicy.isToolEnabled` also returns `false` for everything while it is engaged.

**What it stops:**
- **All** tool execution — `open_url`, `share_text`, `launch_app`, `web_fetch`, and any tool added
  later. Each denial is recorded in the append-only audit log and shown in chat as `⛔ ... denied`.

**What it does NOT stop:**
- **Chat and text generation still work.** The model can still reply, and it can still *propose*
  tool calls — they are simply all denied at the broker. This is intentional: the kill switch stops
  *actions*, not conversation.
- Memory reads/writes and voice.
- The current in-flight turn (takes effect next turn — pair with emergency stop for "now").

This is the single toggle to flip when you want Muse to be able to talk but never touch anything on
the device. It persists across restarts until you turn it off.

---

## 4. Keep online tools + remote engine OFF (the default)

Both of these ship **off by default** (`SettingsRepository` defaults and the `DEFAULT_SETTINGS` in
the view models both set them `false`). Settings screen → **Network (off by default)**.

### Online tools (`onlineToolsEnabled`, default off)

**What it stops:** any tool declared `requiresInternet = true` is denied by the broker
(`PermissionBroker.validate` → `"online tools are disabled"`). Today that is only `web_fetch`
(`tools/impl/WebFetchTool.kt`), so with online tools off, **Muse makes no network request of its
own.**

**What it does NOT stop — read this carefully:**
- `open_url`, `share_text`, and `launch_app` are declared `requiresInternet = false`, so they still
  run while online tools are off. They do not use Muse's network, but they **hand off to another
  app** — `open_url` gives a URL to your browser (which then goes online), `share_text` opens the
  share sheet. If your goal is "nothing leaves the phone at all," also disable those three tools
  (#2), engage the kill switch (#3), or go fully offline (#5).

### Remote model engine (`remoteEngineEnabled`, default off)

The toggle exists and persists (`SettingsRepository.setRemoteEngine`), with an explicit warning in
the UI: turning it on would send prompts to a remote provider, so conversation text would leave the
phone.

**Honest status:** the remote engine adapter is **planned (M2)** and **not implemented**. In M1 the
Orchestrator is wired to the on-device `OfflineRuleEngine` unconditionally
(`di/AppContainer.kt` → `orchestrator(...)` constructs `engine = OfflineRuleEngine()`), and nothing
reads `remoteEngineEnabled` to swap it in. So in M1 your prompts **never** leave the device during
generation, regardless of this toggle. Leaving it off is the correct setting for when the remote
engine does ship in M2 — it will keep prompts on-device then too.

See `docs/02-architecture-and-model-selection.md` for the engine abstraction and
`docs/09-feature-status.md` for the exact feature status.

---

## 5. Full offline operation

The strongest "nothing leaves the phone" posture, built from the controls above plus the OS:

1. Leave **online tools** and **remote engine** off (#4 — the defaults).
2. Disable the intent tools that hand off to networked apps (`open_url`, `share_text`) via #2, or
   engage the kill switch (#3).
3. Put the device in **airplane mode** if you want a hard guarantee at the OS level.

**What this gives you:** Muse runs entirely on-device. The only engine in M1, `OfflineRuleEngine`
(`ai/OfflineRuleEngine.kt`), declares `requiresNetwork = false`, is deterministic, and never touches
the network. Memory lives in a local Room database with sensitive content encrypted at rest via
Android Keystore AES-256-GCM (`security/crypto/AesGcmSecretBox.kt`,
`security/crypto/KeystoreKeyProvider.kt`). Settings are local DataStore. Nothing is uploaded.

**What it does NOT give you:** real language-model answers. The offline engine does not pretend to
be an LLM — it returns short, persona-flavored acknowledgements and points you at a model. Real
on-device inference is **planned (M2)** (`ai/InferenceEngine.kt` is the interface; the MediaPipe /
llama.cpp engine is not built). So "full offline" today means a functional, private app shell, not a
full conversational model.

---

## 6. Uninstall — the permanent off switch

Standard Android uninstall (long-press the icon → Uninstall, or Settings → Apps → Muse → Uninstall).

**What it stops:** everything. It removes the app and all of its local data: the Room database
(`muse.db`, created in `di/AppContainer.kt`) including all memory and the audit log, the DataStore
settings, and the app-scoped key material.

**What it does NOT leave behind / cannot undo:**
- The Keystore-held AES key is destroyed with the app, so any encrypted memory becomes
  **unrecoverable** — there is no cloud copy (M1 has no sync, and export/import is **planned (M2)**).
  Back up anything you want to keep first, once export exists.
- It does not touch side effects that already happened in other apps (a page you opened, text you
  shared) — those live in those apps.

---

## What none of these switches change

A few protections are always on and are not something you need to (or can) toggle off — they are the
floor, not a feature:

- **The broker validates every call, always.** There is no "trusted mode" that bypasses
  `PermissionBroker`. Even `autoAllowIrreversible` (Settings → Automation) only skips the
  *confirmation prompt* for reversible-vs-not; it never skips validation, and **sensitive
  (Level-3) actions are always confirmed** regardless (`security/PermissionLevel.kt`,
  `PermissionBroker.validate`).
- **Tool output is treated as data, not instructions** (`PromptInjectionSanitizer.fence`), so a web
  page or tool result cannot talk Muse into re-enabling a switch.
- **Not implemented, so nothing to disable:** accessibility-based automation, SAF file read/write,
  and export/import are **planned (M3 / M2)** and absent in M1. Sandboxed code execution is
  **deliberately disabled** — it is not shipped because there is no genuine on-device isolation yet,
  rather than pretending a normal process is a sandbox. See `docs/03-threat-model.md` and
  `docs/08-security-architecture.md`.

## Where this is enforced (file map)

| Control | Primary code |
|---------|--------------|
| Emergency stop | `ui/chat/ChatViewModel.kt` (`emergencyStop`), `ui/chat/ChatScreen.kt` |
| Per-tool disable | `ui/security/SecurityScreen.kt`, `data/settings/SettingsRepository.kt` (`disabledToolIds`) |
| Kill switch | `security/SecurityPolicy.kt` (`killSwitchEngaged`), `security/PermissionBroker.kt` |
| Online / remote off | `data/settings/SettingsRepository.kt`, `ui/settings/SettingsScreen.kt`, `di/AppContainer.kt` |
| Bounded loop + data fencing | `agent/Orchestrator.kt`, `security/guards/PromptInjectionSanitizer.kt` |

## Related docs

- `docs/01-compatibility-report.md` — device/OS support.
- `docs/02-architecture-and-model-selection.md` — engines and the on-device-vs-remote choice.
- `docs/03-threat-model.md` — what each control defends against.
- `docs/08-security-architecture.md` — the broker, guards, and crypto in depth.
- `docs/09-feature-status.md` — the honest, per-feature implemented/planned status.
- `docs/10-windows-roadmap.md` — what is deferred to later milestones.
- `docs/11-permissions-guide.md` — Android permissions and package-visibility limits.
- `docs/BUILD-AND-INSTALL.md` — how to build the APK (CI) and install it.

> Note: the APK is built for free by GitHub Actions (`.github/workflows/android.yml`); it has not
> been built, installed, or run on a device from the authoring machine, and the first CI run has not
> necessarily completed. See `docs/BUILD-AND-INSTALL.md`.

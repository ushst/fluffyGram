# fluffyGram — Local Agent Instructions

This file is the primary instruction source for local AI coding agents (Claude Code, Cursor, Windsurf, Copilot, etc.) working on the **fluffyGram** project — a custom Telegram Android fork.

---

## Project Overview

| Property | Value |
|---|---|
| Base app | Telegram for Android (upstream) |
| App package | `org.telegram.messenger` |
| Custom code package | `org.ushastoe.fluffy` |
| Version name | see `gradle.properties` → `APP_VERSION_NAME` |
| Fluffy patch version | see `gradle.properties` → `FLUFFY_PATCH_VERSION` |
| Main delivery branch | `main` |

---

## Repository Layout

```
TMessagesProj/                          # Telegram core (upstream)
  src/main/java/org/telegram/...        # Original Telegram code — keep diffs minimal
  src/main/java/org/ushastoe/fluffy/    # All Fluffy custom code lives here
    patches/    ← business logic, one class per feature
    hooks/      ← thin bridge methods called from Telegram core
    utils/      ← shared helpers
    sync/       ← settings sync machinery
    ui/         ← Fluffy-owned screens / activities
TMessagesProj_App/                      # App-level entry point & ApplicationLoader
fluffyGram_dev/                         # Private submodule: signing keys, dev config
fluffy_config_registry.json             # Machine-readable registry of all Fluffy config keys
build_and_deploy_debug.ps1              # Local debug build + ADB deploy (Windows)
build_and_deploy_debug.sh               # Local debug build + ADB deploy (Linux/macOS)
build_and_install_debug_remote.ps1      # Staged remote ADB installer
.github/workflows/build-release.yml    # CI release pipeline
AGENTS.md                               # Full workflow rules (source of truth for agents)
```

---

## Core Rule: Patch Architecture

**Never place business logic directly in Telegram core classes.**

### Pattern

1. Add a minimal hook call inside the Telegram class:

```java
// org.telegram.ui.SomeActivity.java
private void applyFluffyFeature() {
    SomeFeatureHook.apply(this);
}
```

2. The hook delegates to the patch class:

```java
// org.ushastoe.fluffy.hooks.SomeFeatureHook.java
public static void apply(SomeActivity activity) {
    SomeFeaturePatch.apply(activity);
}
```

3. All logic lives in the patch:

```java
// org.ushastoe.fluffy.patches.SomeFeaturePatch.java
public final class SomeFeaturePatch {
    private SomeFeaturePatch() {}
    public static void apply(SomeActivity activity) {
        // full custom logic here
    }
}
```

### Naming Convention

| Layer | Class name | Package |
|---|---|---|
| Telegram hook point | `SomeFeatureHook` | `org.ushastoe.fluffy.hooks` |
| Patch / business logic | `SomeFeaturePatch` | `org.ushastoe.fluffy.patches` |
| Shared utility | `SomeUtil` | `org.ushastoe.fluffy.utils` |

---

## Config Key Registry

Every new `SharedPreferences` key added to Fluffy-owned storage **must** be registered in **both**:

1. `fluffy_config_registry.json` (machine-readable, backend-facing)
2. `fluffyGram_dev/fluffy_config_registry.md` (human-readable)

**Required fields for each JSON entry:**

```json
{
  "key": "my_setting",
  "storage": "fluffy_appearance_settings",
  "type": "boolean",
  "default": false,
  "sync": "sync",
  "scope": "global"
}
```

`sync` values: `"sync"` | `"local-only"` | `"sync-via-bundle"`  
`scope` values: `"global"` | `"user-scoped"` | `"account-scoped"` | `"dialog-scoped"`

Do **not** introduce a new Fluffy config key in a PR without updating both registry files in the same commit.

---

## Message Actions Standard

Any feature added to the message context menu must:

1. Have a toggle in Fluffy Settings (default: enabled or disabled as appropriate).
2. Use `MessageActionsHook` → `MessageActionsPatch` for storage.
3. Name the SharedPreferences key `<feature_name>_enabled`.

See `.github/docs/MESSAGE_ACTIONS_STANDARD.md` for the full pattern with code examples.

---

## UI Standards

- Use Telegram-native cells: `HeaderCell`, `TextSettingsCell`, `TextCheckCell`, `TextInfoPrivacyCell`, `ShadowSectionCell`.
- Do **not** add gray info rows unless a description is truly necessary.
- Group related settings under a single header in small sections.
- Fluffy screen `ActionBar` titles: `Fluffy Settings` / `Appearance` / `Debug`.
- Never hardcode the string `"Telegram"` in Fluffy UI — use `R.string.AppName` / `R.string.AppNameBeta`.
- App name resource: `fluffyGram`. Beta name: `fluffyGram Beta`.

---

## ActionBar / Title Alignment

Title alignment is driven globally by the Fluffy Appearance setting. Do **not** hardcode per-screen offsets. Reuse:

- `AppearanceSettingsPatch`
- `DialogsCenteredTitlePatch`
- Hooks in `org.ushastoe.fluffy.hooks`

---

## Build & Deploy

### Debug (local device)

**Windows:**
```powershell
.\build_and_deploy_debug.ps1
```

**Linux / macOS:**
```bash
./build_and_deploy_debug.sh
```

### Debug (remote ADB / VPN)

```powershell
.\build_and_install_debug_remote.ps1 -Serial <host:port>
```

The remote script keeps `assemble` and `adb push / install-commit` separate so failures are clearly visible.

### After code changes

Always run the debug build/deploy script unless:
- The change is docs-only, **or**
- The user explicitly says not to build.

---

## Release Workflow

- Releases are published via `.github/workflows/build-release.yml`.
- Each release must include both `fluffyGram-<version>.apk` and `update.json`.
- Versioning: keep the upstream `APP_VERSION_NAME` as-is; bump `FLUFFY_PATCH_VERSION` for Fluffy-only changes.
- Tags follow the pattern `fluffy-v12.5.1` or `fluffy-v12.5.1.1`.
- Before re-triggering a release for an existing tag, move the tag to the correct commit first.

In-app updater entry points:
- `TMessagesProj_App/src/main/java/org/ushastoe/fluffy/updates/FluffyCustomUpdateManager.java`
- `TMessagesProj_App/src/main/java/org/telegram/messenger/ApplicationLoaderImpl.java`

---

## Commit Message Format

```
<type>(<scope>): <short summary>

Why:
- <reason>

What:
- <exact changes>

Patch points:
- org.telegram.<class>

Patch files:
- TMessagesProj/src/main/java/org/ushastoe/fluffy/...

Risk:
- <side effects / compat notes>

Tests:
- not run

Upstream:
- <hash or n/a>
```

**Types:** `feat` `fix` `refactor` `perf` `chore` `build` `docs` `revert` `sync`  
**Scopes:** `fluffy-patch` `hook` `telegram-core` `build` `deps` `ui` `assets`

### Commit Hygiene

- Split unrelated changes into separate commits.
- Do not mix UI, branding, and build-system edits unless tightly coupled.
- Stage only the files relevant to the current commit.

---

## Merge / Upstream Safety

- Keep Telegram-side diffs minimal to reduce upstream merge conflicts.
- If a conflict occurs in a Telegram core file, preserve the hook call and resolve logic inside `org.ushastoe.fluffy`.
- Prefer patch-file updates over editing Telegram core files.

---

## Key Reference Points

| Feature | Location |
|---|---|
| Still your number? suggestion | `SettingsActivity.java` → `SuggestionCell.Factory.of(...)` → `VALIDATE_PHONE_NUMBER` |
| Custom updater | `FluffyCustomUpdateManager.java` |
| Premium access | `PremiumAccessPatch.java` / `PremiumAccessHook.java` |
| Settings deep link | `FluffySettingsDeepLinkPatch.java` |
| Appearance settings | `AppearanceSettingsPatch.java` |
| Main tabs | `MainTabsConfigPatch.java` / `MainTabsUiPatch.java` |
| Sync | `FluffySettingsSyncPatch.java` |

---

## Do Not

- Put business logic in `org.telegram.*` classes directly.
- Hardcode `"Telegram"` brand strings in Fluffy UI.
- Add a new Fluffy config key without updating both registry files.
- Remove or weaken unrelated tests.
- Push directly to `main` — use a branch + PR.

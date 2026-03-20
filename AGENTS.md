# Fluffy Patch Workflow

## Goal
All custom changes in Telegram core code must be implemented as patches.

## Rule
1. In original Telegram files (`org.telegram...`), add only minimal hook calls (bridge methods).
2. Write patch files in Java by default.
3. Keep full custom logic in separate files under:
`TMessagesProj/src/main/java/org/ushastoe/fluffy/...`
4. Do not place business logic directly into Telegram core classes.
5. Each patch must have a clear name tied to a feature or bugfix.

## Package Structure
Use this base package for all custom code:
`org.ushastoe.fluffy`

Recommended subpackages:
- `org.ushastoe.fluffy.patches` for patch logic
- `org.ushastoe.fluffy.hooks` for bridge/helper methods
- `org.ushastoe.fluffy.utils` for shared utilities

## Config Registry
1. Every new key added to any Fluffy config or Fluffy-owned `SharedPreferences` must also be recorded in a dedicated registry file kept in the repo.
2. The registry entry must describe at least:
- key name
- storage file / `SharedPreferences` name
- value type
- default value
- whether the key is local-only or included in sync
3. The primary registry file for this repo is:
`fluffyGram_dev/fluffy_config_registry.md`
4. Do not introduce a new Fluffy config key without updating that registry file in the same change.
5. If a key is account-scoped or user-scoped, record the storage pattern as well, for example `prefix_<userId>`.
6. In addition to the human-readable markdown registry, keep a machine-readable registry file for automation and backend sync:
`fluffyGram_dev/fluffy_config_registry.json`
7. Every new key added to the markdown registry must also be added to the JSON registry in the same change.
8. The JSON registry should be treated as the backend-facing source for scripts that sync config metadata or update remote constants/strings.
9. Each JSON entry should include at least:
- `key`
- `storage`
- `type`
- `default`
- `sync`
- `scope`
10. Prefer keeping markdown and JSON entries semantically aligned so humans and scripts read the same config metadata.

## Implementation Pattern
1. Add a small hook in Telegram class.
2. Delegate to patch class in `org.ushastoe.fluffy...`.
3. Keep patch code isolated and reusable.

Example pattern:

```java
// in org.telegram... class
private void applyFluffyPatch() {
    FluffyFeaturePatch.apply(this);
}
```

```java
// in org.ushastoe.fluffy.patches
public final class FluffyFeaturePatch {
    private FluffyFeaturePatch() {}

    public static void apply(Object target) {
        // custom logic
    }
}
```

## Merge/Update Safety
1. Prefer patch-file updates over heavy edits in Telegram core files.
2. Keep Telegram-side diffs small to reduce conflicts on upstream updates.
3. If conflict appears, preserve hook call and resolve patch logic in `org.ushastoe.fluffy`.

## Build/Deploy Standard
1. For Android verification, prefer:
`.\build_and_deploy_debug.ps1`
2. After code changes, run the debug build/deploy script unless the user explicitly says not to build.
3. Prefer the project script over ad-hoc Gradle install commands so device selection and Java setup stay consistent.
4. If a change is docs-only or the user forbids builds, state that the build was not run.

## Release / Update Workflow
1. The main delivery branch is `main`. Keep local and remote work aligned to `origin/main`.
2. Release builds are published through `.github/workflows/build-release.yml`. This workflow is the single source of truth for GitHub APK releases and app update metadata.
3. Release assets are expected from the private `fluffyGram_dev` submodule or CI secrets fallback. Do not hardcode local secret file paths into the workflow.
4. App updates are delivered from GitHub Releases through:
`FLUFFY_UPDATE_MANIFEST_URL=https://github.com/ushst/fluffyGram/releases/latest/download/update.json`
and
`FLUFFY_UPDATE_PAGE_URL=https://github.com/ushst/fluffyGram/releases/latest`
5. The client-side custom updater is implemented in:
- `TMessagesProj_App/src/main/java/org/telegram/messenger/ApplicationLoaderImpl.java`
- `TMessagesProj_App/src/main/java/org/ushastoe/fluffy/updates/FluffyCustomUpdateManager.java`
6. The release workflow must publish both:
- `fluffyGram-<version>.apk`
- `update.json`
to the same GitHub Release so the in-app updater can resolve the APK URL and checksum.
7. Versioning rule:
- keep Telegram upstream base version as-is
- increment `FLUFFY_PATCH_VERSION` in `gradle.properties` for Fluffy-only patch releases
- use tags that match the public version, for example `fluffy-v12.5.1` or `fluffy-v12.5.1.1`
8. Before retriggering a release for an existing tag, move the tag to the intended commit first. A stale Git tag will cause CI to rebuild old code even if `main` is already fixed.

## UI Standards
1. In Fluffy screens, prefer Telegram native cells/layouts (`HeaderCell`, `TextSettingsCell`, `TextCheckCell`, `TextInfoPrivacyCell`, `ShadowSectionCell`) over custom layouts unless custom UI is clearly required.
2. Do not add gray info rows by default. Use them only when the extra description is necessary for comprehension.
3. Group related settings under a single header when the section is small. Avoid splitting one small section into multiple visual blocks without a clear reason.
4. Fluffy settings screen titles must use section names in the `ActionBar`:
- `Fluffy Settings`
- `Appearance`
- `Debug`
5. In Fluffy-owned UI, use app branding from resources (`R.string.AppName` / `R.string.AppNameBeta`) and avoid hardcoded `Telegram` labels.

## Still Your Number Reference
1. The `Still your number?` / `Is %1$s still your number?` UI is implemented as an inline Telegram suggestion card inside Settings, not as a system notification, bulletin, or dialog.
2. Reference path:
- `TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java`
- `SuggestionCell.Factory.of(...)`
- `pendingSuggestions` entry `VALIDATE_PHONE_NUMBER`
3. Keep this note only as a reference for that specific notification path.

## ActionBar / Title Alignment
1. Title alignment behavior must stay driven by the Fluffy Appearance setting, not by hardcoded per-screen values.
2. Reuse the shared title-alignment path:
- `AppearanceSettingsPatch`
- `DialogsCenteredTitlePatch`
- related hooks in `org.ushastoe.fluffy.hooks`
3. For custom collapsing headers, reuse the shared centered-title calculation instead of introducing new ad-hoc offsets.
4. If a screen creates a custom `ActionBar`, connect it to the shared title-alignment hook path instead of implementing standalone alignment logic.

## Branding Standard
1. The app name resource should be `fluffyGram`.
2. The beta name resource should be `fluffyGram Beta`.
3. When changing branding, update both base and localized `strings.xml` files if those locales override `AppName` or `AppNameBeta`.

## Commit Hygiene
1. Split unrelated changes into separate commits.
2. Do not mix UI, branding, and build-system edits in one commit unless they are tightly coupled.
3. For leftover unrelated working tree changes, stage only the files relevant to the current commit.

## Commit Description Structure
Use this commit message format for all changes.

1. Header format:
`<type>(<scope>): <short summary>`

2. Allowed `type`:
`feat`, `fix`, `refactor`, `perf`, `chore`, `build`, `docs`, `revert`, `sync`

3. Recommended `scope`:
`fluffy-patch`, `hook`, `telegram-core`, `build`, `deps`, `ui`, `assets`

4. Commit body fields (required):
`Why:` short reason for change.
`What:` exact behavior/code changes.
`Patch points:` touched Telegram classes/hooks.
`Patch files:` files under `org.ushastoe.fluffy`.
`Risk:` possible side effects and compatibility notes.

5. Footer fields (optional):
`Tests:` what was run or `not run`.
`Upstream:` related upstream commit/hash if any.
`Breaking:` breaking changes if present.

Template:

```text
<type>(<scope>): <short summary>

Why:
- ...

What:
- ...

Patch points:
- org.telegram...

Patch files:
- TMessagesProj/src/main/java/org/ushastoe/fluffy/...

Risk:
- ...

Tests:
- not run

Upstream:
- <hash or n/a>
```

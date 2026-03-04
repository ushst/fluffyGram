# Fluffy Patch Workflow

## Goal
All custom changes in Telegram core code must be implemented as patches.

## Rule
1. In original Telegram files (`org.telegram...`), add only minimal hook calls (bridge methods).
2. Write patch files in Kotlin by default.
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

```kotlin
// in org.ushastoe.fluffy.patches
object FluffyFeaturePatch {
    fun apply(target: Any?) {
        // custom logic
    }
}
```

## Merge/Update Safety
1. Prefer patch-file updates over heavy edits in Telegram core files.
2. Keep Telegram-side diffs small to reduce conflicts on upstream updates.
3. If conflict appears, preserve hook call and resolve patch logic in `org.ushastoe.fluffy`.

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

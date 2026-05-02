# Project: fluffyGram

`fluffyGram` is a custom Android Telegram client based on the official [Telegram for Android](https://github.com/DrKLO/Telegram) repository.

## 📌 Core Directives

All development must strictly adhere to the guidelines defined in [AGENTS.md](./AGENTS.md). 

### Key Rules:
- **Surgical Patches:** Never implement heavy business logic in `org.telegram` classes. Use minimal hooks and delegate to `org.ushastoe.fluffy.patches`.
- **Config Management:** Any new preference or config key MUST be registered in `fluffyGram_dev/fluffy_config_registry.md` and `fluffyGram_dev/fluffy_config_registry.json`.
- **UI Consistency:** Use Telegram's native components (`TextSettingsCell`, etc.) for custom settings screens.
- **Commit Format:** Follow the mandatory commit message structure defined in `AGENTS.md`.

## 📂 Project Structure

- `TMessagesProj/`: The main Telegram library module (C++ and Java/Kotlin).
  - `src/main/java/org/telegram/`: Original Telegram source code.
  - `src/main/java/org/ushastoe/fluffy/`: Custom Fluffy patches and logic.
- `TMessagesProj_App/`: The application module.
- `fluffyGram_dev/`: Private configuration, keys, and registry files.
- `scripts/`: Utility scripts for building, versioning, and maintenance.

## 🛠 Workflows

### Build & Run
To build and deploy the debug version to a connected device:
```bash
./build_and_deploy_debug.sh
```
(Use `.ps1` variant on Windows).

### Versioning
When releasing Fluffy-specific changes without an upstream update, increment `FLUFFY_PATCH_VERSION` in `gradle.properties`.

### Adding a Feature
1. Identify the hook point in `org.telegram...`.
2. Add a minimal bridge call.
3. Implement the logic in a new or existing class in `org.ushastoe.fluffy.patches`.
4. If the feature has settings, update the registry files in `fluffyGram_dev/`.

## 🔍 Investigation Tips
- Use `grep_search` to find hook points by searching for "Fluffy" or "Patch" in the codebase.
- Look at `TMessagesProj/src/main/java/org/ushastoe/fluffy/hooks/` for existing bridge methods.

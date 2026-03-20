# fluffyGram

<p align="center">
  <img src="TMessagesProj/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="fluffyGram icon" width="128" height="128">
</p>

<p align="center">
  <a href="https://github.com/ushst/fluffyGram/releases/latest">Latest Release</a>
  ·
  <a href="https://github.com/ushst/fluffyGram/actions">GitHub Actions</a>
  ·
  <a href="https://github.com/DrKLO/Telegram">Upstream Telegram</a>
</p>

`fluffyGram` is a Telegram-based Android client with a focus on UI customization, cleaner patch-based changes, and custom release/update tooling.

## What Is Included

- Fluffy-owned settings screens and UI tweaks
- Custom app updater backed by GitHub Releases
- Appearance customization:
  - custom fonts
  - chat list scaling
  - centered titles and chat header options
  - tabs customization
  - formatting toggles
- Debug utilities and Fluffy deeplinks like `fluffy://settings/...`
- Optional `OpenStreetMap` interactive map provider

## Project Structure

This project is based on [Telegram for Android](https://github.com/DrKLO/Telegram), but all custom logic should stay isolated in:

- `TMessagesProj/src/main/java/org/ushastoe/fluffy/patches`
- `TMessagesProj/src/main/java/org/ushastoe/fluffy/hooks`
- `TMessagesProj/src/main/java/org/ushastoe/fluffy/utils`
- `TMessagesProj/src/main/java/org/ushastoe/fluffy/ui`

Telegram core files should contain only minimal hook points.

## Build Setup

Private local assets live in `fluffyGram_dev/` and are not meant to be committed as public app secrets.

Expected local files:

- `fluffyGram_dev/devlocal.properties`
- `fluffyGram_dev/ushastoe-release.keystore`
- `fluffyGram_dev/google-services.json`

Typical `devlocal.properties` keys:

- `storeFile`
- `storePassword`
- `keyAlias`
- `keyPassword`
- `MAPS_V2_API`
- `TELEGRAM_APP_ID`
- `TELEGRAM_APP_HASH`

## Quick Start

Debug build and install:

```powershell
.\build_and_deploy_debug.ps1
```

Release APK/AAB copy:

```powershell
.\build_and_copy.ps1 -Configuration Release -PackageFormat Both
```

All-ABI release APK/AAB copy:

```powershell
.\build_and_copy_all_abis.ps1 -Configuration Release -PackageFormat Both
```

## Releases And Updates

GitHub release publishing is handled by:

- `.github/workflows/build-release.yml`

In-app updates are served from GitHub Releases through:

- `update.json`
- `update-beta.json`

Versioning rule:

- upstream Telegram version stays unchanged
- Fluffy-only releases increment `FLUFFY_PATCH_VERSION` in `gradle.properties`

Example:

- upstream: `12.5.1`
- Fluffy patch releases: `12.5.1.1`, `12.5.1.2`, `12.5.1.3`

## RuStore

This repo includes helper scripts for store packaging and key export:

- `build_and_copy.ps1`
- `build_and_copy_all_abis.ps1`
- `export_rustore_upload_cert.ps1`
- `fluffyGram_dev/export_rustore_upload_cert.ps1`
- `fluffyGram_dev/export_rustore_pepk.ps1`

There is also a dedicated workflow for RuStore publishing:

- `.github/workflows/publish-rustore.yml`

## Upstream

Main upstream source:

- https://github.com/DrKLO/Telegram

API documentation:

- https://core.telegram.org/api
- https://core.telegram.org/mtproto

## Thanks

Thanks to the communities and maintainers of these Telegram forks for ideas, references, and prior work that helped this project:

- [NekoGram](https://github.com/Nekogram/Nekogram)
- [Cherrygram](https://github.com/arsLan4k1390/Cherrygram)
- [NagramX](https://github.com/risin42/NagramX)
- [Forkgram](https://github.com/forkgram/TelegramAndroid)

## License And Branding Notes

If you fork this project further:

- use your own Telegram API credentials
- use your own signing keys
- use your own Firebase project
- do not present the app as the official Telegram client

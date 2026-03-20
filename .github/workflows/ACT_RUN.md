# Local act run guide

This file documents how to run `.github/workflows/build-release.yml` locally with `act`.

## Prerequisites

- Docker Desktop is running
- `act` is installed (`act --version`)
- Secrets file exists: `.github/workflows/.secrets`

Example required secrets in `.github/workflows/.secrets`:

- `GH_PAT=...`
- `TELEGRAM_BOT_TOKEN=...`
- `TELEGRAM_CHANNEL_ID=...`
- plus signing/build secrets used by workflow

`GH_PAT` is recommended for `act` because some runs do not provide `github.token` automatically, and `actions/checkout` can fail with `Input required and not supplied: token`.

## List jobs

```powershell
act -l
```

## Validate only (dry-run)

```powershell
act -n workflow_dispatch -W .github/workflows/build-release.yml --input create_release=false --input post_to_telegram=true --secret-file .github/workflows/.secrets
```

## Real local run (build + telegram post)

```powershell
act workflow_dispatch -W .github/workflows/build-release.yml --input create_release=false --input post_to_telegram=true --secret-file .github/workflows/.secrets --verbose
```

Notes:

- `create_release=false` skips GitHub Release publish locally.
- `post_to_telegram=true` keeps Telegram posting enabled for local test.
- In `act`, `upload-artifact` cannot use `ACTIONS_RUNTIME_TOKEN`, so artifact upload steps are skipped by workflow condition (`env.ACT != 'true'`).

## Check cache behavior

Run the same real command twice.

On second run, inspect logs for cache restore messages in steps:

- `Cache NDK`
- `Set up ccache`
- `Cache CMake intermediates`

## Troubleshooting

- If Telegram step does not run, confirm event is `workflow_dispatch`.
- If Telegram step fails, test token/channel directly:

```powershell
curl.exe -s "https://api.telegram.org/bot<TELEGRAM_BOT_TOKEN>/getMe"
curl.exe -s --get "https://api.telegram.org/bot<TELEGRAM_BOT_TOKEN>/getChat" --data-urlencode "chat_id=<TELEGRAM_CHANNEL_ID>"
```

- If `act` fails on heavy Android build, increase Docker resources (RAM/CPU/disk).

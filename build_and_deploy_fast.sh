#!/usr/bin/env bash

set -euo pipefail

# Fast local release install for day-to-day work.
# Uses signed afatRelease with a single app-level R8 pass
# (library minify is disabled — that alone cuts ~2–3 minutes).
#
# Optional: FLUFFY_FAST_BUILD=true skips R8 entirely, but on this
# codebase full dex is often slower than one R8 pass — not recommended.

CLEAN=0
LAUNCH=1
SERIAL=""
SKIP_R8=0
JAVA_HOME_OVERRIDE=""
ANDROID_SDK_ROOT_OVERRIDE=""
GRADLE_MAX_HEAP="${GRADLE_MAX_HEAP:-8192m}"
GRADLE_MAX_WORKERS="${GRADLE_MAX_WORKERS:-16}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --clean)
      CLEAN=1
      shift
      ;;
    --no-launch)
      LAUNCH=0
      shift
      ;;
    --serial)
      SERIAL="${2:?Missing value for --serial}"
      shift 2
      ;;
    --skip-r8)
      SKIP_R8=1
      shift
      ;;
    --java-home)
      JAVA_HOME_OVERRIDE="${2:?Missing value for --java-home}"
      shift 2
      ;;
    --android-sdk-root)
      ANDROID_SDK_ROOT_OVERRIDE="${2:?Missing value for --android-sdk-root}"
      shift 2
      ;;
    -h|--help)
      cat <<'EOF'
Usage: ./build_and_deploy_fast.sh [options]

Builds signed afatRelease (org.ushastoe.fluffy) and installs it.
Library-module R8 is disabled; app R8 still runs (best speed/size tradeoff).

Options:
  --clean                     Run clean before build/install
  --no-launch                 Skip app launch after install
  --serial <serial>           adb device serial (default: single connected device)
  --skip-r8                   Also skip app R8 (-PFLUFFY_FAST_BUILD=true); often slower
  --java-home <path>          Override JAVA_HOME
  --android-sdk-root <path>   Override ANDROID_SDK_ROOT / ANDROID_HOME
  -h, --help                  Show this help
EOF
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

script_root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$script_root"

add_to_path() {
  local entry="$1"
  [[ -n "$entry" && -d "$entry" ]] || return 0
  case ":$PATH:" in
    *":$entry:"*) ;;
    *) export PATH="$entry:$PATH" ;;
  esac
}

resolve_first_existing_path() {
  local candidate
  for candidate in "$@"; do
    [[ -n "$candidate" ]] || continue
    if [[ -d "$candidate" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  return 1
}

JAVA_CANDIDATES=(
  "$JAVA_HOME_OVERRIDE"
  "${JAVA_HOME:-}"
  "/usr/lib/jvm/java-21-openjdk-amd64"
  "/usr/lib/jvm/java-21-openjdk"
  "/usr/lib/jvm/default-java"
  "/usr/lib/jvm/default"
  "/opt/android-studio/jbr"
  "${HOME}/.jdks/temurin-21"
)

if resolved_java_home="$(resolve_first_existing_path "${JAVA_CANDIDATES[@]}")"; then
  export JAVA_HOME="$resolved_java_home"
  add_to_path "$JAVA_HOME/bin"
  printf 'Using JAVA_HOME: %s\n' "$JAVA_HOME"
else
  echo "Warning: no JAVA_HOME candidate found. Falling back to current environment." >&2
fi

SDK_CANDIDATES=(
  "$ANDROID_SDK_ROOT_OVERRIDE"
  "${ANDROID_SDK_ROOT:-}"
  "${ANDROID_HOME:-}"
  "${HOME}/Android/Sdk"
  "/home/krol/Android/Sdk"
  "/opt/android-sdk"
  "/usr/lib/android-sdk"
)

if resolved_sdk_root="$(resolve_first_existing_path "${SDK_CANDIDATES[@]}")"; then
  export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$resolved_sdk_root}"
  export ANDROID_HOME="${ANDROID_HOME:-$resolved_sdk_root}"
  add_to_path "$resolved_sdk_root/platform-tools"
  printf 'Using Android SDK: %s\n' "$resolved_sdk_root"
fi

if [[ -z "${NDK_CCACHE:-}" ]] && command -v ccache >/dev/null 2>&1; then
  export NDK_CCACHE="$(command -v ccache)"
  printf 'Using NDK_CCACHE: %s\n' "$NDK_CCACHE"
fi

echo "== FluffyGram Fast Release Build & Deploy =="
java -version

gradle_tasks=()
if [[ "$CLEAN" -eq 1 ]]; then
  gradle_tasks+=("clean")
fi
gradle_tasks+=(":TMessagesProj_App:assembleAfatRelease")

extra_props=()
if [[ "$SKIP_R8" -eq 1 ]]; then
  extra_props+=(-PFLUFFY_FAST_BUILD=true)
  echo "Note: --skip-r8 enabled (full dex may be slower than single app R8)."
fi

printf 'Running Gradle tasks: %s\n' "${gradle_tasks[*]}"
printf 'Gradle heap: %s, max workers: %s\n' "$GRADLE_MAX_HEAP" "$GRADLE_MAX_WORKERS"
START=$(date +%s)
./gradlew --console=plain --max-workers="$GRADLE_MAX_WORKERS" \
  "-Dorg.gradle.jvmargs=-Xmx${GRADLE_MAX_HEAP} -XX:+UseParallelGC -Dfile.encoding=UTF-8" \
  "${extra_props[@]}" \
  "${gradle_tasks[@]}"
END=$(date +%s)
printf 'Gradle finished in %ss\n' "$((END - START))"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb was not found in PATH." >&2
  exit 1
fi

apk_path="$script_root/TMessagesProj_App/build/outputs/apk/afat/release/app.apk"
if [[ ! -f "$apk_path" ]]; then
  echo "APK not found: $apk_path" >&2
  exit 1
fi

if [[ -n "$SERIAL" ]]; then
  target_device="$SERIAL"
else
  mapfile -t adb_devices < <(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')
  if [[ "${#adb_devices[@]}" -eq 0 ]]; then
    echo "No connected adb devices found." >&2
    exit 1
  fi
  if [[ "${#adb_devices[@]}" -gt 1 ]]; then
    echo "Multiple adb devices are connected. Pass --serial <id>." >&2
    printf 'Devices: %s\n' "${adb_devices[*]}" >&2
    exit 1
  fi
  target_device="${adb_devices[0]}"
fi

printf 'Installing APK on %s...\n' "$target_device"
adb -s "$target_device" install -r -d "$apk_path"

echo "Fast release build installed successfully (org.ushastoe.fluffy)."

if [[ "$LAUNCH" -eq 1 ]]; then
  echo "Launching app..."
  adb -s "$target_device" shell am force-stop org.ushastoe.fluffy >/dev/null 2>&1 || true
  adb -s "$target_device" shell am start -n org.ushastoe.fluffy/org.telegram.ui.LaunchActivity >/dev/null
  echo "App launched on device."
else
  echo "Launch skipped."
fi

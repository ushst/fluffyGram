#!/usr/bin/env bash

set -euo pipefail

CLEAN=0
LAUNCH=1
JAVA_HOME_OVERRIDE=""
ANDROID_SDK_ROOT_OVERRIDE=""

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
Usage: ./build_and_deploy_debug.sh [options]

Options:
  --clean                     Run clean before build/install
  --no-launch                 Skip app launch after install
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
  export GRADLE_OPTS="-Dorg.gradle.java.home=\"$JAVA_HOME\""
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
else
  echo "Warning: Android SDK root not found in common locations. Gradle may rely on local.properties." >&2
fi

echo "== FluffyGram Debug Build & Deploy =="
java -version

gradle_tasks=()
if [[ "$CLEAN" -eq 1 ]]; then
  gradle_tasks+=("clean")
fi
gradle_tasks+=(":TMessagesProj_App:assembleAfatDebug" ":TMessagesProj_App:installAfatDebug")

printf 'Running Gradle tasks: %s\n' "${gradle_tasks[*]}"
./gradlew "${gradle_tasks[@]}"

echo "Debug build installed successfully."

if [[ "$LAUNCH" -eq 1 ]]; then
  if ! command -v adb >/dev/null 2>&1; then
    echo "adb was not found in PATH. Set ANDROID_SDK_ROOT/ANDROID_HOME or install platform-tools." >&2
    exit 1
  fi

  echo "Launching app..."
  adb shell am start -n org.ushastoe.fluffy.beta/org.telegram.ui.LaunchActivity >/dev/null
  echo "App launched on device."
else
  echo "Launch skipped."
fi

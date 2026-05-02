#!/bin/bash
# Helper script to generate delta patches for the last few releases.
# Usage: ./scripts/make_delta_release.sh <current_apk> <output_dir>

CURRENT_APK=$1
OUTPUT_DIR=$2

if [ -z "$CURRENT_APK" ] || [ -z "$OUTPUT_DIR" ]; then
    echo "Usage: $0 <current_apk> <output_dir>"
    exit 1
fi

mkdir -p "$OUTPUT_DIR"

# Install bsdiff if not present (Ubuntu)
if ! command -v bsdiff &> /dev/null; then
    sudo apt-get update && sudo apt-get install -y bsdiff
fi

# Get current version code
VERSION_CODE=$(grep "APP_VERSION_CODE" gradle.properties | cut -d'=' -f2)
PATCH_VERSION=$(grep "FLUFFY_PATCH_VERSION" gradle.properties | cut -d'=' -f2)
FULL_VERSION_CODE=$(( (VERSION_CODE * 1000 + PATCH_VERSION) * 10 + 9 ))

echo "Ready to generate deltas for version code $FULL_VERSION_CODE"

#!/bin/sh
set -eu
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DIST="$HOME/.gradle/wrapper/dists/gradle-9.3.1-bin"
ZIP="$BASE_DIR/.gradle-wrapper/gradle.zip"
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
if [ ! -x "$BASE_DIR/.gradle-wrapper/gradle-9.3.1/bin/gradle" ]; then
  mkdir -p "$BASE_DIR/.gradle-wrapper"
  if [ ! -f "$ZIP" ]; then
    curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-9.3.1-bin.zip" -o "$ZIP"
  fi
  unzip -q -o "$ZIP" -d "$BASE_DIR/.gradle-wrapper"
fi
exec "$BASE_DIR/.gradle-wrapper/gradle-9.3.1/bin/gradle" "$@"

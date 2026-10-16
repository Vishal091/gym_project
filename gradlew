#!/usr/bin/env sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=8.9
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/gympilot-distributions"
DIST="$BASE/gradle-$GRADLE_VERSION"
mkdir -p "$BASE"
if [ ! -x "$DIST/bin/gradle" ]; then
  ARCHIVE="$BASE/gradle-$GRADLE_VERSION-bin.zip"
  if [ ! -f "$ARCHIVE" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ARCHIVE"
    elif command -v wget >/dev/null 2>&1; then
      wget "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -O "$ARCHIVE"
    else
      echo "curl or wget is required to download Gradle $GRADLE_VERSION." >&2
      exit 1
    fi
  fi
  command -v unzip >/dev/null 2>&1 || { echo "unzip is required to install Gradle." >&2; exit 1; }
  unzip -q -o "$ARCHIVE" -d "$BASE"
  rm -f "$ARCHIVE"
fi
exec "$DIST/bin/gradle" -p "$APP_HOME" "$@"

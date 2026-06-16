#!/bin/sh
# Self-bootstrapping Gradle wrapper (no gradle-wrapper.jar needed)
set -e

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
PROPS="$APP_DIR/gradle/wrapper/gradle-wrapper.properties"

# Parse distribution URL (unescape \: → :)
DIST_URL="$(grep '^distributionUrl=' "$PROPS" | cut -d= -f2- | tr -d '\r' | sed 's/\\//g')"
GRADLE_VER="$(echo "$DIST_URL" | sed 's/.*gradle-\(.*\)-bin\.zip/\1/')"

GRADLE_CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${GRADLE_VER}-bin"
GRADLE_BIN="$GRADLE_CACHE/gradle-${GRADLE_VER}/bin/gradle"

if [ ! -f "$GRADLE_BIN" ]; then
    echo ">> Downloading Gradle $GRADLE_VER ..."
    mkdir -p "$GRADLE_CACHE"
    TMP="/tmp/gradle-${GRADLE_VER}.zip"
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "$DIST_URL" -o "$TMP"
    else
        wget -q "$DIST_URL" -O "$TMP"
    fi
    unzip -q "$TMP" -d "$GRADLE_CACHE"
    rm -f "$TMP"
    echo ">> Gradle $GRADLE_VER ready."
fi

exec "$GRADLE_BIN" "$@"

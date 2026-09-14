#!/usr/bin/env bash
# Cloud Agent install script for the Sharter (KurobaEx fork) Android app.
# Idempotent: safe to re-run. Ensures the Android SDK is present, writes
# Kuroba/local.properties, and warms the Gradle/Kotlin build caches.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
CMDLINE_TOOLS_VERSION="11076708"
COMPILE_SDK="36"
BUILD_TOOLS="36.0.0"

export ANDROID_SDK_ROOT
export ANDROID_HOME="$ANDROID_SDK_ROOT"

echo "==> Using ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"

# 1. Install the Android command-line tools if missing.
SDKMANAGER="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
if [[ ! -x "$SDKMANAGER" ]]; then
  echo "==> Installing Android command-line tools"
  tmp_zip="$(mktemp --suffix=.zip)"
  curl -fsSL -o "$tmp_zip" \
    "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
  tmp_dir="$(mktemp -d)"
  unzip -q "$tmp_zip" -d "$tmp_dir"
  mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  mv "$tmp_dir"/cmdline-tools/* "$ANDROID_SDK_ROOT/cmdline-tools/latest/"
  rm -rf "$tmp_zip" "$tmp_dir"
fi

# 2. Accept licenses and install the SDK packages the build needs.
#    sdkmanager is a no-op for already-installed packages, so this is idempotent.
echo "==> Accepting SDK licenses"
yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true
echo "==> Installing SDK packages (platform-tools, platforms;android-${COMPILE_SDK}, build-tools;${BUILD_TOOLS})"
"$SDKMANAGER" "platform-tools" "platforms;android-${COMPILE_SDK}" "build-tools;${BUILD_TOOLS}" >/dev/null

# 3. Point the Gradle build at the SDK.
echo "==> Writing Kuroba/local.properties"
printf 'sdk.dir=%s\n' "$ANDROID_SDK_ROOT" > "$REPO_ROOT/Kuroba/local.properties"

# 4. Warm the Gradle/Kotlin caches and confirm the app assembles.
echo "==> Warming Gradle caches (assembleDebug, buildType=2/Dev)"
cd "$REPO_ROOT/Kuroba"
chmod +x ./gradlew
./gradlew :app:assembleDebug -PbuildType=2 -Dorg.gradle.jvmargs="-Xmx4096m" --stacktrace

echo "==> Install complete."

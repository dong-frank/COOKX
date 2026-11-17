#!/usr/bin/env bash
set -eu
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "$script_dir/android-env.sh"
missing=0
printf 'ANDROID_HOME=%s\nJAVA_HOME=%s\n' "$ANDROID_HOME" "${JAVA_HOME:-<unset>}"
for tool in java javac android sdkmanager avdmanager adb emulator; do
  if command -v "$tool" >/dev/null 2>&1; then
    printf '%s: %s\n' "$tool" "$(command -v "$tool")"
  else
    printf 'MISSING: %s\n' "$tool"
    missing=1
  fi
done
if [ "$missing" -ne 0 ]; then exit 1; fi
java -version
javac -version
android --version
adb version
emulator -version
emulator -accel-check
printf '\nInstalled SDK packages:\n'
android --sdk="$ANDROID_HOME" sdk list
printf '\nVirtual devices:\n'
emulator -list-avds
printf '\nConnected devices:\n'
adb devices -l

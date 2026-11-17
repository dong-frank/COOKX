# Source this file: source scripts/android-env.sh
# Android Studio's bundled JBR is also used by command-line Gradle.
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
if [ -z "${JAVA_HOME:-}" ] && [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi
if [ -n "${JAVA_HOME:-}" ]; then
  case ":$PATH:" in
    *":$JAVA_HOME/bin:"*) ;;
    *) export PATH="$JAVA_HOME/bin:$PATH" ;;
  esac
fi
for cookx_android_bin in "$ANDROID_HOME/cmdline-tools/latest/bin" "$ANDROID_HOME/platform-tools" "$ANDROID_HOME/emulator"; do
  case ":$PATH:" in
    *":$cookx_android_bin:"*) ;;
    *) export PATH="$cookx_android_bin:$PATH" ;;
  esac
done
unset cookx_android_bin

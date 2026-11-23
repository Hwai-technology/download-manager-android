#!/usr/bin/env bash
# ==============================================================================
# Script: scripts/build-android-apk.sh
# Purpose: Compiles the Android native application (Debug APK) locally,
#          ensuring JDK 17, Android SDK, and JNI libraries are available.
# ==============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "=== [MDM] Building Android Application (Debug APK) Locally ==="

# 1. Detect and configure Java 17
if [ -z "${JAVA_HOME:-}" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "17'; then
    for candidate in /usr/lib/jvm/java-17-temurin-jdk /usr/lib/jvm/temurin-17-jdk /usr/lib/jvm/java-17-openjdk /usr/lib/jvm/java-17; do
        if [ -d "$candidate" ]; then
            export JAVA_HOME="$candidate"
            export PATH="$JAVA_HOME/bin:$PATH"
            break
        fi
    done
fi

if [ -z "${JAVA_HOME:-}" ]; then
    echo "Notice: Java 17 not found explicitly in default locations. Using system java:"
    java -version || true
else
    echo "Using JAVA_HOME: $JAVA_HOME"
fi

# 2. Detect Android SDK
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

if [ ! -d "$ANDROID_HOME" ]; then
    echo "Error: Android SDK not found at '$ANDROID_HOME'. Please set ANDROID_HOME."
    exit 1
fi
echo "Using ANDROID_HOME: $ANDROID_HOME"

# 3. Ensure native Rust JNI libraries (.so) exist
JNI_LIBS_DIR="$ROOT_DIR/android/app/src/main/jniLibs"
if [ ! -f "$JNI_LIBS_DIR/arm64-v8a/libmdm_mobile.so" ] || [ ! -f "$JNI_LIBS_DIR/x86_64/libmdm_mobile.so" ]; then
    echo "Native JNI libraries missing; triggering scripts/build-android-libs.sh..."
    bash "$ROOT_DIR/scripts/build-android-libs.sh"
else
    echo "Verified existing JNI libraries in $JNI_LIBS_DIR."
fi

# 4. Compile APK with Gradle
echo "Running Gradle assembleDebug..."
cd "$ROOT_DIR/android"
chmod +x gradlew
./gradlew assembleDebug

APK_PATH="$ROOT_DIR/android/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo "=========================================================="
    echo "SUCCESS: Android Debug APK compiled successfully:"
    echo "Path: $APK_PATH"
    echo "Size: $(du -h "$APK_PATH" | cut -f1)"
    echo "=========================================================="
else
    echo "Error: Expected APK output not found at $APK_PATH"
    exit 1
fi

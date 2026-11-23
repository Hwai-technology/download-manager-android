#!/usr/bin/env bash
# ==============================================================================
# Script: scripts/build-android-libs.sh
# Purpose: Compiles Rust mobile FFI shared libraries (libmdm_mobile.so) with
#          JNI support for Android architectures (arm64-v8a and x86_64).
# ==============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "=== [MDM] Building Android Native JNI Libraries (libmdm_mobile.so) ==="

# 1. Resolve Android SDK and NDK Paths
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
if [ -z "${ANDROID_NDK_HOME:-}" ] || [ ! -d "$ANDROID_NDK_HOME" ]; then
    for candidate in \
        "$ANDROID_HOME/ndk/27.1.12297006" \
        "$ANDROID_HOME/ndk"/* \
        "/opt/android-sdk/ndk/27.1.12297006" \
        "/opt/android-sdk/ndk"/*; do
        if [ -d "$candidate" ]; then
            export ANDROID_NDK_HOME="$candidate"
            break
        fi
    done
fi

if [ -z "${ANDROID_NDK_HOME:-}" ] || [ ! -d "$ANDROID_NDK_HOME" ]; then
    echo "Error: Android NDK not found. Please set ANDROID_NDK_HOME to your NDK path."
    exit 1
fi
echo "Using Android NDK: $ANDROID_NDK_HOME"

TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64"
API=26

TARGET_DIR="$ROOT_DIR/android/app/src/main/jniLibs"
mkdir -p "$TARGET_DIR/arm64-v8a" "$TARGET_DIR/x86_64"

# 2. Build for aarch64-linux-android (arm64-v8a)
echo "--> Compiling for aarch64-linux-android (arm64-v8a)..."
export CC_aarch64_linux_android="$TOOLCHAIN/bin/aarch64-linux-android${API}-clang"
export AR_aarch64_linux_android="$TOOLCHAIN/bin/llvm-ar"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$TOOLCHAIN/bin/aarch64-linux-android${API}-clang"

cargo build --package mdm-mobile --features jni --target aarch64-linux-android --release
cp "$ROOT_DIR/target/aarch64-linux-android/release/libmdm_mobile.so" "$TARGET_DIR/arm64-v8a/"
echo "    [OK] arm64-v8a placed in $TARGET_DIR/arm64-v8a/libmdm_mobile.so"

# 3. Build for x86_64-linux-android (x86_64)
echo "--> Compiling for x86_64-linux-android (x86_64)..."
export CC_x86_64_linux_android="$TOOLCHAIN/bin/x86_64-linux-android${API}-clang"
export AR_x86_64_linux_android="$TOOLCHAIN/bin/llvm-ar"
export CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER="$TOOLCHAIN/bin/x86_64-linux-android${API}-clang"

cargo build --package mdm-mobile --features jni --target x86_64-linux-android --release
cp "$ROOT_DIR/target/x86_64-linux-android/release/libmdm_mobile.so" "$TARGET_DIR/x86_64/"
echo "    [OK] x86_64 placed in $TARGET_DIR/x86_64/libmdm_mobile.so"

echo "=========================================================="
echo "SUCCESS: All native JNI libraries compiled and synchronized."
echo "=========================================================="

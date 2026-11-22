#!/usr/bin/env bash
set -euo pipefail

export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-/opt/android-sdk/ndk/27.1.12297006}}"
if [ ! -d "$ANDROID_NDK_HOME" ] && [ -d "/home/maya/Android/Sdk/ndk/27.1.12297006" ]; then
    export ANDROID_NDK_HOME="/home/maya/Android/Sdk/ndk/27.1.12297006"
fi
TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64"
API=26

TARGET_DIR="android/app/src/main/jniLibs"
mkdir -p "$TARGET_DIR/arm64-v8a" "$TARGET_DIR/x86_64"

echo "Building mdm-mobile for aarch64-linux-android..."
export CC_aarch64_linux_android="$TOOLCHAIN/bin/aarch64-linux-android${API}-clang"
export AR_aarch64_linux_android="$TOOLCHAIN/bin/llvm-ar"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$TOOLCHAIN/bin/aarch64-linux-android${API}-clang"

cargo build --package mdm-mobile --features jni --target aarch64-linux-android --release
cp target/aarch64-linux-android/release/libmdm_mobile.so "$TARGET_DIR/arm64-v8a/"

echo "Building mdm-mobile for x86_64-linux-android..."
export CC_x86_64_linux_android="$TOOLCHAIN/bin/x86_64-linux-android${API}-clang"
export AR_x86_64_linux_android="$TOOLCHAIN/bin/llvm-ar"
export CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER="$TOOLCHAIN/bin/x86_64-linux-android${API}-clang"

cargo build --package mdm-mobile --features jni --target x86_64-linux-android --release
cp target/x86_64-linux-android/release/libmdm_mobile.so "$TARGET_DIR/x86_64/"

echo "Done. Native libraries placed in $TARGET_DIR."

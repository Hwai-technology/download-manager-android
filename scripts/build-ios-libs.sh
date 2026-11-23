#!/usr/bin/env bash
# ==============================================================================
# Script: scripts/build-ios-libs.sh
# Purpose: Compiles the Rust mobile FFI static library (libmdm_mobile.a)
#          and prepares it for the native iOS Xcode project.
# ==============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "=== [MDM] Building iOS Static Library (libmdm_mobile.a) ==="

echo "--> Cross-compiling mdm-mobile static library for aarch64-apple-ios..."
cargo build --package mdm-mobile --target aarch64-apple-ios --release || {
    echo "Notice: Native iOS compilation requires a macOS host with Xcode and Apple SDK toolchains."
    echo "Run this script directly on macOS to generate the production static archive."
    exit 0
}

TARGET_DIR="$ROOT_DIR/ios/MisterDownloadManager/Libs"
mkdir -p "$TARGET_DIR"
cp "$ROOT_DIR/target/aarch64-apple-ios/release/libmdm_mobile.a" "$TARGET_DIR/"
echo "=========================================================="
echo "SUCCESS: iOS static library placed in $TARGET_DIR/libmdm_mobile.a"
echo "=========================================================="

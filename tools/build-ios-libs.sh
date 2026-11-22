#!/usr/bin/env bash
set -euo pipefail

echo "Building mdm-mobile static library for iOS..."
cargo build --package mdm-mobile --target aarch64-apple-ios --release || {
    echo "Note: Cross-compilation for iOS requires Apple SDK and clang toolchain (available on macOS)."
    exit 0
}

mkdir -p ios/MisterDownloadManager/Libs
cp target/aarch64-apple-ios/release/libmdm_mobile.a ios/MisterDownloadManager/Libs/
echo "Static library built and copied to ios/MisterDownloadManager/Libs/"

# Docker Build Pipeline for Mister Download Manager Native Mobile

This document details the multi-stage Docker build pipeline for building and packaging native mobile artifacts for Mister Download Manager.

## Overview

The build pipeline automates the compilation of:
1. Rust Core Engine & Mobile FFI crate (`mdm-engine`, `mdm-mobile`).
2. Android Native JNI shared libraries (`libmdm_mobile.so` for `arm64-v8a` and `x86_64`).
3. Android Native Application (Debug APK, Release APK, Release AAB).
4. Release artifact collection into `./build-output/` on the host machine.

---

## Prerequisites

- **Docker:** Engine 24.0+ with BuildKit support (or Docker Desktop on Linux/macOS/Windows).
- **Docker Compose:** Compose v2 / v5+.
- Minimum recommended resources: 4 CPU cores, 8 GB RAM, 20 GB free disk space.

---

## One-Command Execution

To build the entire pipeline end-to-end:

```bash
docker compose up --build
```

### Stage Execution & Ordering

The services are executed with sequential dependency constraints (`condition: service_completed_successfully`):

```
┌─────────────────────────────────┐
│     mdm-rust-check              │ (Runs cargo test -p mdm-mobile & cargo check -p mdm-app)
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│     mdm-android-libs            │ (Compiles libmdm_mobile.so for arm64-v8a & x86_64)
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│     mdm-android-app             │ (Compiles assembleDebug, assembleRelease, bundleRelease)
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│     mdm-release-artifacts       │ (Copies APKs, AAB, .so libs & headers to ./build-output)
└─────────────────────────────────┘
```

If any stage fails, downstream stages are halted immediately. Containers terminate upon task completion and do not stay idle.

---

## Target-Specific Execution

To run an individual stage:

```bash
# Verify Rust core & mobile FFI
docker compose up --build rust-check

# Compile Android native libraries only
docker compose up --build android-libs

# Compile Android application only
docker compose up --build android-app

# Run and extract artifacts
docker compose up --build release-artifacts
```

---

## Artifact Structure

Outputs are collected in the host repository's `./build-output/` directory:

```text
build-output/
├── rust/
│   └── jniLibs/
│       ├── arm64-v8a/
│       │   └── libmdm_mobile.so
│       └── x86_64/
│           └── libmdm_mobile.so
├── android/
│   ├── debug/
│   │   └── app-debug.apk
│   ├── release/
│   │   └── app-release-unsigned.apk
│   └── bundle/
│       └── app-release.aab
└── ios/
    └── build-preparation/
        ├── mdm_mobile.h
        └── build-ios-libs.sh
```

---

## Android Signing Configuration

By default, release builds produce unsigned artifacts (`app-release-unsigned.apk` / unsigned AAB).

To sign the release builds using production keystores, provide signing environment variables or mount a keystore file:

```bash
export KEYSTORE_FILE=/path/to/release.keystore
export KEYSTORE_PASSWORD=secret
export KEY_ALIAS=mdm
export KEY_PASSWORD=secret
```

These are passed to Gradle via Docker Compose environment mappings without committing secrets to version control.

---

## iOS / macOS Build Limitations

> [!IMPORTANT]
> **Technical Constraint:** Docker on Linux cannot produce an official signed iOS App Store IPA or Xcode Archive (.xcarchive).
> Apple's code signing framework, CoreCrypto, and Xcode command-line tools (`xcodebuild`) require macOS.

### iOS Workflow on macOS

To build the iOS application:

1. **Build the Rust Static Library:**
   On a Mac with Xcode and Rust installed (`rustup target add aarch64-apple-ios x86_64-apple-ios aarch64-apple-ios-sim`):
   ```bash
   ./tools/build-ios-libs.sh
   ```
2. **Open the Xcode Project:**
   ```bash
   open ios/MisterDownloadManager/MisterDownloadManager.xcodeproj
   ```
3. **Archive & Export IPA:**
   ```bash
   xcodebuild -project ios/MisterDownloadManager/MisterDownloadManager.xcodeproj \
     -scheme MisterDownloadManager \
     -archivePath build/MisterDownloadManager.xcarchive \
     archive
   ```


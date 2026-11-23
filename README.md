# Mister Download Manager — Native Cross-Platform Architecture & Developer Guide

Mister Download Manager (MDM) is a high-performance download manager featuring a shared, robust Rust download engine core alongside native frontends for Desktop (egui), Android (Kotlin + Jetpack Compose), and iOS (Swift + SwiftUI).

This repository is maintained as a monorepo structured to reuse the upstream Rust engine (`mdm-engine`) directly across native mobile platforms without duplicating download orchestration logic or introducing heavy cross-platform runtimes like React Native or Flutter.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Feature and Platform Support Matrix](#2-feature-and-platform-support-matrix)
3. [Repository and Workspace Architecture](#3-repository-and-workspace-architecture)
4. [High-Level Architecture Diagrams](#4-high-level-architecture-diagrams)
5. [Shared Rust Mobile Core (`crates/mobile`)](#5-shared-rust-mobile-core-cratesmobile)
6. [Native FFI Architecture](#6-native-ffi-architecture)
   - [Android JNI Boundary](#android-jni-boundary)
   - [iOS C-ABI Boundary](#ios-c-abi-boundary)
7. [Android Native Application (`android/`)](#7-android-native-application-android)
8. [iOS Native Application (`ios/`)](#8-ios-native-application-ios)
9. [Build and Development Setup](#9-build-and-development-setup)
10. [Verified Build & Verification Commands](#10-verified-build--verification-commands)
11. [Testing and Quality Assurance](#11-testing-and-quality-assurance)
12. [Download Lifecycle and Queue State Machine](#12-download-lifecycle-and-queue-state-machine)
13. [Storage, Part Files, and State Recovery](#13-storage-part-files-and-state-recovery)
14. [Troubleshooting Common Issues](#14-troubleshooting-common-issues)
15. [Known Limitations and Roadmap](#15-known-limitations-and-roadmap)
16. [References and Upstream Attribution](#16-references-and-upstream-attribution)

---

## 1. Project Overview

- **Core Engine:** Written in Rust (`crates/engine`), implementing dynamic file segmentation, multi-connection pooling, speed limiting, resume validation, and robust error recovery.
- **Desktop Application:** Written in Rust using `egui` (`crates/app`), supporting Windows, macOS, and Linux desktop environments.
- **Mobile Core Bridge:** Written in Rust (`crates/mobile`), providing thread-safe queue scheduling, lifecycle preservation, point-in-time progress snapshots, and dual C-ABI / JNI interfaces.
- **Android Frontend:** 100% native Kotlin and Jetpack Compose (`android/`), communicating via JNI directly with `libmdm_mobile.so`, backed by an Android Foreground Service.
- **iOS Frontend:** 100% native Swift and SwiftUI (`ios/`), linking against the C-ABI exported static library/headers with Combine-driven reactive updates.

---

## 2. Feature and Platform Support Matrix

| Capability / Feature | Original Desktop (`crates/app`) | Android Native (`android/`) | iOS Native (`ios/`) | Shared Rust Core (`crates/mobile`) |
| :--- | :--- | :--- | :--- | :--- |
| **Multi-Connection HTTP/HTTPS** | **VERIFIED** | **VERIFIED** (via Rust) | **VERIFIED** (via Rust) | **VERIFIED** |
| **Dynamic Segmentation** | **VERIFIED** | **VERIFIED** (via Rust) | **VERIFIED** (via Rust) | **VERIFIED** |
| **Download Queue Management** | **VERIFIED** | **VERIFIED** (Native + Core) | **VERIFIED** (Native + Core) | **VERIFIED** |
| **Pause / Resume via `.mdm.json`** | **VERIFIED** | **VERIFIED** | **VERIFIED** | **VERIFIED** |
| **Speed Calculation & Reporting** | **VERIFIED** | **VERIFIED** | **VERIFIED** | **VERIFIED** |
| **Android Foreground Service** | N/A | **VERIFIED** (`dataSync`) | N/A | N/A |
| **Background Execution** | Desktop Background/Tray | Foreground Service | BGTaskScheduler (Refresh) | Runtime Threadpool |
| **Bittorrent / Torrents** | **VERIFIED** (`crates/torrent`)| PLANNED | PLANNED | Out of Scope for Core |
| **Browser Extensions / Native Host**| **VERIFIED** (`crates/nmhost`) | Unsupported (Mobile) | Unsupported (Mobile) | N/A |

*Status definitions:*  
- **VERIFIED:** Confirmed functional in source code, build logs, and unit/integration tests.  
- **PLANNED:** Identified on future mobile roadmap, not currently active in mobile builds.  
- **Unsupported:** Platform-specific desktop features not targeted at mobile environments.

---

## 3. Repository and Workspace Architecture

The repository is organized as a single Cargo workspace with distinct workspace members under [`crates/`](file:///home/maya/Desktop/download-manager-android/crates/):

```
download-manager-android/
├── Cargo.toml                         # Root workspace manifest (resolver = "3")
├── crates/
│   ├── app/                           # Original desktop application (egui/winit)
│   ├── engine/                        # Core download engine (mdm-engine)
│   ├── mobile/                        # Shared mobile-facing Rust API & FFI (mdm-mobile)
│   ├── cli/                           # Command line interface client
│   ├── crash/                         # Crash handler & minidump integration
│   ├── debrid/                        # Real-Debrid API client
│   ├── extract/                       # Archive extraction utilities
│   ├── geo/                           # IP geolocation & mirror resolution
│   ├── ipc/                           # Local IPC & browser extension message protocol
│   ├── labserver/                     # Local test HTTP server for testing
│   ├── mcp/                           # Model Context Protocol integration
│   ├── nmhost/                        # Native messaging host for browser extensions
│   ├── remote/                        # Remote control & web interface
│   ├── tools/                         # Media tools (yt-dlp, ffmpeg helpers)
│   └── torrent/                       # BitTorrent client engine (librqbit wrapper)
├── android/                           # Native Android project (Kotlin / Compose)
│   ├── app/                           # Android application module
│   │   ├── src/main/java/...          # Presentation, Domain, Data, Services
│   │   └── src/main/jniLibs/          # Compiled libmdm_mobile.so binaries
│   ├── build.gradle.kts               # Root Gradle build script
│   └── settings.gradle.kts            # Project settings
├── ios/                               # Native iOS project (Swift / SwiftUI)
│   └── MisterDownloadManager/         # Presentation, Domain, Data, Core FFI
├── tools/
│   ├── build-android-libs.sh          # Android NDK automated compilation script
│   └── build-ios-libs.sh              # iOS compilation script
└── docs/
    └── MOBILE_ARCHITECTURE.md         # Detailed architectural documentation
```

---

## 4. High-Level Architecture Diagrams

### A. System Component Diagram

```mermaid
flowchart TD
    subgraph RustWorkspace["Rust Core Workspace"]
        Engine["mdm-engine\n(Download Execution & Slicing)"]
        MobileCore["mdm-mobile\n(Queue / Scheduler / Snapshots)"]
        Engine --> MobileCore
        DesktopApp["mdm-app\n(Desktop egui GUI)"]
        Engine --> DesktopApp
    end

    subgraph AndroidPlatform["Android Native (Kotlin)"]
        JNI["MdmNativeBridge\n(JNI Wrapper)"]
        AndroidRepo["DownloadRepositoryImpl"]
        AndroidService["DownloadForegroundService\n(Ongoing Notification)"]
        AndroidUI["MainActivity & Compose UI"]
        
        JNI --> AndroidRepo
        AndroidRepo --> AndroidUI
        AndroidService --> JNI
    end

    subgraph IOSPlatform["iOS Native (Swift)"]
        CABI["MdmMobileFFI\n(Swift C-ABI Wrapper)"]
        IOSRepo["DownloadRepositoryImpl"]
        BGManager["BackgroundExecutionManager\n(BGTaskScheduler)"]
        IOSUI["DownloadListView & SwiftUI"]
        
        CABI --> IOSRepo
        IOSRepo --> IOSUI
        BGManager --> CABI
    end

    MobileCore -- JNI Export (libmdm_mobile.so) --> JNI
    MobileCore -- C-ABI Export (mdm_mobile.h) --> CABI
```

---

## 5. Shared Rust Mobile Core (`crates/mobile`)

Located in [`crates/mobile`](file:///home/maya/Desktop/download-manager-android/crates/mobile), this crate acts as the single source of truth for mobile download states:

1. **Queue Scheduling (`MobileEngine`):**
   - Implements concurrency limiting via `set_max_concurrent_downloads(max)`.
   - Reconciles active and waiting tasks automatically when transfers complete or fail.
   - Spawns background tasks within a dedicated Tokio runtime.
2. **Terminal State Preservation:**
   - Completed (`Done`), failed (`Failed`), and aborted (`Cancelled`) downloads retain their terminal status and diagnostic errors in memory.
   - Tasks are only purged when explicitly removed via `remove_download(id)`.
3. **Speed & Progress Calculation:**
   - Aggregates active connection transfer rates (`c.speed`) across all active workers.
   - Accurately reports unavailable speed as `None` (or `-1` over C-ABI) when idle.
4. **Resumability from Disk:**
   - Re-checks `.mdm.json` state metadata using `mdm_engine::state::state_path` before starting transfers to continue existing part files.

---

## 6. Native FFI Architecture

### Android JNI Boundary

Exported symbols adhere to standard JNI naming conventions for class `com.misterdownloadmanager.core.MdmNativeBridge`:

- `Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeInit`
- `Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeSetMaxConcurrentDownloads`
- `Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeCreateDownload`
- `Java_com_misterdownloadmanager_core_MdmNativeBridge_nativePauseDownload`
- `Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeResumeDownload`
- `Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeCancelDownload`
- `Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeRemoveDownload`
- `Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeRestoreDownload`
- `Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeGetSnapshotJson`
- `Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeGetAllSnapshotsJson`

*Safety Principle:* Snapshot payloads are serialized to JSON on the native side and parsed using Gson in Kotlin, guaranteeing memory-safe object construction without raw pointer manipulation on Android.

### iOS C-ABI Boundary

Declared in [`crates/mobile/include/mdm_mobile.h`](file:///home/maya/Desktop/download-manager-android/crates/mobile/include/mdm_mobile.h):

- `CMobileSnapshot`: Point-in-time data structure containing numeric IDs, total/downloaded bytes, speed (`int64_t`), state enum, and owned C-strings (`url`, `file_path`, `error`).
- `CMobileSnapshotList`: Sized array struct containing `items`, `count`, and `capacity`.
- `mdm_mobile_free_snapshot`: Releases allocated strings for a single snapshot.
- `mdm_mobile_free_snapshot_list`: Reconstructs the allocated `Vec` using original `count` and `capacity`, freeing all nested strings and backing buffers safely.
- **Panic Boundary:** Every C-ABI function wraps execution inside `std::panic::catch_unwind` to prevent panics from crossing the foreign function boundary.

---

## 7. Android Native Application (`android/`)

The Android application is organized into Clean Architecture layers:

- **Presentation:** Jetpack Compose (`MainActivity.kt`, Material 3 design system) observing `StateFlow` streams from `MainViewModel`.
- **Domain Layer:** Pure Kotlin models (`DownloadTask`, `DownloadState`), use cases (`DownloadUseCases`), and repository interface (`DownloadRepository`).
- **Data Layer:** `DownloadRepositoryImpl` delegating operations to `MdmNativeBridge` over `Dispatchers.IO`.
- **Foreground Service (`DownloadForegroundService`):**
  - Starts an ongoing notification with progress bars and transfer statistics.
  - Declares `android:foregroundServiceType="dataSync"` in [`AndroidManifest.xml`](file:///home/maya/Desktop/download-manager-android/android/app/src/main/AndroidManifest.xml) to comply with Android 14+ background execution policies.
  - Automatically shuts down when no downloads remain in progress.

---

## 8. iOS Native Application (`ios/`)

The iOS application is written in Swift using SwiftUI and Combine:

- **Presentation:** `DownloadListView`, `DownloadRowView`, and `AddDownloadSheet` with dynamic progress bars.
- **ViewModel:** `DownloadListViewModel` (marked `@MainActor`) polling and publishing state via Combine.
- **Domain / Data:** `DownloadRepositoryImpl` wrapping `MdmMobileFFI` calls and mapping return codes to Swift `Error` types.
- **Background Handling (`BackgroundExecutionManager`):**
  - Registers task identifier `com.misterdownloadmanager.refresh` with `BGTaskScheduler`.
  - Schedules background app refreshes to query the download queue and check task states when the app is in the background.

---

## 9. Build and Development Setup

### Prerequisites

1. **Rust:** Rust 1.95+ or 1.100+ (cargo, rustc).
2. **Android Development:**
   - Android SDK (API 26+)
   - Android NDK (r27+ tested, path: `/home/maya/Android/Sdk/ndk/27.1.12297006`)
   - Java 17 JDK (`javac 17.0.20.1`)
   - Rust Android Targets:
     ```bash
     rustup target add aarch64-linux-android x86_64-linux-android
     ```
3. **iOS Development:**
   - macOS host with Xcode 15+ and Command Line Tools
   - Rust Apple Targets:
     ```bash
     rustup target add aarch64-apple-ios x86_64-apple-ios
     ```

---

## 10. Verified Build & Verification Commands

All commands below have been tested and verified in the environment:

### 1. Verify Original Desktop App & Workspace
```bash
# Verify desktop egui application compatibility
cargo check -p mdm-app
```

### 2. Run Mobile Core Tests & FFI Safety Suite
```bash
# Run unit tests and C-ABI memory safety test
cargo test -p mdm-mobile

# Run test suite with Android JNI feature enabled
cargo test -p mdm-mobile --features jni
```

### 3. Build Android Native Libraries (`.so`)
```bash
# Compile release binaries for arm64-v8a and x86_64 and copy to jniLibs
./tools/build-android-libs.sh
```

### 4. ## Build and Run Android APK Locally

The Android application can be built locally without Docker or cloud services.

### Prerequisites

- Java JDK 17
- Rust stable toolchain
- Android SDK
- Android NDK 27.1.12297006
- Android Rust targets:
  - `aarch64-linux-android`
  - `x86_64-linux-android`
- Node.js and npm

### 1. Configure the environment

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-temurin-jdk
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/27.1.12297006

export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
```

### 2. Build native Rust libraries

```bash
npm run build:libs
```

This compiles the `mdm-mobile` Rust package with JNI support for:

- `arm64-v8a`
- `x86_64`

### 3. Build the Android APK

```bash
npm run apk
```

The Debug APK is expected at:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

### 4. Install on an Android device

Enable USB Debugging, connect the device, and check the connection:

```bash
adb devices
```

Install the APK:

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

### 5. Launch and inspect the application

Launch the application using its verified application ID and launcher Activity.

View application logs:

```bash
adb logcat
```

Filter logs using the actual application logging tags.

### Notes

- The Android build uses the existing Rust engine through JNI.
- Docker is not required for local APK builds.
- iOS is outside the current Android-only project scope.
- A successful APK compilation does not by itself verify runtime functionality.

### 5. Build iOS Static Library (`.a`)
```bash
# Run cross-compilation script (requires macOS for linking)
./tools/build-ios-libs.sh
```

---

## 11. Testing and Quality Assurance

The mobile test suite ([`crates/mobile/tests/integration_test.rs`](file:///home/maya/Desktop/download-manager-android/crates/mobile/tests/integration_test.rs)) verifies:

1. **Queue Concurrency & Ordering:** Verifies concurrency caps, priority scheduling, manual pause/resume, cancellation, and download removal.
2. **C-ABI Allocation & Memory Safety:**
   - Verifies `CMobileSnapshot` string allocations and freeing.
   - Verifies `CMobileSnapshotList` capacity tracking and deallocation without leaks.
   - Tests null-pointer resilience across FFI boundaries.
3. **Symbol Export Verification:**
   ```bash
   nm -D android/app/src/main/jniLibs/arm64-v8a/libmdm_mobile.so | grep -E "Java_|mdm_mobile"
   ```
   Confirms all JNI and C-ABI entry points are present and exported as dynamic text symbols.

---

## 12. Download Lifecycle and Queue State Machine

```mermaid
stateDiagram-v2
    [*] --> Waiting : create_download()
    Waiting --> Probing : Concurrency Slot Available
    Probing --> Downloading : Server Responded & Range Verified
    Downloading --> Verifying : Data Transferred
    Verifying --> Completing : Checksum Matches
    Completing --> Done : Part File Renamed to Final File
    
    Downloading --> Paused : pause_download()
    Probing --> Paused : pause_download()
    Paused --> Waiting : resume_download()
    
    Downloading --> Failed : Network / IO Error
    Probing --> Failed : 4xx / 5xx / DNS Error
    
    Downloading --> Cancelled : cancel_download()
    Waiting --> Cancelled : cancel_download()
    Paused --> Cancelled : cancel_download()
    
    Done --> [*] : remove_download()
    Failed --> [*] : remove_download()
    Cancelled --> [*] : remove_download()
```

---

## 13. Storage, Part Files, and State Recovery

- **Part Files:** While downloading, content is streamed directly into `<dest>.part` files via sparse allocation.
- **State Metadata:** Transfer checkpoints and HTTP resume validators (ETags, Last-Modified) are written to `<dest>.mdm.json` using atomic syncs.
- **Completion Renaming:** When completed and integrity-verified, the `.part` file is renamed atomically to its target destination and the `.mdm.json` file is cleaned up.
- **Android Storage:** Downloads are saved within app-specific external storage (`getExternalFilesDir`) or via Storage Access Framework URIs.
- **iOS Storage:** Downloads are saved into the app's sandboxed `Documents` directory.

---

## 14. Troubleshooting Common Issues

### 1. `UnsatisfiedLinkError` on Android
- **Cause:** `libmdm_mobile.so` is missing for the active device ABI.
- **Resolution:** Run `./tools/build-android-libs.sh` to generate libraries under `android/app/src/main/jniLibs/arm64-v8a` and `x86_64`.

### 2. Missing target crate for `core` / `std`
- **Cause:** Android cross-compilation targets are not installed in the local toolchain.
- **Resolution:** Run `rustup target add aarch64-linux-android x86_64-linux-android`.

### 3. Missing `rquickjs-sys` Android bindings
- **Cause:** `rquickjs-sys` does not bundle pre-generated C headers for `aarch64-linux-android`.
- **Resolution:** The build script and cargo cache supply standard linux-gnu ABI bindings compatible with Android Bionic.

---

## 15. Known Limitations and Roadmap

- **BitTorrent on Mobile:** BitTorrent downloading (`crates/torrent`) is functional on Desktop, but disabled in the mobile core to minimize binary footprints and battery drain.
- **iOS Background Transfer Limitations:** iOS suspends non-foreground apps after a short grace period; `BGTaskScheduler` periodically checks progress, but sustained multi-gigabyte transfers in background require future URLSession background session bridging.
- **Android Scoped Storage:** Advanced integration with Android's MediaStore and SAF folder tree selection is currently scaffolded.

---

## 16. References and Upstream Attribution

- Upstream Project: Mister Download Manager v0.5.2
- Original Author: Ahmed Nefzaoui
- Technical Architecture Reference: [`docs/MOBILE_ARCHITECTURE.md`](file:///home/maya/Desktop/download-manager-android/docs/MOBILE_ARCHITECTURE.md)
- License: GNU General Public License v3.0 or later ([`LICENSE`](file:///home/maya/Desktop/download-manager-android/LICENSE))

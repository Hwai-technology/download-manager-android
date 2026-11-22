# Mister Download Manager Mobile Architecture & Verification Report

## 1. Overview and Engineering Status

This document details the production-oriented architecture, C-ABI/JNI memory safety boundaries, and build verification for Mister Download Manager on Android and iOS using the shared Rust core (`mdm-engine`).

---

## 2. Core Architecture & Layers

```
                      +-----------------------------+
                      |         mdm-engine          |
                      |  (Original Download Engine) |
                      +--------------+--------------+
                                     |
                      +--------------v--------------+
                      |         mdm-mobile          |
                      |  (Queue / Lifecycle Core)   |
                      +--------------+--------------+
                                     |
             +-----------------------+-----------------------+
             |                                               |
             | JNI Bridge (feature="jni")                    | C-ABI (Direct Native Calls)
             v                                               v
+-------------------------+                     +-------------------------+
|   Android Application   |                     |     iOS Application     |
| (Kotlin + Jetpack UI)   |                     | (Swift + SwiftUI)       |
+-------------------------+                     +-------------------------+
| - UI: Jetpack Compose   |                     | - UI: SwiftUI           |
| - ViewModel & StateFlow |                     | - ViewModel & Combine   |
| - Use Cases             |                     | - Use Cases             |
| - Repository            |                     | - Repository            |
| - MdmNativeBridge       |                     | - MdmMobileFFI          |
| - Foreground Service    |                     | - BGTaskScheduler       |
+-------------------------+                     +-------------------------+
```

---

## 3. Rust Mobile Core (`crates/mobile`)

### A. Lifecycle Management & Queue Scheduling
- **State Preservation:** When a download reaches terminal state (`Done`, `Failed`, `Cancelled`), its terminal state and error description are explicitly preserved. It is never converted back into `Waiting`.
- **Automatic Queue Advancement:** A background async monitoring loop (`reconcile_queue`) checks active downloads and automatically promotes queued downloads into available concurrent slots when any running task finishes or fails.
- **Dynamic Speed Calculation:** Snapshot speed is computed dynamically by summing connection speeds (`c.speed`). If a download is idle, speed is represented as unavailable (`-1` / `None`).
- **Resumption from Part Files:** Queue reconciliation checks if an existing `.mdm.json` state file exists via `mdm_engine::state::state_path(&entry.dest)` and invokes `Download::resume(...)` with saved state.

### B. C-ABI & Memory Safety Audit
- **Header Guards & Declarations:** `crates/mobile/include/mdm_mobile.h` includes include guards (`#ifndef MDM_MOBILE_H`), `extern "C"` blocks, and exact `int64_t`/`uint64_t` types matching `#[repr(C)]`.
- **List Allocation & Deallocation:** `CMobileSnapshotList` tracks both `count` and `capacity`. Deallocation reconstructs `Vec::from_raw_parts(l.items, l.count, l.capacity)` to avoid mismatched capacity deallocation.
- **Panic Boundaries:** Every exported C-ABI function is wrapped in `std::panic::catch_unwind` to prevent Rust panics from unwinding across foreign stack frames.
- **String Safety:** Null pointers and invalid UTF-8 sequences are checked upfront and return negative error codes.

---

## 4. Platform Integrations

### Android (`android/`)
1. **JNI Integration:** `com.misterdownloadmanager.core.MdmNativeBridge` links to `libmdm_mobile.so`.
2. **Native Libraries:** Built with Android NDK 27 (`API 26`) for `arm64-v8a` and `x86_64`. Placed in `android/app/src/main/jniLibs/`.
3. **Foreground Service:** `DownloadForegroundService` displays real snapshot progress in ongoing notifications and specifies `android:foregroundServiceType="dataSync"` compliant with Android 14+.

### iOS (`ios/`)
1. **Swift Bridge:** `MdmMobileFFI.swift` calls C-ABI functions directly and manages `CMobileSnapshotList` cleanup via `mdm_mobile_free_snapshot_list`.
2. **Background Execution:** `BackgroundExecutionManager.swift` integrates with `BGTaskScheduler` (`BGAppRefreshTask`) and documents that persistent background execution requires proper background modes.

---

## 5. Verification Results

| Target | Command | Result |
| :--- | :--- | :--- |
| **Desktop App** | `cargo check -p mdm-app` | **PASSED** (0 errors) |
| **Mobile Core Tests** | `cargo test -p mdm-mobile` | **PASSED** (2 tests: C-ABI memory safety, queue lifecycle) |
| **Mobile Core JNI** | `cargo test -p mdm-mobile --features jni` | **PASSED** (2 tests) |
| **Android NDK Compilation** | `./tools/build-android-libs.sh` | **PASSED** (Built `arm64-v8a` & `x86_64` `.so` binaries) |
| **iOS Build** | `./tools/build-ios-libs.sh` | Scaffolded (Requires macOS / Xcode SDK for linking) |

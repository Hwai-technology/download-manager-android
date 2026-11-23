# Contributing to Mister Download Manager (MDM)

Thank you for your interest in contributing to **Mister Download Manager**! We welcome bug reports, feature proposals, documentation improvements, translation updates, and code contributions.

This document outlines the development workflow, coding standards, and guidelines for contributing to this monorepo.

---

## Table of Contents

1. [Principles & Clean-Room Standard](#1-principles--clean-room-standard)
2. [Development Environment Setup](#2-development-environment-setup)
   - [Rust Engine & Desktop](#rust-engine--desktop)
   - [Android Native (Kotlin & Compose)](#android-native-kotlin--compose)
   - [iOS Native (Swift & SwiftUI)](#ios-native-swift--swiftui)
3. [Coding Standards & Architecture](#3-coding-standards--architecture)
4. [Translations & Internationalization (`i18n`)](#4-translations--internationalization-i18n)
5. [Pre-Pull Request Verification Checklist](#5-pre-pull-request-verification-checklist)
6. [Pull Request Process](#6-pull-request-process)
7. [Reporting Issues & Security Vulnerabilities](#7-reporting-issues--security-vulnerabilities)

---

## 1. Principles & Clean-Room Standard

- **Strict Clean-Room Development:** MDM is built from scratch. It contains **no code, binaries, artwork, icons, sounds, or text** from Internet Download Manager (IDM) or any other proprietary download manager.
  - Never disassemble, decompile, or reverse-engineer proprietary software.
  - Never copy code from non-GPL-compatible or closed-source implementations.
- **Safe, Portable Rust:** All crates strictly enforce `#![deny(unsafe_code)]`. Windows-specific FFI is confined to `crates/app/src/win32.rs`, and Unix specifics to `unix.rs`.
- **Single Source of Truth:** Download orchestration, state persistence (`.mdm.json`), and dynamic segmentation live exclusively in the shared Rust engine (`crates/engine` and `crates/mobile`). Mobile frontends act as reactive UI layers over native FFI.

---

## 2. Development Environment Setup

### Rust Engine & Desktop
- **Rust Toolchain:** Rust 1.95+ stable (Cargo, Clippy, Rustfmt).
- **Build & Run Desktop App:**
  ```bash
  cargo build
  cargo run -p mdm-app
  ```
- **Local Isolated Data Directory:** Test without modifying your personal downloads:
  ```bash
  MDM_DATA_DIR="/tmp/mdm-test" cargo run -p mdm-app
  ```
- **Fault-Injection Lab Server:** Test connection failures, speed caps, and resume logic offline:
  ```bash
  cargo run -p mdm-labserver -- --faults "rate=2M"
  ```

### Android Native (Kotlin & Compose)
- **Requirements:** Java JDK 17, Android SDK (API 26+), Android NDK (r27+).
- **Target Setup:**
  ```bash
  rustup target add aarch64-linux-android x86_64-linux-android
  ```
- **Build Native JNI Libraries (`libmdm_mobile.so`):**
  ```bash
  npm run build:libs
  # or: ./tools/build-android-libs.sh
  ```
- **Compile Debug APK:**
  ```bash
  npm run apk
  # or: ./tools/build-local-apk.sh
  ```
- **Docker Alternative:** If local Android SDK is not configured:
  ```bash
  docker compose up --build
  ```

### iOS Native (Swift & SwiftUI)
- **Requirements:** macOS with Xcode 15+, Apple command line tools.
- **Compile Rust Static Library & C-ABI Headers:**
  ```bash
  ./tools/build-ios-libs.sh
  ```
- Open `ios/MisterDownloadManager/MisterDownloadManager.xcodeproj` in Xcode.

---

## 3. Coding Standards & Architecture

### Rust Code Style
- Format with standard `cargo fmt --all`.
- Lint with `cargo clippy --workspace --all-targets -- -D warnings`.
- Document public APIs and preserve invariant assertions.

### Android Native Style
- Follow **Clean Architecture** patterns established in `android/app/src/main/java/com/misterdownloadmanager/`:
  - `presentation/`: Jetpack Compose UI, Material 3 theme tokens, ViewModels observing Kotlin `StateFlow`.
  - `domain/`: Pure Kotlin data models (`DownloadTask`, `DownloadState`), use cases, and repository interfaces.
  - `data/`: `DownloadRepositoryImpl` delegating to `MdmNativeBridge` over `Dispatchers.IO`.
  - `service/`: Android Foreground Service (`DownloadForegroundService`) with ongoing notification.
- Never manipulate raw JNI pointers in Kotlin. All data transfer across JNI uses JSON snapshot serialization.

### Visual Design & Color Palette
- Adhere to the official design tokens documented in [`.agents/rules/DESIGN_TOKENS_AND_PALETTE.md`](.agents/rules/DESIGN_TOKENS_AND_PALETTE.md).
- Use designated semantic colors for states (Downloading: `#10B981`, Done: `#3B82F6`, Paused: `#F59E0B`, Failed: `#EF4444`).

---

## 4. Translations & Internationalization (`i18n`)

Every string displayed to the user must be localized:

1. **Adding Strings:** Wrap strings in `crate::i18n` calls (`t`, `tc`, `tn`, `fmt`).
2. **Extracting Strings:** Run the catalog extractor after adding strings:
   ```bash
   python tools/i18n/extract.py
   git diff --exit-code i18n/en.tsv
   ```
3. **Translating:** Edit `i18n/<code>.tsv`. Rows marked `#, machine` await human review. Once verified by a native speaker, delete the `#, machine` tag.
4. **Bi-directional Support:** Variables and dynamic text inserted into sentences must use `i18n::ltr` or `i18n::isolate` to ensure proper layout in RTL languages (Arabic, Hebrew, Persian).

---

## 5. Pre-Pull Request Verification Checklist

Before opening a pull request, run the exact verification checks enforced by CI:

```bash
# 1. Check code formatting
cargo fmt --all --check

# 2. Run Clippy linter with zero warnings
cargo clippy --workspace --all-targets -- -D warnings

# 3. Run all workspace unit and integration tests
cargo test --workspace

# 4. Verify translation catalog synchronization
python tools/i18n/extract.py
git diff --exit-code i18n/en.tsv

# 5. Run browser extension unit tests
node tools/extension/test.mjs
```

---

## 6. Pull Request Process

1. Fork the repository and create your feature branch:
   ```bash
   git checkout -b feature/my-new-feature
   ```
2. Commit your changes with clear, descriptive commit messages.
3. Push to your branch and open a Pull Request against the `main` branch.
4. Ensure all GitHub Actions CI checks pass.
5. Address reviewer feedback constructively.

---

## 7. Reporting Issues & Security Vulnerabilities

- **Bug Reports & Enhancements:** Open a ticket using our GitHub Issue templates. Include detailed reproduction steps, logs, and OS details.
- **In-App Reporting:** In the desktop app, navigate to **Help → Report a problem** to automatically attach environment diagnostic data.
- **Security Inquiries:** For sensitive security issues, follow the instructions in [`docs/torrent-security.md`](docs/torrent-security.md) or contact the maintainers directly.

---

## License

By contributing to Mister Download Manager, you agree that your contributions are licensed under the **GNU General Public License v3.0 or later** ([LICENSE](LICENSE)).

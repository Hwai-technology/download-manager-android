# Cross-Platform Mobile Parity & Dual-Target Synchronization Rule

This document defines the strict rule and mandatory synchronization requirements between **Android (Kotlin + Jetpack Compose)** and **iOS (Swift + SwiftUI)** for **Mister Download Manager (MDM)**.

---

## 1. Absolute Rule: Dual Mobile Parity (مزامنة التطوير المتبادل)
Whenever any feature, architectural enhancement, security fix, UI component, URL validation, or file-handling logic is added or modified in the Android codebase (`android/`), **THE EXACT SAME CAPABILITY MUST BE SIMULTANEOUSLY REFLECTED IN THE iOS CODEBASE (`ios/`)**, and vice-versa.

Neither platform may be left behind as a second-class citizen.

---

## 2. Core Synchronization Domains

### A. URL Validation & Smart Detection
- **YouTube & Streaming Playlists:**
  Both Android (`AddDownloadDialog.kt`) and iOS (`AddDownloadSheet` in `DownloadListView.swift`) MUST detect non-direct media pages such as YouTube playlists (`youtube.com/playlist`, `list=`, `youtu.be`) and display warning feedback explaining that direct files or desktop extraction are required.
- **Filename Sanitization & Path Traversal:**
  Both platforms must sanitize user-supplied filenames before passing to the Rust mobile engine, preventing directory traversal tokens (`../`) and illegal filesystem characters.

### B. File & Folder Navigation (Open Action)
- Both platforms must provide an option when a download completes to:
  1. Open the file directly with the system viewer (`QuickLook` / `UIDocumentInteractionController` on iOS, `FileProvider` / `Intent.ACTION_VIEW` on Android).
  2. Reveal / open the parent directory/folder in the native file browser (`Files.app` / `sharedDocuments` on iOS, `Storage Access Framework / File Manager` on Android).

### C. Design Tokens & Visual Palette
- Both platforms MUST implement the official design palette defined in `.agents/rules/DESIGN_TOKENS_AND_PALETTE.md`:
  - **Electric Blue** (`#2563EB` / `#3B82F6`)
  - **Vivid Emerald** (`#10B981` / `#22C55E`)
  - **Speed Cyan** (`#06B6D4` / `#0284C7`)
  - **Royal Indigo** (`#6366F1` / `#4F46E5`)
  - **Midnight Slate** (`#0A0F1D` / `#131B2E`)
- Dynamic gradients (`DownloadAccelerationGradient`, `OrbitStreamGradient`) and semantic state colors MUST match between Jetpack Compose and SwiftUI.

### D. About & License Transparency
- Both platforms must expose:
  1. The official project repository link: `https://github.com/Hwai-technology/download-manager-android`.
  2. The full open-source license text (**GNU General Public License v3.0**).
  3. Consistent version reporting synchronizing with Rust `mdm-mobile` engine versions.

---

## 3. Checklist for Any Mobile PR or Code Edit
Before concluding any task affecting mobile functionality:
- [ ] Has Android logic been checked and verified?
- [ ] Has iOS counterpart (`ios/MisterDownloadManager/`) been updated with identical logic/views?
- [ ] Are color tokens and state titles unified?
- [ ] Have both build/check commands (`./scripts/build-android-apk.sh` and `./scripts/build-ios-libs.sh`) been run or validated?

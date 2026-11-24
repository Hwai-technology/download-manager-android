# Android / Rust-First Engineering Rules

## 1. Product Scope and Platform Commitment

- The product target is **Android only**.
- All architecture, implementation, testing, and release decisions must serve the Android platform.
- iOS is explicitly out of scope.
- React Native, Flutter, and cross-platform UI frameworks must not be introduced.
- Do not add desktop application scope or desktop-specific product requirements.
- Do not introduce additional platforms, frameworks, or technology stacks unless explicitly approved.
- Android compatibility must be established through actual build and runtime verification, not assumptions or documentation claims.

## 2. Rust-First Architecture 
  
- Rust is the primary implementation language for reusable application logic and the download engine.
- Download orchestration, HTTP/HTTPS transfer logic, segmentation, scheduling, retry policies, download state management, integrity verification, and other reusable engine responsibilities should remain in Rust whenever the existing architecture supports them.
- Preserve and reuse existing upstream Rust implementations before considering replacements.
- Do not rewrite existing Rust functionality in Kotlin merely to simplify integration.
- Do not duplicate business logic across Rust and Kotlin.
- Kotlin must not become a second implementation of the download engine.
- Keep platform-independent logic independent of Android framework APIs.
- Introduce new Rust modules or crates only when their responsibility and architectural boundary are justified by the existing repository.

## 3. Kotlin Responsibilities

Kotlin is the Android integration layer, not the primary application logic layer.

Kotlin may be used when Android framework integration requires it, including:

- Android application entry points.
- Activity and application lifecycle integration.
- Foreground Service implementation.
- Android notifications and notification channels.
- Runtime permissions.
- Android storage APIs and Storage Access Framework.
- Android-specific network and connectivity callbacks.
- Android system integrations that cannot appropriately be implemented in portable Rust.
- JNI or another verified Rust-to-Android integration mechanism.
- Android-specific UI and presentation.

Rules:

- Keep Kotlin code minimal and focused on Android-specific responsibilities.
- Do not move reusable download logic into Kotlin.
- Do not introduce unnecessary Kotlin abstraction layers.
- Do not create Kotlin wrappers that duplicate Rust functionality.
- Keep the Rust/Kotlin boundary explicit, documented, and as small as reasonably possible.
- Every cross-language interface must have clearly defined ownership, data types, error handling, threading behavior, and lifecycle expectations.
- Do not invent JNI interfaces, FFI contracts, DTOs, or callback structures. Inspect the actual implementation and document verified contracts before extending them.

## 4. Android Background Execution and Lifecycle

A Rust thread is not a substitute for Android background-execution mechanisms.

The existence of a running Rust thread does not guarantee that a download will continue after the application moves to the background or its process is terminated.

Before implementing background downloads, investigate and document:

- Android application lifecycle.
- Activity lifecycle and configuration changes.
- Foreground Service requirements.
- Foreground Service types and applicable platform restrictions.
- Notification channels and persistent service notifications.
- Runtime notification permissions.
- Service startup and shutdown behavior.
- Process death and process recreation.
- Application force-stop behavior.
- Device reboot behavior.
- Doze mode and battery optimization restrictions.
- Background execution limits.
- Task removal and application removal from recent tasks.
- WorkManager or other Android scheduling mechanisms, if relevant.
- Recovery of active and interrupted downloads.
- Persistent download state and consistency after unexpected termination.

Requirements:

- Select the appropriate Android execution mechanism based on verified platform requirements.
- Do not assume that WorkManager can replace a Foreground Service for continuous user-visible downloads.
- Do not assume that a Foreground Service guarantees uninterrupted execution under every system condition.
- Separate the download engine lifecycle from the Android component lifecycle.
- Ensure that the engine can recover from process recreation using persisted state where the existing architecture permits it.
- Define expected behavior for interrupted, paused, failed, and recoverable downloads.
- Never claim guaranteed background execution without defining and testing the relevant operating-system conditions.

## 5. Rust / Android Integration

Before selecting or implementing an integration mechanism, investigate:

- Existing Cargo workspace structure.
- Existing crate boundaries.
- Existing public Rust APIs.
- Native library requirements.
- Current dependency compatibility.
- JNI, UniFFI, or other integration options, where applicable.
- Android NDK and Rust target support.
- Threading and asynchronous runtime requirements.
- Memory ownership across the language boundary.
- Error propagation.
- Callback and event delivery.
- Cancellation and shutdown behavior.
- Native library loading and packaging.
- Android application startup and initialization order.

Requirements:

- Prefer the smallest integration boundary that satisfies the product requirements.
- Preserve existing Rust APIs where practical.
- Do not introduce a new FFI framework without investigating existing project capabilities and integration costs.
- Do not expose internal Rust implementation details unnecessarily to Kotlin.
- Avoid blocking Android's main thread with Rust operations.
- Define how asynchronous Rust events reach Android components.
- Define how Android lifecycle events reach the Rust engine.
- Ensure that callbacks cannot access destroyed Android components.
- Ensure that shutdown and cancellation do not leave unmanaged threads or resources.
- Do not assume that desktop Rust binaries or libraries can be used directly on Android.

## 6. Storage and Download Persistence

Investigate Android storage restrictions before designing file-management behavior.

Consider:

- App-specific internal storage.
- App-specific external storage.
- Shared storage.
- Storage Access Framework.
- Content URIs.
- Scoped storage.
- Runtime permissions where applicable.
- Temporary files and partial downloads.
- Resume metadata.
- Atomic file operations.
- File rename and finalization behavior.
- Available storage space.
- File access after process recreation.
- Behavior when the user changes or removes the destination.

Requirements:

- Preserve download integrity during interruption and recovery.
- Do not assume that a filesystem path is equivalent to a content URI.
- Do not assume that every Android storage location supports the same operations as a desktop filesystem.
- Do not expose desktop-only filesystem assumptions to the Rust engine without a verified abstraction.
- Define which layer owns file access and storage-specific behavior.
- Avoid duplicating persistence responsibilities between Rust and Kotlin.
- Verify partial-file recovery and finalization on actual Android storage implementations.

## 7. Networking and Connectivity

Investigate the differences between desktop and Android networking environments.

Required investigation areas:

- HTTP and HTTPS support.
- TLS implementation and certificate validation.
- Android trust store behavior.
- Proxy configuration where relevant.
- VPN and network transitions.
- Wi-Fi and mobile data transitions.
- Connectivity callbacks.
- Network restrictions and metered connections.
- DNS behavior.
- Socket lifecycle.
- Connection timeouts.
- Interrupted connections.
- Retry and resume behavior.
- IPv4 and IPv6 compatibility.
- Native TLS and networking dependencies.
- Android ABI and native-library compatibility.

Requirements:

- Preserve secure TLS certificate validation.
- Never disable certificate verification as a workaround.
- Do not assume that a successful desktop network request proves Android compatibility.
- Define the expected behavior when connectivity is lost or changes during a download.
- Avoid unnecessary network polling when Android connectivity callbacks or existing engine mechanisms are more appropriate.
- Verify network behavior on representative Android devices or emulators.

## 8. Native Dependencies and ABI Compatibility

Every native dependency must be evaluated for Android compatibility.

Investigate:

- Rust target triples.
- Android NDK compatibility.
- Native C/C++ dependencies.
- System libraries.
- Build scripts.
- Linker requirements.
- Dynamic library loading.
- Required native symbols.
- Minimum Android API requirements.
- ABI-specific limitations.

Required ABI support must be determined from actual product requirements and build evidence.

Do not assume that a dependency supports Android simply because it supports Linux, Windows, or macOS.

Do not silently remove functionality to make a build succeed.

When a dependency is unsupported:

1. Identify the exact dependency and affected functionality.
2. Determine whether an existing Android-compatible alternative exists.
3. Assess whether the functionality is essential.
4. Evaluate the impact of replacing or isolating it.
5. Document the trade-offs.
6. Request architectural approval when the decision changes product behavior or upstream engine responsibilities.

## 9. Desktop-Specific Code and UI Isolation

The upstream repository may contain desktop-oriented code, UI components, operating-system integrations, and platform-specific APIs.

Requirements:

- Identify desktop-only dependencies before including crates in the Android build.
- Separate reusable engine functionality from desktop application functionality.
- Preserve the upstream implementation whenever possible.
- Do not force desktop UI frameworks into the Android application.
- Do not assume that egui, eframe, or any other desktop-oriented UI framework is Android-ready without a dedicated proof of concept.
- Do not import desktop-specific OS APIs into Android-facing modules.
- Use existing conditional compilation and feature boundaries where appropriate.
- Do not introduce broad conditional compilation changes without understanding their effect on other targets.
- Do not remove upstream desktop functionality merely because it is unnecessary for Android.
- Keep Android-specific integration isolated from platform-independent engine logic.

## 10. Build and Verification Requirements

Android compatibility must be demonstrated through reproducible technical evidence.

Before claiming that a component or the application supports Android, verify the relevant stages:

1. Rust dependencies resolve for the selected Android targets.
2. Required Rust crates compile for those targets.
3. Native dependencies compile and link successfully.
4. Required native libraries are packaged correctly.
5. Android application integration builds successfully.
6. The application installs on a supported emulator or physical device.
7. Rust initialization succeeds at runtime.
8. The Rust/Kotlin integration works correctly.
9. Download execution works on Android.
10. Storage operations work on the intended storage destinations.
11. Notifications and background execution behave correctly.
12. Interrupted downloads recover according to the defined requirements.

Requirements:

- Record exact build commands and target configurations.
- Record tested Android API levels and ABIs.
- Distinguish compilation success from runtime compatibility.
- Distinguish emulator verification from physical-device verification.
- Record untested scenarios explicitly.
- Do not report a successful build as proof that all Android functionality works.
- Do not claim full Android support while essential integration or lifecycle tests remain unverified.

## 11. Testing Strategy

Testing must cover both reusable Rust behavior and Android-specific integration.

### Rust Tests

Where applicable, test:

- Download planning.
- Segmentation.
- Worker coordination.
- Retry behavior.
- Pause and resume.
- Cancellation.
- State persistence.
- Error propagation.
- Integrity verification.
- Concurrent downloads.
- Resource cleanup.

Preserve existing tests and test contracts.

### Android Integration Tests

Where applicable, test:

- Application startup.
- Rust initialization.
- Kotlin/Rust communication.
- Foreground Service startup and shutdown.
- Notification behavior.
- Permission handling.
- Storage access.
- Network transitions.
- Activity recreation.
- Process death and recovery.
- Interrupted downloads.
- Device reboot recovery where supported.
- Native library loading across supported ABIs.

### Verification Rules

- Do not replace meaningful tests with superficial smoke tests.
- Do not silently weaken existing assertions.
- Do not claim a scenario is verified unless the test was actually executed.
- Document unavailable test environments and remaining verification gaps.

## 12. Upstream Preservation and Change Control

The existing upstream Rust project is a reusable foundation, not a codebase to rewrite without justification.

Requirements:

- Inspect existing implementation before proposing replacements.
- Preserve upstream behavior unless a verified Android requirement makes a change necessary.
- Prefer adapters, isolated platform-specific modules, and narrowly scoped integration changes.
- Avoid unrelated refactoring.
- Avoid unnecessary changes to existing desktop functionality.
- Identify all affected crates and dependencies before modifying shared engine code.
- Evaluate regression risks for existing engine behavior.
- Keep changes minimal, reviewable, and attributable to a specific requirement.

Any change that substantially modifies engine architecture, public APIs, persistence contracts, or download behavior requires an explicit technical justification and impact assessment.

## 13. Mandatory Investigation Before Implementation

Before implementing an Android or Rust integration change:

1. Inspect the actual repository version and current Git state.
2. Identify the relevant files, crates, modules, and dependencies.
3. Trace the existing behavior and its callers.
4. Verify existing public APIs and integration boundaries.
5. Identify Android-specific constraints.
6. Identify affected lifecycle, storage, networking, and native-library responsibilities.
7. Evaluate alternatives using evidence from the repository and platform requirements.
8. Document risks, dependencies, and expected behavior.
9. Present an implementation plan before making changes when the task requires architectural or multi-file modifications.
10. Implement only the approved scope.
11. Run the relevant builds and tests.
12. Report verified results and unresolved issues.

If required implementation details cannot be verified, stop and report the missing evidence. Do not invent an implementation contract.

## 14. Mandatory Technical Reporting

Every significant Android/Rust engineering task must distinguish:

- **Verified:** Directly confirmed by source code, build output, or executed tests.
- **Inferred:** A reasonable technical conclusion that still needs verification.
- **Unknown:** Information that cannot currently be established.
- **Blocked:** A missing dependency, unsupported API, unavailable environment, or unresolved architectural decision.

Reports must include, where relevant:

- Files and crates inspected.
- Files changed.
- Existing behavior identified.
- Android-specific implications.
- Dependencies and integration boundaries.
- Build and test commands executed.
- Actual results.
- Untested scenarios.
- Remaining risks.
- Decisions requiring approval.

Never present assumptions as verified implementation facts.

## 15. Final Architectural Principle

The intended architecture is:

- **Rust:** Reusable download engine and platform-independent application logic.
- **Kotlin:** Minimal Android framework integration and Android-specific presentation.
- **Android Framework:** Lifecycle, services, notifications, permissions, storage access, and platform facilities.
- **Integration Boundary:** Explicit, minimal, documented, and verified.

The objective is not to rewrite the upstream project as a native Android application.

The objective is to reuse its proven Rust capabilities and integrate them into a reliable Android product while preserving architectural integrity, download correctness, security, maintainability, and verifiable platform compatibility.

**No Android compatibility claim without evidence. No unnecessary Kotlin duplication. No desktop assumptions. No unverified architectural changes.**
# Android Target Structure — Proposed, Not Yet Implemented

## 1. Document Status and Purpose

**Status: PROPOSED — NOT IMPLEMENTED — NOT YET ARCHITECTURALLY APPROVED**

This document describes the intended responsibility boundaries for the downstream Android application built around the existing upstream Rust project.

It is an architectural direction, not an instruction to create every described module, crate, package, or directory immediately.

The actual checked-out repository remains the source of truth.

The final architecture must be determined through repository investigation, Android compatibility analysis, build verification, and working proofs of concept.

### Core Product Decisions

- Product target: Android only.
- Primary implementation language: Rust.
- Kotlin: minimal Android framework integration.
- Existing upstream Rust engine: reuse and preserve wherever practical.
- UI technology: unresolved until a real Android proof of concept demonstrates suitability.
- iOS, React Native, and Flutter: out of scope.

## 2. Architectural Objectives

The downstream Android application should:

1. Reuse verified portable functionality from the upstream Rust project.
2. Preserve existing upstream crate boundaries and dependency direction.
3. Keep the download engine independent of Android framework and UI code.
4. Isolate Android-specific functionality behind justified integration boundaries.
5. Minimize duplicated business and download logic.
6. Keep the Rust/Kotlin integration surface explicit and small.
7. Support Android lifecycle and background-execution requirements.
8. Preserve download correctness across interruption and recovery.
9. Avoid unnecessary architectural layers and dependencies.
10. Remain maintainable and compatible with future upstream improvements.

These objectives must be balanced against actual implementation constraints. They do not authorize speculative restructuring.

## 3. Target Responsibility Layers

The following layers describe intended responsibilities. They do not prescribe exact directory names, crate names, package names, or module paths.

### 3.1 Existing Upstream Rust Crates

**Status: Existing upstream components — individual Android suitability must be verified.**

Responsibilities:

- Provide existing reusable download functionality.
- Preserve platform-independent algorithms and business logic.
- Maintain their existing crate boundaries and dependency direction.
- Expose existing public APIs where appropriate.
- Preserve established error handling, state management, and event behavior.

Requirements:

- Inspect each crate before deciding whether the Android application needs it.
- Verify Android target compatibility rather than assuming it.
- Identify desktop-specific dependencies and OS assumptions.
- Reuse existing functionality instead of implementing parallel download logic.
- Avoid modifying upstream crates unless a concrete Android integration requirement justifies the change.
- Do not assume that every upstream crate belongs in the Android build.
- Do not move existing crates merely to distinguish upstream and downstream code.

The exact set of crates required by Android remains unknown until dependency and build analysis is complete.

### 3.2 Android Application Module

**Status: Proposed downstream application responsibility.**

The Android application module is responsible for Android application packaging and framework integration.

Expected responsibilities may include:

- Android application packaging.
- Android manifest.
- Application resources.
- Application initialization.
- Minimal Kotlin entry points.
- Android-specific component registration.
- Integration with the selected Rust native library.
- Application-level configuration.
- Android-specific presentation integration.

Requirements:

- Use the actual Android build system and configuration established by the implementation.
- Keep reusable download logic out of the Android application module.
- Avoid turning the application module into a second download engine.
- Do not introduce unnecessary application-level abstraction layers.
- Do not duplicate responsibilities already owned by existing Rust components.
- Keep application packaging concerns separate from engine responsibilities.

The exact Gradle module layout, package names, and source directories must be determined after the Android build approach is verified.

### 3.3 Android Platform Adapter

**Status: Proposed responsibility boundary.**

This layer isolates Android framework functionality from the reusable Rust engine.

Its responsibilities may include:

#### Background Execution

- Foreground Service integration.
- Android service lifecycle.
- Starting and stopping background execution.
- Communication between the Android lifecycle and the Rust engine.
- Handling process recreation.
- Coordinating recovery of interrupted downloads.

#### Notifications

- Notification channel management.
- Foreground Service notifications.
- Download progress notifications.
- Completion and failure notifications.
- Notification permission handling.
- User actions associated with notifications.

#### Permissions

- Runtime permission handling.
- Permission state checks.
- Permission denial and revocation behavior.
- Android API-level-specific permission requirements.

#### Storage Integration

- Android storage access.
- Storage Access Framework integration where required.
- Content URI handling.
- Scoped storage behavior.
- Destination selection.
- File access and finalization.
- Storage-related failure handling.

#### Lifecycle Integration

- Application and service lifecycle callbacks.
- Communication of lifecycle events to the appropriate Rust integration boundary.
- Initialization and shutdown coordination.
- Safe handling of destroyed Android components.
- Process recreation and recovery coordination.

#### Secure Platform Facilities

- Android Keystore integration where required.
- Secure credential storage.
- Platform-specific security facilities.
- Android-specific trust and networking configuration when necessary.

Requirements:

- Do not allow Android framework dependencies to leak into the portable download engine.
- Do not create a separate adapter for every API unless the responsibility requires it.
- Do not duplicate existing Rust functionality.
- Keep platform-specific implementation details inside the appropriate Android integration boundary.
- Do not assume that every responsibility requires its own class, package, module, or service.
- Use actual Android platform capabilities and supported APIs.

The final implementation may combine several responsibilities when doing so preserves clear ownership and avoids unnecessary indirection.

### 3.4 Rust Application Adapter

**Status: OPTIONAL — ONLY IF JUSTIFIED BY VERIFIED INTEGRATION NEEDS.**

A separate Rust application adapter may be appropriate if the Android application requires an orchestration boundary between the existing engine and Android presentation.

Potential responsibilities:

- Translate application-level commands into existing engine operations.
- Coordinate engine initialization and shutdown.
- Expose a controlled integration API to Kotlin.
- Translate existing engine events into integration-safe events.
- Coordinate cancellation and lifecycle-related operations.
- Provide a stable boundary between the engine and Android integration.

Requirements:

- Do not create this layer automatically.
- First inspect whether existing upstream APIs already provide the required functionality.
- Do not duplicate engine scheduling, transfer, retry, segmentation, or persistence logic.
- Do not create another state-management system that conflicts with the engine.
- Do not introduce a new crate merely to hold thin forwarding functions.
- Do not expose internal engine details unnecessarily.
- Keep the integration API minimal and explicit.
- Define ownership, threading, error propagation, and callback lifecycle.
- Verify the need through an actual integration proof of concept.

If existing engine APIs are sufficient, use them directly through the smallest appropriate integration boundary.

A separate adapter is justified only when it solves a demonstrated responsibility or coupling problem.

### 3.5 User Interface

**Status: UNDECIDED — REQUIRES A REAL PROOF OF CONCEPT.**

The UI technology must not be selected solely because the upstream project already contains a desktop UI or because a particular framework is familiar.

Requirements:

- Evaluate UI options against Android-specific requirements.
- Do not assume that desktop egui/eframe can be embedded unchanged.
- Do not force desktop rendering or window-management assumptions into Android.
- Do not duplicate download state or business logic inside the UI.
- Keep presentation separate from engine implementation.
- Ensure that the UI can observe download state and events without owning the engine's internal state.
- Verify lifecycle behavior and UI recreation.
- Verify progress updates, cancellation, pause, resume, and error presentation through a working prototype.

The final UI framework and implementation structure remain unresolved until the proof of concept is completed and an explicit decision is made.

## 4. Responsibility Ownership

The following table defines intended ownership, not verified implementation.

| Responsibility | Intended owner |
|---|---|
| Download algorithms | Existing Rust engine |
| Segmentation and workers | Existing Rust engine, if supported by its implementation |
| Download scheduling | Existing Rust engine, subject to source verification |
| Download state and persistence | Existing engine mechanisms where appropriate |
| Android application packaging | Android application module |
| Android manifest and resources | Android application module |
| Foreground Service | Android platform integration |
| Notifications | Android platform integration |
| Runtime permissions | Android platform integration |
| Android storage access | Android platform integration |
| Android lifecycle callbacks | Android platform integration |
| Secure Android storage | Android platform integration where required |
| Rust engine orchestration | Existing APIs or optional Rust application adapter |
| User presentation | UI implementation, technology not yet selected |

If existing upstream responsibilities differ from this table, investigate and document the difference before changing ownership.

## 5. Integration Boundaries

### 5.1 Rust Engine Boundary

The engine must remain independent of Android UI and framework implementation.

Android integration should interact with the engine through verified APIs and explicitly defined contracts.

Do not introduce Android dependencies into reusable engine crates merely to simplify application integration.

### 5.2 Rust/Kotlin Boundary

The Rust/Kotlin boundary must be deliberately designed and verified.

Investigate:

- Available native library integration options.
- Existing Rust APIs.
- JNI, UniFFI, or other possible bridge mechanisms.
- Data ownership.
- Memory lifetime.
- Threading and asynchronous execution.
- Error propagation.
- Event delivery.
- Cancellation.
- Initialization and shutdown.
- Native library loading.

The final bridge technology is not predetermined by this document.

Choose it only after evaluating compatibility, implementation complexity, maintenance, and actual Android build results.

### 5.3 Android Lifecycle Boundary

Android lifecycle events must not be implemented as assumptions inside the portable engine.

Define how the Android integration layer coordinates:

- Service startup.
- Service shutdown.
- Background execution.
- Process death.
- Process recreation.
- Engine initialization.
- Download recovery.
- Cancellation and cleanup.

A Rust thread alone is not a complete Android background-execution architecture.

### 5.4 Storage Boundary

Define which layer owns Android-specific storage access and which layer owns download-state persistence.

Do not assume that desktop filesystem paths and Android content URIs are interchangeable.

Preserve existing engine persistence behavior where possible, and introduce a storage adapter only when a demonstrated incompatibility requires it.

## 6. Required Proof Before Finalizing the Structure

The target architecture must not be finalized based only on documentation, diagrams, or theoretical compatibility.

The following proofs are mandatory.

### Proof 1 — Engine Android Build

- Identify the exact Rust crates required by the Android application.
- Identify the intended Android target triples and ABIs.
- Resolve dependencies for those targets.
- Build the required engine functionality.
- Record failures and unsupported dependencies.
- Distinguish successful compilation from full application compatibility.

**Acceptance evidence:**

- Exact target configuration.
- Reproducible build commands.
- Successful build output.
- Identified limitations.

### Proof 2 — Dependency and OS Compatibility

Inspect the required engine dependency graph for:

- Desktop-only APIs.
- Unsupported system libraries.
- Native dependencies.
- Build scripts.
- Platform-specific assumptions.
- Filesystem assumptions.
- Networking and TLS dependencies.
- Threading and asynchronous runtime requirements.

**Acceptance evidence:**

- Dependency compatibility assessment.
- List of incompatible or unresolved components.
- Proposed handling for each blocker.
- Verified impact on upstream functionality.

### Proof 3 — Rust Library Loading and Bridge Strategy

Demonstrate that the Android application can:

- Package the required native Rust library.
- Load the library on the selected Android target.
- Initialize the Rust integration.
- Invoke the required engine operations.
- Receive relevant results and events.
- Propagate errors safely.
- Handle cancellation and shutdown.
- Avoid blocking the Android main thread.

**Acceptance evidence:**

- Working Android integration prototype.
- Verified bridge mechanism.
- Tested Rust/Kotlin communication.
- Documented ownership and lifecycle behavior.

### Proof 4 — Foreground Execution and Process Recreation

Verify behavior on an Android emulator or physical device.

Test:

- Foreground Service startup.
- Persistent notification behavior.
- Background execution.
- Activity destruction and recreation.
- Service lifecycle transitions.
- Application process termination.
- Process recreation.
- Recovery of interrupted downloads.
- User-initiated cancellation.
- Relevant Android power-management restrictions.

**Acceptance evidence:**

- Tested Android API levels.
- Reproducible test scenarios.
- Actual runtime observations.
- Identified limitations and recovery behavior.

Do not claim guaranteed uninterrupted downloads under every operating-system condition.

### Proof 5 — Storage and Notification Behavior

Verify:

- Intended download destination.
- App-private storage where applicable.
- Shared storage or Storage Access Framework where required.
- Content URI handling.
- Partial-file persistence.
- File finalization.
- Insufficient storage behavior.
- Notification creation and updates.
- Permission denial and revocation.
- Notification behavior across supported Android versions.

**Acceptance evidence:**

- Tested storage destinations.
- Tested Android versions and API levels.
- Verified file integrity.
- Verified notification behavior.
- Documented limitations.

### Proof 6 — UI Proof of Concept

Before selecting a UI framework:

- Build a minimal Android prototype.
- Verify rendering and interaction on Android.
- Verify communication with the Rust integration.
- Verify progress updates.
- Verify pause, resume, and cancellation interactions.
- Verify behavior during Activity recreation.
- Evaluate maintenance and integration costs.

**Acceptance evidence:**

- Working prototype.
- Tested target devices or emulators.
- Documented technical limitations.
- Explicit UI technology decision.

## 7. Architecture Decision Gates

The architecture must progress through explicit decision gates.

### Gate A — Repository Understanding

Required:

- Exact repository revision identified.
- Existing crate structure inspected.
- Relevant APIs and dependencies traced.
- Portable and platform-specific responsibilities identified.

Output:

- Verified repository map.
- Candidate Android integration boundaries.
- Unresolved compatibility questions.

No restructuring is authorized at this gate.

### Gate B — Android Build Feasibility

Required:

- Target selection justified.
- Relevant engine crates identified.
- Dependency compatibility investigated.
- Required native build configuration established.

Output:

- Android build feasibility report.
- Blocker list.
- Minimal build integration proposal.

### Gate C — Rust/Android Integration

Required:

- Native library successfully built and packaged.
- Bridge strategy demonstrated.
- Engine initialization and communication verified.
- Ownership and lifecycle contracts documented.

Output:

- Working integration prototype.
- Verified integration contract.
- Identified remaining risks.

### Gate D — Android Runtime Feasibility

Required:

- Foreground execution tested.
- Process recreation tested.
- Storage behavior tested.
- Notifications and permissions tested.
- Download recovery evaluated.

Output:

- Android runtime verification report.
- Lifecycle and persistence decisions.
- Documented platform limitations.

### Gate E — UI and Final Structure

Required:

- UI proof of concept completed.
- Integration responsibilities established.
- Unnecessary layers removed from the proposal.
- Dependencies and module boundaries justified.

Output:

- Approved target architecture.
- Actual directory and module proposal.
- Implementation roadmap.

Do not skip a gate merely to create the final directory structure earlier.

## 8. Rules Against Premature Structure Creation

- Do not create placeholder directories simply because they appear in this document.
- Do not create empty crates for future responsibilities.
- Do not create an Android adapter before identifying its platform-specific responsibility.
- Do not create a Rust application adapter if existing APIs are sufficient.
- Do not create a separate UI architecture before the UI proof of concept.
- Do not move upstream crates to match the downstream application layout.
- Do not create generic abstractions for hypothetical future requirements.
- Do not introduce multiple layers that only forward calls without adding meaningful ownership or isolation.
- Do not interpret this document as permission for broad repository restructuring.

Every new layer must have a demonstrated responsibility, an identified consumer, a justified dependency boundary, and a verification strategy.

## 9. Unknowns That Must Remain Explicit

Until verified, the following decisions remain open:

- Exact upstream crates required by Android.
- Exact Rust target triples and supported ABIs.
- Android minimum API level.
- Android Gradle module structure.
- Kotlin package and component organization.
- Rust native library packaging approach.
- Rust/Kotlin bridge technology.
- Need for a separate Rust application adapter.
- UI framework and rendering architecture.
- Foreground Service implementation details.
- Storage abstraction and ownership.
- Required Android permissions.
- Android-specific native dependency replacements.
- Final source directory structure.

Do not fill these unknowns with guessed names, APIs, or implementation details.

Each decision must be resolved using source evidence, build results, platform verification, and explicit approval where required.

## 10. Required Architecture Documentation

Before final approval, maintain documentation covering:

- Verified upstream crate map.
- Required Android dependency graph.
- Responsibility ownership.
- Rust/Kotlin integration contracts.
- Android lifecycle and background-execution design.
- Storage and persistence design.
- UI proof-of-concept findings.
- Build and ABI configuration.
- Security boundaries.
- Known limitations.
- Approved architecture decisions.

Documentation must distinguish existing upstream implementation from downstream Android additions.

## 11. Final Architecture Approval Criteria

The Android target structure can be considered ready for implementation only when:

1. The relevant upstream implementation has been inspected.
2. Required engine functionality builds for the intended Android targets.
3. Incompatible dependencies and OS assumptions are understood.
4. The Rust library loads and communicates with Android successfully.
5. Foreground execution and process recreation have been tested.
6. Storage and notification behavior have been verified.
7. The UI direction has a working proof of concept.
8. Each proposed architectural layer has a justified responsibility.
9. Dependency direction and upstream crate boundaries are preserved.
10. Unresolved risks and limitations are documented.
11. The final structure has been explicitly reviewed and approved.

A successful prototype does not automatically establish production readiness.

## 12. Final Architectural Principle

The target structure must emerge from verified implementation requirements, not from a predetermined folder tree.

The intended direction is:

- Reuse the existing portable Rust engine.
- Keep Android framework integration separate.
- Keep Kotlin minimal.
- Introduce Rust application orchestration only when justified.
- Select the UI only after a real Android proof of concept.
- Preserve upstream architecture wherever practical.
- Create only the layers that solve demonstrated problems.

**Proposed structure is not implemented structure.**

**Build evidence before architecture finalization.**

**Working Android proof before compatibility claims.**

**Responsibility before abstraction.**
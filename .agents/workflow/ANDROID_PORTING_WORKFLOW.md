# Android Porting Workflow

## 1. Purpose and Scope

This document defines the mandatory engineering workflow for evaluating, integrating, and delivering the upstream Rust download engine as an Android application.

The objective is to reuse existing upstream functionality while introducing only the Android-specific integration required by verified platform constraints.

The workflow prioritizes:

- Source-backed investigation.
- Preservation of upstream architecture.
- Rust-first implementation.
- Minimal Kotlin framework integration.
- Evidence-driven technical decisions.
- Early validation of Android compatibility.
- Incremental delivery.
- Preservation of existing desktop functionality.

### Product Scope

The target product is Android only.

Do not introduce iOS, React Native, Flutter, or cross-platform mobile requirements unless the product scope is explicitly changed.

### Core Principle

**Prove technical feasibility before committing to architecture.**

Do not begin by creating a large empty directory tree or introducing new crates, modules, adapters, or abstractions before their responsibilities and necessity have been established.

## 2. Mandatory Rules

Every stage must follow these rules:

1. Inspect the checked-out source before relying on documented architecture.
2. Record the current Git revision.
3. Read applicable repository instructions.
4. Verify actual crate names, APIs, dependencies, and contracts.
5. Do not invent missing symbols or implementation details.
6. Preserve existing upstream crate boundaries.
7. Reuse existing engine functionality wherever possible.
8. Keep Android framework responsibilities outside the portable engine.
9. Use Kotlin only where Android framework integration requires it.
10. Avoid broad refactoring and unrelated changes.
11. Record blockers instead of hiding them with speculative workarounds.
12. Do not claim successful Android support without actual build and runtime evidence.
13. Preserve desktop build behavior throughout the port.
14. Make architecture decisions only after the required technical proofs.

### Evidence Classification

Every important finding must be classified as:

- `VERIFIED`
- `DOCUMENTED`
- `INFERRED`
- `UNKNOWN`
- `BLOCKED`
- `PROPOSED`

Build and runtime claims must include actual commands, target configurations, and results.

## 3. Workflow Overview

The porting process consists of eight stages:

1. Inventory.
2. Classification.
3. Engine proof.
4. Rust/Kotlin bridge proof.
5. Android lifecycle proof.
6. Android storage proof.
7. Architecture decision.
8. Incremental delivery.

<escape>Inventory -> Classification -> Engine Proof -> Bridge Proof -> Lifecycle Proof -> Storage Proof -> Architecture Decision -> Incremental Delivery</escape>

The stages are ordered dependencies, not merely a checklist of independent tasks.

A stage may identify additional investigation requirements, but unresolved critical blockers must be addressed before dependent architectural commitments are made.

## 4. Stage 1 — Repository and Dependency Inventory

**Objective:** Establish what the existing repository actually contains and identify potential Android compatibility constraints.

### 4.1 Establish Repository Identity

Inspect:

- Repository root.
- Git remote.
- Current branch.
- Current commit.
- Checked-out tag.
- Working-tree state.
- Selected upstream revision.

Do not assume that the local checkout matches public `main` or a previously intended release.

### 4.2 Enumerate Workspace Crates

Inspect:

- Root `Cargo.toml`.
- Workspace members.
- Workspace exclusions.
- Every relevant crate manifest.
- Package names.
- Library and binary targets.
- Features.
- Optional dependencies.
- Target-specific dependencies.
- Build scripts.

Use actual Cargo metadata to establish workspace membership.

Do not infer workspace membership from directory names.

### 4.3 Dependency Inventory

For each relevant crate, identify:

- Direct dependencies.
- Important transitive dependencies.
- Optional dependencies.
- Development dependencies.
- Build dependencies.
- Target-specific dependencies.
- Native libraries.
- System libraries.
- External executable dependencies.
- Feature-gated functionality.

Identify dependencies that may:

- Lack Android support.
- Require unavailable system libraries.
- Require desktop operating-system APIs.
- Depend on unsupported native build configurations.
- Introduce ABI compatibility requirements.
- Require external executables.
- Depend on desktop windowing or UI frameworks.

Do not classify a dependency as incompatible without evidence.

### 4.4 Operating-System and Runtime Inventory

Inspect actual source usage of:

- Operating-system APIs.
- Filesystem operations.
- Process creation.
- Subprocess management.
- Environment variables.
- Signals.
- Threading.
- Async runtimes.
- Network interfaces.
- Native libraries.
- System notifications.
- Desktop windowing.
- Platform-specific storage.

Identify assumptions about:

- Writable filesystem locations.
- Absolute paths.
- File permissions.
- File locking.
- Temporary directories.
- Process lifetime.
- Long-running execution.
- External tools.
- Desktop application lifecycle.

### 4.5 UI and Application Entry Points

Identify:

- Actual executable entry points.
- Desktop UI initialization.
- Application orchestration.
- Engine initialization.
- Configuration loading.
- Event handling.
- Persistence initialization.
- IPC initialization.
- Shutdown behavior.

Determine which responsibilities belong to the existing application and which belong to the engine.

Do not assume that the desktop application's initialization flow can be reused on Android.

### 4.6 Inventory Deliverables

Produce:

1. Verified workspace inventory.
2. Dependency inventory.
3. Native dependency inventory.
4. OS API usage inventory.
5. Filesystem assumptions inventory.
6. Subprocess usage inventory.
7. UI and application entry-point map.
8. Initial Android compatibility risks.

### Stage 1 Exit Criteria

<escape>- [ ] Repository revision is verified.
- [ ] Relevant workspace crates are enumerated.
- [ ] Relevant manifests are inspected.
- [ ] Dependencies and features are identified.
- [ ] Native and system dependencies are identified.
- [ ] OS and filesystem assumptions are documented.
- [ ] Application and UI entry points are traced.
- [ ] Important unknowns and blockers are recorded.</escape>

Do not proceed using an assumed repository structure.

## 5. Stage 2 — Component Portability Classification

**Objective:** Classify each relevant component according to its actual implementation and Android requirements.

### 5.1 Classification Categories

#### PORTABLE

The component:

- Does not depend on incompatible desktop-only APIs.
- Has compatible dependencies.
- Uses supported target capabilities.
- Has a viable Android build configuration.
- Does not require unverified platform workarounds.

This classification still requires actual target build evidence before claiming Android build support.

#### CONDITIONALLY PORTABLE

The component may be reusable after specific changes or integration work.

Examples of potential conditions:

- Platform-specific dependency replacement.
- Android-compatible filesystem integration.
- Native library configuration.
- Feature selection.
- Narrow platform adapters.

Every condition must be supported by an actual finding.

#### DESKTOP-ONLY

The component is inherently tied to desktop-specific behavior or dependencies and is not part of the Android target in its current form.

Examples may include desktop windowing or desktop-only executable integration.

The classification must be supported by source evidence.

#### UNKNOWN

Available evidence is insufficient to establish compatibility.

Do not classify unknown components as portable merely because they are written in Rust.

### 5.2 Classification Matrix

Maintain a matrix similar to:

| Component | Classification | Evidence | Required action |
|---|---|---|---|
| Verified engine crate | Unknown until audited | Source and manifest references | Android build proof |
| Desktop UI | Unknown until audited | UI dependencies and entry points | UI compatibility investigation |
| CLI | Unknown until audited | Binary and runtime dependencies | Determine Android relevance |
| IPC | Unknown until audited | Transport and platform APIs | Integration assessment |
| Browser native host | Unknown until audited | Native messaging and subprocess usage | Determine Android relevance |
| Remote control | Unknown until audited | Server implementation and dependencies | Security and compatibility review |
| Supporting crates | Unknown until audited | Individual source and manifests | Requirement-based assessment |

This table is a template, not a claim about the selected release.

### 5.3 Classification Rules

- Classify components individually.
- Do not classify an entire workspace based on the engine alone.
- Do not infer compatibility from the programming language.
- Do not treat successful compilation of one feature configuration as proof that every feature works.
- Distinguish build compatibility from runtime compatibility.
- Identify the exact dependency or API causing a blocker.
- Record whether a workaround changes existing upstream behavior.

### Stage 2 Exit Criteria

<escape>- [ ] Every relevant component has a classification.
- [ ] Each classification includes source evidence.
- [ ] Conditional portability requirements are explicit.
- [ ] Desktop-only components are identified.
- [ ] Unknowns remain explicit.
- [ ] Android product requirements are separated from upstream feature availability.</escape>

## 6. Stage 3 — Engine Android Build Proof

**Objective:** Determine whether the existing download engine can compile for Android without redesigning it prematurely.

This stage validates the engine before application integration.

### 6.1 Select the Actual Engine Crate

Verify:

- Actual engine crate path.
- Actual Cargo package name.
- Library target.
- Required features.
- Dependency configuration.
- Target-specific dependencies.
- Build scripts.

Do not assume the documented crate name or path is correct for the checked-out revision.

### 6.2 Verify the Android Build Environment

Inspect:

- Rust toolchain version.
- Installed Rust targets.
- Android NDK.
- Android SDK.
- Linker configuration.
- Cargo target configuration.
- Native build tools.
- Required ABI targets.

The ABI and minimum Android API level must be selected based on actual product requirements and dependency support.

Do not assume that one successful ABI build covers all intended Android devices.

### 6.3 Attempt Target Compilation

Use the actual verified package name and selected target.

A command template is:

```bash
cargo check -p VERIFIED_ENGINE_PACKAGE --target VERIFIED_ANDROID_TARGET
```

Replace placeholders only after inspecting the actual package and target configuration.

Use `cargo build` when a linkable artifact is required.

Inspect the actual compiler and linker output.

### 6.4 Resolve Only Evidenced Blockers

For every failure:

1. Capture the complete error.
2. Identify the failing crate or build step.
3. Locate the responsible dependency or source implementation.
4. Verify the target-specific cause.
5. Determine whether the problem is configuration, dependency compatibility, native linking, or source incompatibility.
6. Evaluate the smallest safe correction.
7. Assess desktop regression risk.
8. Implement only an approved correction.
9. Re-run the relevant target build.

Do not:

- Disable features randomly.
- Remove dependencies without tracing their usage.
- Add speculative `cfg` conditions.
- Rewrite the engine before locating the actual blocker.
- Replace existing functionality with a new implementation merely to obtain a successful build.

### 6.5 Engine Build Verification

Record:

- Rust target.
- Android ABI.
- NDK version.
- SDK/API configuration.
- Cargo command.
- Enabled features.
- Build result.
- Warnings.
- Failures.
- Required modifications.
- Remaining blockers.

### Stage 3 Exit Criteria

<escape>- [ ] The actual engine package is verified.
- [ ] Android target configuration is documented.
- [ ] Required dependencies are inspected.
- [ ] Engine target compilation is attempted.
- [ ] Failures are traced to evidenced causes.
- [ ] Required changes are narrowly scoped.
- [ ] Desktop build behavior is checked.
- [ ] Build evidence is recorded.</escape>

A successful engine build proves compilation for that target and configuration only. It does not prove runtime behavior or Android application compatibility.

## 7. Stage 4 — Rust/Kotlin Bridge Proof

**Objective:** Validate communication between Rust and Kotlin before committing to an integration architecture.

### 7.1 Bridge Selection Must Follow Investigation

Do not choose JNI, UniFFI, or another bridge solely because it is familiar or popular.

Evaluate candidate approaches against:

- Existing Rust dependency compatibility.
- Android support.
- Build integration complexity.
- API ergonomics.
- Memory ownership.
- Error propagation.
- Async execution.
- Threading behavior.
- Cancellation.
- Data conversion.
- Maintenance.
- Debugging.
- ABI compatibility.
- Long-term project requirements.

The chosen bridge must be justified by actual requirements and a working prototype.

### 7.2 Prototype Scope

Create the smallest isolated or throwaway prototype necessary to validate:

1. Android can load the compiled Rust library.
2. Kotlin can invoke a Rust operation.
3. Rust can return a result.
4. Rust errors can be represented safely.
5. Strings and structured data cross the boundary correctly.
6. Memory ownership is clear.
7. Threading assumptions are valid.
8. Async execution can be coordinated.
9. Cancellation can be represented where required.
10. Native library packaging works for the selected ABI.

The prototype must not become a permanent architecture layer by default.

### 7.3 Ownership and Lifecycle

Verify:

- Which side owns native objects.
- Whether references can outlive their valid scope.
- How Rust resources are released.
- How Kotlin handles native errors.
- How concurrent calls are handled.
- How callback lifetimes are managed.
- How application shutdown affects native operations.
- Whether native calls can block Android-sensitive threads.

Do not expose internal engine implementation types directly without a demonstrated need.

### 7.4 API Boundary

Define only the minimal interface needed to prove integration.

Prefer stable application-level operations over exposing worker, planner, and internal state implementation details.

The final bridge API must be based on actual engine capabilities.

Do not invent engine methods to fit a prototype.

### Stage 4 Exit Criteria

<escape>- [ ] Bridge candidates are compared.
- [ ] Selection is justified.
- [ ] Rust library loading is verified.
- [ ] Kotlin-to-Rust invocation is verified.
- [ ] Return values and error propagation are verified.
- [ ] Ownership and resource cleanup are verified.
- [ ] Threading and async behavior are investigated.
- [ ] ABI packaging is verified.
- [ ] Prototype limitations are documented.</escape>

## 8. Stage 5 — Android Lifecycle Proof

**Objective:** Verify that downloads can operate correctly within Android application and process lifecycle constraints.

This is a critical gate.

A Rust thread or async task alone does not establish reliable long-running background execution on Android.

### 8.1 Foreground Execution

Investigate and validate the appropriate Android foreground-execution mechanism.

Verify:

- Foreground service requirements.
- Service startup behavior.
- Notification creation.
- Notification update behavior.
- Required permissions and declarations.
- Android version-specific restrictions.
- Service shutdown behavior.
- Interaction with application navigation.

Do not assume that a desktop background worker can continue running under Android lifecycle rules.

### 8.2 Long-Running Download Scenario

Execute a real download while testing:

1. Start a download.
2. Navigate away from the visible screen.
3. Keep the download running under the intended Android execution model.
4. Observe notification behavior.
5. Return to the application.
6. Verify progress consistency.
7. Pause the download.
8. Resume the download.
9. Cancel the download.
10. Verify cleanup and terminal state.

### 8.3 Process Recreation

Test:

- Application recreation.
- Process termination under controlled conditions.
- Application relaunch.
- Engine reinitialization.
- State recovery.
- Download status restoration.
- Resume eligibility.
- Incomplete output handling.

Do not treat Activity recreation as equivalent to process death.

Test these scenarios separately.

### 8.4 Network Interruption

Test:

- Temporary connectivity loss.
- Network reconnection.
- Wi-Fi to mobile-data transition where applicable.
- Server disconnection.
- Long network timeout.
- Request failure.
- Resume behavior after interruption.

Determine whether recovery belongs to the engine, Android integration, or application orchestration.

### 8.5 Concurrency and Resource Limits

Test:

- Multiple active downloads.
- Worker shutdown.
- Application navigation.
- Pause and resume under concurrency.
- Cancellation during active transfer.
- Process recreation with multiple downloads.
- Notification consistency.

### Stage 5 Exit Criteria

<escape>- [ ] Foreground execution strategy is validated.
- [ ] Notifications behave correctly.
- [ ] Download survives navigation under the intended execution model.
- [ ] Pause and resume work.
- [ ] Cancellation works.
- [ ] Process recreation is tested.
- [ ] State recovery is verified.
- [ ] Network interruption behavior is verified.
- [ ] Resource cleanup is verified.
- [ ] Unsupported or unresolved lifecycle scenarios are documented.</escape>

A successful foreground-service demonstration does not automatically prove reliability under every Android version or device policy.

## 9. Stage 6 — Android Storage Proof

**Objective:** Verify that the existing engine can write download data safely using Android-supported storage APIs.

### 9.1 Inspect Existing Filesystem Assumptions

Before implementing storage integration, inspect:

- How destination paths are represented.
- How files are created.
- How temporary files are created.
- How files are renamed.
- How partial output is persisted.
- How files are deleted.
- How resume state references output files.
- Whether the engine assumes unrestricted filesystem access.
- Whether file handles remain open during transfer.

### 9.2 Evaluate Android Storage Options

Evaluate the appropriate Android-supported storage mechanism based on the product's requirements.

Consider:

- App-private storage.
- User-selected destinations.
- Storage Access Framework where applicable.
- Content URI handling.
- Persisted URI permissions.
- File descriptor requirements.
- Temporary-file strategy.
- Rename and move semantics.
- Available-space checks.
- Storage revocation and failure behavior.

Do not assume that every Android content URI behaves like a regular filesystem path.

### 9.3 Destination Selection Proof

Verify:

1. The user can select an allowed destination.
2. The application retains the required permission.
3. Rust can write through the chosen integration strategy.
4. File creation succeeds.
5. Progress updates remain accurate.
6. Partial output remains consistent.
7. Resume works where supported.
8. Completion produces the expected destination.
9. Cancellation cleans up appropriately.
10. Storage failures are reported correctly.

### 9.4 Storage Architecture Decision

Determine whether the engine's current filesystem abstraction is sufficient.

If not, evaluate the narrowest integration strategy that preserves engine responsibilities.

Possible approaches must be evaluated from actual implementation constraints rather than selected in advance.

Do not redesign the engine's storage layer before understanding its current persistence and file ownership model.

### Stage 6 Exit Criteria

<escape>- [ ] Existing filesystem assumptions are documented.
- [ ] Android storage strategy is justified.
- [ ] Destination selection is tested.
- [ ] Rust writes successfully through the selected strategy.
- [ ] Temporary-file behavior is verified.
- [ ] Resume behavior is tested where supported.
- [ ] Completion and cleanup are verified.
- [ ] Permission revocation and storage failures are considered.
- [ ] Desktop storage behavior remains intact.</escape>

## 10. Stage 7 — Architecture Decision

**Objective:** Finalize the Android architecture only after the required technical proofs have produced sufficient evidence.

### 10.1 Required Evidence Before Approval

The architecture decision must be based on:

- Verified repository inventory.
- Component portability classifications.
- Engine Android build results.
- Native dependency and ABI assessment.
- Working Rust/Kotlin bridge prototype.
- Lifecycle proof.
- Foreground execution proof.
- Storage proof.
- Identified security implications.
- Desktop regression assessment.

A failed proof does not automatically mean the project must be abandoned.

It means the architecture must account for the actual limitation before proceeding.

### 10.2 Architecture Decisions

Document:

- Which upstream crates will be reused.
- Which upstream crates will remain desktop-only.
- Which optional capabilities are required.
- Which components require Android adapters.
- Whether a Rust application adapter is justified.
- Which Kotlin responsibilities are necessary.
- The selected Rust/Kotlin bridge.
- The lifecycle integration model.
- The storage integration model.
- The UI approach.
- Native dependency and ABI strategy.
- Build and test strategy.
- Upstream synchronization strategy.

### 10.3 Folder and Crate Layout

Only now should the final target structure be approved.

Requirements:

- Preserve existing upstream crate boundaries.
- Avoid moving upstream code without a justified reason.
- Introduce new crates only for concrete architectural responsibilities.
- Avoid placeholder directories.
- Avoid duplicating existing engine functionality.
- Separate Android-specific integration from portable Rust code.
- Keep Kotlin focused on Android framework integration.
- Ensure dependency direction remains clear.
- Minimize changes to the existing workspace.

The approved structure must represent actual responsibilities, not a desired folder hierarchy.

### 10.4 Architecture Decision Records

For each significant decision, record:

- Decision identifier.
- Context.
- Verified evidence.
- Options considered.
- Selected option.
- Reasons for selection.
- Consequences.
- Risks.
- Alternatives rejected.
- Follow-up work.

Distinguish approved decisions from proposals.

### Stage 7 Exit Criteria

<escape>- [ ] Required technical proofs have sufficient evidence.
- [ ] Unresolved blockers are explicitly accepted or addressed.
- [ ] Reusable upstream components are identified.
- [ ] Android-specific responsibilities are defined.
- [ ] Bridge, lifecycle, and storage strategies are justified.
- [ ] UI direction is supported by a technical proof.
- [ ] Final folder and crate layout is approved.
- [ ] Architecture decisions are documented.</escape>

## 11. Stage 8 — Incremental Delivery

**Objective:** Implement the approved architecture through small, independently verifiable vertical slices.

Do not implement the entire Android application in one large change.

### 11.1 Vertical Slice Principle

Each slice must deliver one coherent capability across the required layers.

A slice may involve:

- Android UI.
- Kotlin integration.
- Rust bridge.
- Engine API.
- Persistence.
- Notifications.
- Tests.

Only modify the layers necessary for the selected capability.

### 11.2 Suggested Delivery Order

The exact order must be adjusted according to verified dependencies and approved architecture.

A possible sequence is:

1. Android application startup and native library loading.
2. Minimal Rust/Kotlin integration.
3. Engine initialization.
4. Basic download request.
5. Progress observation.
6. Download completion and failure reporting.
7. Destination selection and storage.
8. Pause and resume.
9. Cancellation.
10. Foreground execution and notifications.
11. Persistent state recovery.
12. Multiple concurrent downloads.
13. Optional product features.

This sequence is a proposed delivery plan, not a claim that these operations are already supported by the engine.

### 11.3 Requirements for Every Slice

Before implementation:

- State the objective.
- Inspect existing code.
- Identify affected components.
- Identify dependencies.
- Identify public contracts.
- Define acceptance criteria.
- Identify tests.
- Identify regression risks.

During implementation:

- Keep changes narrowly scoped.
- Preserve existing contracts.
- Reuse existing helpers and abstractions.
- Avoid unrelated refactoring.
- Do not introduce speculative compatibility layers.

After implementation:

- Run relevant Rust tests.
- Run relevant Android tests.
- Build affected targets.
- Verify desktop compatibility.
- Inspect the diff.
- Report actual results.
- Record unresolved risks.

### 11.4 Preserve Desktop Builds

Desktop behavior is a regression boundary.

For every change affecting shared Rust code:

- Run relevant existing desktop checks.
- Verify affected desktop targets.
- Inspect feature-selection changes.
- Verify target-specific dependencies.
- Review changes to public APIs.
- Test affected integrations where practical.

Do not claim desktop preservation merely because Android compilation succeeds.

### Stage 8 Exit Criteria

<escape>- [ ] Each slice has explicit acceptance criteria.
- [ ] Changes are limited to the approved scope.
- [ ] Relevant tests pass or failures are documented.
- [ ] Android builds are verified.
- [ ] Desktop regression checks are performed.
- [ ] Public contract changes are reviewed.
- [ ] Architecture documentation is updated when necessary.
- [ ] Actual implementation status is reported.</escape>

## 12. Change-Control and Approval Gates

The following changes require explicit architectural justification and approval before implementation:

- Moving or restructuring upstream crates.
- Replacing existing engine responsibilities.
- Introducing a new engine or orchestration crate.
- Changing public engine APIs.
- Replacing the IPC mechanism.
- Introducing a new storage architecture.
- Selecting a permanent Rust/Kotlin bridge.
- Changing download persistence semantics.
- Introducing new native dependencies.
- Changing desktop integration behavior.
- Introducing a new UI architecture with broad project impact.

Small, isolated, evidence-backed fixes may proceed within the approved task scope.

Do not expand a narrow compatibility fix into a broad architecture rewrite.

## 13. Failure and Blocker Management

When a stage cannot be completed:

1. Record the exact blocker.
2. Identify the affected stage.
3. Record the attempted commands or tests.
4. Include the relevant error output.
5. Identify the responsible source or dependency.
6. Classify the finding.
7. Determine whether the blocker is temporary, environmental, architectural, or unresolved.
8. Identify the smallest next investigation.
9. Avoid speculative implementation.
10. Prevent dependent architecture decisions from being marked approved.

Use the following statuses:

- `NOT STARTED`
- `IN PROGRESS`
- `PASSED`
- `FAILED`
- `BLOCKED`
- `NEEDS REVIEW`
- `APPROVED`

A stage may be `PASSED` only when its defined exit criteria have sufficient evidence.

## 14. Required Project Progress Report

At the end of each major stage, report:

### Stage

Name and current status.

### Objective

What the stage was intended to establish.

### Investigation

What source files, manifests, dependencies, commands, and tests were inspected.

### Verified Findings

Facts directly established through evidence.

### Inferred Findings

Conclusions that still require verification.

### Blockers

Unresolved technical or environmental limitations.

### Changes

Exact files modified, if any.

### Verification

Commands executed and actual results.

### Risks

Potential impact on engine correctness, desktop compatibility, Android lifecycle, storage, or security.

### Next Gate

The specific evidence required before proceeding.

Never report a planned test as an executed test.

Never report an attempted build as a successful build.

Never report a prototype as production-ready integration.

## 15. Final Engineering Principles

1. Inventory before classification.
2. Classification before porting.
3. Engine proof before application integration.
4. Bridge proof before permanent integration architecture.
5. Lifecycle proof before claiming background-download reliability.
6. Storage proof before finalizing file-handling architecture.
7. Architecture decisions after evidence.
8. Incremental delivery instead of broad rewrites.
9. Preserve existing desktop behavior.
10. Reuse upstream functionality wherever justified.
11. Keep Android-specific behavior behind narrow platform boundaries.
12. Record unknowns and blockers honestly.

**Do not build the architecture first and search for proof afterward.**

**Validate the engine, integration, lifecycle, and storage before committing to the final design.**

**Deliver Android capabilities incrementally while preserving upstream functionality.**
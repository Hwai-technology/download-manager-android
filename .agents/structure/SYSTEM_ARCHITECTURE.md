# System Architecture

## 1. Document Status and Purpose

**Status: UPSTREAM DOCUMENTATION-BASED ARCHITECTURE — LOCAL SOURCE AUDIT REQUIRED**

This document describes the conceptual system architecture documented by the upstream project.

It establishes an initial understanding of:

- Major system components.
- Architectural responsibilities.
- Dependency direction.
- Download request flows.
- Browser integration.
- Remote-control integration.
- Platform-specific boundaries.

It is not a verified description of the selected local repository revision.

### Source-of-Truth Requirements

- The checked-out source code is authoritative for implemented behavior.
- Cargo manifests are authoritative for crate configuration and dependencies.
- The checked-out Git revision determines which architecture documentation is applicable.
- Public `main` documentation must not be assumed to match the selected release.
- Actual call paths must be traced before documenting component interactions as verified.
- Conceptual flows must not be converted into assumed APIs, events, or message contracts.

Every architectural statement must be classified as:

- `VERIFIED`
- `DOCUMENTED`
- `INFERRED`
- `UNKNOWN`
- `BLOCKED`
- `PROPOSED`

## 2. Upstream System Overview

The upstream architecture documentation describes a Rust workspace containing separate components for:

- Download engine.
- Desktop application.
- Command-line interface.
- Automation and MCP integration.
- Remote control.
- Browser native messaging.
- IPC.
- Supporting capabilities.

The documented architecture separates the download engine from the user-facing and integration components.

The intended architectural principle is that multiple front ends and integration processes consume reusable engine or IPC capabilities without embedding UI concerns inside the download engine.

### Important Qualification

The existence, package names, dependencies, and responsibilities of these components must be verified against the selected local revision.

Refer to:

`REPOSITORY_MAP.md`

for the initial component inventory and mandatory repository audit.

## 3. Main Architectural Principle

### 3.1 Engine Independence

The download engine should remain independent of user-interface concerns.

Its primary responsibility is download execution and related transfer mechanics.

The engine should not become responsible for:

- Rendering application screens.
- Managing desktop windows.
- Handling Android Activity lifecycle.
- Managing Android notifications.
- Implementing Android foreground services.
- Owning UI navigation.
- Controlling platform-specific presentation behavior.

These responsibilities belong to appropriate application or platform integration layers.

### 3.2 Front-End and Integration Boundaries

Front ends and integration processes should consume verified engine capabilities through established interfaces.

Potential consumers described by the upstream architecture include:

- Desktop application.
- CLI.
- Automation and MCP processes.
- Remote-control integration.
- Browser integration.

The exact communication mechanism must be discovered from the source.

Do not assume that every consumer communicates directly with the engine.

Some consumers may use an application orchestration layer or IPC boundary instead.

### 3.3 Dependency Direction

The desired conceptual dependency principle is:

- User-facing components depend on application or engine capabilities.
- Integration components use established application, IPC, or engine interfaces.
- The engine remains independent of UI implementation.
- Platform-specific behavior remains isolated behind justified boundaries.

This is an architectural principle, not proof of the actual Cargo dependency graph.

Verify dependency direction through:

- Cargo workspace membership.
- Crate manifests.
- Public exports.
- Actual callers.
- Runtime initialization.
- IPC message definitions.
- Integration tests.

Do not infer dependency direction from folder names.

## 4. Conceptual System Components

The following sections describe the documented component roles.

They are not a verified list of local crates or exact Rust APIs.

### 4.1 Download Engine

Documented responsibility:

Reusable single-file download execution and associated transfer mechanics.

Conceptual capabilities include:

- Resource probing.
- Transfer planning.
- Worker execution.
- Connection management.
- Transfer state.
- Resume support.
- Progress reporting.
- Completion and failure handling.

The engine's internal responsibilities are documented separately in:

`ENGINE_STRUCTURE.md`

Architectural requirement:

Keep the engine independent from desktop UI and Android framework responsibilities.

### 4.2 Desktop Application

The upstream architecture documentation describes a desktop application based on egui.

Its conceptual responsibilities include:

- User interaction.
- Application-level orchestration.
- Download presentation.
- Application state.
- Integration with engine capabilities.
- Desktop-specific behavior.

These responsibilities must be verified against the actual application crate.

Do not assume:

- The desktop application is a reusable Android application layer.
- egui desktop configuration automatically supports Android.
- Desktop window-management code can be reused on Android.
- The application's initialization model is compatible with Android lifecycle constraints.
- All application dependencies support Android targets.

The desktop application and the Android application are separate architectural concerns until a technical proof establishes a reusable integration strategy.

### 4.3 CLI

Documented responsibility:

Command-line access to download functionality.

Investigate:

- Actual binary entry points.
- Argument parsing.
- Configuration loading.
- Engine initialization.
- Application orchestration dependencies.
- Output and logging behavior.
- Process lifecycle.
- Exit codes.
- Signal handling.

Do not assume the CLI directly invokes the engine without an intermediate application layer.

### 4.4 Automation and MCP

The upstream architecture documentation identifies automation and MCP-related functionality.

Investigate:

- Actual server entry points.
- Supported protocols.
- Tool or command definitions.
- Authentication and authorization.
- Engine access.
- Application orchestration dependencies.
- Process lifecycle.
- Error propagation.
- Security boundaries.

Do not assume that an automation process is a required Android component.

Do not expose internal engine operations as external tools without examining the actual contracts and security implications.

### 4.5 Remote Control

The documented remote feature provides HTTPS-based control from a paired device.

This describes a remote-control integration, not a native Android application.

Investigate:

- Actual remote server implementation.
- HTTPS and TLS configuration.
- Pairing flow.
- Authentication.
- Authorization.
- Session lifecycle.
- Request validation.
- Allowed operations.
- Engine or application integration.
- Error handling.
- Connection lifecycle.

Do not assume that remote pairing is equivalent to Android account authentication.

Do not assume that remote-control functionality provides native Android lifecycle, notification, storage, or background execution support.

Remote-control security requirements are documented separately in:

`../rules/SECURITY_AND_LICENSE.md`

### 4.6 Browser Extension and Native Messaging

The documented browser integration follows this conceptual path:

<escape>Browser extension -> native messaging -> native host -> IPC -> running application</escape>

This is a conceptual integration flow.

Every transition must be verified against the selected source revision.

Investigate:

- Browser extension message creation.
- Extension permissions.
- Native messaging protocol.
- Native host registration.
- Message framing.
- Message validation.
- Host startup and shutdown.
- IPC connection establishment.
- Application discovery.
- Request acceptance.
- Request rejection.
- Cancellation.
- Error propagation.
- Browser disconnect behavior.

#### Acceptance and Cancellation Semantics

Do not change browser integration behavior before establishing the exact request lifecycle.

Specifically determine:

1. When a browser request is considered received.
2. When the native host accepts or rejects it.
3. Whether acceptance means queued, initialized, or actively transferring.
4. How the running application confirms acceptance.
5. How request identifiers are correlated.
6. Whether cancellation is supported.
7. Whether cancellation is possible before acceptance.
8. Whether cancellation is possible after transfer initialization.
9. How cancellation propagates through IPC.
10. What happens when the browser, native host, or application disconnects.
11. Whether duplicate requests are detected.
12. How failures are reported to the browser extension.

Do not invent acceptance states, message fields, or cancellation commands.

The actual protocol and source implementation are authoritative.

#### Android Scope

Browser native messaging is not automatically an Android integration requirement.

Do not port the desktop native-messaging host merely because the repository contains one.

An Android browser-integration requirement must be established separately, with a verified platform-compatible communication mechanism.

### 4.7 IPC

The upstream architecture documentation identifies IPC as an integration capability.

Investigate:

- Actual IPC transport.
- Message definitions.
- Serialization format.
- Connection lifecycle.
- Process discovery.
- Authentication.
- Authorization.
- Request-response correlation.
- Event delivery.
- Cancellation.
- Error handling.
- Version compatibility.
- Shutdown behavior.

Determine which components actually use IPC and whether IPC is required for Android.

Do not assume that an existing desktop IPC mechanism is suitable for Android application-process communication.

Do not introduce a second IPC system without proving that the existing integration boundary cannot satisfy the requirement.

### 4.8 Supporting Capabilities

The repository map identifies additional documented components, including:

- Debrid provider integration.
- Archive extraction.
- Geographic lookup.
- External video/gallery tools.
- Crash and freeze reporting.
- BitTorrent and magnet support.
- Fault-injecting test servers.

Their actual responsibilities and dependency relationships must be verified independently.

Do not assume all supporting capabilities belong in the core engine.

Do not include every upstream capability in the Android product by default.

## 5. Conceptual Desktop Download Flow

The upstream architecture documentation describes the following conceptual flow:

<escape>UI / CLI / automation request -> application orchestration -> engine API -> probe / planner / workers / state -> engine events -> application state / UI</escape>

This flow describes a high-level architectural relationship.

It is not a verified call graph.

### 5.1 Request Initiation

Possible request sources include:

- Desktop UI.
- CLI.
- Automation integration.

Verify the actual entry points for each source.

Identify whether they share an application-level request contract.

### 5.2 Application Orchestration

Investigate the actual orchestration responsibilities.

Determine whether this layer:

- Validates incoming requests.
- Resolves configuration.
- Creates download operations.
- Tracks active downloads.
- Coordinates engine calls.
- Receives engine events.
- Maintains application-level state.
- Coordinates persistence.
- Handles cancellation.
- Exposes capabilities to other processes.

Do not assume all these responsibilities exist in one component.

Trace the actual implementation.

### 5.3 Engine Execution

The application or integration layer invokes the verified engine interface.

The engine performs its actual transfer responsibilities.

Refer to `ENGINE_STRUCTURE.md` for the detailed investigation of:

- Probe.
- Planner.
- Workers.
- State.
- Events.
- Finalization.

### 5.4 Event Propagation

Determine how engine events reach the application.

Inspect:

- Event producer.
- Event type.
- Event transport.
- Event consumer.
- Event ordering.
- Event loss behavior.
- Backpressure.
- Application-state updates.
- UI notification behavior.

Do not assume that engine events directly update UI state.

### 5.5 Application State and Presentation

Investigate how application state is maintained and exposed.

Determine:

- Which component owns authoritative download state.
- Whether UI state is derived from engine state.
- Whether application state is persisted separately.
- How progress updates are reflected.
- How completion and failures are presented.
- How cancellation is reflected.
- How application restart affects state restoration.

Avoid duplicating engine-owned transfer state without a verified architectural need.

## 6. Browser Capture Flow

Documented conceptual flow:

<escape>Browser extension -> native messaging -> native host -> IPC -> running application</escape>

### Required Investigation

Trace the complete flow in source code.

Record:

| Stage | Evidence required |
|---|---|
| Extension | Actual request creation and message format |
| Native messaging | Actual protocol and framing |
| Native host | Entry point and message handling |
| IPC | Actual transport and contracts |
| Application | Request acceptance and processing |
| Engine | Actual download creation path |
| Cancellation | Exact propagation and terminal behavior |
| Failure | Actual error reporting path |

### Required Behavioral Scenarios

Inspect and test where possible:

- Valid request.
- Invalid request.
- Unsupported URL.
- Duplicate request.
- Request rejected by application.
- Application unavailable.
- Native host startup failure.
- IPC connection failure.
- Browser disconnect.
- Cancellation before acceptance.
- Cancellation after acceptance.
- Cancellation during active transfer.
- Application shutdown during request processing.

Do not claim that cancellation is supported merely because a cancellation-related symbol exists.

Trace its complete path and inspect tests.

## 7. Remote-Control Flow

The upstream documentation describes HTTPS-based control from a paired device.

The exact request lifecycle and security model must be established from source.

Investigate the following conceptual sequence without assuming the actual implementation:

- Remote client connection.
- Pairing or authentication.
- Request validation.
- Authorization.
- Application or engine operation.
- Response delivery.
- Session termination.

Verify:

- Whether pairing is one-time or session-based.
- How credentials or tokens are generated.
- How credentials are stored.
- How sessions expire.
- Whether remote requests can control all or only selected operations.
- How remote cancellation works.
- How concurrent remote clients are handled.
- How network failures affect active operations.
- How TLS certificates are configured and validated.

Remote-control functionality must be treated as a security-sensitive integration boundary.

It must not be interpreted as proof of native Android support.

## 8. Platform Boundaries

The upstream architecture documentation describes desktop-specific code isolated behind platform modules.

This must be verified in the checked-out source.

### 8.1 Required Inspection

Inspect:

- Conditional compilation using `cfg` and `cfg_attr`.
- Platform-specific modules.
- Target-specific Cargo dependencies.
- Build scripts.
- Operating-system API usage.
- Filesystem behavior.
- Process management.
- Windowing and UI frameworks.
- System notifications.
- Native library dependencies.
- External executable requirements.

### 8.2 Platform Classification

Classify each relevant component or module as:

- Platform-independent.
- Desktop-specific.
- Android-specific.
- Target-dependent.
- Shared through an adapter.
- Unknown.

Base classifications on actual implementation and dependency evidence.

### 8.3 Android Porting Constraints

The Android architecture must not assume that desktop modules are reusable.

Investigate separately:

- Android application lifecycle.
- Foreground execution requirements.
- Process termination and recreation.
- Notifications.
- Storage access.
- Network permissions and behavior.
- Background execution restrictions.
- Native library loading.
- Rust/Kotlin integration.
- ABI compatibility.

Rust threads alone do not establish reliable Android background execution.

Android framework lifecycle responsibilities must be handled by the appropriate Android integration layer.

## 9. Architectural Boundaries for the Android Target

The Android product should reuse upstream functionality where technically justified.

It must not inherit desktop architecture blindly.

### 9.1 Rust Engine

Expected role:

- Reuse verified download execution functionality.
- Preserve transfer correctness.
- Own engine-level state and transfer mechanics according to actual implementation.
- Expose only the integration capabilities required by the Android application.

### 9.2 Kotlin Android Integration

Expected role:

- Integrate with Android framework APIs.
- Coordinate Android lifecycle-sensitive behavior.
- Manage platform-specific notifications and permissions.
- Integrate with Android storage mechanisms.
- Provide required background-execution integration.
- Bridge to Rust through a verified integration strategy.

Keep Kotlin responsibilities minimal and platform-focused.

### 9.3 Application-Level Coordination

Determine whether the existing upstream application-level orchestration can be reused.

If not, define the smallest necessary Android coordination layer.

Do not duplicate engine responsibilities in this layer.

Do not create a new Rust application adapter or orchestration crate unless source inspection and integration requirements justify it.

### 9.4 UI Boundary

The Android UI technology and architecture must be decided separately.

Do not assume that the existing desktop egui UI is the correct Android UI.

Require a real technical proof of concept before committing to UI reuse or replacement.

Refer to:

`ANDROID_TARGET_STRUCTURE.md`

for the proposed target responsibility boundaries and required technical proof gates.

## 10. System-Wide State Ownership

Before integration or architectural modification, identify ownership of:

- Download request state.
- Active download state.
- Transfer progress.
- Worker state.
- Persistent resume state.
- Application-level state.
- UI presentation state.
- IPC request state.
- Remote session state.
- Browser request state.

For every state category, determine:

1. Which component creates it?
2. Which component owns it?
3. Which component can modify it?
4. How is it persisted?
5. How is it observed?
6. How is it synchronized?
7. What happens after process termination?
8. Which component is responsible for cleanup?

Avoid creating multiple competing sources of truth.

A presentation-layer state copy must not silently become authoritative engine state.

## 11. Cross-Component Failure and Lifecycle Analysis

The system architecture must account for failures across component boundaries.

Investigate:

- Engine initialization failure.
- Application startup failure.
- IPC disconnection.
- Browser host termination.
- Remote client disconnection.
- Network interruption.
- Worker failure.
- Storage failure.
- Event consumer termination.
- Application shutdown.
- Process termination.
- Process recreation.
- Partial finalization.
- Duplicate requests.

For every failure scenario, determine:

- Detection point.
- Owning component.
- Error propagation path.
- Cleanup responsibility.
- Persistent state impact.
- Recovery behavior.
- User-visible outcome.

Do not assume that a component's local error handling guarantees system-wide recovery.

## 12. Required Architecture Audit

The system architecture audit must be completed against the selected local revision.

### 12.1 Component Inventory

- [ ] Verify actual workspace members.
- [ ] Verify actual package names.
- [ ] Identify libraries and binaries.
- [ ] Read relevant manifests.
- [ ] Inspect actual source directories.
- [ ] Identify component entry points.
- [ ] Identify runtime processes.

### 12.2 Dependency Graph

- [ ] Inspect Cargo workspace configuration.
- [ ] Inspect direct dependencies.
- [ ] Inspect important transitive dependencies.
- [ ] Identify target-specific dependencies.
- [ ] Identify optional features.
- [ ] Verify dependency direction.
- [ ] Identify shared interfaces.
- [ ] Identify unexpected coupling.

### 12.3 Download Request Flow

- [ ] Trace UI request entry points.
- [ ] Trace CLI request entry points.
- [ ] Trace automation request entry points.
- [ ] Identify application orchestration.
- [ ] Trace engine API calls.
- [ ] Trace event delivery.
- [ ] Trace application-state updates.
- [ ] Inspect cancellation.
- [ ] Inspect error propagation.

### 12.4 Browser Integration

- [ ] Inspect extension implementation.
- [ ] Inspect native messaging.
- [ ] Inspect native host.
- [ ] Inspect IPC contracts.
- [ ] Trace request acceptance.
- [ ] Trace cancellation.
- [ ] Inspect disconnect behavior.
- [ ] Inspect duplicate-request behavior.

### 12.5 Remote Control

- [ ] Inspect server entry point.
- [ ] Inspect HTTPS configuration.
- [ ] Inspect pairing.
- [ ] Inspect authentication.
- [ ] Inspect authorization.
- [ ] Trace supported operations.
- [ ] Inspect cancellation.
- [ ] Inspect session lifecycle.
- [ ] Inspect failure behavior.

### 12.6 Platform Boundaries

- [ ] Inspect conditional compilation.
- [ ] Inspect target-specific dependencies.
- [ ] Identify desktop-only APIs.
- [ ] Identify external executable dependencies.
- [ ] Identify filesystem assumptions.
- [ ] Identify native dependency requirements.
- [ ] Inspect Android target compatibility.

### 12.7 Tests

- [ ] Inspect cross-component integration tests.
- [ ] Inspect IPC tests.
- [ ] Inspect browser integration tests.
- [ ] Inspect remote-control tests.
- [ ] Inspect request lifecycle tests.
- [ ] Inspect cancellation tests.
- [ ] Inspect failure and recovery tests.
- [ ] Identify untested integration boundaries.

## 13. Required Architecture Deliverables

The completed audit must produce the following.

### 13.1 Verified Component Diagram

A diagram showing only components confirmed in the checked-out revision.

The diagram must distinguish:

- Libraries.
- Executable processes.
- Application layers.
- Integration boundaries.
- Platform-specific components.

Do not include conceptual components as verified nodes.

### 13.2 Verified Dependency Graph

A source-backed representation of actual crate dependencies.

Distinguish:

- Compile-time dependencies.
- Runtime communication.
- IPC relationships.
- Optional integrations.
- Target-specific dependencies.

Cargo dependency edges must not be confused with runtime communication edges.

### 13.3 Download Request Sequence

A verified sequence covering:

- Request source.
- Application orchestration.
- Engine invocation.
- Engine execution.
- Event propagation.
- Application-state update.
- Completion or failure.

Include actual symbol names and source locations only after verification.

### 13.4 Browser Integration Sequence

A verified sequence covering:

- Extension.
- Native messaging.
- Native host.
- IPC.
- Application.
- Engine.
- Acceptance.
- Cancellation.
- Failure.

Document unsupported or unverified transitions explicitly.

### 13.5 Remote-Control Security Boundary

A verified description of:

- Connection establishment.
- Pairing and authentication.
- Authorization.
- Operation execution.
- Response handling.
- Session termination.

### 13.6 Platform Boundary Map

A verified classification of:

- Shared Rust code.
- Desktop-only code.
- Android integration requirements.
- Target-specific dependencies.
- Unknown compatibility areas.

## 14. Evidence and Reporting Rules

Every important architectural statement must identify its evidence.

Use the following classifications:

### VERIFIED

Confirmed by checked-out source, manifests, actual dependency metadata, tests, or runtime evidence.

### DOCUMENTED

Described in upstream architecture documentation but not confirmed against the selected revision.

### INFERRED

A conclusion derived from available evidence but not directly established.

### UNKNOWN

The architecture or behavior has not been established.

### BLOCKED

Required source, environment, dependency, or test evidence is unavailable.

### PROPOSED

A suggested future architectural decision that has not been implemented or approved.

Do not describe a proposed Android component as an existing upstream component.

Do not describe a documented flow as a verified execution sequence.

Do not claim that an integration works because both endpoints exist.

## 15. Rules Before System-Wide Architectural Changes

Before changing component boundaries or communication mechanisms:

1. Identify the exact requirement.
2. Inspect the current architecture.
3. Verify the relevant workspace members.
4. Trace actual dependency relationships.
5. Trace runtime communication.
6. Identify existing interfaces and contracts.
7. Inspect callers and consumers.
8. Assess state ownership.
9. Assess failure propagation.
10. Assess lifecycle implications.
11. Assess security implications.
12. Assess Android compatibility.
13. Propose the smallest justified change.
14. Identify affected components and tests.
15. Obtain approval before broad architectural changes.

Do not:

- Introduce a second download engine.
- Couple the engine to a UI framework.
- Replace IPC without investigating the existing protocol.
- Reimplement desktop integrations without an Android requirement.
- Assume remote control is equivalent to native Android integration.
- Move crates solely to create a preferred directory tree.
- Create abstractions without a concrete responsibility.
- Modify unrelated integrations during a focused Android port.

## 16. Relationship to Other Architecture Documents

This document describes system-wide relationships.

It must remain consistent with:

- `REPOSITORY_MAP.md` — actual repository components and workspace structure.
- `ENGINE_STRUCTURE.md` — engine responsibilities and implementation details.
- `ANDROID_TARGET_STRUCTURE.md` — proposed Android target architecture.
- `ARCHITECTURE_AND_REUSE.md` — architecture preservation and reuse requirements.
- `ANDROID_RUST_FIRST.md` — Rust-first Android integration principles.
- `NO_GUESSING_AND_SOURCE_OF_TRUTH.md` — evidence and verification rules.
- `SECURITY_AND_LICENSE.md` — security and license requirements.

When these documents appear to conflict:

1. Establish actual implementation facts from the checked-out source.
2. Distinguish documented upstream design from local implementation.
3. Distinguish current architecture from proposed Android architecture.
4. Correct unsupported or outdated statements.
5. Preserve unresolved questions as explicit unknowns.

## 17. Final System Architecture Principles

1. The engine must remain independent of UI concerns.
2. Front ends and integrations must use verified interfaces.
3. Cargo dependencies and runtime communication must be analyzed separately.
4. Application orchestration must be understood before introducing another coordination layer.
5. Browser acceptance and cancellation semantics must be traced before modification.
6. Remote control must be treated as a security-sensitive integration, not as native Android support.
7. Desktop-specific code must be identified through actual platform boundaries.
8. Android lifecycle and background execution require a dedicated platform integration strategy.
9. State ownership and failure propagation must be understood across component boundaries.
10. Preserve existing upstream architecture unless a verified requirement justifies a change.

**Understand component relationships before changing architecture.**

**Trace real execution paths instead of relying on conceptual diagrams.**

**Reuse verified capabilities without inheriting unnecessary desktop assumptions.**

**Keep the Android target distinct from the upstream desktop architecture.**
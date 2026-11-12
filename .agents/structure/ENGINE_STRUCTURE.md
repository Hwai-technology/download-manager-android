# Engine Structure and Responsibilities

## 1. Document Status and Source

**Status: UPSTREAM DOCUMENTATION-BASED OVERVIEW — LOCAL SOURCE AUDIT REQUIRED**

This document describes the conceptual responsibilities attributed to the upstream Rust download engine.

According to the upstream architecture documentation, the engine is located at:

- `crates/engine`
- Crate name documented as `mdm-engine`

These paths and names must be verified against the checked-out repository revision before being treated as confirmed local implementation details.

### Source-of-Truth Rules

- The checked-out source code is authoritative.
- The checked-out crate manifest determines the actual crate configuration.
- The checked-out `lib.rs` and module declarations determine the actual exported API surface.
- The current Git revision must be recorded before using upstream architecture documentation.
- Documentation from `main` must not be assumed to describe an older release such as `v0.5.2`.
- Conceptual names in this document are not guaranteed Rust symbol names.
- No module, struct, enum, function, event, payload, or error type may be introduced based solely on this document.

This document is an investigation guide and responsibility map, not a substitute for source inspection.

## 2. Engine Mission

According to the upstream architecture documentation, the engine is responsible for single-file HTTP(S)/FTP(S) downloads and their associated scheduling and transfer mechanics.

The documented responsibilities include:

- Resource probing.
- Transfer planning.
- Byte-range segmentation.
- Worker connection management.
- Per-host connection limits.
- Download speed limiting.
- Resumable download state.
- Checksum handling.
- Proxy and site-specific behavior.
- Progress and lifecycle event reporting.
- Download completion and failure handling.

These responsibilities describe documented engine capabilities.

Their exact implementation, availability, configuration, and platform compatibility must be verified against the checked-out source.

### Architectural Objective

The engine should remain the reusable download component.

The Android application must reuse its verified capabilities instead of creating a parallel download engine.

Android-specific framework responsibilities must remain outside the portable engine unless an existing upstream contract or verified technical requirement establishes otherwise.

## 3. Conceptual Download Lifecycle

The upstream architecture documentation describes the following conceptual lifecycle:

<escape>request -> probe -> planner -> workers -> state -> events -> finalize</escape>

This sequence is a conceptual model, not a verified call graph or guaranteed implementation order.

Each stage must be traced through the actual source code.

### 3.1 Request

Investigation objectives:

- Identify how a download request enters the engine.
- Locate the actual public entry point, if one exists.
- Identify request types and their definitions.
- Determine how URL, headers, destination, and options are represented.
- Identify validation performed before transfer initialization.
- Trace request ownership and lifecycle.
- Identify whether request processing is synchronous or asynchronous.

Questions to answer:

- What component initiates a download?
- What is the actual request contract?
- How are invalid requests handled?
- Where is request state created?
- Can multiple requests execute concurrently?
- How is cancellation represented?

Do not assume that the request uses a particular struct or method name.

### 3.2 Probe

Documented responsibility:

Inspect the remote resource before planning the transfer.

Investigation objectives:

- Locate the actual probing implementation.
- Identify supported protocols.
- Determine which HTTP or FTP requests are performed.
- Inspect redirect behavior.
- Inspect server capability detection.
- Determine how resource size is obtained.
- Determine how range support is detected.
- Inspect filename and metadata extraction.
- Identify timeout and error handling.
- Determine whether authentication, cookies, headers, and proxies are supported.

Questions to answer:

- How does the engine determine whether a resource supports range requests?
- What happens when the server does not provide a reliable resource size?
- How are redirects handled?
- What happens when probing fails?
- Does the engine support a single-connection fallback?
- Which response headers are trusted, validated, or ignored?

Do not assume that all servers support byte ranges or provide reliable metadata.

### 3.3 Planner

Documented responsibility:

Plan the transfer, including byte segmentation where supported.

Investigation objectives:

- Locate the actual transfer-planning implementation.
- Determine how segment counts are selected.
- Determine how segment boundaries are calculated.
- Inspect minimum and maximum segment sizes.
- Identify how unknown resource sizes are handled.
- Determine how server range support affects planning.
- Inspect per-host constraints.
- Determine how configuration affects planning.
- Identify fallback behavior when parallel transfers are unavailable.

Questions to answer:

- How are byte ranges represented?
- Are segment boundaries inclusive or exclusive?
- How are offsets and lengths validated?
- How are overlapping or missing byte ranges prevented?
- What happens when the resource changes during transfer?
- How does the planner behave when the server does not support range requests?

Verify the exact implementation and its tests before documenting a definitive algorithm.

### 3.4 Workers

Documented responsibility:

Manage connections that execute transfer work.

Investigation objectives:

- Locate worker implementation and creation paths.
- Determine how workers obtain transfer assignments.
- Inspect HTTP and FTP connection handling.
- Determine how connections are reused or recreated.
- Inspect concurrency controls.
- Identify per-host connection limits.
- Inspect retry and failure behavior.
- Determine how workers stop.
- Inspect cancellation propagation.
- Determine how worker failures affect other workers.

Questions to answer:

- How many workers may operate concurrently?
- What limits concurrency?
- Are connection limits global, per-host, or configured at another scope?
- How are failed segments reassigned?
- What happens when a worker exits unexpectedly?
- How is worker shutdown coordinated?
- How are partial writes handled?

Do not assume that worker count, thread count, and connection count are equivalent.

### 3.5 State and Persistence

Documented responsibility:

Maintain resumable download state.

Investigation objectives:

- Locate the actual state-management implementation.
- Identify the state representation.
- Determine where state is stored.
- Inspect serialization and deserialization.
- Determine when state is persisted.
- Inspect how completed segments are recorded.
- Identify recovery behavior after interruption.
- Inspect state consistency and corruption handling.
- Determine how temporary files are managed.
- Identify the relationship between transfer state and final output.

Questions to answer:

- What information is required to resume a download?
- How are completed byte ranges recorded?
- How is the original resource identity verified?
- What happens when a remote file changes?
- How are incomplete or corrupted state records handled?
- What happens after process termination?
- Are writes atomic or otherwise protected against partial persistence?
- How are state and downloaded file data kept consistent?

Do not assume that resumability means that every interrupted transfer can always resume safely.

### 3.6 Events

Documented responsibility:

Report progress, completion, and failure events.

Investigation objectives:

- Locate event definitions.
- Identify event producers and consumers.
- Inspect event payload types.
- Determine delivery semantics.
- Determine whether events are synchronous or asynchronous.
- Inspect ordering guarantees.
- Determine whether events can be missed.
- Inspect event-channel capacity and backpressure.
- Identify cancellation and shutdown behavior.
- Determine how event errors are handled.

Questions to answer:

- Which events are actually defined?
- Which component emits them?
- Who consumes them?
- Are events guaranteed to arrive in order?
- Can progress updates be coalesced or dropped?
- How are terminal events represented?
- Can event delivery block transfer workers?
- What happens when a consumer disconnects?

Do not invent event names or payload fields from the conceptual descriptions in this document.

### 3.7 Finalization

Documented lifecycle concept:

Complete the transfer and finalize its output.

Investigation objectives:

- Locate the actual completion and finalization paths.
- Inspect final file handling.
- Determine when temporary files are renamed or moved.
- Inspect checksum verification.
- Determine how integrity failures are reported.
- Inspect cleanup of temporary data.
- Identify behavior when finalization fails.
- Determine when completion is reported.
- Inspect state cleanup after success.

Questions to answer:

- When is a download considered complete?
- Is checksum verification mandatory or optional?
- Which checksum algorithms are supported?
- What happens when verification fails?
- Can a completed transfer fail during finalization?
- How is partial output handled?
- Can cleanup failures leave inconsistent state?

Do not equate all workers finishing with successful download finalization.

## 4. Cross-Cutting Engine Responsibilities

The following areas must be investigated across the relevant engine modules and execution paths.

### 4.1 Per-Host Connection Limits

Verify:

- Actual connection-limit configuration.
- Scope of each limit.
- Default values, if any.
- Enforcement location.
- Interaction with concurrent downloads.
- Behavior under host aliases and redirects.
- Whether limits are shared between protocols.
- Tests for limit enforcement.

Do not assume that a documented per-host limit is enforced globally unless the implementation proves it.

### 4.2 Speed Limiting

Verify:

- Whether speed limiting is implemented.
- Where limits are enforced.
- Whether limits are per download, per worker, per host, or global.
- How bandwidth is measured.
- Whether throttling affects all supported protocols.
- How cancellation interacts with throttling.
- Whether configuration changes take effect immediately.
- Relevant tests.

Do not assume the limiter's scope or exact timing behavior from documentation alone.

### 4.3 Checksums and Integrity

Verify:

- Supported algorithms.
- Whether checksums are calculated during transfer or after completion.
- Whether expected checksums can be supplied externally.
- How mismatches are reported.
- Whether corrupted output is retained or removed.
- Whether verification is optional or mandatory.
- How checksum work affects resource consumption.

Do not claim that a download is integrity-verified merely because checksum-related code exists.

### 4.4 Proxy and Site-Specific Handling

Verify:

- Existing proxy support.
- Supported proxy protocols.
- Proxy configuration ownership.
- Authentication behavior.
- Site-specific rules and their source.
- Matching and precedence behavior.
- Redirect interactions.
- Security implications.
- Tests for supported configurations.

Do not invent a site-specific rule system or assume that proxy behavior is uniform across protocols.

### 4.5 Retry and Failure Handling

Verify:

- Retryable error classification.
- Retry limits and backoff behavior.
- Segment-specific retry behavior.
- Worker failure handling.
- Network interruption handling.
- Server-side failures.
- Authentication failures.
- Storage failures.
- Cancellation semantics.
- Terminal state transitions.

Do not assume that every failure is retryable or that every retry preserves the same transfer state.

### 4.6 Cancellation and Shutdown

Verify:

- Public cancellation entry points.
- Cancellation propagation.
- Worker termination.
- Network request cancellation.
- Pending write behavior.
- State persistence during cancellation.
- Resource cleanup.
- Engine shutdown behavior.
- Interaction with process termination.

Distinguish user cancellation, task shutdown, process death, and unexpected runtime failure.

## 5. Concurrency and Resource Ownership

The engine must be understood as a concurrent system, not merely a sequence of functions.

Inspect:

- Worker execution model.
- Thread and task ownership.
- Async runtime usage.
- Shared state.
- Synchronization primitives.
- Locks and channels.
- Cancellation tokens or equivalent mechanisms.
- File-handle ownership.
- Network connection ownership.
- Buffer allocation and release.
- Shutdown ordering.
- Potential deadlocks and resource leaks.

Questions to answer:

- Which component owns each worker?
- Who owns the download state?
- Which component owns output file handles?
- Can multiple workers write to the same file?
- How are byte ranges protected from conflicting writes?
- How is shared state synchronized?
- Can event consumers slow down transfer execution?
- What resources remain active after cancellation?

Do not introduce a new concurrency model before understanding the existing one.

## 6. Engine and Android Integration Boundary

The engine must remain reusable and independent of Android-specific framework code.

Investigate which existing engine capabilities can be integrated directly and which require an Android adapter.

Potential integration concerns include:

- Download creation.
- Download status observation.
- Progress reporting.
- Pause and resume.
- Cancellation.
- Completion and failure reporting.
- Persistent state recovery.
- File destination handling.
- Network configuration.
- Engine initialization and shutdown.

Requirements:

- Use verified existing APIs wherever practical.
- Do not expose internal worker or planner implementation details unnecessarily.
- Do not implement Android lifecycle behavior inside the portable engine.
- Do not create a parallel Android download scheduler.
- Do not duplicate transfer state management in Kotlin.
- Do not create a Rust application adapter unless the actual integration requires one.
- Define ownership and event delivery across the integration boundary.
- Verify whether the engine's existing persistence mechanism is sufficient for Android process recreation.

If Android storage requirements conflict with existing filesystem assumptions, investigate a narrow storage integration strategy rather than redesigning the engine prematurely.

## 7. Mandatory Local Source Audit

The following checklist must be completed against the checked-out repository before this document is treated as an implementation-accurate engine reference.

### 7.1 Establish Repository Context

- [ ] Identify the repository root.
- [ ] Record the current Git commit.
- [ ] Record the current branch and checked-out tag, if applicable.
- [ ] Inspect the working-tree state.
- [ ] Confirm whether the source corresponds to the intended upstream release.
- [ ] Identify whether the architecture documentation matches the checked-out revision.

### 7.2 Enumerate the Engine Source

- [ ] Verify whether `crates/engine` exists.
- [ ] Verify the actual crate name in its `Cargo.toml`.
- [ ] Enumerate the contents of `crates/engine/src`, if that path exists.
- [ ] Identify nested modules.
- [ ] Identify module declarations and re-exports.
- [ ] Identify target-specific source files.
- [ ] Identify build scripts and feature gates.
- [ ] Record the actual source tree.

Do not create missing paths to satisfy this checklist.

### 7.3 Inspect Crate Configuration

- [ ] Read the engine crate's `Cargo.toml`.
- [ ] Inspect the root workspace manifest.
- [ ] Inspect relevant dependency versions.
- [ ] Inspect enabled and optional features.
- [ ] Inspect target-specific dependencies.
- [ ] Inspect build scripts.
- [ ] Inspect relevant `Cargo.lock` entries.
- [ ] Identify Android-incompatible dependencies and assumptions.

### 7.4 Inspect the Public API

- [ ] Read the engine's `lib.rs`.
- [ ] Identify exported modules.
- [ ] Identify public functions and types.
- [ ] Trace public download entry points.
- [ ] Locate all relevant callers.
- [ ] Inspect request and configuration contracts.
- [ ] Inspect error types.
- [ ] Inspect event contracts.
- [ ] Identify which functionality is actually accessible to downstream crates.

Do not document a symbol as public merely because it exists in a private module.

### 7.5 Trace Core Implementations

- [ ] Trace resource probing.
- [ ] Trace transfer planning.
- [ ] Trace byte-range calculation.
- [ ] Trace worker creation and scheduling.
- [ ] Trace network request execution.
- [ ] Trace connection-limit enforcement.
- [ ] Trace speed-limiting behavior.
- [ ] Trace state persistence.
- [ ] Trace resume and recovery.
- [ ] Trace checksum calculation and verification.
- [ ] Trace proxy handling.
- [ ] Trace site-specific handling.
- [ ] Trace event production and consumption.
- [ ] Trace finalization and cleanup.

For every responsibility, record:

- Actual file path.
- Actual symbol names.
- Relevant callers.
- Inputs and outputs.
- Side effects.
- Error behavior.
- Tests.
- Platform dependencies.

### 7.6 Inspect Tests

- [ ] Read relevant unit tests.
- [ ] Read relevant integration tests.
- [ ] Inspect resume tests.
- [ ] Inspect range-handling tests.
- [ ] Inspect retry tests.
- [ ] Inspect cancellation tests.
- [ ] Inspect network-failure tests.
- [ ] Inspect storage-failure tests.
- [ ] Inspect checksum tests.
- [ ] Inspect concurrency tests.
- [ ] Identify untested critical scenarios.

Do not infer test coverage from test filenames alone.

### 7.7 Inspect Platform Compatibility

- [ ] Identify desktop-specific APIs.
- [ ] Identify operating-system assumptions.
- [ ] Inspect filesystem operations.
- [ ] Inspect networking and TLS backends.
- [ ] Inspect native dependencies.
- [ ] Inspect threading and async runtime assumptions.
- [ ] Inspect conditional compilation.
- [ ] Inspect target-specific feature selection.
- [ ] Identify potential Android compatibility blockers.

A portable-looking API does not prove that its implementation is portable.

## 8. Required Source Audit Deliverables

The local source audit should produce the following evidence-based documentation.

### 8.1 Verified Engine Source Map

Record:

- Actual crate path.
- Actual crate name.
- Actual source files.
- Actual module hierarchy.
- Actual public exports.
- Responsibilities verified for each module.

### 8.2 Public API Map

Record:

- Actual public entry points.
- Actual request and configuration types.
- Actual response and result types.
- Actual error contracts.
- Actual event definitions.
- Actual downstream consumers.

Do not include guessed or conceptual symbol names as actual APIs.

### 8.3 Download Execution Trace

Produce a source-backed execution trace from the public download entry point through:

- Request validation.
- Probing.
- Planning.
- Worker execution.
- State updates.
- Event delivery.
- Completion and finalization.

If the actual implementation differs from the conceptual lifecycle, document the difference.

### 8.4 State and Persistence Map

Record:

- State ownership.
- State representation.
- Persistence mechanism.
- Resume conditions.
- Recovery behavior.
- File ownership.
- Cleanup behavior.
- Consistency limitations.

### 8.5 Concurrency and Event Map

Record:

- Worker ownership.
- Threading or task model.
- Synchronization mechanisms.
- Event producers.
- Event consumers.
- Delivery guarantees.
- Cancellation behavior.
- Shutdown behavior.

### 8.6 Android Compatibility Assessment

Record:

- Required crates.
- Android build results.
- Incompatible dependencies.
- Platform-specific assumptions.
- Storage integration requirements.
- Networking and TLS considerations.
- Native library requirements.
- Remaining blockers.

## 9. Evidence Classification

Every finding must use one of the following classifications:

### VERIFIED

Directly established through the checked-out source, actual configuration, executed tests, or runtime verification.

Include source paths and symbols where possible.

### DOCUMENTED

Described by upstream documentation but not yet verified against the checked-out source.

### INFERRED

A reasonable conclusion derived from available evidence but not directly established.

Explain the evidence and required verification.

### UNKNOWN

The implementation or behavior has not been established.

### BLOCKED

The investigation cannot proceed safely because required evidence or access is missing.

### NOT APPLICABLE

The audited source confirms that a concept does not apply to the checked-out implementation or selected integration scope.

Do not use `VERIFIED` merely because a README or architecture document describes a capability.

## 10. Differences Between Documentation and Implementation

When the upstream documentation differs from the local implementation:

1. Record the documentation claim.
2. Record the checked-out revision.
3. Inspect the relevant implementation.
4. Trace actual callers.
5. Inspect tests and configuration.
6. Determine whether the behavior is absent, changed, renamed, conditional, or version-specific.
7. Update the engine documentation to reflect the verified implementation.
8. Preserve the original documentation discrepancy as an audit finding where relevant.

Do not modify upstream source simply to make it match documentation.

## 11. Rules Before Modifying Engine Code

Before changing engine functionality:

1. Identify the exact requirement.
2. Inspect the current implementation.
3. Trace all relevant callers and consumers.
4. Inspect related tests.
5. Identify concurrency and persistence implications.
6. Identify platform-specific implications.
7. Determine whether existing functionality can be reused.
8. Identify the smallest safe change.
9. Document affected modules and public contracts.
10. Obtain approval for changes that alter major engine responsibilities or public APIs.
11. Implement only the approved scope.
12. Run relevant tests and builds.
13. Report actual results and unresolved risks.

Do not create replacement download logic when the existing engine already provides the required capability.

Do not modify unrelated engine modules.

## 12. Android Reuse Assessment

Before integrating the engine into Android, classify each required capability:

| Capability | Assessment |
|---|---|
| Source implementation | Verified / Unknown |
| Existing public API | Verified / Missing / Unknown |
| Required dependencies | Verified / Unknown |
| Android compilation | Verified / Failed / Not tested |
| Android runtime behavior | Verified / Not tested |
| Integration work | Defined / Proposed / Unknown |
| Remaining blockers | Explicitly documented |

Possible outcomes:

- **Reusable directly:** Existing functionality and APIs satisfy the requirement.
- **Reusable through an adapter:** Existing functionality is suitable, but platform integration is required.
- **Requires a narrow extension:** Existing functionality is close, but a specific verified capability is missing.
- **Not Android-compatible:** A verified dependency or OS assumption prevents the intended use.
- **Unknown:** More source investigation or testing is required.

Do not classify a capability as reusable merely because its source code compiles on a desktop target.

## 13. Mandatory Audit Report Format

The final engine audit must contain:

### 1. Repository Context

- Release or tag.
- Git commit.
- Working-tree status.
- Documentation revision.

### 2. Actual Engine Structure

- Verified crate path and name.
- Actual source tree.
- Module responsibilities.
- Public exports.

### 3. Public API and Call Graph

- Actual entry points.
- Actual consumers.
- Request and configuration contracts.
- Error and event contracts.

### 4. Execution Lifecycle

- Verified download execution sequence.
- Differences from the documented conceptual lifecycle.
- Important side effects and failure paths.

### 5. Internal Responsibilities

- Probe.
- Planner.
- Workers.
- State and persistence.
- Connection limits.
- Speed limiting.
- Checksums.
- Proxy and site-specific handling.
- Events.
- Finalization.
- Cancellation and retries.

Include only capabilities verified in the checked-out source.

### 6. Tests and Build Evidence

- Tests inspected.
- Tests executed.
- Actual results.
- Untested scenarios.
- Target-specific build results.

### 7. Android Compatibility

- Reusable functionality.
- Incompatible dependencies.
- Required adapters.
- Unresolved integration questions.
- Verified blockers.

### 8. Findings and Recommendations

Classify conclusions as:

- `VERIFIED`
- `DOCUMENTED`
- `INFERRED`
- `UNKNOWN`
- `BLOCKED`
- `NOT APPLICABLE`

Provide evidence and next steps for every significant unresolved issue.

## 14. Final Engine Architecture Principles

1. Preserve and reuse the existing Rust engine.
2. Treat upstream documentation as a starting point, not implementation proof.
3. Verify the checked-out revision before relying on documented architecture.
4. Inspect actual public APIs and call paths before integration.
5. Understand concurrency, state ownership, and event delivery before modification.
6. Preserve download correctness and resumability.
7. Keep Android framework responsibilities outside the portable engine.
8. Do not invent modules, types, events, or contracts.
9. Do not create parallel download implementations.
10. Do not claim Android compatibility without target build and runtime evidence.

**Documented responsibility is not verified implementation.**

**Existing source before new abstractions.**

**Trace execution before changing behavior.**

**Verify the engine before integrating it into Android.**
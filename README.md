# Repository Structure and Architecture

## 1. Purpose

This document is the central guide to understanding the Mister Download Manager repository structure.

It explains:

- How the repository is organized.
- The responsibility of each Rust Crate.
- Where the main application logic lives.
- Where the download engine logic lives.
- How the major components relate to one another.
- Which parts are relevant to the Android port.
- Which architectural details still require local source-code verification.

This document describes the existing upstream architecture. It does not authorize restructuring, moving Crates, or introducing new architectural layers.

---

## 2. Repository Architecture Overview

Mister Download Manager is organized as a Rust workspace containing multiple Crates.

The project does not have one centralized `logic/` or `domain/` directory.

Instead, responsibilities are separated across Crates according to their technical purpose.

The two most important areas are:

| Area | Location | Responsibility |
|---|---|---|
| Application Logic | `crates/app` | Application state, orchestration, user interactions, and application-level behavior |
| Download Engine | `crates/engine` | Core download execution, planning, networking, workers, and download state |

Other Crates provide specialized functionality used by the application and its supporting tools.

### Conceptual architecture

```text
                    Mister Download Manager
                              |
                 +------------+------------+
                 |                         |
           Application                  Other Clients
             crates/app          cli / mcp / remote / nmhost
                 |                         |
                 +------------+------------+
                              |
                     Shared Infrastructure
                              |
                 +------------+------------+
                 |                         |
              Engine                     IPC
          crates/engine              crates/ipc
                 |
       +---------+----------+
       |         |          |
     Planner   Workers   Networking
       |
   Download Execution
```

This is a conceptual responsibility diagram, not a verified runtime call graph.

---

## 3. Workspace Structure

The local repository inspection confirmed the following Crate directories in version `v0.5.2`:

```text
crates/
├── app/
├── cli/
├── crash/
├── debrid/
├── engine/
├── extract/
├── geo/
├── ipc/
├── labserver/
├── mcp/
├── nmhost/
├── remote/
├── tools/
└── torrent/
```

Each Crate has its own source directory and may have additional tests, examples, assets, or data.

The root workspace is defined in `Cargo.toml`.

Its workspace configuration includes:

- Workspace members under `crates/*`.
- Shared package metadata.
- Shared dependency definitions.
- Workspace-level lint configuration.

Do not assume that all Crates are required by every application target.

---

## 4. Core Application and Engine

### 4.1 Application — `crates/app`

**Role:** Main desktop application layer.

The application Crate is responsible for application-level behavior rather than implementing the underlying download engine.

Its documented responsibilities include:

- Managing application state.
- Translating user actions into application operations.
- Handling menu, toolbar, keyboard, and IPC commands.
- Coordinating application features.
- Integrating with the engine.
- Presenting download information through the desktop interface.
- Managing application-level interactions and workflows.

The upstream architecture documentation also describes a headless execution mode in which application logic can run without rendering the normal desktop UI.

**Android relevance:**

This Crate is an important investigation target because it contains application-level orchestration.

However, its existing desktop UI implementation must not be assumed to be Android-compatible.

The Android application may require a different presentation layer while preserving reusable application behavior where technically appropriate.

Do not copy or port the entire desktop UI into Android without a verified technical reason.

### 4.2 Download Engine — `crates/engine`

**Role:** Core download functionality.

This Crate is the most important starting point for understanding the reusable download functionality.

Its documented responsibilities include:

- Download probing.
- Download planning.
- Segment planning.
- Download workers.
- Download execution.
- Resume and state management.
- Per-host connection limits.
- Speed limiting.
- Checksums and integrity-related operations.
- Proxy-related functionality.
- Networking and protocol handling.

The engine is designed to provide download functionality independently of the user interface.

**Verified local directory structure:**

```text
crates/engine/
├── src/
│   ├── planner/
│   └── sites/
└── tests/
    └── data/
```

This is only the directory structure confirmed by the local inspection command. It is not a complete inventory of the engine's source modules.

**Android relevance:**

The engine is the primary candidate for reuse in the Android application.

Its Android compatibility must be established through actual compilation and runtime tests, not inferred from the existence of Android-specific conditional compilation.

### 4.3 Application Logic vs. Download Logic

These two responsibilities must remain conceptually distinct.

| Concern | Responsible Area |
|---|---|
| Application state and orchestration | `crates/app` |
| User-facing desktop interactions | `crates/app` |
| Download planning | `crates/engine` |
| Download execution | `crates/engine` |
| Download workers | `crates/engine` |
| Download networking | `crates/engine` |
| Application-to-engine coordination | Application integration boundary |

The application coordinates the overall user-facing workflow.

The engine performs the underlying download operations.

Do not treat the engine as the entire application, and do not treat the application Crate as the implementation of all download mechanics.

---

## 5. Supporting Crates

The following responsibilities are described in the upstream architecture documentation.

Their exact implementation details and dependency relationships must be verified against the local `v0.5.2` source before making code changes.

| Crate | Documented Responsibility |
|---|---|
| `crates/cli` | Command-line interface and headless download operations |
| `crates/crash` | Crash-related functionality |
| `crates/debrid` | Debrid service integration |
| `crates/extract` | Extraction functionality |
| `crates/geo` | Geographic data and related functionality |
| `crates/ipc` | Inter-process communication and shared application infrastructure |
| `crates/labserver` | Development and testing server |
| `crates/mcp` | Model Context Protocol server |
| `crates/nmhost` | Browser native messaging host |
| `crates/remote` | HTTPS-based remote-control functionality |
| `crates/tools` | Supporting tools |
| `crates/torrent` | Torrent-related functionality |

### Important distinctions

- `crates/remote` is not an Android application.
- `crates/nmhost` is not the Android application entry point.
- `crates/cli` is not the Android user interface.
- `crates/labserver` must not be assumed to be a production application service.
- `crates/ipc` provides infrastructure and must not automatically be treated as the application's business logic.

A Crate should only be included in the Android build when its functionality is required and its platform compatibility has been verified.

---

## 6. Supporting Repository Directories

The repository also contains non-Crate directories.

Examples documented by the upstream repository include:

| Directory | General Purpose |
|---|---|
| `extension/` | Browser extension |
| `docs/` | Project documentation |
| `website/` | Project website |

These directories are separate from the Rust workspace's Crates.

Their current presence and exact contents should be verified locally before relying on them.

---

## 7. How the Main Components Work Together

The following describes the conceptual architecture documented by upstream.

### 7.1 Application Workflow

The application receives an action from the user or another supported integration.

The application layer coordinates that action and communicates with the appropriate functionality.

For download-related operations, the engine performs the underlying download work.

The application receives or processes engine events to update application state and presentation.

### 7.2 Download Engine Workflow

The documented conceptual download lifecycle is:

```text
Download Request
       |
       v
     Probe
       |
       v
    Planner
       |
       v
  Download Workers
       |
       v
  Host Limits / Speed Limiter
       |
       v
   Download State
       |
       v
      Events
       |
       v
   Finalization
```

This diagram explains the conceptual stages only.

It does not establish exact function names, source files, invocation order, or all internal dependencies.

Those details must be established by inspecting the actual engine source code.

### 7.3 Integration Boundaries

The application and supporting Crates use shared functionality through their established interfaces.

The engine should remain independent of desktop presentation concerns.

Any Android integration must preserve this separation rather than introducing Android UI dependencies into the reusable engine.

---

## 8. Android Porting Relevance

The target product is an Android-native application.

The repository currently has no confirmed Android application project in the local directory inspection.

The inspected search did not identify:

- Gradle settings or build files.
- `AndroidManifest.xml`.
- Kotlin source files.
- Java source files.

However, the engine contains Android-specific conditional compilation in some source locations.

This is evidence of partial Android-aware code paths, not proof that the engine or complete application builds and runs successfully on Android.

### Android investigation priorities

| Priority | Area | Investigation Goal |
|---|---|---|
| P0 | `crates/engine` | Establish whether the engine builds for Android targets |
| P0 | Engine dependencies | Identify unsupported or incompatible dependencies |
| P0 | Networking | Verify Android network-interface and connectivity behavior |
| P1 | `crates/app` | Identify reusable application logic independently of desktop UI |
| P1 | `crates/ipc` | Determine whether existing IPC functionality is suitable for Android |
| P1 | Storage | Verify Android storage and download persistence requirements |
| P1 | Lifecycle | Determine how download execution survives Android lifecycle events |
| P2 | Supporting Crates | Evaluate whether each integration is needed on Android |

### Architecture principle

Use Rust for the reusable download engine and suitable shared application logic.

Use Kotlin for the minimum Android Framework integration required by the platform.

Do not assume that desktop UI technology, desktop lifecycle behavior, or desktop infrastructure can be reused unchanged on Android.

---

## 9. Source-of-Truth and Evidence Policy

This document distinguishes between three types of information.

### VERIFIED — Local Source

A fact directly confirmed by inspecting the checked-out local repository.

Examples:

- Crate directories found under `crates/`.
- The local workspace configuration.
- Actual source files and definitions.
- Actual dependencies in local manifests.

### DOCUMENTED — Upstream Documentation

A responsibility or architectural relationship described by upstream documentation.

This does not automatically prove that the same implementation exists in the local checkout.

For this repository, architecture descriptions taken from `main` must be treated as documentation references until confirmed against `v0.5.2`.

### UNKNOWN — Not Yet Established

A detail that has not been verified.

Examples:

- Exact runtime call graphs.
- Exact public API contracts.
- Complete Crate dependency relationships.
- Android runtime compatibility.
- Whether a desktop-specific module can be reused.
- Exact ownership of individual business rules.

Never convert an UNKNOWN fact into a VERIFIED statement without evidence.

---

## 10. Mandatory Rules for Future Architecture Work

Before changing any Crate:

1. Inspect its `Cargo.toml`.
2. Inspect its source modules.
3. Identify its public APIs.
4. Trace the relevant call sites.
5. Inspect its tests.
6. Identify its direct and transitive dependencies where relevant.
7. Verify whether the behavior is desktop-specific or reusable.
8. Identify the impact on other Crates.
9. Confirm the Android target compatibility when applicable.
10. Propose the smallest change that satisfies the requirement.

Do not:

- Invent modules, files, APIs, or dependencies.
- Move Crates without a verified architectural reason.
- Duplicate existing engine functionality.
- Introduce a new domain layer merely because it is common in other architectures.
- Couple the engine to Android UI code.
- Refactor unrelated Crates during Android porting.
- Claim Android support based only on conditional compilation.
- Treat upstream `main` as identical to the checked-out `v0.5.2` revision.

---

## 11. Recommended Reading Order

For an engineer joining the project or beginning the Android port, use this order:

1. Root `Cargo.toml` — understand the workspace.
2. `crates/engine/Cargo.toml` — understand engine dependencies.
3. `crates/engine/src/` — investigate download functionality.
4. `crates/app/Cargo.toml` — understand application dependencies.
5. `crates/app/src/` — investigate application orchestration.
6. `crates/ipc/src/` — understand inter-process communication where relevant.
7. Supporting Crates — inspect only when required by the task.

This is an investigation order, not a required implementation sequence.

---

## 12. Final Architectural Understanding

The most important distinction in this repository is:

**`crates/app` manages the application.**

**`crates/engine` performs the core download work.**

The remaining Crates provide specialized capabilities and integrations.

The Android port should identify which existing capabilities can be reused, which desktop-specific parts must be replaced, and which platform integrations must be implemented natively.

The objective is not to redesign the upstream project.

The objective is to preserve its proven functionality and architectural boundaries while introducing the smallest technically justified Android-specific layer.

All implementation decisions must be based on the actual checked-out source code and verified build results.

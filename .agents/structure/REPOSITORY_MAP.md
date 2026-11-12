# Repository Map

## 1. Document Status and Purpose

**Status: INITIAL UPSTREAM REPOSITORY REFERENCE — LOCAL REVISION VERIFICATION REQUIRED**

This document provides an initial map of the upstream repository and its documented Rust crate responsibilities.

The repository map serves three purposes:

1. Establish an initial understanding of the upstream project structure.
2. Identify candidate components and their documented responsibilities.
3. Guide a source-backed audit of the exact checked-out revision before architectural or implementation decisions are made.

This document is not a verified description of the local checkout.

### Source-of-Truth Rules

The following sources have different authority:

| Source | Authority |
|---|---|
| Checked-out source tree | Authoritative for files and directories currently present |
| Checked-out `Cargo.toml` manifests | Authoritative for actual crate and workspace configuration |
| Checked-out Rust source | Authoritative for implemented behavior and APIs |
| Checked-out Git commit and tag | Authoritative for revision identity |
| Documentation at the matching revision | Evidence of documented design and intended behavior |
| Public repository `main` | Reference only; not proof of older release contents |
| This document | Initial investigation map, not implementation proof |

Never assume that `main` and the selected release contain identical files, crates, dependencies, or responsibilities.

## 2. Repository Identity

The upstream repository identified during initial preparation is:

`https://github.com/anefzaoui/misterdownloadmanager`

The selected target version during initial preparation was:

`v0.5.2`

The actual local checkout must be inspected before treating this version as confirmed.

### Required Repository Context

Record the following during the first local audit:

| Item | Required evidence |
|---|---|
| Repository root | Actual filesystem location |
| Remote URL | Git remote configuration |
| Current branch | Git branch information |
| Current commit | Full Git commit hash |
| Checked-out tag | Exact tag, if applicable |
| Working-tree status | Clean or modified |
| Selected release | Verified tag or commit |
| Documentation revision | Matching revision or unknown |

Do not claim that the local repository is at `v0.5.2` based solely on an earlier clone command or an intended checkout.

## 3. Public Repository Layout Observed During Initial Preparation

The public repository page showed the following top-level entries:

- `.cargo/`
- `.claude/`
- `.github/`
- `.impeccable/`
- `.well-known/`
- `art/`
- `crates/`
- `docker/`
- `docs/`
- `extension/`
- `i18n/`
- `tools/`
- `website/`

The public page also showed Cargo manifests and project documentation.

**Evidence classification: DOCUMENTED / OBSERVED ON PUBLIC REPOSITORY PAGE**

This list is an initial reference only.

It must not be interpreted as the complete directory tree of the selected local revision.

### Local Verification Requirements

For every listed entry:

- Verify whether it exists locally.
- Verify its actual type: directory, file, symlink, or another filesystem entry.
- Inspect its contents when relevant.
- Determine whether it belongs to the selected release.
- Identify whether it is build-time, runtime, documentation, distribution, development, or tooling-related.
- Determine whether it is relevant to the Android target.

Do not recreate missing upstream directories to match this reference.

Do not assume that every top-level directory is part of the application runtime.

## 4. Rust Crates Described by Public `main` Architecture Documentation

The following table summarizes crate paths and responsibilities described in `docs/ARCHITECTURE.md` on the public `main` branch during initial preparation.

<escape>| Path | Documented crate | Documented responsibility |
|---|---|---|
| `crates/engine` | `mdm-engine` | Single-file HTTP(S)/FTP(S) download engine |
| `crates/app` | `mdm-app` | Desktop egui application |
| `crates/cli` | `mdm-cli` | Command-line interface |
| `crates/mcp` | `mdm-mcp` | MCP server |
| `crates/remote` | `mdm-remote` | Remote-control server/page |
| `crates/nmhost` | `mdm-nmhost` | Browser native-messaging host |
| `crates/ipc` | `mdm-ipc` | IPC and related application integration |
| `crates/debrid` | `mdm-debrid` | Debrid provider APIs |
| `crates/extract` | `mdm-extract` | Archive extraction |
| `crates/geo` | `mdm-geo` | IP-to-country lookup |
| `crates/tools` | `mdm-tools` | Video/gallery external-tool integration |
| `crates/crash` | `mdm-crash` | Crash/freeze reporting |
| `crates/torrent` | `mdm-torrent` | BitTorrent/magnet support |
| `crates/labserver` | `mdm-labserver` | Fault-injecting test servers |</escape>

**Evidence classification: DOCUMENTED ON PUBLIC `main`; NOT YET VERIFIED AGAINST THE SELECTED LOCAL REVISION.**

The table must not be treated as proof that all listed crates exist in `v0.5.2`.

### Mandatory Verification for Each Entry

For every candidate crate:

1. Check whether the documented directory exists.
2. Locate its actual `Cargo.toml`.
3. Read the manifest's `[package]` section.
4. Verify the actual package name.
5. Determine whether it is a workspace member.
6. Inspect its `src/` tree.
7. Read its `lib.rs` or `main.rs`, where present.
8. Inspect its dependencies and features.
9. Inspect its actual callers and consumers.
10. Determine whether its documented responsibility is implemented.
11. Inspect tests and target-specific configuration.
12. Classify its relevance to Android.

If the path exists but the package name differs, document the discrepancy.

If the crate is absent, mark it absent from the checked-out revision. Do not invent a replacement path.

If a crate exists but is not a workspace member, investigate how it is built and referenced before classifying its role.

## 5. Repository Components Must Be Classified by Actual Role

The presence of a directory or crate does not automatically establish its architectural role.

Classify verified components using evidence from manifests, source code, build configuration, and callers.

Possible classifications include:

### 5.1 Core Engine

Components responsible for reusable download execution and related transfer mechanics.

The documented candidate is `crates/engine`.

Its exact implementation and public contracts are documented separately in:

`ENGINE_STRUCTURE.md`

### 5.2 User-Facing Applications

Components that provide user interfaces or application-level orchestration.

The documented candidate is `crates/app`, described as a desktop egui application.

Do not assume the existing desktop UI can be reused on Android.

### 5.3 Command-Line and Service Interfaces

Documented candidates include:

- `crates/cli`
- `crates/mcp`
- `crates/remote`

Verify their actual entry points, runtime requirements, dependencies, and integration boundaries.

Their existence does not establish that Android requires them.

### 5.4 Platform and Integration Components

Documented candidates include:

- `crates/nmhost`
- `crates/ipc`

Inspect their actual responsibilities and platform assumptions.

Do not assume browser native messaging, desktop IPC, or another integration mechanism is suitable for Android.

### 5.5 Optional Feature Components

Documented candidates include:

- `crates/debrid`
- `crates/extract`
- `crates/geo`
- `crates/tools`
- `crates/torrent`

Their inclusion in an Android application must be justified by explicit product requirements and verified technical compatibility.

Do not include them automatically merely because they exist upstream.

### 5.6 Diagnostics and Testing Infrastructure

Documented candidates include:

- `crates/crash`
- `crates/labserver`

Inspect their actual purpose, target dependencies, and intended consumers.

Testing infrastructure must not accidentally become a production runtime dependency.

## 6. Mandatory Local Repository Audit

The local audit must establish the actual repository layout and Cargo workspace structure.

Run the following commands from the repository root:

### 6.1 Identify the Checkout

```bash
pwd
git remote -v
git status --short --branch
git rev-parse HEAD
git describe --tags --always --dirty
git tag --points-at HEAD
```

Record the actual output.

Do not assume the branch name, tag, or commit.

### 6.2 Discover Repository Instructions and Cargo Manifests

```bash
find . -maxdepth 3 -type f \( \
  -name 'Cargo.toml' -o \
  -name 'AGENTS.md' -o \
  -name 'CLAUDE.md' \
\) -print | sort
```

Read applicable repository instruction files before investigating or modifying source.

Do not assume that instruction files exist merely because the search command includes them.

### 6.3 Enumerate Crate Files

```bash
find crates -maxdepth 3 -type f -print | sort
```

If the `crates/` directory does not exist, record that fact and investigate the actual workspace layout instead.

Do not treat a failed directory listing as evidence that the entire repository is invalid.

### 6.4 Inspect the Root Workspace Manifest

```bash
sed -n '1,260p' Cargo.toml
```

Inspect:

- `[workspace]`
- `members`
- `default-members`
- `exclude`
- `[workspace.dependencies]`
- `[workspace.package]`
- Workspace resolver configuration
- Shared features and dependency declarations
- Relevant target configuration

The actual manifest may contain additional sections or configuration beyond the displayed range. Inspect the complete file when necessary.

### 6.5 Inspect Every Discovered Crate Manifest

For every discovered crate manifest:

- Read the complete `Cargo.toml`.
- Verify `[package].name`.
- Identify its version and edition.
- Inspect `[lib]` and `[[bin]]` declarations.
- Inspect dependencies.
- Inspect development dependencies.
- Inspect build dependencies.
- Inspect optional dependencies.
- Inspect features.
- Inspect target-specific dependencies.
- Identify workspace-inherited values.

Do not assume that a directory name matches its Cargo package name.

### 6.6 Inspect the Cargo Workspace

Use the actual Cargo toolchain available in the checkout:

```bash
cargo metadata --format-version 1 --no-deps
```

Inspect the resulting workspace metadata.

Determine:

- Actual workspace root.
- Actual workspace members.
- Package names.
- Manifest paths.
- Target declarations.
- Dependency relationships.
- Workspace exclusions.

If the command fails, report the exact failure and investigate its cause.

Do not fabricate workspace membership from directory names.

### 6.7 Inspect Dependency Relationships

Use Cargo metadata and the actual manifests to establish dependency relationships.

Where appropriate, inspect:

```bash
cargo tree --workspace
```

For a specific package, use its verified package name:

```bash
cargo tree -p VERIFIED_PACKAGE_NAME
```

Do not replace `VERIFIED_PACKAGE_NAME` with an assumed package name.

Distinguish:

- Direct dependencies.
- Transitive dependencies.
- Development dependencies.
- Build dependencies.
- Optional dependencies.
- Target-specific dependencies.
- Feature-activated dependencies.

A dependency appearing in a lockfile does not by itself prove that a production Android target needs it.

## 7. Workspace and Dependency Architecture Audit

The repository audit must establish how components are connected.

### 7.1 Workspace Membership

For each discovered crate, record:

- Directory path.
- Package name.
- Workspace membership.
- Manifest path.
- Library or binary targets.
- Build configuration.
- Relevant features.

### 7.2 Dependency Direction

Inspect actual dependencies to determine:

- Which crates depend on the engine.
- Which crates depend on application-level components.
- Whether integration crates introduce platform-specific dependencies.
- Whether circular dependencies exist.
- Whether optional features change dependency direction.
- Whether shared abstractions are genuinely shared.

Do not infer dependency direction from crate names or folder structure.

### 7.3 Runtime Boundaries

Identify which components:

- Execute as standalone binaries.
- Provide reusable libraries.
- Start network services.
- Access the filesystem.
- Interact with operating-system APIs.
- Require external processes.
- Depend on desktop UI frameworks.
- Provide test-only functionality.

Use actual entry points, dependency declarations, and call paths.

### 7.4 Feature and Configuration Boundaries

Inspect:

- Cargo features.
- Conditional compilation.
- Target-specific dependencies.
- Environment variables.
- Configuration files.
- Build scripts.
- Runtime initialization.

Determine which functionality is included by default and which requires explicit feature activation.

Do not assume that a crate's presence means all of its functionality is enabled.

## 8. Android Relevance Assessment

The repository map must distinguish upstream existence from Android product relevance.

For every verified component, assess:

| Assessment | Meaning |
|---|---|
| Required | Explicitly needed by the Android product |
| Reusable | Existing functionality can be integrated with acceptable compatibility |
| Adapter required | Existing functionality is useful but needs a platform integration boundary |
| Optional | Could be included if a concrete product requirement justifies it |
| Excluded | Not needed in the current Android scope |
| Blocked | Compatibility or integration cannot yet be established |
| Unknown | Insufficient evidence for classification |

### Mandatory Questions

For each component:

1. Does the Android product require its functionality?
2. Is the component a library or a standalone executable?
3. Does it depend on desktop-only frameworks?
4. Does it require an external executable?
5. Does it rely on unsupported operating-system interfaces?
6. Does it have target-specific dependencies?
7. Can its existing public API be reused?
8. Does it require a narrow Android adapter?
9. Does including it increase native dependency or ABI complexity?
10. Is its inclusion justified by a documented product requirement?

### Important Constraints

- The product scope is Android only.
- Rust remains the primary implementation language for reusable core logic.
- Kotlin is reserved for necessary Android framework integration.
- Do not include every upstream crate by default.
- Do not move existing crates merely to create a new directory structure.
- Do not duplicate upstream functionality.
- Do not create replacement modules before investigating existing capabilities.
- Do not assume that a desktop executable can be embedded as an Android library.
- Do not assume that an existing library automatically compiles for Android.

## 9. Repository Map Deliverable

After completing the local audit, maintain a verified repository map.

The map should include the following information for every relevant component:

| Field | Required information |
|---|---|
| Path | Actual checked-out path |
| Package name | Verified Cargo package name, if applicable |
| Component type | Library, binary, tooling, documentation, assets, or other |
| Workspace status | Member, excluded, or not applicable |
| Responsibility | Source-backed responsibility |
| Main entry points | Verified public API or executable entry point |
| Consumers | Verified callers or dependents |
| Dependencies | Relevant direct and target-specific dependencies |
| Platform assumptions | Verified operating-system or runtime assumptions |
| Android relevance | Required, reusable, adapter required, optional, excluded, blocked, or unknown |
| Evidence | Source paths, manifest declarations, tests, and command results |

The final map must distinguish between:

- Verified components.
- Documented but unverified components.
- Components absent from the selected revision.
- Components whose responsibilities changed between revisions.
- Components not relevant to the Android target.

## 10. Handling Version Differences

When the selected local revision differs from public `main`:

1. Record the selected local commit and tag.
2. Record the source revision of the public architecture documentation.
3. Compare the actual workspace members.
4. Compare crate manifests and package names.
5. Compare relevant source directories.
6. Identify added, removed, renamed, or reorganized crates.
7. Identify changes in documented responsibilities.
8. Preserve version-specific findings.
9. Avoid applying `main` architecture assumptions to the older checkout.

The selected release remains the implementation source of truth for the current project.

Newer upstream changes may be useful references, but they must be identified as newer changes rather than silently incorporated into the local architecture description.

## 11. Rules for Repository Modifications

Before adding, removing, moving, or reorganizing a crate:

1. Inspect the actual workspace.
2. Inspect existing crate boundaries.
3. Inspect dependency relationships.
4. Identify the concrete architectural problem.
5. Determine whether an existing crate already owns the responsibility.
6. Assess the effect on workspace configuration.
7. Assess the effect on downstream consumers.
8. Assess the effect on upstream synchronization.
9. Assess build and Android compatibility implications.
10. Propose the smallest justified change.

Do not:

- Move upstream crates into an `original/` directory without a verified technical reason.
- Create a new crate solely to match a proposed folder tree.
- Rename packages without a demonstrated requirement.
- Duplicate existing functionality.
- Remove crates based only on their names.
- Change unrelated workspace members.
- Introduce new dependencies without investigating existing alternatives.
- Rewrite the workspace before understanding its dependency graph.

Preserve upstream crate boundaries wherever practical.

## 12. Required Audit Report

The completed repository audit must report:

### 1. Repository Identity

- Repository root.
- Remote URL.
- Branch.
- Commit.
- Tag.
- Working-tree state.

### 2. Actual Repository Layout

- Verified top-level directories.
- Relevant instruction files.
- Actual Cargo manifests.
- Actual crate source directories.

### 3. Workspace Structure

- Workspace members.
- Excluded packages.
- Package names.
- Library and binary targets.
- Relevant features.

### 4. Dependency Architecture

- Direct dependencies.
- Important transitive dependencies.
- Target-specific dependencies.
- Dependency direction.
- External tools and runtime requirements.

### 5. Component Responsibilities

- Source-backed responsibility for each relevant component.
- Actual entry points.
- Verified consumers.
- Documentation discrepancies.

### 6. Android Relevance

- Required components.
- Reusable components.
- Components requiring adapters.
- Optional components.
- Excluded components.
- Compatibility blockers.
- Unknowns requiring further investigation.

### 7. Findings and Recommendations

Classify each significant finding as:

- `VERIFIED`
- `DOCUMENTED`
- `INFERRED`
- `UNKNOWN`
- `BLOCKED`
- `NOT APPLICABLE`
- `PROPOSED`

Include evidence and the next required action.

## 13. Relationship to Other Architecture Documents

This document is the repository-level map.

It must remain consistent with the responsibilities of the following documents:

- `SYSTEM_ARCHITECTURE.md` — system-wide component relationships and architectural boundaries.
- `ENGINE_STRUCTURE.md` — detailed download engine responsibilities and implementation audit.
- `ANDROID_TARGET_STRUCTURE.md` — proposed Android target responsibilities and integration boundaries.
- `ARCHITECTURE_AND_REUSE.md` — rules for preserving upstream architecture and avoiding unnecessary restructuring.
- `NO_GUESSING_AND_SOURCE_OF_TRUTH.md` — evidence and source-of-truth requirements.

If these documents disagree:

1. Use the checked-out source and manifests to establish actual implementation facts.
2. Identify which document contains the outdated or unsupported statement.
3. Correct the relevant document.
4. Keep proposed architecture separate from verified upstream architecture.

## 14. Final Repository Architecture Principles

1. The checked-out repository determines the actual structure.
2. Cargo manifests determine package and workspace configuration.
3. Public `main` documentation is reference material, not proof of the selected release.
4. Directory names do not establish package names or responsibilities.
5. Dependency relationships must be inspected, not guessed.
6. Crate existence does not imply Android relevance.
7. Preserve upstream boundaries and avoid unnecessary relocation.
8. Reuse verified functionality before introducing new components.
9. Keep proposed Android architecture separate from the upstream repository map.
10. Record unknowns and blockers instead of filling gaps with assumptions.

**Map the repository that actually exists.**

**Understand dependencies before changing boundaries.**

**Select Android components by verified requirements, not by directory presence.**
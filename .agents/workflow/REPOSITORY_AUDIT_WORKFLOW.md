# Repository Audit Workflow

## 1. Purpose

This document defines the mandatory procedure for conducting a repository-wide engineering audit.

The audit establishes the actual structure, architecture, dependencies, capabilities, and technical constraints of the checked-out repository revision.

Its primary purpose is to replace assumptions and incomplete documentation with revision-specific evidence.

This workflow is particularly important before:

- Android porting.
- Major architectural changes.
- Dependency replacement.
- Engine reuse.
- Platform compatibility assessment.
- Repository restructuring.
- Long-term maintenance planning.

The audit must describe what actually exists in the checked-out revision, not what is expected to exist based on another branch, tag, release, or documentation snapshot.

## 2. Audit Rules

### 2.1 Read-Only Requirement

A repository audit is strictly read-only.

Do not:

- Modify source files.
- Modify manifests.
- Modify configuration.
- Reformat files.
- Generate source code.
- Add or remove dependencies.
- Change branches or tags.
- Reset the working tree.
- Delete or overwrite user files.
- Apply automatic fixes.

Do not run commands that intentionally modify the repository.

If a verification command may generate files or alter repository state, assess its effects first and avoid it unless explicitly authorized.

Build and test execution are separate from the read-only audit. If they are required, obtain authorization where necessary and isolate generated artifacts from the source audit.

### 2.2 Repository Source of Truth

The checked-out repository is authoritative.

Use:

1. Git revision and working-tree state.
2. Actual source files.
3. Cargo manifests.
4. Toolchain configuration.
5. Project instructions.
6. Tests.
7. Local architecture documentation.
8. External documentation only as supplementary evidence.

Do not substitute public `main` documentation for the checked-out revision.

### 2.3 Evidence Classification

Classify audit findings as:

- `VERIFIED`: Directly confirmed by the checked-out source, manifests, Git output, or executed commands.
- `DOCUMENTED`: Described in repository documentation but not independently verified in implementation.
- `INFERRED`: A reasonable conclusion derived from verified evidence.
- `UNKNOWN`: Not established by the available evidence.
- `BLOCKED`: Cannot be established because required information or access is unavailable.
- `PROPOSED`: A recommendation for future work, not an existing repository fact.

Every important architectural or compatibility claim must have an evidence classification.

### 2.4 No Invented Repository Details

Never invent:

- Crate names.
- Package names.
- Workspace members.
- Module paths.
- Public APIs.
- Dependencies.
- Feature flags.
- Build targets.
- Platform support.
- Runtime capabilities.
- Internal call relationships.

If a path or symbol does not exist in the checked-out revision, do not describe it as an existing component.

## 3. Phase 1 — Repository Identity and Revision

**Objective:** Establish exactly which repository and revision are being audited.

Run from the repository root.

### 3.1 Initial Commands

```bash
pwd
git rev-parse --show-toplevel
git remote -v
git status --short
git status --short --branch
git branch --show-current
git rev-parse HEAD
git describe --tags --always
git tag --points-at HEAD
```

Record:

- Repository root.
- Remote URLs.
- Current branch.
- Current commit hash.
- Closest descriptive tag.
- Tags pointing directly to the current commit.
- Working-tree modifications.
- Staged changes.
- Untracked files.

### 3.2 Revision Verification

Determine whether the checked-out commit corresponds to the expected release or tag.

Do not assume that:

- A branch name identifies a release.
- A nearby tag is the exact checked-out revision.
- A shallow clone contains complete history.
- A commit is clean because `git describe` prints a tag.
- Local modifications are included in the upstream release.

If the requested version cannot be established, report the limitation.

### 3.3 User Changes

Identify all existing user modifications.

Treat them as protected.

Do not clean, stash, reset, or overwrite them.

### Phase 1 Deliverable

A revision identity record containing the exact commit, branch, tags, remote, and worktree state.

## 4. Phase 2 — Repository Instructions and Documentation

**Objective:** Understand the repository's documented development rules and architecture.

### 4.1 Discover Instruction Files

Inspect relevant files such as:

- `AGENTS.md`.
- `CLAUDE.md`.
- Nested instruction files.
- `.agents/` documentation.
- Other repository-specific agent instructions.

Use the actual discovered paths.

Example discovery command:

```bash
find . -type f \( \
  -name 'AGENTS.md' -o \
  -name 'CLAUDE.md' \
\) -print | sort
```

Do not assume all instruction files are at the repository root.

### 4.2 Inspect Core Documentation

Inspect the files that actually exist, including where applicable:

- `README.md`.
- `CONTRIBUTING.md`.
- `CLAUDE.md`.
- `docs/ARCHITECTURE.md`.
- `docs/` architecture references.
- Build documentation.
- Platform documentation.
- Development instructions.

Record missing expected files as `UNKNOWN` or `NOT PRESENT`, rather than inventing their contents.

### 4.3 Compare Documentation with Implementation

For each important documented capability:

1. Identify the documented component.
2. Locate the corresponding source implementation.
3. Inspect its public entry points.
4. Trace relevant callers.
5. Inspect associated tests.
6. Determine whether the capability is implemented and integrated.

Distinguish:

- Documented capability.
- Declared API.
- Implemented behavior.
- Integrated behavior.
- Compiled behavior.
- Tested behavior.
- Runtime-verified behavior.

These are different evidence levels.

### Phase 2 Deliverable

A documentation and instruction inventory, including discrepancies between documented architecture and actual implementation.

## 5. Phase 3 — Workspace and Build Configuration Audit

**Objective:** Establish the actual Rust workspace and build configuration.

### 5.1 Discover Cargo Manifests

Use:

```bash
find . -type f -name 'Cargo.toml' -print | sort
```

For a more focused crate inventory:

```bash
find crates -type f -name 'Cargo.toml' -print | sort
```

Use the actual repository layout. Do not assume a `crates/` directory exists.

When searching for instruction files and Cargo manifests together, use explicit grouping:

```bash
find . -maxdepth 3 -type f \( \
  -name 'Cargo.toml' -o \
  -name 'AGENTS.md' -o \
  -name 'CLAUDE.md' \
\) -print | sort
```

### 5.2 Inspect Root Cargo Manifest

Read the root `Cargo.toml` if it exists.

Inspect:

- Workspace declaration.
- Workspace members.
- Workspace exclusions.
- Resolver version.
- Workspace package metadata.
- Shared dependencies.
- Workspace lints.
- Build configuration.
- Profiles.
- Patches.
- Replacements.
- Target-specific configuration.

Do not infer workspace membership from directory names.

### 5.3 Inspect Toolchain Configuration

Inspect actual toolchain configuration files, including:

- `rust-toolchain.toml`.
- `rust-toolchain`.
- Relevant Cargo configuration.
- Target-specific linker configuration.
- Environment configuration documented by the repository.

Record:

- Required Rust channel.
- Required components.
- Required targets.
- Target-specific settings.
- Native toolchain requirements.
- Build environment assumptions.

Do not claim a toolchain is installed merely because the repository declares it.

### 5.4 Cargo Metadata

When authorized and when the environment supports it, use:

```bash
cargo metadata --format-version 1 --no-deps
```

This helps establish package metadata and workspace membership without resolving the full dependency graph.

If full dependency information is needed, use appropriate Cargo metadata or dependency-tree commands and record their exact scope.

Distinguish:

- Manifest declarations.
- Workspace membership.
- Resolved dependencies.
- Target-specific dependencies.
- Feature-activated dependencies.

Do not treat `--no-deps` output as a complete dependency-resolution report.

### 5.5 Build Configuration

Inspect:

- `.cargo/config.toml`.
- `.cargo/config`.
- Build scripts.
- `build.rs` files.
- Target-specific configuration.
- Feature definitions.
- Conditional compilation.
- Native linking configuration.

Identify build requirements that may affect Android compatibility.

### Phase 3 Deliverable

A verified workspace and build configuration inventory.

## 6. Phase 4 — Crate Inventory and Public Entry Points

**Objective:** Identify actual packages and understand their responsibilities.

### 6.1 Enumerate Workspace Members

Use the root workspace declaration and Cargo metadata to establish actual membership.

For each package, record:

- Package name.
- Manifest path.
- Package version.
- Workspace membership.
- Library target.
- Binary targets.
- Build scripts.
- Declared features.
- Target-specific dependencies.

A directory containing a `Cargo.toml` is not automatically a workspace member.

### 6.2 Inspect Each Crate Manifest

Read each relevant crate's `Cargo.toml`.

Inspect:

- Package metadata.
- Library and binary declarations.
- Dependencies.
- Development dependencies.
- Build dependencies.
- Optional dependencies.
- Feature definitions.
- Target-specific dependencies.
- Rust version requirements.
- Native dependency requirements.

Identify whether dependencies are:

- Direct.
- Optional.
- Development-only.
- Build-time.
- Target-specific.
- Workspace-inherited.

### 6.3 Inspect Public Entry Points

Inspect actual public entry points, including where applicable:

- `src/lib.rs`.
- `src/main.rs`.
- Public module declarations.
- Public types.
- Public traits.
- Public functions.
- Re-exports.
- Feature-gated exports.

Do not infer public API availability from an internal module.

For each crate, identify:

1. Its declared purpose.
2. Its actual public surface.
3. Its internal responsibilities.
4. Its known callers.
5. Its external dependencies.
6. Its platform-specific assumptions.
7. Its relevant tests.

### 6.4 Inspect Module Structure

Inspect the actual source tree.

Example:

```bash
find crates -maxdepth 3 -type f | sort
```

Adjust the path and depth to the actual repository structure.

A shallow listing is only an initial inventory. Inspect deeper files where required to understand important responsibilities and execution paths.

Do not treat folder names as proof of architectural boundaries.

### Phase 4 Deliverable

A crate inventory with evidence-backed responsibilities and public entry points.

## 7. Phase 5 — Dependency Map

**Objective:** Build an accurate dependency map from actual Cargo manifests and configuration.

### 7.1 Direct Dependency Map

For every workspace package, inspect its manifest and record:

- Internal workspace dependencies.
- External dependencies.
- Optional dependencies.
- Build dependencies.
- Development dependencies.
- Target-specific dependencies.
- Feature relationships.

Distinguish internal crate dependencies from external library dependencies.

### 7.2 Dependency Direction

Construct a directed graph using declared dependencies.

For every important relationship, identify:

- Consumer.
- Dependency.
- Dependency type.
- Optional feature conditions.
- Target restrictions.

Do not assume that a dependency relationship represents a runtime call.

Cargo dependency graphs and runtime execution graphs answer different questions.

### 7.3 Resolved Dependency Graph

When the environment permits, inspect the resolved graph using appropriate Cargo commands.

For example:

```bash
cargo tree --workspace
```

For more detailed analysis, inspect relevant package, feature, or target-specific dependency trees.

Record:

- Resolved versions.
- Duplicate dependency versions.
- Feature activation.
- Native dependencies.
- Potentially platform-restricted dependencies.

Do not claim a complete dependency graph when only direct manifest declarations were inspected.

### 7.4 Dependency Risk Assessment

Identify dependencies that may affect:

- Android compilation.
- Native linking.
- Filesystem access.
- Networking.
- TLS.
- Threading.
- Async execution.
- Process execution.
- Desktop windowing.
- System integration.
- Platform-specific APIs.

A dependency name alone is not enough to classify it as incompatible.

Inspect its actual features, target configuration, build scripts, and platform requirements.

### Phase 5 Deliverable

A dependency map distinguishing declared relationships, resolved dependencies, and runtime relationships where independently established.

## 8. Phase 6 — Feature and Runtime Architecture Audit

**Objective:** Understand how major capabilities are actually implemented and connected.

### 8.1 Identify Major Components

Derive component boundaries from actual code, manifests, and documented responsibilities.

For each component, inspect:

- Entry points.
- Public APIs.
- Internal modules.
- Callers.
- Consumers.
- State ownership.
- Persistence.
- Events.
- Error handling.
- External dependencies.

### 8.2 Trace Important Execution Paths

For capabilities relevant to the project, trace actual execution paths.

Examples may include:

- Download creation.
- Download execution.
- Download cancellation.
- Retry and recovery.
- Progress reporting.
- Persistence.
- Configuration loading.
- Application initialization.
- Shutdown.
- External communication.

These are audit targets, not assumptions that every capability exists.

Record exact source paths and relevant symbols.

### 8.3 State and Lifecycle Ownership

Identify:

- Which component owns the authoritative state.
- Which components can mutate it.
- How state is persisted.
- How changes are propagated.
- How cancellation is handled.
- What happens after failures.
- What happens during shutdown or restart.

Distinguish documented behavior from verified implementation.

### 8.4 Events and Communication

Inspect actual communication mechanisms:

- Internal events.
- Channels.
- Callbacks.
- IPC.
- HTTP APIs.
- Native messaging.
- Background workers.

For each relevant mechanism, record:

- Producer.
- Consumer.
- Payload contract.
- Ordering assumptions.
- Error handling.
- Lifecycle ownership.

Do not infer runtime behavior solely from a dependency or type declaration.

### Phase 6 Deliverable

An evidence-backed component and execution-flow map.

## 9. Phase 7 — Platform and Android Relevance Audit

**Objective:** Assess the Android relevance of existing components without prematurely changing the architecture.

The product target is Android only.

Do not expand the audit into iOS, React Native, or Flutter requirements.

### 9.1 Platform-Specific Code Inventory

Inspect:

- Conditional compilation.
- Platform-specific dependencies.
- OS APIs.
- Filesystem operations.
- Process execution.
- Native libraries.
- Windowing and desktop UI.
- Networking.
- TLS.
- Threading.
- Async runtime assumptions.
- Background execution.
- Environment access.

Record the exact evidence supporting each finding.

### 9.2 Component Portability Classification

Classify each relevant component as:

- `PORTABLE`: Evidence supports reuse on Android without a platform-specific redesign.
- `CONDITIONALLY PORTABLE`: Reuse may be possible after identified adaptations or dependency changes.
- `DESKTOP-ONLY`: The component depends on desktop-specific functionality that cannot be reused directly as an Android component.
- `UNKNOWN`: Insufficient evidence to classify.

A component must not be labeled portable merely because it is written in Rust.

### 9.3 Android Compatibility Evidence

Inspect:

- Target-specific dependency support.
- Native library availability.
- ABI requirements.
- Filesystem assumptions.
- Network stack requirements.
- Threading and async behavior.
- Platform APIs.
- Build scripts.
- FFI boundaries.

Do not claim Android compatibility without appropriate target-build evidence.

### 9.4 Android Runtime Constraints

Identify requirements that need separate proof, including:

- Foreground execution.
- Background restrictions.
- Process termination.
- Process recreation.
- Notifications.
- Storage access.
- Network changes.
- Cancellation.
- Persistent download state.

A Rust background thread is not proof of reliable Android background execution.

### 9.5 Reuse Assessment

For every potentially reusable component, report:

- What can be reused.
- Why it appears reusable.
- What must be adapted.
- What remains unknown.
- Which proof is required before adoption.

Do not propose moving crates or creating a new application architecture unless the evidence justifies it.

### Phase 7 Deliverable

A component portability matrix and an Android compatibility blocker list.

## 10. Phase 8 — Knowledge Pack Reconciliation

**Objective:** Identify differences between the checked-out revision and existing `.agents` knowledge documents.

The knowledge pack is guidance, not repository evidence.

### 10.1 Compare Repository Identity

Compare:

- Expected repository.
- Checked-out revision.
- Documented release.
- Actual source layout.
- Actual workspace membership.

### 10.2 Compare Repository Structure

For each documented path:

- Verify whether it exists.
- Verify its actual purpose.
- Identify renames.
- Identify removals.
- Identify additions.
- Identify changed boundaries.

### 10.3 Compare Crate Inventory

Compare documented crate names and paths against actual workspace members.

Record:

- Confirmed crates.
- Missing crates.
- Renamed crates.
- New crates.
- Non-workspace packages.
- Changed responsibilities.

Do not silently update the knowledge pack based on assumptions.

### 10.4 Compare Architectural Claims

Review claims about:

- Engine responsibilities.
- Application orchestration.
- Desktop integration.
- CLI.
- Remote control.
- IPC.
- Browser integration.
- Supporting components.
- Platform boundaries.

Classify each claim as:

- `CONFIRMED`.
- `DOCUMENTED ONLY`.
- `PARTIALLY CONFIRMED`.
- `CONTRADICTED`.
- `NOT PRESENT`.
- `UNKNOWN`.

### 10.5 Revision-Specific Differences

Every difference must include:

- Knowledge-pack document.
- Documented claim.
- Actual checked-out evidence.
- Difference classification.
- Potential impact.
- Recommended documentation action.

Do not modify the knowledge pack during the audit.

Documentation updates are a separate task requiring an explicit scope.

### Phase 8 Deliverable

A revision-specific reconciliation report.

## 11. Phase 9 — Risk and Gap Assessment

**Objective:** Identify engineering risks before implementation begins.

Evaluate relevant risks in:

### 11.1 Architecture

- Unclear ownership.
- Unexpected dependency direction.
- Tight coupling.
- Duplicate functionality.
- Unverified abstractions.
- Platform leakage.

### 11.2 Build and Dependencies

- Unsupported targets.
- Native build requirements.
- Feature conflicts.
- Toolchain constraints.
- Duplicate dependencies.
- Unresolved dependency versions.

### 11.3 Runtime

- Lifecycle assumptions.
- Concurrency.
- State recovery.
- Cancellation.
- Error propagation.
- Persistence consistency.
- Background execution.

### 11.4 Security

- Untrusted input.
- Secret handling.
- Network security.
- Filesystem access.
- Native boundaries.
- Dependency risks.

### 11.5 Maintainability

- Divergence from upstream.
- Unnecessary restructuring.
- Unclear ownership.
- Missing tests.
- Documentation drift.

For each significant risk, record:

- Evidence.
- Classification.
- Impact.
- Likelihood when reasonably assessable.
- Mitigation.
- Whether it blocks the next stage.

Do not assign artificial precision to risks that cannot be assessed.

## 12. Audit Execution Commands

The following commands are initial discovery and inspection tools. Adjust them to the actual repository layout.

### Repository State

```bash
pwd
git rev-parse --show-toplevel
git remote -v
git status --short --branch
git rev-parse HEAD
git describe --tags --always
git branch --show-current
git tag --points-at HEAD
```

### Instruction and Manifest Discovery

```bash
find . -maxdepth 3 -type f \( \
  -name 'Cargo.toml' -o \
  -name 'AGENTS.md' -o \
  -name 'CLAUDE.md' \
\) -print | sort
```

### Source Inventory

```bash
find crates -maxdepth 3 -type f -print | sort
```

Use only if `crates/` exists. Adjust the path and depth as needed.

### Workspace Configuration

```bash
sed -n '1,300p' Cargo.toml
```

Inspect the actual toolchain file if present:

```bash
sed -n '1,200p' rust-toolchain.toml
```

### Cargo Metadata

```bash
cargo metadata --format-version 1 --no-deps
```

### Dependency Tree

```bash
cargo tree --workspace
```

The metadata and dependency commands may depend on Cargo availability and repository configuration. Record failures accurately.

These commands are not a substitute for reading the actual manifests and source files.

## 13. Required Audit Deliverables

The audit is incomplete until its findings are organized into the following deliverables.

### 13.1 Repository Identity Report

Include:

- Repository root.
- Remote.
- Branch.
- Commit hash.
- Tag information.
- Worktree state.
- Version certainty.

### 13.2 Instruction and Documentation Inventory

Include:

- Discovered instruction files.
- Relevant architecture documents.
- Build documentation.
- Missing expected documents.
- Documentation discrepancies.

### 13.3 Workspace and Crate Inventory

For each actual workspace member, include:

- Package name.
- Manifest path.
- Targets.
- Features.
- Public entry points.
- Main responsibilities.
- Evidence classification.

### 13.4 Dependency Map

Include:

- Internal crate dependencies.
- External dependencies.
- Optional dependencies.
- Target-specific dependencies.
- Native dependencies.
- Relevant feature relationships.

Clearly distinguish manifest declarations from resolved dependencies.

### 13.5 Architecture and Execution Map

Include:

- Component responsibilities.
- Dependency direction.
- Important execution paths.
- State ownership.
- Persistence boundaries.
- Event and communication mechanisms.
- Failure propagation.

### 13.6 Android Relevance Matrix

Use this structure:

| Component | Actual Responsibility | Portability | Evidence | Required Proof |
|---|---|---|---|---|
| Verified component | Verified responsibility | PORTABLE / CONDITIONALLY PORTABLE / DESKTOP-ONLY / UNKNOWN | Source paths | Build or runtime checks |

Do not populate this table with unverified component names.

### 13.7 Knowledge Pack Difference Report

Include:

- Confirmed claims.
- Partially confirmed claims.
- Contradictions.
- Missing components.
- New components.
- Revision-specific differences.
- Recommended documentation updates.

### 13.8 Risks and Blockers

List:

- Architectural risks.
- Build risks.
- Dependency risks.
- Platform risks.
- Runtime risks.
- Security concerns.
- Missing evidence.
- Approval requirements.

### 13.9 Recommended Follow-Up Plan

For each follow-up action, include:

- Exact objective.
- Relevant verified paths.
- Required evidence.
- Dependencies.
- Expected deliverable.
- Whether approval is required.

Order actions by dependency and priority.

## 14. Audit Completion Criteria

The audit is complete only when:

- [ ] Repository identity is established.
- [ ] Current revision is recorded.
- [ ] Working-tree changes are documented.
- [ ] Applicable project instructions are inspected.
- [ ] Relevant documentation is reviewed.
- [ ] Workspace membership is established.
- [ ] Toolchain configuration is inspected.
- [ ] Actual crate manifests are inspected.
- [ ] Public entry points are identified.
- [ ] Dependency relationships are mapped to the stated evidence level.
- [ ] Relevant execution paths are traced.
- [ ] State and lifecycle ownership are assessed where relevant.
- [ ] Platform-specific assumptions are identified.
- [ ] Android portability is classified with evidence.
- [ ] Knowledge-pack differences are documented.
- [ ] Risks and blockers are recorded.
- [ ] Follow-up steps are prioritized.
- [ ] No repository files were modified during the audit.

If any required area cannot be completed, report the audit as partial and explain the limitation.

## 15. Final Audit Report Format

Use the following structure.

### A. Executive Summary

- Repository identity.
- Audited revision.
- Overall audit status.
- Major verified findings.
- Major blockers.

### B. Repository State

- Root.
- Remote.
- Branch.
- Commit.
- Tags.
- Worktree state.

### C. Verified Repository Structure

- Workspace members.
- Crates.
- Public entry points.
- Important source paths.

### D. Architecture Findings

- Component responsibilities.
- Dependency direction.
- Execution paths.
- State ownership.
- Integration boundaries.

### E. Build and Dependency Findings

- Toolchain.
- Build configuration.
- Direct dependencies.
- Resolved dependencies.
- Native requirements.
- Platform-specific constraints.

### F. Android Relevance

- Portability matrix.
- Reusable components.
- Required adapters.
- Build blockers.
- Runtime proof requirements.

### G. Knowledge Pack Reconciliation

- Confirmed claims.
- Documentation-only claims.
- Contradictions.
- Missing or changed components.

### H. Risks and Unknowns

- Severity where assessable.
- Evidence.
- Impact.
- Required resolution.

### I. Recommended Next Steps

- Ordered follow-up actions.
- Exact source paths.
- Required approvals.
- Expected verification.

### J. Audit Integrity Statement

Explicitly state:

- Whether the audit was read-only.
- Whether any files were modified.
- Which commands were actually executed.
- Which checks were not executed.
- Which conclusions remain unverified.

## 16. Final Engineering Principles

**Audit the checked-out revision, not an assumed version.**

**Read manifests before drawing dependency conclusions.**

**Inspect public entry points before claiming API availability.**

**Trace execution before claiming runtime behavior.**

**Separate documented architecture from implemented architecture.**

**Classify Android portability using evidence, not language assumptions.**

**Reconcile the knowledge pack against the actual repository.**

**Never modify files during a read-only audit.**

**Report unknowns and blockers instead of filling gaps with assumptions.**
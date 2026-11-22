# No Guessing and Source of Truth Rules

## 1. Authoritative Source of Truth

- The checked-out source code and its actual build manifests are the primary source of truth.
- The current Git revision determines which implementation is being investigated.
- Repository documentation must be evaluated against the checked-out revision.
- README files, architecture documents, examples, comments, generated documentation, and external references are supporting evidence, not automatic proof of current implementation behavior.
- Never assume that documentation describes the checked-out code accurately.
- Never assume that the latest upstream implementation matches an older release or tag.
- Never assume that a feature described in the README is implemented, enabled, or functional in the current revision.
- Never treat a proposed architecture, folder tree, issue description, or previous AI-generated explanation as proof of implementation.

When sources disagree, prioritize direct evidence from the checked-out implementation and its actual build configuration. Investigate the discrepancy and report it.
 
## 2. No Invented Repository Details

Never invent or assume the existence of:

- Files or directories.
- Crates or Cargo workspace members.
- Rust modules or Kotlin packages.
- Functions, methods, structs, enums, traits, or constants.
- Public APIs or exported symbols.
- Types, fields, variants, or default values.
- Events, callbacks, and message channels.
- DTOs, payloads, or response structures.
- Database tables, persistence mechanisms, or storage formats.
- Dependencies, libraries, and framework integrations.
- Build scripts, commands, targets, or configuration options.
- Tests or test coverage.
- Android components or platform capabilities.
- Existing implementation behavior.

A name mentioned in a previous conversation or proposed design is not evidence that the corresponding symbol exists in the repository.

If a required symbol or file cannot be found, report that it was not verified. Do not silently create an assumed equivalent.

## 3. Mandatory Repository Inspection

Before relying on any repository symbol or implementation detail:

1. Identify the actual repository root.
2. Inspect the current Git revision and working-tree state.
3. Inspect the relevant directory and file.
4. Read the complete relevant definition, not just its name.
5. Inspect its imports and dependencies.
6. Search for its callers and usages.
7. Inspect related types and contracts.
8. Inspect relevant tests.
9. Inspect applicable Cargo manifests, build scripts, and feature configurations.
10. Verify whether the symbol is actually compiled or enabled for the relevant target.

Do not rely on filenames or symbol names alone.

For example, finding a module named `download` does not prove that it implements a particular download capability. Its implementation, call paths, and tests must be inspected.

## 4. Symbol Definition and Usage Verification

Before modifying or extending an existing symbol:

- Locate its authoritative definition.
- Identify its visibility and ownership.
- Determine its actual input and output types.
- Inspect all relevant callers.
- Inspect how its results and errors are handled.
- Identify any side effects.
- Inspect relevant asynchronous and threading behavior.
- Identify target-specific compilation conditions.
- Determine whether changing it affects other crates or application components.

Do not infer a function's contract from its name.

Do not infer a type's purpose from its fields alone.

Do not assume that a public symbol is used only by the component currently being modified.

If usage cannot be traced completely, document the investigation limits before making a potentially breaking change.

## 5. Revision and Version Verification

Before using external documentation, upstream examples, or information from another repository revision:

1. Identify the checked-out Git commit.
2. Identify the current branch, if applicable.
3. Identify the checked-out tag or release, if applicable.
4. Determine whether the documentation refers to the same revision.
5. Compare relevant source paths and APIs when revisions differ.
6. Record any version-specific discrepancies.

Rules:

- Never assume that `main` matches the checked-out release.
- Never copy an API from `main` into an older release without verifying that it exists there.
- Never assume that a dependency version or feature flag is identical across releases.
- Never use documentation for a different version as proof of current behavior.
- If version matching cannot be established, mark the external information as contextual rather than verified.

The checked-out revision takes precedence over assumptions derived from newer or older upstream versions.

## 6. Build Manifest and Configuration Verification

Build configuration is part of the implementation contract.

Before relying on a dependency, feature, target, or build capability, inspect the actual relevant configuration, including where applicable:

- Root `Cargo.toml`.
- Crate-level `Cargo.toml` files.
- `Cargo.lock`.
- Cargo workspace configuration.
- Build scripts.
- Target-specific dependencies.
- Conditional compilation attributes.
- Cargo features.
- Rust toolchain configuration.
- Android Gradle configuration.
- Android manifests.
- Native build and linking configuration.
- Environment-dependent build settings.

Rules:

- Do not claim a dependency is available merely because it appears in an unrelated manifest.
- Do not assume a workspace dependency is enabled in every crate.
- Do not assume a feature is active because it is declared.
- Do not assume a target-specific dependency is compiled for Android.
- Do not assume a successful dependency resolution proves successful compilation.
- Do not assume that a successful compilation proves runtime compatibility.

Distinguish declared configuration from configuration that was actually used during a build.

## 7. Evidence Classification

Every significant technical statement must be classified according to the strength of its evidence.

### Verified

Use `VERIFIED` only when directly supported by evidence such as:

- Checked-out source definitions.
- Actual call sites and usage paths.
- Build manifests and configuration.
- Executed build output.
- Executed test results.
- Reproducible runtime observations.

Where practical, identify the relevant file, symbol, command, or test.

### Inferred

Use `INFERRED` when a conclusion is technically reasonable but has not been directly established.

Examples:

- A likely architectural responsibility based on several related modules.
- An expected integration approach based on existing abstractions.
- A potential Android limitation that has not been tested in the target environment.

Explain the evidence supporting the inference and what would be required to verify it.

### Unknown

Use `UNKNOWN` when the available evidence is insufficient to establish the answer.

Examples:

- An undocumented runtime behavior.
- An uninspected dependency.
- An unavailable build environment.
- An API whose implementation or consumers cannot be located.

Do not convert unknown information into a confident technical statement.

### Blocked

Use `BLOCKED` when progress requires missing information, unavailable access, an unresolved dependency, or an explicit architectural decision.

A blocker must state:

1. What is missing.
2. Why it is required.
3. What cannot safely proceed.
4. What evidence or decision would unblock the work.

### Proposed

Use `PROPOSED` for a suggested implementation, architecture, or technical decision that has not been approved or verified.

Never describe a proposal as an existing repository capability.

## 8. Evidence Quality and Traceability

- Prefer direct source evidence over secondary descriptions.
- Prefer executed verification over theoretical compatibility claims.
- Prefer actual call paths over assumptions based on module names.
- Prefer version-matched documentation over latest-version documentation.
- Prefer reproducible commands and test output over statements that something should work.
- Reference the relevant file paths and symbols when explaining implementation behavior.
- Include line references when they can be established reliably.
- Do not fabricate line numbers, test names, build output, or source references.
- Distinguish evidence from interpretation.
- Preserve enough investigation detail for another engineer to reproduce the conclusion.

A technical claim should be traceable to its supporting evidence.

## 9. Handling Missing or Incomplete Evidence

When evidence is unavailable:

- State precisely what could not be verified.
- Explain why that missing information matters.
- Identify the next inspection or test required.
- Do not invent a likely implementation.
- Do not create replacement types or contracts based on assumptions.
- Do not silently introduce fallbacks that change behavior.
- Do not report incomplete investigation as successful verification.

If a task depends on an unknown contract, stop the affected implementation until the contract is established.

It is acceptable to complete independent parts of a task while reporting a specific blocked part, provided that doing so does not introduce assumptions or compromise correctness.

## 10. Conflicting Evidence

When different sources provide conflicting information:

1. Identify the conflicting claims.
2. Identify the revision and context of each source.
3. Inspect the checked-out implementation.
4. Inspect relevant build configuration.
5. Trace actual usages.
6. Execute a targeted test or build when necessary.
7. Record the result and any remaining uncertainty.

Do not choose whichever explanation appears more convenient.

Examples of conflicting evidence include:

- README describes a feature that is absent from source code.
- Architecture documentation describes an older crate structure.
- A function signature differs between `main` and the checked-out release.
- A feature is declared but disabled for Android.
- A test passes on desktop but the Android implementation uses different code.
- A comment describes behavior that the implementation no longer performs.

Report unresolved conflicts rather than hiding them.

## 11. Feature and Capability Claims

Never claim that a feature is implemented, available, supported, or production-ready without evidence appropriate to that claim.

Distinguish the following states:

1. **Documented:** Mentioned in project documentation.
2. **Declared:** Present in source or build configuration.
3. **Implemented:** Relevant implementation exists.
4. **Integrated:** Connected to the relevant application or execution path.
5. **Compiled:** Successfully compiled for the specified target.
6. **Tested:** Relevant tests were executed successfully.
7. **Runtime Verified:** Observed working in the specified runtime environment.
8. **Validated:** Required functional and failure scenarios were verified against defined acceptance criteria.
9. **Production Ready:** Operational, security, compatibility, and release requirements have been satisfied.

These states are not interchangeable.

For example:

- A Rust crate compiling on Linux does not prove Android compatibility.
- A Rust library compiling for Android does not prove that the Android application can load it.
- An Android APK building successfully does not prove that background downloads work.
- A test passing does not prove that every related failure scenario is handled.
- A documented feature does not prove that the feature is enabled or integrated.

Always state the exact level of verification achieved.

## 12. Test and Build Evidence

- Never claim that a test passed unless it was actually executed and its result inspected.
- Never claim that a build succeeded based only on a manifest inspection.
- Never invent test coverage.
- Never claim that a test exists without locating it.
- Never claim that an entire test suite passed when only selected tests were run.
- Never hide skipped tests, ignored tests, or unavailable test environments.
- Distinguish compilation, linking, packaging, installation, and runtime execution.
- Report the exact command or relevant configuration used for important verification.
- Report failures and warnings that materially affect the conclusion.
- Identify what remains untested.

If a command was not executed, state that it was not executed.

## 13. External Information and Upstream References

External information may support investigation but must not replace repository verification.

Rules:

- Identify the version or revision to which external information applies.
- Prefer official documentation and upstream source references.
- Do not assume an external tutorial describes the current project architecture.
- Do not copy third-party examples without checking dependency versions and API compatibility.
- Treat issue comments and community discussions as supporting evidence, not definitive implementation contracts.
- Verify licensing and dependency compatibility before adopting external code.
- Never allow external documentation to override actual checked-out source behavior without investigating the discrepancy.

When external information cannot be matched to the current revision, label it as contextual or unverified.

## 14. No Fabricated Investigation Results

Never claim to have:

- Inspected a file that was not opened.
- Searched the entire repository when only selected paths were checked.
- Traced all callers when the search was incomplete.
- Executed a build or test that was not run.
- Tested an Android device or emulator that was not used.
- Verified a network or storage scenario that was not reproduced.
- Confirmed a dependency's Android compatibility without relevant evidence.
- Updated a local repository that was not accessible.
- Created or modified a file that was not actually written.

Report the actual scope of investigation and tool access.

If the local repository is inaccessible, request the necessary source files, command output, or local-agent investigation results. Do not pretend to have local access.

## 15. Mandatory Investigation Workflow

Before implementation, follow this evidence-driven sequence:

### Phase A — Establish Context

- Identify the repository root.
- Identify the checked-out revision.
- Inspect the Git working-tree state.
- Identify the relevant product target and requested behavior.

### Phase B — Inspect the Source

- Locate relevant files.
- Read actual definitions.
- Inspect imports and dependencies.
- Trace callers and consumers.
- Inspect related types and contracts.
- Inspect tests and configuration.

### Phase C — Establish Existing Behavior

- Trace the actual execution path.
- Identify side effects and error handling.
- Identify target-specific behavior.
- Distinguish documented behavior from implemented behavior.

### Phase D — Evaluate the Requirement

- Compare the requested behavior with the existing implementation.
- Identify what can be reused.
- Identify what is missing.
- Identify risks and dependencies.
- Mark all material unknowns.

### Phase E — Plan or Block

- Propose the smallest evidence-supported change.
- Explain affected files and components.
- Identify verification requirements.
- Request approval when required.
- Stop if a critical contract or architectural fact is unknown.

### Phase F — Verify

- Execute relevant builds and tests.
- Inspect actual output.
- Verify behavior at the required target level.
- Record failures and untested scenarios.

### Phase G — Report

- Separate verified facts from inferences.
- Identify unresolved unknowns and blockers.
- List actual modifications.
- Provide evidence for significant conclusions.

## 16. Mandatory Technical Report Format

For significant investigation or implementation work, use the following structure:

### 1. Repository Context
- Repository version.
- Git commit or tag.
- Working-tree status.
- Relevant build target.

### 2. Evidence
- Files and symbols inspected.
- Relevant definitions and usages.
- Manifest and dependency findings.
- Tests and configuration inspected.

### 3. Existing Behavior
- Verified execution path.
- Existing contracts.
- Relevant side effects.
- Known limitations.

### 4. Findings
Classify important conclusions as:

- `VERIFIED`
- `INFERRED`
- `UNKNOWN`
- `BLOCKED`
- `PROPOSED`

### 5. Impact Assessment
- Affected files and components.
- Dependency implications.
- Compatibility risks.
- Existing behavior at risk.

### 6. Implementation or Investigation Result
- Actual changes, if any.
- Exact commands executed.
- Actual build and test outcomes.
- Unexecuted checks.

### 7. Remaining Work
- Unresolved questions.
- Missing evidence.
- Required decisions.
- Next verification steps.

## 17. Final Rule

**Evidence is mandatory. Assumptions are not implementation facts.**

The checked-out source code and actual build configuration are authoritative.

When evidence is missing, investigate further or report a blocker.

Never invent repository structure, implementation contracts, capabilities, test results, or platform support.

Never claim more verification than was actually achieved.

**Inspect before relying. Trace before changing. Verify before claiming.**
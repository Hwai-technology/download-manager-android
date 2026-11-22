# Architecture and Code Reuse Rules

## 1. Architectural Integrity

- Preserve the upstream project's established architecture unless a verified technical requirement justifies a change.
- Treat the existing repository structure, crate boundaries, module responsibilities, and dependency direction as architectural contracts that must be understood before modification.
- Do not redesign the architecture merely to make it look cleaner or more consistent with a proposed folder structure.
- Do not assume that a different architecture is better simply because it introduces more separation, abstraction, or layers.
- Every architectural change must solve a concrete, identified problem.
- Prefer the smallest change that satisfies the requirement while preserving existing behavior.
- Architectural consistency and functional correctness take priority over cosmetic organization.

## 2. Preserve Upstream Crate Boundaries 

- Preserve existing upstream Rust crate boundaries.
- Preserve the current responsibilities of each crate unless a concrete technical reason requires a change.
- Respect the existing dependency direction between crates.
- Do not introduce circular dependencies.
- Do not make lower-level reusable crates depend on application-level or UI-specific crates.
- Do not move responsibilities between crates without tracing their existing callers and dependencies.
- Do not merge or split crates merely to simplify the apparent project structure.
- Do not rename crates, modules, or public APIs without an explicit technical justification.
- Before changing a crate boundary, inspect its public API, internal modules, dependencies, tests, and consumers.

A crate boundary is not just a directory arrangement. It may represent an important separation of responsibilities, compilation features, dependency constraints, and reuse contracts.

## 3. Download Engine Independence

- The download engine must remain independent of UI implementation and Android lifecycle code.
- The engine must not directly depend on Android Activities, Services, notification APIs, or other Android framework components.
- Do not introduce UI state management into the engine.
- Do not make the engine depend on Android presentation or navigation concerns.
- Keep reusable download behavior inside the existing engine architecture whenever practical.
- Android-specific execution and platform integration must be handled outside the platform-independent engine through explicit integration boundaries.
- Do not introduce Android-specific assumptions into reusable download algorithms.
- Preserve the engine's ability to be tested independently of Android UI and framework components.

The engine should expose or retain the smallest practical integration surface needed by the Android application, based on verified existing APIs and actual integration requirements.

## 4. Reuse Existing Engine Functionality

- Reuse the existing upstream download engine instead of creating a parallel implementation.
- Before implementing any download-related functionality, investigate whether the repository already provides it.
- Inspect existing modules, public APIs, internal helpers, and tests before introducing new code.
- Extend existing functionality when the requirement genuinely fits its responsibility.
- Prefer adapting existing engine capabilities over duplicating them in Android-specific code.
- Do not independently reimplement functionality that already exists in the upstream engine without a documented technical reason.
- Do not replace a working implementation merely because another implementation appears simpler.
- Preserve established behavior, error handling, concurrency guarantees, persistence semantics, and retry behavior unless the requirement explicitly demands a verified change.

Before implementing a new capability, establish:

1. Whether the capability already exists.
2. Where its responsibility currently belongs.
3. Whether its existing API is sufficient.
4. Whether an adapter or narrow extension is sufficient.
5. Whether a new implementation is genuinely necessary.
6. What existing behavior could be affected.

If these points cannot be established from the repository, investigate further before implementing.

## 5. Do Not Relocate Upstream Code Without Justification

- Do not relocate upstream crates into an `original/` directory merely to separate upstream code from Android application code.
- Do not create artificial copies of upstream crates.
- Do not duplicate the upstream workspace to preserve an untouched copy alongside a modified implementation.
- Do not change the repository root or Cargo workspace organization solely to satisfy a proposed directory layout.
- Do not introduce an `original/`, `legacy/`, `upstream/`, or similarly named source hierarchy without a concrete technical requirement.
- Preserve upstream paths and workspace relationships unless the actual build or integration requirements justify changing them.

If source isolation is genuinely required, investigate alternatives first, such as:

- Git history and version control.
- Small, isolated integration changes.
- Existing Cargo workspace features.
- Explicit dependency boundaries.
- Separate Android-specific crates where justified.
- Documented patching or upstream synchronization strategies.

Any relocation proposal must explain:

1. The concrete problem it solves.
2. Why the existing structure cannot solve that problem.
3. The effect on Cargo workspace membership.
4. The effect on relative paths and build scripts.
5. The effect on imports and dependencies.
6. The effect on upstream updates and maintenance.
7. The migration and regression risks.

Do not perform the relocation without explicit approval when it changes the repository's fundamental organization.

## 6. Android-Specific Adapter Boundaries

- Keep Android-specific APIs behind clearly defined Android integration boundaries.
- Do not allow Android framework dependencies to leak into platform-independent engine modules.
- Use adapters only where they provide a real boundary between platform-specific behavior and reusable logic.
- Do not create adapters that simply forward calls without providing meaningful isolation or translation.
- Do not introduce an abstraction for a hypothetical future platform.
- Do not create generic platform interfaces when only one concrete Android implementation exists and no real separation is needed.
- Keep Android lifecycle, storage, notification, permission, and connectivity integration outside the download engine unless an existing upstream contract explicitly requires otherwise.

Before introducing an adapter, identify:

- The platform-specific responsibility.
- The consumer of that responsibility.
- The required input and output contracts.
- The lifecycle and ownership requirements.
- The error-handling behavior.
- Whether an existing abstraction already provides the same boundary.

## 7. Dependency Direction and Dependency Management

- Preserve the established dependency direction.
- Lower-level engine components must not acquire unnecessary dependencies on application-level components.
- Avoid dependency cycles between crates or modules.
- Do not add a dependency when existing project capabilities are sufficient.
- Do not add multiple libraries that provide overlapping functionality without a justified requirement.
- Evaluate Android target compatibility before adding native dependencies.
- Inspect existing dependency versions and feature flags before changing Cargo manifests.
- Avoid unnecessary dependency upgrades.
- Do not enable broad default features without investigating their transitive dependencies and platform implications.
- Do not introduce desktop-only dependencies into Android build paths.
- Keep dependency changes scoped to the crate that owns the relevant responsibility whenever the workspace permits it.

Every new dependency must have an identified purpose, compatibility assessment, maintenance consideration, and justification.

## 8. New Crate and Module Creation

A new crate or module is allowed only when it represents a justified architectural responsibility.

Before creating one, establish:

1. What distinct responsibility it owns.
2. Why that responsibility does not belong in an existing module or crate.
3. Which components will depend on it.
4. Which dependencies it requires.
5. Whether its boundary reduces coupling or improves isolation.
6. Whether the same result can be achieved through an existing API or a smaller extension.
7. Whether its existence creates unnecessary build or maintenance complexity.

Rules:

- Do not create crates simply to match a proposed folder tree.
- Do not split a module only to increase the number of files.
- Do not create empty or placeholder modules without an approved implementation purpose.
- Do not introduce generic `core`, `common`, `shared`, `utils`, or `platform` crates without a clearly defined responsibility.
- Do not move code into a new module when doing so creates more indirection without meaningful separation.
- Prefer existing crate and module conventions when adding justified functionality.
- Keep ownership of each responsibility explicit.

A new crate is an architectural decision, not a file-organization preference.

## 9. Avoid Broad Refactoring

- Avoid broad refactoring when implementing a focused feature or integration requirement.
- Do not change unrelated modules or crates.
- Do not rename unrelated symbols.
- Do not reformat entire files or repositories unnecessarily.
- Do not change public APIs merely to improve naming or consistency.
- Do not combine unrelated cleanup tasks with functional implementation.
- Do not replace existing working mechanisms without demonstrating why replacement is necessary.
- Do not change desktop behavior as an incidental consequence of Android integration.
- Do not weaken existing tests to accommodate a refactor.

For each proposed change, identify:

- The direct requirement.
- The affected files and crates.
- Existing behavior that may change.
- Dependencies and consumers.
- Regression risks.
- Verification required.

If a focused change unexpectedly requires broad architectural modifications, stop and present the reason and impact before proceeding.

## 10. Public API and Internal Contract Preservation

- Treat existing public APIs as compatibility contracts until their consumers and intended usage are understood.
- Inspect all relevant callers before changing function signatures, public types, error variants, or exported modules.
- Avoid breaking changes unless explicitly required and approved.
- Preserve established error semantics and ownership rules.
- Do not invent new DTOs, enums, interfaces, callback contracts, or data structures when existing definitions already satisfy the requirement.
- Reuse existing types when appropriate.
- Avoid exposing internal implementation details through public APIs.
- Keep cross-crate and Rust/Android integration contracts explicit and minimal.

Any public API change must include an impact assessment covering all known consumers and relevant tests.

## 11. Cargo Workspace and Build Structure

- Preserve the existing Cargo workspace organization unless a verified build or integration requirement justifies a change.
- Inspect the root `Cargo.toml` and relevant crate manifests before modifying workspace configuration.
- Understand workspace members, dependency declarations, feature flags, profiles, target-specific dependencies, and build scripts.
- Do not add every existing crate to the Android application build by default.
- Determine which crates are actually required by the Android product.
- Avoid pulling desktop-only crates and dependencies into Android build paths.
- Do not change workspace resolver settings, profiles, or dependency versions without understanding their effects.
- Preserve existing build configurations for unaffected targets.
- Validate workspace changes using the relevant Cargo commands and Android target builds.

Do not infer workspace membership or dependency relationships from directory names alone.

## 12. Upstream Synchronization and Maintainability

- Preserve the ability to understand and maintain the relationship with the upstream project.
- Keep Android-specific changes distinguishable from upstream functionality where practical.
- Avoid unnecessary modifications to upstream source files.
- Prefer small, traceable patches over widespread source changes.
- Record significant upstream modifications and their technical reasons.
- Identify potential conflicts with future upstream updates.
- Do not assume that a newer upstream revision is compatible with existing Android integration.
- Before updating upstream code, compare affected APIs, dependencies, crate boundaries, and behavior.
- Preserve existing Android-specific requirements during upstream synchronization.

The goal is to retain the benefits of upstream development rather than creating an unnecessarily divergent fork.

## 13. Mandatory Architecture Investigation

Before making architectural changes:

1. Identify the exact repository version and current Git state.
2. Inspect the relevant source files and Cargo manifests.
3. Trace the existing dependency direction.
4. Identify the ownership of the affected responsibility.
5. Inspect public APIs and all relevant consumers.
6. Inspect tests and build configurations.
7. Determine whether existing functionality can be reused.
8. Identify the smallest viable change.
9. Evaluate alternative solutions.
10. Document affected crates, modules, dependencies, and compatibility implications.
11. Present an implementation plan for changes that affect multiple components or architectural boundaries.
12. Implement only the approved scope.
13. Run relevant tests and builds.
14. Report the actual changes and verification results.

Do not proceed with an architectural assumption when repository evidence is missing.

## 14. Architecture Decision Records

Significant architectural decisions must be documented when they affect:

- Crate boundaries.
- Dependency direction.
- Public APIs.
- Engine responsibilities.
- Android integration boundaries.
- Persistence ownership.
- Threading or asynchronous execution.
- Native dependency selection.
- Cargo workspace organization.
- Upstream synchronization strategy.

Each decision record should include:

- Context and identified problem.
- Verified repository facts.
- Decision.
- Alternatives considered.
- Reasons for selection.
- Consequences and trade-offs.
- Affected crates and modules.
- Compatibility and migration implications.
- Verification requirements.
- Whether the decision is approved, proposed, or unresolved.

Do not present a proposed decision as an approved architectural rule.

## 15. Mandatory Reporting and Change Discipline

For every significant architecture or reuse task, report:

### Investigation
- Repository version and Git state.
- Files and crates inspected.
- Existing implementation identified.
- Existing dependency and API boundaries.

### Decision
- Reuse, extension, adapter, or new implementation.
- Why the selected approach is appropriate.
- Why broader alternatives were rejected.

### Impact
- Files and crates affected.
- Public APIs or dependencies changed.
- Existing behavior at risk.
- Upstream compatibility implications.

### Verification
- Commands actually executed.
- Tests and builds actually completed.
- Results and failures.
- Untested areas.
- Remaining risks.

Clearly distinguish:

- **Verified:** Confirmed directly from source code or executed checks.
- **Inferred:** Technically plausible but not yet proven.
- **Unknown:** Not established by available evidence.
- **Proposed:** Suggested architecture awaiting approval.
- **Approved:** Explicitly accepted architectural decision.

Never claim that a change preserves architecture or upstream compatibility without checking the relevant implementation and dependencies.

## 16. Final Architectural Principles

The project must follow these principles:

1. Preserve the upstream architecture by default.
2. Reuse the existing download engine.
3. Keep the engine independent of UI and Android lifecycle concerns.
4. Preserve crate boundaries and dependency direction.
5. Isolate Android-specific behavior behind justified integration boundaries.
6. Avoid unnecessary crates, modules, dependencies, and abstractions.
7. Never reorganize source code merely to match a proposed folder tree.
8. Avoid broad refactoring and unrelated changes.
9. Preserve upstream maintainability and updateability.
10. Require repository evidence and explicit justification for architectural changes.

**Architecture must emerge from verified responsibilities and dependency boundaries, not from a desired directory layout.**

**Reuse before replacement. Extension before duplication. Evidence before restructuring.**
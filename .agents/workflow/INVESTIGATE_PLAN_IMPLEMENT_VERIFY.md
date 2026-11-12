# Investigation → Plan → Implementation → Verification

## 1. Purpose

This document defines the mandatory engineering workflow for all repository tasks.

The objective is to ensure that every change is:

- Based on verified source-code evidence.
- Consistent with the existing architecture.
- Limited to the actual task scope.
- Safe for existing behavior and public contracts.
- Supported by relevant tests and verification.
- Reported accurately and transparently.

This workflow applies to:

- Bug investigations.
- Feature implementation.
- Refactoring.
- Architecture changes.
- Dependency changes.
- Platform porting.
- Performance improvements.
- Security fixes.
- Build and tooling changes.
- Documentation changes.

No implementation task should bypass investigation and verification.

## 2. Core Engineering Rules

### 2.1 Source of Truth

The checked-out repository is the primary source of truth.

Inspect actual:

- Source files.
- Manifests.
- Configuration.
- Project instructions.
- Tests.
- Callers.
- Dependency definitions.
- Git revision.
- Working-tree changes.

Do not invent:

- File paths.
- Modules.
- Crates.
- Types.
- Functions.
- Structs.
- Enums.
- Fields.
- Events.
- Error variants.
- API contracts.
- Dependency relationships.
- Runtime behavior.
- Test results.

When evidence is insufficient, report the unknown or blocker.

### 2.2 Evidence Classification

Classify important findings as:

- `VERIFIED`: Directly confirmed by source, manifests, tests, command output, or runtime evidence.
- `DOCUMENTED`: Described by project documentation but not independently verified in implementation.
- `INFERRED`: Derived from available evidence but not directly established.
- `UNKNOWN`: Not yet established.
- `BLOCKED`: Cannot be established because required evidence or environment access is unavailable.
- `PROPOSED`: A suggested solution that has not been implemented or approved.

Do not present an inference as a verified fact.

### 2.3 Minimal Complete Changes

The implementation must be the smallest complete solution that satisfies the task.

Minimal does not mean incomplete.

A correct fix must cover the relevant behavior, error handling, state transitions, and tests without introducing unrelated changes.

Avoid:

- Unnecessary abstractions.
- Duplicate implementations.
- Broad refactoring.
- Speculative compatibility layers.
- Unrequested dependency changes.
- Unrelated formatting.
- Placeholder code.
- Dead code introduced for hypothetical future requirements.

### 2.4 Architecture Preservation

Preserve:

- Existing module and crate boundaries.
- Dependency direction.
- Public interfaces.
- Existing data contracts.
- Established error-handling patterns.
- Existing lifecycle behavior.
- Platform separation.
- Persistence semantics.

Change these only when the task requires it and the investigation justifies it.

### 2.5 No False Verification Claims

Never claim that a command, test, build, or runtime scenario succeeded unless it was actually executed and its result was observed.

Distinguish:

- Planned.
- Attempted.
- Executed.
- Passed.
- Failed.
- Blocked.
- Not run.

A successful compilation does not prove runtime correctness.

A passing unit test does not prove all integration behavior.

## 3. Phase 1 — Investigation (Read-Only)

<badge color="info">Mandatory</badge>

**Objective:** Understand the current implementation, establish the actual problem or requested change, and identify its full impact before modifying files.

No source-code implementation changes are permitted during this phase.

### 3.1 Confirm Repository Identity and State

Inspect:

- Repository root.
- Git remote.
- Current branch.
- Current commit.
- Checked-out tag.
- Working-tree status.
- Existing staged changes.
- Existing unstaged changes.
- Relevant untracked files.

Determine whether the repository contains user modifications.

Do not:

- Reset the working tree.
- Discard user changes.
- Overwrite existing modifications.
- Assume untracked files are disposable.
- Change branches or revisions without authorization.

Existing user changes must be preserved.

### 3.2 Read Project Instructions

Before inspecting or modifying relevant implementation files, identify and read applicable instructions.

Inspect relevant:

- `AGENTS.md`.
- `CLAUDE.md`.
- `.agents/` documents.
- Repository README files.
- Architecture documents.
- Feature-specific instructions.
- Build and contribution instructions.

Follow the instruction hierarchy and scope applicable to the target files.

Do not assume that a project instruction applies globally if its documented scope is narrower.

### 3.3 Understand the Task

Translate the request into explicit engineering requirements.

Identify:

- Expected behavior.
- Current behavior, if known.
- User-visible impact.
- Relevant components.
- Constraints.
- Acceptance criteria.
- Explicit exclusions.
- Whether the request authorizes implementation or requires a review-first workflow.

Distinguish the requested outcome from the user's suggested implementation.

A suggested implementation is not automatically the correct technical solution.

### 3.4 Inspect Relevant Source

Inspect the actual implementation before relying on symbol names or behavior.

Depending on the task, inspect:

- Definitions.
- Callers.
- Consumers.
- Related modules.
- Shared helpers.
- Interfaces.
- Types.
- Enums.
- Error handling.
- Configuration.
- Feature flags.
- Platform-specific code.
- Initialization.
- Shutdown behavior.

Trace symbols to their actual definitions.

Do not rely only on search-result snippets or file names.

### 3.5 Trace Data and Control Flow

Follow the relevant execution path across component boundaries.

Inspect:

- Input creation.
- Input validation.
- Transformation.
- Internal calls.
- State changes.
- Persistence.
- Event production.
- Event consumption.
- External side effects.
- Response generation.
- Error propagation.
- Cleanup.

Identify the actual source of truth for every relevant piece of state.

Do not assume that the component receiving a request owns the state it modifies.

### 3.6 Inspect Persistence and State Transitions

When the task involves state or stored data, investigate:

- State ownership.
- Creation.
- Updates.
- Deletion.
- Persistence timing.
- Transaction boundaries.
- Consistency requirements.
- Recovery.
- Concurrent access.
- Cache interaction.
- Event ordering.

Identify whether a change can leave partially updated state.

Do not introduce new persistence semantics without understanding existing behavior.

### 3.7 Inspect Events and External Side Effects

When relevant, trace:

- Event definitions.
- Event producers.
- Event consumers.
- Payload contracts.
- Ordering.
- Delivery guarantees.
- Duplicate handling.
- Retry behavior.
- Cancellation.
- External integrations.

Inspect side effects such as:

- Network operations.
- Filesystem writes.
- Notifications.
- IPC messages.
- Background tasks.
- Native calls.
- Process execution.

Do not assume that a successful local operation means its downstream side effects also succeeded.

### 3.8 Inspect Tests and Feature Flags

Inspect:

- Existing unit tests.
- Integration tests.
- Regression tests.
- Test helpers.
- Fixtures.
- Feature-gated tests.
- Build configurations.
- Target-specific test behavior.

Determine which existing tests cover the affected behavior and which important scenarios are missing.

Do not invent test infrastructure when existing patterns are sufficient.

### 3.9 Identify Regression Risks

Analyze possible effects on:

- Existing callers.
- Public APIs.
- Shared components.
- Other platforms.
- Persistent state.
- Event consumers.
- Concurrent operations.
- Error handling.
- Build configurations.
- Optional features.
- External integrations.

Identify both direct and indirect impact.

### 3.10 Investigation Deliverable

Before planning, establish:

1. Current verified behavior.
2. Requested behavior.
3. Relevant source paths.
4. Actual execution flow.
5. Relevant contracts.
6. Existing tests.
7. Root cause, if established.
8. Suspected causes, if not established.
9. Regression risks.
10. Unresolved questions and blockers.

### Phase 1 Exit Criteria

- [ ] Repository revision and working-tree state are checked.
- [ ] Applicable project instructions are read.
- [ ] Task requirements are understood.
- [ ] Relevant definitions and usages are inspected.
- [ ] Execution and data flow are traced.
- [ ] Persistence and event behavior are inspected where relevant.
- [ ] Existing tests are reviewed.
- [ ] Regression risks are identified.
- [ ] Findings are classified by evidence level.
- [ ] No implementation changes were made.

If the investigation cannot establish the required facts, report the blocker rather than guessing.

## 4. Phase 2 — Plan

<badge color="info">Required before implementation</badge>

**Objective:** Convert investigation findings into a precise, reviewable implementation plan.

The plan must be based on evidence from Phase 1.

### 4.1 Root Cause or Current-State Findings

Explain:

- What currently happens.
- What should happen.
- Where the behavior originates.
- Why the observed behavior occurs, if the root cause is verified.
- Which findings remain hypotheses.
- What cannot yet be established.

If the root cause is unknown, say so explicitly.

Do not invent a root cause to make the plan appear complete.

### 4.2 Evidence and Exact Source Paths

List the relevant files and explain why each matters.

For every important path, identify its actual role based on source inspection.

Where practical, include:

- Relevant symbols.
- Callers.
- Consumers.
- Tests.
- Manifest entries.
- Configuration.
- Relevant line ranges.

Do not list files merely because their names look related.

### 4.3 Proposed File Changes

Separate proposed changes into:

- Files to modify.
- Files to add.
- Files that may require changes depending on verification.
- Files explicitly out of scope.

For each proposed file, explain:

- Why it must change.
- What responsibility it owns.
- What behavior will change.
- Whether it affects a public contract.
- Which dependencies it has.

Do not invent file paths.

New paths must be justified by an existing architectural convention or an approved architectural decision.

### 4.4 Intended Behavior

Define the expected behavior precisely.

Include where relevant:

- Input conditions.
- Validation.
- State transitions.
- Success behavior.
- Failure behavior.
- Cancellation behavior.
- Persistence behavior.
- Event behavior.
- Cleanup.
- Concurrency expectations.
- Platform-specific behavior.

Avoid ambiguous statements such as:

- Fix the issue.
- Improve handling.
- Make it more reliable.
- Support all cases.

Convert requirements into observable acceptance criteria.

### 4.5 Compatibility and Regression Risks

Identify risks involving:

- Public APIs.
- Existing consumers.
- Data compatibility.
- Persisted state.
- Event contracts.
- Runtime behavior.
- Dependencies.
- Platform support.
- Performance.
- Security.
- Build configurations.
- Existing user modifications.

Describe mitigation for each significant risk.

### 4.6 Tests and Checks

Define the verification plan before implementation.

Identify relevant:

- Unit tests.
- Integration tests.
- Regression tests.
- Formatting.
- Linting.
- Static checks.
- Compilation.
- Platform-specific builds.
- Runtime scenarios.

Use actual repository commands and existing test infrastructure.

If a command is not yet verified, label it as a proposed command.

### 4.7 Unresolved Questions

List:

- Missing source evidence.
- Ambiguous requirements.
- Unverified assumptions.
- Environmental dependencies.
- External constraints.
- Architectural decisions requiring approval.

Do not hide blockers inside implementation details.

### 4.8 Approval Requirements

Implementation must not begin before approval when the task requires review or involves a significant architectural decision.

Explicit approval is required before:

- Broad architectural restructuring.
- Moving crates or modules.
- Changing major dependency boundaries.
- Introducing new permanent architecture layers.
- Replacing public contracts.
- Changing persistence semantics.
- Replacing communication mechanisms.
- Introducing significant native dependencies.
- Making broad cross-platform changes.
- Performing destructive operations.

A routine, narrowly scoped bug fix may proceed when the user has clearly authorized implementation and no additional approval gate applies.

If approval is required, stop after presenting the plan.

Do not interpret silence as approval.

### Phase 2 Exit Criteria

- [ ] Root cause or current-state findings are documented.
- [ ] Evidence and source paths are identified.
- [ ] Proposed file changes are explicit.
- [ ] Intended behavior is defined.
- [ ] Compatibility risks are assessed.
- [ ] Verification strategy is defined.
- [ ] Unknowns and blockers are documented.
- [ ] Required approval is obtained before implementation.

## 5. Phase 3 — Implementation

**Objective:** Execute the approved plan while preserving existing architecture and behavior outside the requested scope.

### 5.1 Reconfirm the Implementation Scope

Before editing:

- Review the approved plan.
- Recheck relevant files if the repository changed.
- Check the working tree.
- Identify newly introduced user changes.
- Confirm that the implementation still matches the current source.

If the source has changed since investigation, reassess the plan.

Do not apply a stale plan blindly.

### 5.2 Make the Smallest Complete Change

Implementation must:

- Address the verified root cause or defined requirement.
- Modify only necessary files.
- Reuse existing functionality.
- Follow established project conventions.
- Preserve existing public contracts unless explicitly changed.
- Preserve existing behavior outside the requested scope.
- Include appropriate error handling.
- Maintain state consistency.
- Update tests when behavior changes.

Avoid unrelated refactoring.

### 5.3 Preserve Existing Contracts

Do not change without justification:

- Public function signatures.
- Struct fields.
- Enum variants.
- Serialization formats.
- Message contracts.
- Event payloads.
- Configuration formats.
- Persistence representations.
- External API behavior.
- Feature flags.

If a contract change is necessary:

1. Identify every known consumer.
2. Assess compatibility.
3. Update affected callers and consumers.
4. Update tests.
5. Document migration or compatibility requirements.
6. Obtain required approval.

### 5.4 Maintain Code Quality

Follow existing architecture and coding conventions.

Requirements:

- Clear responsibility boundaries.
- Single-purpose functions and modules.
- Appropriate error propagation.
- Explicit ownership and lifecycle.
- No unnecessary duplication.
- No speculative abstractions.
- No dead code.
- No unrelated dependencies.
- No unnecessary formatting changes.

Do not introduce a new architectural pattern for a problem already handled by an existing project pattern.

### 5.5 Update Tests with Behavior Changes

When behavior changes:

- Add or update relevant tests.
- Cover the corrected behavior.
- Cover important boundary conditions.
- Cover relevant failure cases.
- Preserve existing regression coverage.
- Verify that tests assert observable behavior rather than implementation details unnecessarily.

Do not modify tests merely to make incorrect implementation pass.

If an existing test conflicts with the intended requirement, establish whether the test or implementation is wrong before changing either.

### 5.6 Scope Discipline

If implementation reveals that the approved plan is insufficient:

1. Stop expanding the change.
2. Record the new evidence.
3. Explain the additional impact.
4. Update the proposed plan.
5. Obtain approval when required.
6. Continue only within the updated scope.

Do not silently expand a focused task into a broader rewrite.

### Phase 3 Exit Criteria

- [ ] The approved behavior is implemented.
- [ ] Changes remain within scope.
- [ ] Existing contracts are preserved or explicitly reviewed.
- [ ] Relevant tests are updated.
- [ ] Error and edge-case behavior is considered.
- [ ] No unrelated refactoring was introduced.
- [ ] The final diff is reviewable.
- [ ] No unapproved architectural expansion occurred.

## 6. Phase 4 — Verification

**Objective:** Establish whether the implementation works and whether existing behavior remains intact.

Verification must be performed after implementation.

### 6.1 Inspect the Final Diff

Review:

- Modified files.
- Added files.
- Deleted files.
- Public API changes.
- Dependency changes.
- Configuration changes.
- Generated files.
- Unrelated modifications.
- Accidental formatting.
- Debug code.
- Temporary files.

Confirm that no user modifications were overwritten.

### 6.2 Formatting

Run the repository's established formatter.

Verify:

- Relevant files are formatted.
- Formatting does not create unrelated changes.
- Generated files follow project conventions where applicable.

Do not introduce a new formatter or formatting policy without justification.

### 6.3 Lint and Static Checks

Run relevant checks supported by the project.

Examples may include:

- Compiler warnings.
- Linter checks.
- Static analysis.
- Type checks.
- Dependency checks.

Use the actual tools and commands configured by the repository.

Do not assume a tool is installed or configured.

### 6.4 Compilation and Build Checks

Run the relevant build checks for affected targets.

Where applicable, verify:

- Changed package.
- Relevant workspace members.
- Required feature combinations.
- Target-specific builds.
- Native linking.
- Desktop compatibility.
- Android compatibility.

A successful package build does not automatically establish workspace-wide compatibility.

Report the exact scope of the build performed.

### 6.5 Tests

Run relevant:

- Unit tests.
- Integration tests.
- Regression tests.
- Feature-specific tests.
- Platform-specific tests.

When appropriate, test:

- Normal operation.
- Invalid input.
- Boundary conditions.
- Failure handling.
- Cancellation.
- Concurrency.
- Persistence.
- Recovery.
- External integration behavior.

Do not claim complete coverage merely because the test suite passed.

### 6.6 Runtime Verification

When the task changes runtime behavior, compilation alone is insufficient.

Perform the relevant runtime checks when the environment supports them.

Examples:

- Execute the affected workflow.
- Verify actual state transitions.
- Observe event delivery.
- Verify persistence.
- Verify cleanup.
- Test cancellation.
- Verify external communication.
- Test platform-specific lifecycle behavior.

If runtime testing is unavailable, classify it as `BLOCKED` or `NOT RUN` and explain why.

### 6.7 Separate Source Failures from Environment Failures

For every failed verification, determine whether the cause is:

#### Source Failure

Examples:

- Compilation error in modified source.
- Failing regression test.
- Incorrect behavior.
- Broken contract.
- Invalid implementation.

#### Dependency Failure

Examples:

- Dependency cannot be resolved.
- Incompatible dependency version.
- Native dependency build failure.

#### Environment Failure

Examples:

- Missing compiler.
- Missing SDK or NDK.
- Missing system library.
- Unavailable external service.
- Missing device or emulator.
- Insufficient permissions.

#### Configuration Failure

Examples:

- Incorrect target configuration.
- Missing environment variable.
- Incorrect feature selection.
- Invalid build configuration.

Do not report an environment failure as proof that the source implementation is incorrect.

Likewise, do not dismiss a source failure as an environment issue without evidence.

### 6.8 Verification Record

For every executed command, record:

- Exact command.
- Relevant target or package.
- Relevant feature configuration.
- Actual outcome.
- Important output or error.
- Whether the command completed successfully.

Use these statuses:

- `PASSED`
- `FAILED`
- `BLOCKED`
- `NOT RUN`
- `NOT APPLICABLE`

### Phase 4 Exit Criteria

- [ ] Final diff is inspected.
- [ ] Formatting checks are performed where applicable.
- [ ] Relevant lint and static checks are performed.
- [ ] Relevant builds are performed.
- [ ] Relevant tests are performed.
- [ ] Runtime checks are performed where required and possible.
- [ ] Failures are classified correctly.
- [ ] Exact commands and outcomes are recorded.
- [ ] Remaining verification gaps are explicit.

## 7. Phase 5 — Final Report

**Objective:** Provide a concise but complete account of the work, evidence, and remaining risks.

Every implementation task must end with a factual report.

### 7.1 Summary

Explain:

- What was requested.
- What was implemented.
- Whether the task is complete.
- Any significant deviation from the original plan.

### 7.2 Exact Files Changed

List every added, modified, or deleted file.

For each file, state its actual purpose in the change.

Do not omit indirect changes.

### 7.3 Behavior Affected

Explain:

- Previous behavior.
- New behavior.
- Relevant state or data changes.
- Affected integration points.
- Preserved behavior.
- Known limitations.

### 7.4 Verification Performed

List:

- Exact commands.
- Build results.
- Test results.
- Formatting and lint results.
- Runtime scenarios.
- Failed or blocked checks.

Separate successful checks from unexecuted checks.

### 7.5 Remaining Risks and Unknowns

Report:

- Unverified behavior.
- Remaining blockers.
- Compatibility concerns.
- Potential regression areas.
- Missing tests.
- Required follow-up work.

Do not label a task fully verified if important required checks remain unexecuted.

### 7.6 Final Status

Use an accurate completion status:

- `COMPLETED`: Required implementation and verification criteria are satisfied.
- `IMPLEMENTED — VERIFICATION INCOMPLETE`: Code changes are made, but required verification remains.
- `PARTIALLY COMPLETED`: Some requirements are satisfied and others remain.
- `BLOCKED`: A required dependency, environment, approval, or source fact prevents completion.
- `NOT IMPLEMENTED`: Investigation or planning was completed without implementation.
- `NO CHANGE REQUIRED`: Investigation established that no source change is necessary.

## 8. Special Case — Investigation-Only Requests

When the user requests:

- Investigate.
- Audit.
- Analyze.
- Review.
- Find the root cause.
- Explain current behavior.
- Identify affected files.
- Assess risks.
- Propose an architecture.

Treat the task as read-only unless implementation is explicitly authorized.

The required result is:

- Verified findings.
- Evidence and source paths.
- Current behavior.
- Root cause or unresolved hypotheses.
- Affected components.
- Potentially affected files.
- Risks.
- Recommended next steps.

Do not modify source files during an investigation-only task.

## 9. Special Case — Documentation-Only Changes

Documentation changes must still follow the workflow.

Inspect:

- Existing documentation structure.
- Related documents.
- Actual implementation.
- Applicable project instructions.
- Naming and linking conventions.

Do not document proposed behavior as implemented behavior.

Do not claim an architecture is verified when only a proposal exists.

Preserve consistency between related documents.

## 10. Special Case — Build and Environment Failures

When a build or test cannot run:

1. Record the exact attempted command.
2. Capture the actual error.
3. Identify the missing requirement.
4. Determine whether it is an environment or source problem.
5. State what remains unverified.
6. Avoid claiming success.

Do not modify source code merely to hide an unavailable tool or incomplete environment.

## 11. Special Case — Architectural Changes

Architectural changes require additional investigation.

Before implementation:

- Map current responsibilities.
- Trace dependency direction.
- Identify consumers.
- Identify existing alternatives.
- Assess lifecycle and state ownership.
- Assess public contract impact.
- Assess platform implications.
- Evaluate simpler alternatives.
- Define migration and regression risks.

Present the plan and obtain approval when required.

Prefer the smallest architecture change that solves the verified problem.

## 12. Special Case — Security-Sensitive Changes

For security-sensitive tasks, additionally inspect:

- Trust boundaries.
- Authentication.
- Authorization.
- Input validation.
- Secret handling.
- Serialization.
- Network security.
- Filesystem permissions.
- Native boundaries.
- Dependency security.

Do not weaken existing security protections to make an implementation easier.

Security claims require evidence appropriate to the claim.

A passing functional test does not prove security.

## 13. Stop Conditions

Stop implementation and report the blocker when:

- The actual repository revision cannot be established and version-specific correctness is required.
- Required source files are unavailable.
- Relevant public contracts cannot be verified.
- The requested behavior conflicts with an existing contract and no decision has been approved.
- The implementation requires an unapproved architectural change.
- Existing user changes would be overwritten.
- A destructive action lacks authorization.
- The available environment cannot safely verify a critical requirement.
- Evidence is insufficient to select a safe implementation.

Do not fill missing evidence with assumptions.

## 14. Final Workflow Checklist

### Investigation

- [ ] Confirm repository and working-tree state.
- [ ] Read applicable project instructions.
- [ ] Understand the request and acceptance criteria.
- [ ] Inspect source, manifests, callers, and tests.
- [ ] Trace data flow and state ownership.
- [ ] Inspect events, persistence, and side effects where relevant.
- [ ] Identify root cause or unresolved hypotheses.
- [ ] Identify regression risks.

### Plan

- [ ] Present evidence and exact source paths.
- [ ] Identify proposed file changes.
- [ ] Define intended behavior.
- [ ] Assess compatibility risks.
- [ ] Define verification strategy.
- [ ] List unresolved questions.
- [ ] Obtain required approval.

### Implementation

- [ ] Reconfirm source and working-tree state.
- [ ] Follow the approved plan.
- [ ] Make the smallest complete change.
- [ ] Preserve existing contracts.
- [ ] Update relevant tests.
- [ ] Avoid unrelated changes.
- [ ] Review the resulting diff.

### Verification

- [ ] Run relevant formatting checks.
- [ ] Run relevant lint and static checks.
- [ ] Run relevant builds.
- [ ] Run relevant tests.
- [ ] Perform runtime verification where required.
- [ ] Classify failures correctly.
- [ ] Record exact commands and outcomes.

### Report

- [ ] Summarize the result.
- [ ] List exact files changed.
- [ ] Explain affected behavior.
- [ ] Report verification performed.
- [ ] Identify remaining risks and unknowns.
- [ ] State an accurate final status.

## 15. Final Engineering Principle

**Investigate before changing.**

**Understand before designing.**

**Plan before implementing.**

**Obtain approval before significant architectural changes.**

**Verify before claiming success.**

**Report evidence, not assumptions.**

Every change must be justified by the actual repository, limited to the approved scope, and verified to the extent the environment permits.
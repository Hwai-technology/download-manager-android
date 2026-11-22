# Improvement Roadmap

This is a proposed sequence, not authorization to implement. No source changes are included. Compatibility and test plans are explicit so maintainers can decide where policy changes are appropriate.

## ROAD-001: Harden Part-File Creation and Finalization

- **Related findings:** SEC-002, SEC-004.
- **Priority:** First security/correctness task.
- **Change direction:** Use exclusive/no-follow creation for explicit-file sidecars; define safe resume behavior if the part path is a symlink or unexpected file type. Use a platform-appropriate atomic no-replace finalization strategy or a reservation that actually covers competing processes.
- **Expected benefit:** Prevent truncating a symlink target and reduce destination replacement races.
- **Components:** `crates/engine/src/download.rs`, possibly `naming.rs` and platform-specific filesystem helpers.
- **Compatibility risks:** Existing users may have legitimate part files or filesystem behavior that differs on network volumes; preserve resumability and Windows antivirus rename retries.
- **Required tests:** Symlink, hard-link, existing-part, concurrent final-target creation, directory/file destinations, Windows/Linux/macOS behavior, resume regression and no overwrite assertions.

## ROAD-002: Add a Caller-Aware Destination Policy

- **Related findings:** SEC-001.
- **Priority:** High where the app runs as a remote service or accepts automation inputs.
- **Change direction:** Document trust boundaries first. Consider policy inputs for browser/manual downloads versus remote/automation/service requests; decide whether loopback, private, link-local, multicast and metadata ranges are allowed. Apply checks after DNS resolution and on every redirect/reconnect, with explicit exceptions for intentionally supported LAN downloads.
- **Expected benefit:** Reduce SSRF exposure when URLs come from less-trusted callers without breaking local download use cases.
- **Components:** Engine DNS/HTTP redirect layer and caller metadata in IPC/remote/MCP/browser paths.
- **Compatibility risks:** Blocking private ranges would break legitimate LAN/local-server downloads; DNS rebinding and proxies complicate enforcement.
- **Required tests:** IPv4/IPv6 special ranges, mapped IPv6, DNS answer changes, redirect chains, proxy routes, selected adapters and explicit user-approved local destinations.

## ROAD-003: Global Resource Admission

- **Related findings:** PERF-001.
- **Priority:** High for headless/remote and many-download workloads.
- **Change direction:** Add a process-level budget for active downloads, total worker connections, buffered bytes and blocking disk jobs. Queueing should be fair and cancellation-aware; retain per-download caps as local limits.
- **Expected benefit:** Bound aggregate RSS, sockets, server pressure and Tokio blocking-pool backlog.
- **Components:** App scheduler and an engine-shared admission/resource controller.
- **Compatibility risks:** Lower peak throughput for users who intentionally run many files; UI must show queued versus active work and manual starts must remain predictable.
- **Required tests:** Many simultaneous small/large downloads, slow-flush simulation, per-host limits, starvation/fairness, cancel while queued, global speed limit interaction and peak RSS/socket acceptance thresholds.

## ROAD-004: Redact Sensitive URL Material

- **Related findings:** SEC-003.
- **Priority:** High privacy hardening.
- **Change direction:** Strip URL user-info before persistence; avoid persisting known secret query keys when possible. Consider encrypting opaque signed URLs only if they must survive resume, otherwise re-resolve through the original URL. Preserve enough information for legitimate resume and refresh flows.
- **Expected benefit:** Reduce credential/token exposure in sidecars, backups and shared download folders.
- **Components:** `state.rs`, HTTP fresh/resume/redirect paths, FTP parity and serialization tests.
- **Compatibility risks:** Redaction can make signed URLs impossible to resume; URL equivalence and refresh behavior must remain correct.
- **Required tests:** HTTP basic user-info, signed query tokens, redirects, resume/refresh, FTP password stripping, legacy state migration and ensuring headers remain absent.

## ROAD-005: Bound Resume Metadata

- **Related findings:** SEC-005.
- **Priority:** Medium.
- **Change direction:** Check sidecar metadata length before reading; impose defensible segment-count and string-length limits; reject impossible layouts early and return a stable error.
- **Expected benefit:** Bound memory and parse CPU for malformed/corrupt state.
- **Components:** `crates/engine/src/state.rs`, planner restore validation.
- **Compatibility risks:** Older or unusually fragmented but valid states might exceed a new cap; choose limits based on planner behavior and migrations.
- **Required tests:** Oversized JSON, huge segment arrays, integer boundaries, invalid tiling, legacy version/state and normal large-download round trips.

## ROAD-006: Explicit Transport-Security Policy

- **Related findings:** SEC-006.
- **Priority:** Medium, policy decision.
- **Change direction:** Decide whether to warn, block, or allow HTTPS-to-HTTP redirect and plain FTP per caller/setting. Preserve explicit support for local/insecure servers where intended and make downgrade state visible.
- **Expected benefit:** Make confidentiality/integrity tradeoffs visible and prevent silent downgrades for users who require secure transport.
- **Components:** Redirect handling, URL/options UI, CLI/headless messages, i18n.
- **Compatibility risks:** Some legitimate mirrors and legacy FTP endpoints depend on plaintext or downgrade redirects.
- **Required tests:** HTTP→HTTPS, HTTPS→HTTP, redirect chains, credentials stripping, FTP/FTPS behavior, proxy and user override.

## ROAD-007: Audit FTP Data-Channel Boundaries

- **Related findings:** SEC-007.
- **Priority:** Medium security verification.
- **Change direction:** Inspect the exact locked suppaftp source for PASV/EPSV/active-mode address selection, TLS data protection, and target host validation. Add engine-level checks only after confirming current dependency behavior.
- **Expected benefit:** Establish whether passive data connections can be redirected to unintended hosts and whether adapter binding holds for all paths.
- **Components:** `crates/engine/src/ftp.rs`, pinned suppaftp revision and platform socket binding.
- **Compatibility risks:** Rewriting server-provided PASV addresses can break unusual NAT/proxy deployments; implicit FTPS with bound adapter is currently explicitly refused.
- **Required tests:** PASV private/wrong address, EPSV, active mode, explicit/implicit TLS, route-bound control/data channels, server close and reuse.

## ROAD-008: Measure and Tune Segmentation

- **Related findings:** PERF-002 and PERF-004.
- **Priority:** After resource bounds.
- **Change direction:** Re-run current benchmark cases and add cold TLS, high latency, slow disk, many concurrent files, range refusal and small-file scenarios. Tune initial piece thresholds or request ramp-up only from distributions and request/bytes-wasted metrics.
- **Expected benefit:** Reduce avoidable requests and connection churn while retaining fast recovery on slow or asymmetric servers.
- **Components:** `planner.rs`, `download.rs`, `tools/bench`, `docs/bench`.
- **Compatibility risks:** Different server range semantics, per-IP connection limits and small-file behavior may regress; avoid replacing dynamic splitting without fault tests.
- **Required tests:** Deterministic lab-server scenarios, repeated trials, CI functional tests and cross-platform performance smoke runs.

## ROAD-009: Validate Durability and Flush Tradeoffs

- **Related findings:** PERF-003, REL-001, REL-003.
- **Priority:** Medium reliability task.
- **Change direction:** Verify craft-atomic implementation and parent-directory persistence guarantees on supported filesystems. Document recovery-point behavior. Where a trusted hash exists, consider verifying the final file in the crash-after-rename recovery path.
- **Expected benefit:** Make power-loss and crash-recovery guarantees precise and avoid accepting a same-size replacement as complete.
- **Components:** `state.rs`, `download.rs`, `craft-atomic` pinned source, platform filesystem tests.
- **Compatibility risks:** More frequent sync/checksum work can slow HDD/network-volume downloads; changing atomic replacement behavior can affect existing sidecars.
- **Required tests:** Fault injection around part sync/state write/final sync/rename/state deletion, power-cut simulation where feasible, slow-flush test and same-size replacement test.

## ROAD-010: Strengthen Range Response Validation

- **Related findings:** REL-002.
- **Priority:** Medium correctness task.
- **Change direction:** Validate `start <= end`, declared range length against response/body length where applicable, and requested segment boundaries. Define handling for `Content-Range: */total`, 416 and valid shorter partial responses.
- **Expected benefit:** Reject malformed or inconsistent server replies sooner and make errors deterministic.
- **Components:** `download.rs` response parser/request checks and labserver fault injection.
- **Compatibility risks:** Some nonconforming servers may currently work by accident; keep an explicit compatibility fallback only if tests justify it.
- **Required tests:** Missing/malformed header, reversed end, wrong start/end/total, wildcard total, 416, truncated/oversized body, changed ETag and complete byte-for-byte result assertions.

## Suggested Order

## ROAD-011: Verify Dependency Advisories

- **Related findings:** DEP-001.
- **Priority:** Security verification; no vulnerability is currently known from this audit.
- **Change direction:** After authorization for registry/cache access, run a lockfile-only advisory check with a recorded database date and tool version. Do not resolve, update, or remove dependencies during validation.
- **Expected benefit:** Establish whether any known advisory affects a reachable locked package/feature combination.
- **Components:** `Cargo.lock`, workspace manifests, CI/security review process.
- **Compatibility risks:** No application behavior changes; false positives and unreachable-feature advisories need manual triage.
- **Required verification:** Reproduce the report from the same lockfile, inspect affected code paths/features, document fixed/affected/unaffected status, and keep dependency changes as a separate approved task.

## Suggested Order

1. ROAD-001 and ROAD-004: address local data safety and persisted secret material.
2. ROAD-002 and ROAD-007: establish network trust boundaries before expanding remote/service use.
3. ROAD-005 and ROAD-010: bound parser inputs and make range semantics explicit.
4. ROAD-003: establish aggregate resource limits, then tune the policies within those limits.
5. ROAD-006 and ROAD-009: make product-level transport and durability guarantees explicit.
6. ROAD-008: optimize only after measuring the current revision with repeatable methodology.

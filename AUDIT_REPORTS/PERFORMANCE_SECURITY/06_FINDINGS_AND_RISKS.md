# Findings and Risks

## Rating and Evidence Key

- **P0:** critical security or correctness risk.
- **P1:** high impact if conditions are met; notable reliability/performance risk.
- **P2:** medium impact or hardening/correctness gap.
- **P3:** optimization or future capability.
- **VERIFIED:** directly established by source behavior or a completed test. No tests were run in this audit, so “verified source behavior” does not mean an exploit was reproduced.
- **PARTIALLY VERIFIED:** source evidence exists but dependency/platform/runtime behavior remains open.
- **INFERRED:** technically plausible and requires a discriminating test.
- **UNKNOWN:** insufficient evidence.

No P0 confirmed defect was identified. Findings prioritize hardening and verification rather than claiming unproven exploits.

## Authoritative Finding Registry

This is the canonical ID, category, severity, confidence, evidence and recommendation index. IDs below are unique. The former `PERF-005` reference is consolidated under `REL-002`; it is not a separate finding. `UNKNOWN` means severity or implementation behavior cannot be safely classified from repository evidence alone.

| ID       | Title                                            | Category                           | Severity                                | Confidence             | Evidence location                                                                                                                                                                                                                                                                     | Affected component                                          | Recommended action                                                                        |
| -------- | ------------------------------------------------ | ---------------------------------- | --------------------------------------- | ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| SEC-001  | No private-address policy                        | Security / URL boundary            | P2, conditional                         | VERIFIED source gap    | [net.rs:186-L197](../../crates/engine/src/net.rs#L186-L197), [dns.rs:497-L535](../../crates/engine/src/dns.rs#L497-L535)                                                                                                                                                              | HTTP/DNS destinations and callers                           | [ROAD-002](07_IMPROVEMENT_ROADMAP.md#road-002-add-a-caller-aware-destination-policy)      |
| SEC-002  | Explicit part path follows symlinks              | Security / filesystem              | P2, local                               | VERIFIED behavior      | [download.rs:1746-L1752](../../crates/engine/src/download.rs#L1746-L1752), [download.rs:1764-L1768](../../crates/engine/src/download.rs#L1764-L1768)                                                                                                                                  | Explicit-file part creation                                 | [ROAD-001](07_IMPROVEMENT_ROADMAP.md#road-001-harden-part-file-creation-and-finalization) |
| SEC-003  | URL secrets persisted in resume state            | Security / sensitive data          | P2, local                               | VERIFIED behavior      | [download.rs:1655-L1662](../../crates/engine/src/download.rs#L1655-L1662), [state.rs:20-L30](../../crates/engine/src/state.rs#L20-L30), [download.rs:1851-L1856](../../crates/engine/src/download.rs#L1851-L1856)                                                                     | HTTP resume metadata                                        | [ROAD-004](07_IMPROVEMENT_ROADMAP.md#road-004-redact-sensitive-url-material)              |
| SEC-004  | Final rename has a TOCTOU gap                    | Security / filesystem              | P2, local                               | PARTIALLY VERIFIED     | [download.rs:2148-L2169](../../crates/engine/src/download.rs#L2148-L2169)                                                                                                                                                                                                             | Destination finalization                                    | [ROAD-001](07_IMPROVEMENT_ROADMAP.md#road-001-harden-part-file-creation-and-finalization) |
| SEC-005  | Resume metadata is unbounded                     | Security / resource exhaustion     | P2                                      | VERIFIED behavior      | [state.rs:20-L30](../../crates/engine/src/state.rs#L20-L30), [state.rs:70-L81](../../crates/engine/src/state.rs#L70-L81)                                                                                                                                                              | Resume-state parser                                         | [ROAD-005](07_IMPROVEMENT_ROADMAP.md#road-005-bound-resume-metadata)                      |
| SEC-006  | Downgrade/plaintext transports allowed           | Security / network transport       | P2, policy-dependent                    | PARTIALLY VERIFIED     | [download.rs:536-L551](../../crates/engine/src/download.rs#L536-L551), [download.rs:590-L600](../../crates/engine/src/download.rs#L590-L600), [ftp.rs:105-L133](../../crates/engine/src/ftp.rs#L105-L133)                                                                             | HTTP redirects and FTP                                      | [ROAD-006](07_IMPROVEMENT_ROADMAP.md#road-006-explicit-transport-security-policy)         |
| SEC-007  | FTP passive endpoint behavior unresolved         | Security / FTP data channel        | UNKNOWN                                 | UNKNOWN                | [ftp.rs:271-L291](../../crates/engine/src/ftp.rs#L271-L291)                                                                                                                                                                                                                           | suppaftp PASV/EPSV data connection                          | [ROAD-007](07_IMPROVEMENT_ROADMAP.md#road-007-audit-ftp-data-channel-boundaries)          |
| PERF-001 | No global resource admission                     | Performance / resource limits      | P1 for high-concurrency use; P2 desktop | VERIFIED design limit  | [download.rs:46-L47](../../crates/engine/src/download.rs#L46-L47), [download.rs:3243-L3247](../../crates/engine/src/download.rs#L3243-L3247), [download.rs:3284-L3297](../../crates/engine/src/download.rs#L3284-L3297), [queue.rs:155-L173](../../crates/app/src/queue.rs#L155-L173) | App-wide downloads, connections, memory and blocking writes | [ROAD-003](07_IMPROVEMENT_ROADMAP.md#road-003-global-resource-admission)                  |
| PERF-002 | Extra requests in selected workloads             | Performance / segmentation         | P2                                      | PARTIALLY VERIFIED     | [planner.rs:184-L250](../../crates/engine/src/planner.rs#L184-L250), [run5/REPORT.md](../../docs/bench/run5/REPORT.md)                                                                                                                                                                | Planner policy                                              | [ROAD-008](07_IMPROVEMENT_ROADMAP.md#road-008-measure-and-tune-segmentation)              |
| PERF-003 | Sync policy trades recovery point for throughput | Performance / disk I/O             | P2 trade-off                            | VERIFIED design        | [download.rs:2248-L2271](../../crates/engine/src/download.rs#L2248-L2271)                                                                                                                                                                                                             | Resume flush cadence                                        | [ROAD-009](07_IMPROVEMENT_ROADMAP.md#road-009-validate-durability-and-flush-tradeoffs)    |
| PERF-004 | HTTP/2 and HTTP/3 not configured                 | Performance / network              | P3 opportunity                          | VERIFIED configuration | [Cargo.toml:90](../../Cargo.toml#L90), [download.rs:1472-L1479](../../crates/engine/src/download.rs#L1472-L1479)                                                                                                                                                                      | HTTP client                                                 | [ROAD-008](07_IMPROVEMENT_ROADMAP.md#road-008-measure-and-tune-segmentation)              |
| REL-001  | Resume integrity is conditional                  | Reliability / integrity            | P2                                      | VERIFIED limitation    | [state.rs:118-L153](../../crates/engine/src/state.rs#L118-L153), [download.rs:99](../../crates/engine/src/download.rs#L99), [download.rs:2102-L2120](../../crates/engine/src/download.rs#L2102-L2120)                                                                                 | Resume validators and optional checksums                    | [ROAD-009](07_IMPROVEMENT_ROADMAP.md#road-009-validate-durability-and-flush-tradeoffs)    |
| REL-002  | Content-Range end not validated                  | Reliability / response correctness | P2                                      | VERIFIED source gap    | [download.rs:2997-L3002](../../crates/engine/src/download.rs#L2997-L3002), [download.rs:3382-L3398](../../crates/engine/src/download.rs#L3382-L3398)                                                                                                                                  | HTTP range parser and workers                               | [ROAD-010](07_IMPROVEMENT_ROADMAP.md#road-010-strengthen-range-response-validation)       |
| REL-003  | Crash recovery accepts expected size only        | Reliability / crash recovery       | P2, conditional                         | VERIFIED behavior      | [download.rs:1927-L1934](../../crates/engine/src/download.rs#L1927-L1934)                                                                                                                                                                                                             | Resume completion shortcut                                  | [ROAD-009](07_IMPROVEMENT_ROADMAP.md#road-009-validate-durability-and-flush-tradeoffs)    |
| DEP-001  | Advisory status not checked                      | Dependency security                | UNKNOWN                                 | UNKNOWN                | [05_DEPENDENCY_SECURITY.md](05_DEPENDENCY_SECURITY.md#advisory-status), [Cargo.lock](../../Cargo.lock)                                                                                                                                                                                | Full resolved dependency graph                              | [ROAD-011](07_IMPROVEMENT_ROADMAP.md#road-011-verify-dependency-advisories)               |

## Security Findings

### SEC-001 — No explicit private-address policy for download destinations

- **Severity:** P2; potentially P1 only when untrusted callers can submit URLs and the host has sensitive internal services.
- **Severity:** P2; potentially P1 when untrusted callers can submit URLs to a host with sensitive internal services.
- **Status/confidence:** VERIFIED source gap; high confidence that no such filter exists in the inspected engine route/resolver path. Exploitability is conditional and not verified.
- **Evidence:** `check_url` tests route reachability, not address privacy ([net.rs:186-L197](../../crates/engine/src/net.rs#L186-L197)); resolved addresses are filtered through the same route constraint ([dns.rs:497-L535](../../crates/engine/src/dns.rs#L497-L535)). The redirect policy is wrapped with route checks only when a route is bound ([download.rs:1432-L1458](../../crates/engine/src/download.rs#L1432-L1458)).
- **Evidence:** `check_url` tests route reachability, not address privacy ([net.rs:186-L197](../../crates/engine/src/net.rs#L186-L197)); resolved addresses are filtered through the same route constraint ([dns.rs:497-L535](../../crates/engine/src/dns.rs#L497-L535)). The client wraps redirect checks with route reachability only when a route is bound ([download.rs:1432-L1458](../../crates/engine/src/download.rs#L1432-L1458)).
- **Behavior/impact:** URLs to loopback/private/metadata destinations can be used where routing and caller permissions allow. The app is designed to fetch user-selected resources, so this is not automatically a vulnerability; remote/automation/browser caller trust must be defined.
- **Verification:** Add safe lab tests using reserved local test addresses and redirect chains, including DNS answers changing between lookups. Confirm intended support for local-network downloads before enforcing a policy.
- **Recommendation:** [ROAD-002](07_IMPROVEMENT_ROADMAP.md#road-002-add-a-caller-aware-destination-policy).

### SEC-002 — Explicit-file part creation follows a pre-existing sidecar symlink

- **Severity:** P2 (local, requires writable destination directory; impact can be arbitrary truncation of files writable by the same user).
- **Status/confidence:** VERIFIED source behavior; high confidence. No exploit was run.
- **Evidence:** `Destination::File` uses `.create(true).truncate(true)` on `part_path`, not exclusive create/no-follow ([download.rs:1746-L1752](../../crates/engine/src/download.rs#L1746-L1752)). Directory mode uses `create_new(true)` ([download.rs:1764-L1768](../../crates/engine/src/download.rs#L1764-L1768)).
  - **Evidence:** `Destination::File` uses `.create(true).truncate(true)` on `part_path`, not exclusive create/no-follow ([download.rs:1746-L1752](../../crates/engine/src/download.rs#L1746-L1752)). Directory mode uses `create_new(true)` ([download.rs:1764-L1768](../../crates/engine/src/download.rs#L1764-L1768)).
- **Root cause/impact:** Two destination modes have different symlink-race guarantees. A planted `.mdm.part` symlink can be followed and its target truncated when opened.
- **Verification:** In an isolated temp directory, place a symlink at the explicit destination's part path, start a controlled lab download, and observe whether target bytes change. Do not use a real user file.
- **Recommendation:** [ROAD-001](07_IMPROVEMENT_ROADMAP.md#road-001-harden-part-file-creation-and-finalization).

### SEC-003 — URL credentials and secret query strings can enter resume metadata

- **Severity:** P2; local disclosure depending on sidecar permissions and URL content.
- **Status/confidence:** VERIFIED source behavior; high confidence.
- **Evidence:** HTTP fresh path serializes the original URL into resume state ([download.rs:1655-L1662](../../crates/engine/src/download.rs#L1655-L1662)); the state has original and final URL fields ([state.rs:20-L30](../../crates/engine/src/state.rs#L20-L30)). FTP strips its password before persistence ([download.rs:1851-L1856](../../crates/engine/src/download.rs#L1851-L1856)).
- **Evidence:** HTTP fresh path serializes the original URL into resume state ([download.rs:1655-L1662](../../crates/engine/src/download.rs#L1655-L1662)); the state has original and final URL fields ([state.rs:20-L30](../../crates/engine/src/state.rs#L20-L30)). FTP strips its password before persistence ([download.rs:1851-L1856](../../crates/engine/src/download.rs#L1851-L1856)).
- **Impact:** HTTP user-info and signed/query tokens may remain in `<dest>.mdm.json`, backups, or copied download folders. Header credentials are correctly omitted, but that does not redact URL credentials.
- **Verification:** Use a local test URL with dummy user-info and a dummy query token, inspect the generated sidecar in an authorized temp folder, and confirm whether reqwest/URL normalization changes stored values.
- **Recommendation:** [ROAD-004](07_IMPROVEMENT_ROADMAP.md#road-004-redact-sensitive-url-material).

### SEC-004 — Final destination check and rename are not one atomic no-replace operation

- **Severity:** P2, local race; platform-dependent overwrite behavior.
- **Status/confidence:** PARTIALLY VERIFIED; high confidence that a TOCTOU gap exists, medium confidence on exact consequences across supported platforms.
- **Evidence:** `finish` selects the name after an existence check and later calls rename in a separate operation ([download.rs:2148-L2169](../../crates/engine/src/download.rs#L2148-L2169)).
- **Evidence:** `finish` selects the name after an existence check and later calls rename in a separate operation ([download.rs:2148-L2169](../../crates/engine/src/download.rs#L2148-L2169)).
- **Impact:** Another process may create the candidate destination between check and rename. The code comment's “never replace” guarantee is not established atomically on all targets.
- **Verification:** Platform-specific contention test with a helper racing target creation at finalization; verify preservation of both existing and downloaded bytes.
- **Recommendation:** [ROAD-001](07_IMPROVEMENT_ROADMAP.md#road-001-harden-part-file-creation-and-finalization).

### SEC-005 — Resume metadata has no explicit input-size or segment-count ceiling

- **Severity:** P2 hardening/resource concern; local or caller-controlled state required.
- **Status/confidence:** VERIFIED source behavior; high confidence.
- **Evidence:** `segments` is an unconstrained `Vec` field in the serialized state ([state.rs:20-L30](../../crates/engine/src/state.rs#L20-L30)); `load` reads all bytes before deserializing ([state.rs:70-L81](../../crates/engine/src/state.rs#L70-L81)).
- **Evidence:** `segments` is an unconstrained `Vec` field in the serialized state ([state.rs:20-L30](../../crates/engine/src/state.rs#L20-L30)); `load` reads all bytes before deserializing ([state.rs:70-L81](../../crates/engine/src/state.rs#L70-L81)).
- **Impact:** A huge/malformed sidecar can consume memory and parsing CPU. The normal state file is app-generated and stored beside the part; no remote reachability was established.
- **Verification:** Add bounded parser tests for oversized and excessive-segment input in a temporary directory.
- **Recommendation:** [ROAD-005](07_IMPROVEMENT_ROADMAP.md#road-005-bound-resume-metadata).

### SEC-006 — HTTPS downgrade and plaintext FTP remain permitted

- **Severity:** P2 transport-hardening gap; risk depends on URL/protocol chosen and network threat model.
- **Status/confidence:** PARTIALLY VERIFIED. The engine's policy delegates redirects to reqwest's limited policy and does not add a scheme-downgrade rejection; actual automatic-redirect behavior was not runtime-tested.
- **Evidence:** manually scoped credentials are removed on HTTPS downgrade ([download.rs:536-L551](../../crates/engine/src/download.rs#L536-L551)); the redirect policy applies the hop limit but no explicit scheme check ([download.rs:590-L600](../../crates/engine/src/download.rs#L590-L600)). Plain FTP is selected for `ftp://` and has no TLS connector ([ftp.rs:105-L133](../../crates/engine/src/ftp.rs#L105-L133)).
- **Evidence:** manually scoped credentials are removed on HTTPS downgrade ([download.rs:536-L551](../../crates/engine/src/download.rs#L536-L551)); the redirect policy applies the hop limit but no explicit scheme check ([download.rs:590-L600](../../crates/engine/src/download.rs#L590-L600)). Plain FTP is selected for `ftp://` and has no TLS connector ([ftp.rs:105-L133](../../crates/engine/src/ftp.rs#L105-L133)).
- **Impact:** Download bytes may be fetched without transport confidentiality/integrity after an explicit HTTP URL or downgrade; FTP credentials/data are cleartext on plain FTP.
- **Verification:** Use a local HTTPS test endpoint redirecting to HTTP and a lab FTP endpoint; inspect whether a configured strict mode blocks or warns. Existing source indicates no universal strict mode.
- **Recommendation:** [ROAD-006](07_IMPROVEMENT_ROADMAP.md#road-006-explicit-transport-security-policy).

### SEC-007 — FTP passive data endpoint validation depends on third-party behavior

- **Severity:** UNKNOWN.
- **Status/confidence:** UNKNOWN; no vulnerability asserted.
- **Evidence:** `data_mode` configures EPSV/PASV and installs a custom route-bound connector only when an adapter is selected ([ftp.rs:271-L291](../../crates/engine/src/ftp.rs#L271-L291)). Unbound passive endpoint selection is delegated to pinned `suppaftp`, whose internals were not inspected.
- **Evidence:** `data_mode` configures EPSV/PASV and installs a custom route-bound connector only when an adapter is selected ([ftp.rs:271-L291](../../crates/engine/src/ftp.rs#L271-L291)). Unbound passive endpoint selection is delegated to pinned `suppaftp`, whose internals were not inspected.
- **Impact:** Exact protection against a malicious PASV address, unexpected data endpoint or control/data host mismatch cannot be established from this repository alone.
- **Verification:** Inspect locked `suppaftp` source and test PASV addresses pointing to unrelated/local targets, EPSV, redirects and route binding against a local harness.
- **Recommendation:** [ROAD-007](07_IMPROVEMENT_ROADMAP.md#road-007-audit-ftp-data-channel-boundaries).

## Performance Findings

### PERF-001 — Memory and blocking writes are not globally admitted

- **Severity:** P1 for high-concurrency service/automation use; P2 for ordinary desktop use.
- **Severity:** P1 for high-concurrency service/automation use; P2 for ordinary desktop use.
- **Status/confidence:** VERIFIED design limitation; high confidence.
- **Evidence:** the 256 MiB hold budget is attached to each download ([download.rs:46-L47](../../crates/engine/src/download.rs#L46-L47)) and divided by that download's connection count ([download.rs:3243-L3247](../../crates/engine/src/download.rs#L3243-L3247)). Batch writes use `spawn_blocking` ([download.rs:3278-L3297](../../crates/engine/src/download.rs#L3278-L3297)); queue admission counts are per queue ([queue.rs:155-L173](../../crates/app/src/queue.rs#L155-L173)).
- **Evidence:** the 256 MiB hold budget is attached to each download ([download.rs:46-L47](../../crates/engine/src/download.rs#L46-L47)) and divided by that download's connection count ([download.rs:3243-L3247](../../crates/engine/src/download.rs#L3243-L3247)). Batch writes use `spawn_blocking` ([download.rs:3278-L3297](../../crates/engine/src/download.rs#L3278-L3297)); queue admission counts are per queue ([queue.rs:155-L173](../../crates/app/src/queue.rs#L155-L173)).
- **Impact:** Concurrent downloads can multiply memory, sockets and queued blocking work; no global cap was found.
- **Verification:** Stress-test many downloads against a slow-flush/fault server while measuring peak RSS, blocking queue latency and active connections.
- **Recommendation:** [ROAD-003](07_IMPROVEMENT_ROADMAP.md#road-003-global-resource-admission).

### PERF-002 — Segment policy can generate extra requests

- **Severity:** P2.
- **Status/confidence:** PARTIALLY VERIFIED by checked-in historical benchmark, not independently rerun.
- **Evidence:** the planner splits remaining work by a fixed midpoint policy and initial target piece size is fixed ([planner.rs:184-L250](../../crates/engine/src/planner.rs#L184-L250), [download.rs:57](../../crates/engine/src/download.rs#L57)). Historical `run5` reports 31 vs 9 requests on one fast 1 GiB scenario and 24 vs 8 on a latency scenario ([run5/REPORT.md](../../docs/bench/run5/REPORT.md)); it is not a measurement of the audited revision.
- **Evidence:** the planner splits remaining work by a fixed midpoint policy and initial target piece size is fixed ([planner.rs:184-L250](../../crates/engine/src/planner.rs#L184-L250), [download.rs:57](../../crates/engine/src/download.rs#L57)). Historical `run5` reports 31 vs 9 requests on one fast 1 GiB scenario and 24 vs 8 on a latency scenario ([run5/REPORT.md](../../docs/bench/run5/REPORT.md)); it is not a measurement of the audited revision.
- **Impact:** Additional request, TLS/connection and scheduling overhead may affect some workloads; other scenarios in the same report favor MDM, so no general slowdown is concluded.
- **Verification:** Repeat controlled benchmark at audited revision on supported platforms, reporting distributions, request count, bytes wasted, CPU and connection churn.
- **Recommendation:** [ROAD-008](07_IMPROVEMENT_ROADMAP.md#road-008-measure-and-tune-segmentation).

### PERF-003 — Flush policy deliberately trades recovery point for disk throughput

- **Severity:** P2 reliability/performance tradeoff, not a defect.
- **Status/confidence:** VERIFIED design statement; not independently measured here.
- **Evidence:** sync thresholds and the derived maximum recovery interval are implemented in `Synced::due` ([download.rs:2248-L2271](../../crates/engine/src/download.rs#L2248-L2271)). This validates the policy, not its performance on the audit host.
- **Impact:** Less synchronization stalls, but crash/power loss can require redownloading the unsynced interval.
- **Recommendation:** Preserve as an explicit policy; measure on HDD/network volumes and expose durability tradeoffs if product requirements call for it. See [ROAD-009](07_IMPROVEMENT_ROADMAP.md#road-009-validate-durability-and-flush-tradeoffs).

## Reliability Findings

### PERF-004 — HTTP/2 and HTTP/3 are not configured

- **Severity:** P3 optimization opportunity; not a current correctness defect.
- **Status/confidence:** VERIFIED configuration. Performance impact is unmeasured.
- **Evidence:** reqwest features omit HTTP/2 and the engine explicitly calls `http1_only()` ([Cargo.toml:90](../../Cargo.toml#L90), [download.rs:1472-L1479](../../crates/engine/src/download.rs#L1472-L1479)).
- **Impact:** Multiplexing/HTTP/3 benefits are unavailable; the current design intentionally gives workers separate HTTP/1 connections.
- **Recommendation:** Evaluate only through controlled experiments that preserve worker/connection semantics; see [ROAD-008](07_IMPROVEMENT_ROADMAP.md#road-008-measure-and-tune-segmentation).

## Reliability Findings

### REL-001 — Resume has conditional rather than universal full-file integrity

- **Severity:** P2.
- **Status/confidence:** VERIFIED limitation; high confidence.
- **Evidence:** Resume validates available strong validators and rechecks default 64-byte overlap, but whole-file checksum is optional ([state.rs:118](../../crates/engine/src/state.rs#L118), [download.rs:99](../../crates/engine/src/download.rs#L99), [download.rs:2102](../../crates/engine/src/download.rs#L2102)).
- **Evidence:** resume uses strong validators ([state.rs:118-L153](../../crates/engine/src/state.rs#L118-L153)) and a default 64-byte overlap setting ([download.rs:99](../../crates/engine/src/download.rs#L99)); whole-file verification is conditional on a supplied/announced hash ([download.rs:2102-L2120](../../crates/engine/src/download.rs#L2102-L2120)).
- **Impact:** Changes outside re-fetched overlap cannot be ruled out when no strong validator or trusted hash exists.
- **Recommendation:** [ROAD-009](07_IMPROVEMENT_ROADMAP.md#road-009-validate-durability-and-flush-tradeoffs); document guarantee as conditional and add changed-file tests.

### REL-002 — Content-Range end is not validated

- **Severity:** P2 correctness validation gap.
- **Status/confidence:** VERIFIED source gap; no corruption verified.
- **Evidence:** `parse_content_range` returns `(start, end, total)` ([download.rs:3383](../../crates/engine/src/download.rs#L3383)); request/probe call sites ignore the end value ([download.rs:2997](../../crates/engine/src/download.rs#L2997)).
- **Evidence:** `parse_content_range` parses and returns `(start, end, total)` ([download.rs:3382-L3398](../../crates/engine/src/download.rs#L3382-L3398)); the request call site discards the end and checks start/known total ([download.rs:2997-L3002](../../crates/engine/src/download.rs#L2997-L3002)).
- **Impact:** An inconsistent declared range end can evade metadata-level rejection; body length and commit boundaries still provide some protection. Failure mode is not established without a targeted lab test.
- **Verification:** Fuzz Content-Range syntax/semantics and serve mismatched start/end/total/body combinations; assert no incorrect final bytes or false success.
- **Recommendation:** [ROAD-010](07_IMPROVEMENT_ROADMAP.md#road-010-strengthen-range-response-validation).

### REL-003 — Crash-window recovery identifies completion by size only

- **Severity:** P2 narrow local correctness gap.
- **Status/confidence:** VERIFIED source behavior; impact conditional; no false completion reproduced.
- **Evidence:** If part is absent and destination length equals saved size, resume removes state and reports success without checksum verification ([download.rs:1925](../../crates/engine/src/download.rs#L1925)).
- **Evidence:** If part is absent and destination length equals saved size, resume removes state and reports success without checksum verification ([download.rs:1927-L1934](../../crates/engine/src/download.rs#L1927-L1934)).
- **Impact:** A replacement file of the same size in the rename/state-removal window may be accepted as completed content.
- **Recommendation:** [ROAD-009](07_IMPROVEMENT_ROADMAP.md#road-009-validate-durability-and-flush-tradeoffs).

## Capability Matrix

| Capability                         | Current Status                | Evidence                                                                                                                                                                      | Risk                                                             | Recommendation |
| ---------------------------------- | ----------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------- | -------------- |
| Connection Pooling                 | Verified                      | Per-download reqwest client, HTTP/1 pool; FTP session pool ([download.rs:1472](../../crates/engine/src/download.rs#L1472), [ftp.rs:394](../../crates/engine/src/ftp.rs#L394)) | Pool is not shared across downloads                              | ROAD-003       |
| Adaptive Segmentation              | Partial                       | Largest remaining segment midpoint splits; fixed thresholds ([planner.rs:184](../../crates/engine/src/planner.rs#L184))                                                       | Request count/segment balance varies                             | ROAD-008       |
| TLS Verification                   | Verified in source            | Platform verifier, fail closed without roots ([tls.rs:30-L58](../../crates/engine/src/tls.rs#L30-L58), [tls.rs:92-L102](../../crates/engine/src/tls.rs#L92-L102))             | No runtime platform matrix in this audit                         | Test plan      |
| Path Traversal Protection          | Verified for server filenames | Single-component sanitization ([naming.rs:132](../../crates/engine/src/naming.rs#L132))                                                                                       | Explicit sidecar symlink and final rename race remain            | ROAD-001       |
| Memory Limits                      | Partial                       | 256 MiB per-download hold cap ([download.rs:46](../../crates/engine/src/download.rs#L46))                                                                                     | No global aggregate bound                                        | ROAD-003       |
| Resume Integrity                   | Partial                       | Strong validators + 64-byte overlap + optional hash ([download.rs:99](../../crates/engine/src/download.rs#L99))                                                               | No full guarantee without trusted validator/hash                 | ROAD-009       |
| Private/Local Destination Blocking | Missing                       | Route-reachability check only; no private-range policy ([net.rs:186-L197](../../crates/engine/src/net.rs#L186-L197))                                                          | Caller trust boundary dependent                                  | ROAD-002       |
| HTTP/2 / HTTP/3                    | Missing                       | HTTP/1-only ([download.rs:1472](../../crates/engine/src/download.rs#L1472))                                                                                                   | Potential efficiency loss; intentional worker model              | ROAD-008       |
| Redirect Limit                     | Verified                      | 20 HTTP redirects and 5 page hops ([download.rs:585](../../crates/engine/src/download.rs#L585), [download.rs:2522](../../crates/engine/src/download.rs#L2522))                | Automatic reqwest credential semantics not independently checked | Test plan      |
| Disk Preflight                     | Partial                       | 128 MiB margin and bounded probe ([disk.rs:11](../../crates/engine/src/disk.rs#L11), [disk.rs:37](../../crates/engine/src/disk.rs#L37))                                       | Skips unknown volumes; check/use race                            | Keep and test  |
| Global Download/Connection Limit   | Missing                       | Per-queue and per-download caps only ([queue.rs:155-L173](../../crates/app/src/queue.rs#L155-L173))                                                                           | Aggregate exhaustion                                             | ROAD-003       |
| Dependency Advisories              | Unknown / not verified        | No advisory database queried; lockfile is only an inventory ([Cargo.lock](../../Cargo.lock))                                                                                  | Vulnerable reachability unknown                                  | ROAD-011       |
| FTP Passive Data Destination       | Unknown                       | Default PASV/EPSV endpoint behavior delegated to suppaftp ([ftp.rs:271-L291](../../crates/engine/src/ftp.rs#L271-L291))                                                       | Malicious endpoint handling unverified                           | ROAD-007       |

## Prioritized Summary

| Priority | Findings                                  | Classification                                                                         |
| -------- | ----------------------------------------- | -------------------------------------------------------------------------------------- |
| P0       | None identified                           | No verified critical issue.                                                            |
| P1       | PERF-001 (service/high-concurrency use)   | Confirmed missing global admission bound; no observed production failure.              |
| P2       | SEC-001..006, PERF-002..003, REL-001..003 | Verified gaps/trade-offs with conditional impact; no exploit or corruption reproduced. |
| P3       | PERF-004                                  | HTTP/2/3 is an unmeasured optimization opportunity, not a defect.                      |

SEC-007 and DEP-001 remain unclassified because the relevant third-party FTP behavior and dependency advisory status are UNKNOWN / NOT VERIFIED, respectively.

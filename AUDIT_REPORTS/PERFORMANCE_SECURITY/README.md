# Performance and Security Audit

**Project:** Mister Download Manager (MDM)  
**Declared version:** 0.5.2 (`Cargo.toml`)  
**Audited revision:** `3f96c9bf43ceca9ddb01a2c8203e6f5b9b21e8f3` (`main`)  
**Method:** read-only source and checked-in evidence review. No build, test, benchmark, dependency audit, or external service query was run.

## Executive Summary

MDM has a substantial, purpose-built Rust download engine rather than a thin wrapper around a downloader. The engine implements parallel HTTP range and FTP REST transfers, dynamic segmentation, bounded per-download connections and write buffering, connection reuse, retry/cancellation logic, positional file writes, resume validation, atomic state replacement, optional checksums, and a test lab with fault injection. Existing design evidence and historical benchmarks show thoughtful attention to both throughput and recovery behavior.

The strongest verified protections are TLS certificate verification through the platform verifier, credential scoping on the engine's manually followed page redirects, server-filename sanitization, exclusive part-file creation when saving into a directory, request-header exclusion from resume metadata, strong HTTP validators and overlap checks for resume, and checksums when a trusted hash is supplied or announced. These controls are not universal: ordinary user-supplied URLs can reach loopback/private destinations, explicit-file part creation follows an existing symlink, HTTP URL user-info/query material is persisted, and HTTP/FTP cleartext modes remain available by design.

No P0 issue was established. The highest-priority confirmed hardening issue is the symlink-following behavior for a pre-existing `.mdm.part` sidecar in `Destination::File`; exploitability requires a writable destination directory and is local. The absence of a private-address policy is a verified capability gap, but is not by itself a verified SSRF vulnerability because the engine intentionally downloads user-selected URLs and the trust boundary of each caller differs. Resource use is bounded per transfer but not globally across an unbounded number of simultaneous downloads.

This is not a live security certification. The repository has an untracked `read.md` input file, which was preserved. The audit directory did not exist before this work. Dependency advisories, runtime behavior, cross-platform behavior, and performance measurements were not independently verified.

## Overall Assessment

| Area        | Assessment                                                                                                                                                                                                                                            |
| ----------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Performance | Strong concurrency and streaming design; most evident opportunities are global admission control and high request counts on some workloads. Historical Windows benchmark data is useful but not current, independently reproduced evidence.           |
| Security    | Good TLS, naming, and credential-handling controls in several paths; filesystem sidecar handling, URL credential persistence, and destination policy need explicit hardening decisions.                                                               |
| Reliability | Strong resume-state ordering, cancellation propagation, and end-to-end fault-injection coverage. Integrity without validators or a trusted checksum remains inherently limited; parent-directory durability and some crash windows need verification. |
| Confidence  | High for source-described control flow; medium for cross-platform filesystem semantics and dependency behavior; low/unknown where third-party internals or runtime tests are required.                                                                |

## Finding Summary

The authoritative register in [Findings and Risks](06_FINDINGS_AND_RISKS.md#authoritative-finding-registry) contains all 15 canonical findings and their evidence classifications. The executive summary covers each ID:

| ID       | Priority / status          | Summary                                                                                              |
| -------- | -------------------------- | ---------------------------------------------------------------------------------------------------- |
| SEC-001  | P2, conditional            | No engine-level private/local destination policy; caller trust determines exposure.                  |
| SEC-002  | P2, verified behavior      | Explicit-file `.mdm.part` creation follows and truncates an existing symlink target.                 |
| SEC-003  | P2, verified behavior      | HTTP URL user-info/query secrets can be persisted in resume state.                                   |
| SEC-004  | P2, partial                | Final destination check and rename have a platform-dependent TOCTOU race.                            |
| SEC-005  | P2, verified behavior      | Resume JSON has no explicit input-size or segment-count ceiling.                                     |
| SEC-006  | P2, partial                | HTTPS downgrade/plain FTP are allowed by the selected transport paths.                               |
| SEC-007  | Unknown                    | FTP passive data endpoint behavior depends on unreviewed suppaftp internals.                         |
| PERF-001 | P1 in high-concurrency use | Per-download limits do not bound aggregate memory, sockets or blocking writes.                       |
| PERF-002 | P2, partial                | Historical data shows extra requests in selected workloads; not a current benchmark.                 |
| PERF-003 | P2 trade-off               | Adaptive sync policy trades recovery point for disk throughput.                                      |
| PERF-004 | P3 opportunity             | Engine is intentionally HTTP/1-only; HTTP/2/3 impact is unmeasured.                                  |
| REL-001  | P2, verified limitation    | Resume integrity is conditional without a trusted validator or full-file hash.                       |
| REL-002  | P2, verified source gap    | Content-Range end metadata is parsed but not validated; absorbs the former PERF-005 cross-reference. |
| REL-003  | P2, conditional            | Crash-window completion is recognized by file size only.                                             |
| DEP-001  | Unknown / not verified     | No advisory database or dependency audit was consulted.                                              |

No P0 issue was established. SEC-002 is the leading local filesystem hardening issue; PERF-001 is the highest operational scaling concern for service/high-concurrency use.

## Reports

1. [Repository and Architecture](01_REPOSITORY_AND_ARCHITECTURE.md)
2. [Performance Assessment](02_PERFORMANCE_ASSESSMENT.md)
3. [Security Assessment](03_SECURITY_ASSESSMENT.md)
4. [Reliability Assessment](04_RELIABILITY_ASSESSMENT.md)
5. [Dependency Security](05_DEPENDENCY_SECURITY.md)
6. [Findings and Risks](06_FINDINGS_AND_RISKS.md)
7. [Improvement Roadmap](07_IMPROVEMENT_ROADMAP.md)
8. [Testing and Benchmark Plan](08_TESTING_AND_BENCHMARK_PLAN.md)

The capability matrix and prioritized findings are in [Findings and Risks](06_FINDINGS_AND_RISKS.md#capability-matrix). Recommendations use `ROAD-*` IDs and link back to finding IDs. See [Audit Validation](09_AUDIT_VALIDATION_REPORT.md) for the subsequent ID and evidence reconciliation.

# Dependency Security Assessment

## Scope

This review read the workspace manifests and lockfile references only. No `cargo tree`, `cargo audit`, `cargo deny`, advisory database lookup, dependency download, build, or update was run. Those operations can access registries/caches and are outside the approved read-only scope. **No claim that dependencies are vulnerability-free is made.**

## Security-Relevant Direct Dependencies

Exact requested versions/features are declared in the workspace manifest; `Cargo.lock` records resolved versions for the full transitive graph.

| Component                   | Manifest declaration                                                                                                       | Security/performance role and review note                                                                                                                                                |
| --------------------------- | -------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| reqwest                     | 0.13.5, rustls/stream/system-proxy/socks, no defaults ([Cargo.toml:90](../../Cargo.toml#L90))                              | HTTP client, redirect machinery, pooling, TLS integration and proxies. Engine sets HTTP/1-only. Dependency-level automatic redirect credential semantics were not audited independently. |
| rustls + platform verifier  | 0.23.46 and 0.7.1 ([Cargo.toml:94-L95](../../Cargo.toml#L94-L95))                                                          | TLS and system certificate validation. Engine source uses platform verifier; no insecure bypass was found in the reviewed client path.                                                   |
| Tokio / tokio-util          | 1.54.1 / 0.7.20 ([Cargo.toml:122-L123](../../Cargo.toml#L122-L123))                                                        | Async runtime, blocking pool, tasks, timeouts and cancellation tokens.                                                                                                                   |
| hickory-resolver            | 0.26.3 ([Cargo.toml:99](../../Cargo.toml#L99))                                                                             | Configurable DNS, DoT/DoH, resolver implementation. Its transitive TLS/HTTP internals were not separately assessed.                                                                      |
| suppaftp                    | 12.2.1 ([Cargo.toml:93](../../Cargo.toml#L93))                                                                             | FTP/FTPS control/data handling and session behavior; passive endpoint validation needs dependency-source review.                                                                         |
| URL/HTTP and hyper-util     | url 2.5.8 ([Cargo.toml:125](../../Cargo.toml#L125)); http/hyper-util ([engine/Cargo.toml](../../crates/engine/Cargo.toml)) | URL parsing, typed HTTP values and system-proxy matching.                                                                                                                                |
| serde / serde_json          | 1.0.229 / 1.0.154 ([Cargo.toml:115-L116](../../Cargo.toml#L115-L116))                                                      | Resume-state and application data serialization. Resume JSON has no explicit input byte limit before read.                                                                               |
| craft-atomic                | Git revision `1a11a4897865f7410c40d25ca6e452ce399a1a2c` ([Cargo.toml:28](../../Cargo.toml#L28))                            | Atomic persistence; source implementation is not vendored in this repository, so crash durability is partial/unknown.                                                                    |
| rquickjs                    | 0.14.0, std feature ([engine/Cargo.toml](../../crates/engine/Cargo.toml))                                                  | PAC script execution; manifest comment says no module loader/file/OS access. Native C build behavior and resource limits were not tested.                                                |
| socket2 / network-interface | 0.6.5 / 2.0.5 ([Cargo.toml:103-L104](../../Cargo.toml#L103-L104))                                                          | Adapter binding and enumeration; platform-specific socket behavior requires platform CI.                                                                                                 |
| aws-lc-rs and tokio-rustls  | 1.19.0 / 0.26.6 ([Cargo.toml:108](../../Cargo.toml#L108), [Cargo.toml:112](../../Cargo.toml#L112))                         | Cryptography and FTP/DNS TLS support; native/prebuilt components are part of supply-chain review.                                                                                        |
| Hash implementations        | md-5, sha1, crc32fast ([Cargo.toml:83-L85](../../Cargo.toml#L83-L85)); sha2 ([Cargo.toml:119](../../Cargo.toml#L119))      | Optional file integrity checks; not security boundaries when the expected hash is attacker-controlled.                                                                                   |

The application additionally depends on GUI, image, archive, XML/regex, keychain, platform API and native libraries; a full app supply-chain audit should enumerate all manifests, not just engine dependencies ([app/Cargo.toml](../../crates/app/Cargo.toml)).

## Transitive and Pinned Sources

- `Cargo.lock` is present and tracks crates.io packages and git-sourced packages. Workspace patches pin selected forks of librqbit, egui and winit to commit revisions ([Cargo.toml:190](../../Cargo.toml#L190)). The craft libraries are also revision-pinned ([Cargo.toml:28](../../Cargo.toml#L28)). Pinning gives reproducibility when the source is fetched, but does not prove upstream safety or review provenance.
- Runtime transport and crypto dependency families include reqwest/hyper/hickory/rustls/aws-lc-rs/tokio-rustls; parsing/state include url/http/serde; FTP includes suppaftp; PAC includes QuickJS. Their transitive versions are in `Cargo.lock` and should be checked against an up-to-date advisory feed in an authorized environment.
- No lockfile diff or dependency update was performed. No package is labeled vulnerable or unmaintained because advisory and maintenance data were not queried.

## Unsafe Rust and Native Build Surface

- Workspace Rust lint is `unsafe_code = "deny"` by default ([Cargo.toml:131](../../Cargo.toml#L131)). Engine's inspected unsafe blocks are limited to platform disk probes and Windows sparse-file IOCTL code ([disk.rs:86](../../crates/engine/src/disk.rs#L86), [sparse.rs:12](../../crates/engine/src/sparse.rs#L12)). The app has platform FFI in `unix.rs`; native messaging host has Windows FFI. These are explicit exceptions and need target-specific review.
- `crates/app/build.rs` is the identified project build script. App build dependencies include brotli and Windows `winresource` ([app/Cargo.toml:125](../../crates/app/Cargo.toml#L125)). Engine QuickJS and cryptography crates can involve native code/build artifacts. Build scripts were not executed or inspected dynamically.
- Source review did not establish the integrity of prebuilt native artifacts, the reproducibility of release builds, or whether every platform build uses identical dependency inputs.

## Advisory Status

**DEP-001 — UNKNOWN. Dependency advisory status: NOT VERIFIED.** No advisory database was queried. This is not a finding that a dependency is vulnerable. The next step is a read-only advisory scan with a current database after explicit authorization for registry/cache access, followed by review of each reachable advisory against the locked version and feature set. Do not update dependencies as part of that scan. See [ROAD-011](07_IMPROVEMENT_ROADMAP.md#road-011-verify-dependency-advisories).

## Recommended Dependency Verification

1. In an authorized environment, record the exact tool/database versions and run a lockfile-only advisory check; do not resolve or update dependencies.
2. Review reachable issues for reqwest/hyper, rustls/platform verifier, hickory, suppaftp, serde_json, Tokio, QuickJS, aws-lc and `craft-atomic` first.
3. Inspect the pinned source for craft-atomic atomicity/durability and suppaftp PASV/EPSV endpoint behavior.
4. Capture `cargo tree --locked` and a machine-readable SBOM as separate, non-mutating deliverables after confirming offline/cache behavior.
5. Preserve pinned revisions and test the complete app/engine feature graph before any upgrade.

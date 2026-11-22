# Repository and Architecture

## Audit Context

- Workspace version is 0.5.2, edition 2024, Rust version requirement 1.95, with resolver 3 and members selected by `crates/*` ([Cargo.toml:2](../../Cargo.toml#L2), [Cargo.toml:3](../../Cargo.toml#L3), [Cargo.toml:6-L8](../../Cargo.toml#L6-L8)).
- Audited Git revision: `3f96c9bf43ceca9ddb01a2c8203e6f5b9b21e8f3`, branch `main`; the working tree was clean except for untracked `read.md`. No local release tag was returned by `git tag --list`; the source workspace version, not a tag, establishes 0.5.2.
- Audit commands were source/list/status reads only. Build, test, benchmark, dependency resolution, cache-mutating, and network operations were not run.

## Workspace and Crate Responsibilities

The workspace has these project crates and support areas. The crate roles below are from the repository architecture and manifests; this audit focuses on `mdm-engine` and its app/CLI callers.

| Path                                                      | Responsibility                                                                            |
| --------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| `crates/engine`                                           | HTTP(S)/FTP(S) download engine, planning, network, disk, state, checksums, proxy scripts. |
| `crates/app`                                              | Desktop UI, queues, scheduler, settings, persistence, lifecycle and the Tokio runtime.    |
| `crates/cli`                                              | Command-line operations implemented through the app and engine.                           |
| `crates/ipc`                                              | Local single-instance and automation channel, data directory and capture rules.           |
| `crates/remote`                                           | Paired remote-control HTTPS server.                                                       |
| `crates/nmhost` and `extension/`                          | Browser native messaging and extension integration.                                       |
| `crates/mcp`                                              | MCP-facing automation integration.                                                        |
| `crates/debrid`                                           | Third-party debrid service APIs and link conversion.                                      |
| `crates/torrent`                                          | BitTorrent engine integration.                                                            |
| `crates/extract`                                          | Archive extraction.                                                                       |
| `crates/geo`                                              | Offline IP-to-country lookup.                                                             |
| `crates/tools`                                            | Video/gallery helper process integration and network relay.                               |
| `crates/crash`                                            | Crash/freeze reporting.                                                                   |
| `crates/labserver`                                        | Local HTTP range and FTP/FTPS test server with fault injection.                           |
| `docs/`, `tools/`, `website/`, `i18n/`, `art/`, `docker/` | Documentation, benchmarks/build tooling, website, translations, assets, deployment.       |

The documented dependency direction is inward: app, CLI, MCP, native host and remote layers use the engine/IPC, while the engine is UI-independent ([ARCHITECTURE.md](../../docs/ARCHITECTURE.md)). The workspace denies unsafe Rust by default ([Cargo.toml:131](../../Cargo.toml#L131)).

## Engine Components

The public engine module exposes download, disk, DNS, FTP, hosts, limiter, naming, network, PAC, planner, state, TLS, unlock and checksum verification modules ([lib.rs:1](../../crates/engine/src/lib.rs#L1)). The main implementation is in `download.rs`; the planner is a separate pure state machine; FTP has its own control/data-session pool.

| Concern                                                       | Implementation location                                                                                                                                                           | Observed responsibility                                                                                                 |
| ------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| Download lifecycle, options, HTTP clients, workers and events | [download.rs:937](../../crates/engine/src/download.rs#L937), [download.rs:1483](../../crates/engine/src/download.rs#L1483)                                                        | `Download::start`, `resume`, `cancel`, `wait`; `run_fresh`, `run_resume`, supervision and workers.                      |
| HTTP probe and initial response                               | [download.rs:1512](../../crates/engine/src/download.rs#L1512), [download.rs:2369](../../crates/engine/src/download.rs#L2369)                                                      | The first GET (or configured POST) becomes the byte-zero worker; redirects and supported waiting pages are followed.    |
| Dynamic segmentation                                          | [planner.rs:184](../../crates/engine/src/planner.rs#L184)                                                                                                                         | Assign open work, preempt pending work, split largest remaining segment; claims precede writes and commits follow them. |
| HTTP request/response range validation                        | [download.rs:2929](../../crates/engine/src/download.rs#L2929), [download.rs:3383](../../crates/engine/src/download.rs#L3383)                                                      | Range request, status handling, content-range parsing, start/known-total checks.                                        |
| Buffering and disk writes                                     | [download.rs:3084](../../crates/engine/src/download.rs#L3084), [download.rs:3278](../../crates/engine/src/download.rs#L3278)                                                      | Stream chunks into per-worker batches, positional writes through `spawn_blocking`, commit the written offset.           |
| FTP/FTPS                                                      | [ftp.rs:47](../../crates/engine/src/ftp.rs#L47), [ftp.rs:340](../../crates/engine/src/ftp.rs#L340)                                                                                | FTP scheme selection, SIZE/MDTM/FEAT, REST+RETR ranges, active/passive modes, FTPS, pooled sessions.                    |
| HTTP client/network route                                     | [download.rs:1325](../../crates/engine/src/download.rs#L1325), [net.rs:212](../../crates/engine/src/net.rs#L212)                                                                  | Proxy selection, DNS, adapter binding, redirects and HTTP/1-only client setup.                                          |
| DNS                                                           | [dns.rs:497](../../crates/engine/src/dns.rs#L497)                                                                                                                                 | System DNS or user-selected plain TCP, DoT or DoH; connection racing and adapter routing.                               |
| Resume and validators                                         | [state.rs:20](../../crates/engine/src/state.rs#L20), [download.rs:1904](../../crates/engine/src/download.rs#L1904)                                                                | JSON sidecar state, validators, saved ranges and state restoration.                                                     |
| Naming and path selection                                     | [naming.rs:12](../../crates/engine/src/naming.rs#L12), [download.rs:1719](../../crates/engine/src/download.rs#L1719)                                                              | Server/URL filename parsing, sanitization, reservation and part-file creation.                                          |
| Finalization and checksum                                     | [download.rs:2102](../../crates/engine/src/download.rs#L2102), [download.rs:2127](../../crates/engine/src/download.rs#L2127), [verify.rs:1](../../crates/engine/src/verify.rs#L1) | Optional full-file hash, final length, sparse-file conversion, sync and rename.                                         |
| Disk-space check                                              | [disk.rs:1](../../crates/engine/src/disk.rs#L1)                                                                                                                                   | Free-space/filesystem probe on a blocking thread with a bounded wait; 128 MiB margin and FAT32 check.                   |
| Speed limits and per-host caps                                | [limiter.rs:1](../../crates/engine/src/limiter.rs#L1), [hosts.rs](../../crates/engine/src/hosts.rs)                                                                               | Per-download and shared token buckets; learned host connection limits.                                                  |

## Execution Flows

1. **New HTTP(S) download:** a caller constructs a `Request`/`Options`; `Download::start` spawns a Tokio task. The engine creates a client, optionally obtains a service URL, sends the first request, interprets response size/range/validators/name, preflights disk space, creates a sidecar part file, and starts the planner/supervisor ([download.rs:943](../../crates/engine/src/download.rs#L943), [download.rs:1512](../../crates/engine/src/download.rs#L1512)).
2. **Segments:** one initial body is retained from offset zero; workers get remaining or split ranges, write using positional I/O, and commit offsets. Completion is followed by optional checksum verification, sync and rename ([download.rs:2684](../../crates/engine/src/download.rs#L2684), [download.rs:2773](../../crates/engine/src/download.rs#L2773)).
3. **Resume:** load the sidecar, validate its identity/path/version and segment tiling, open the part file, refresh an expired redirect target when possible, and continue ranges with validators and overlap comparison ([state.rs:70-L81](../../crates/engine/src/state.rs#L70-L81), [download.rs:1904](../../crates/engine/src/download.rs#L1904)).
4. **FTP(S):** create a per-download `Site`, establish control sessions, inspect facts and fetch byte ranges using REST+RETR through the same planner; return sessions to an idle pool ([ftp.rs:72](../../crates/engine/src/ftp.rs#L72), [ftp.rs:340](../../crates/engine/src/ftp.rs#L340)). FTP is not routed through the HTTP proxy, per the implementation comment in `ftp.rs`.
5. **Application scheduling:** the app's `Runtime::new()` runs downloads on Tokio; UI reads snapshots ([main.rs:314](../../crates/app/src/main.rs#L314)). Each queue starts at most its configured `files_at_once` count ([queue.rs:153](../../crates/app/src/queue.rs#L153)); the engine has no observed application-wide download admission limit.

## Dependency Map

Direct engine dependencies include reqwest, Tokio, bytes, URL/HTTP types, serde/JSON, rustls/platform verification, hickory-resolver, suppaftp, socket2, hyper-util, `craft-atomic`, hashing crates and QuickJS for PAC ([engine/Cargo.toml](../../crates/engine/Cargo.toml)). Workspace versions/features are declared in [Cargo.toml:90](../../Cargo.toml#L90), [Cargo.toml:93-L99](../../Cargo.toml#L93-L99), and [Cargo.toml:115-L125](../../Cargo.toml#L115-L125). The workspace pins craft-libs and selected forked dependencies to Git revisions ([Cargo.toml:28](../../Cargo.toml#L28), [Cargo.toml:190](../../Cargo.toml#L190)); `Cargo.lock` contains the resolved graph. Dependency advisory status is not established; see [Dependency Security](05_DEPENDENCY_SECURITY.md).

## Tests and Benchmarks Located

Engine integration suites include `e2e.rs`, `ftp.rs`, `dns.rs`, `drive.rs`, `post.rs`, `server.rs` and `unlock.rs`; the planner has a dedicated unit-test module. The local labserver provides range and fault scenarios. A Python benchmark harness and historical result bundle exist under `tools/bench/` and `docs/bench/run5/`. These were inspected, not run; details and limitations are in [Testing and Benchmark Plan](08_TESTING_AND_BENCHMARK_PLAN.md).

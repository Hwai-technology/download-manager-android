# Security Assessment

## Scope and Conclusion

This is a static source review. No malicious server, browser, proxy, filesystem race, packet capture, fuzzing, or runtime test was executed. Findings distinguish verified code behavior from conditional security impact. There is no confirmed remote code execution or remotely exploitable vulnerability in the evidence reviewed.

The strongest controls are certificate verification, credential-aware redirect handling in engine-managed page hops, filename sanitization, selected network-adapter binding, and resume-state omission of request headers. These coexist with deliberately broad user-selected URL access and supported plaintext protocols.

## URL and Input Validation

- The engine API receives parsed `url::Url` values and `supported()` recognizes HTTP, HTTPS and FTP-family schemes ([download.rs:199](../../crates/engine/src/download.rs#L199)). Header values use typed `HeaderMap`/`HeaderValue`, and reqwest builds requests from those values.
- The engine's `net::check_url` checks only whether a destination IP is reachable through the selected adapter; it does not classify or reject private, loopback, link-local, multicast, or metadata-service addresses ([net.rs:186-L197](../../crates/engine/src/net.rs#L186-L197)). DNS filtering likewise applies route reachability rather than a private-address policy ([dns.rs:497-L535](../../crates/engine/src/dns.rs#L497-L535)).
- Redirects are capped at 20 in the HTTP client, with up to 5 additional page-driven redirects; this bounds redirect loops ([download.rs:585](../../crates/engine/src/download.rs#L585), [download.rs:2522](../../crates/engine/src/download.rs#L2522)). With no adapter selected, redirects can lead to any scheme/host accepted by reqwest; with a bound adapter, route checks apply but do not prevent reaching local services through that adapter.
- **SEC-001:** no explicit SSRF/private destination defense is verified. Because MDM is a user-facing download manager, permitting user-chosen local URLs can be intentional. If browser, remote-control, automation or MCP inputs are considered untrusted, this becomes a trust-boundary issue. Severity is conditional; not a verified vulnerability.
- FTP passive mode uses EPSV when supported and a NAT workaround for PASV. The engine does not itself show explicit validation of every data-channel endpoint when using an unbound route; exact behavior is delegated to pinned `suppaftp` and is **UNKNOWN** pending dependency-source review ([ftp.rs:256](../../crates/engine/src/ftp.rs#L256)).

## Network and TLS Security

- HTTPS clients flow through `net::bind_client`, which applies `tls::configure`; the platform verifier is used when system roots exist. On Linux without roots, secure connections fail closed rather than accepting invalid certificates ([net.rs:212-L228](../../crates/engine/src/net.rs#L212-L228), [tls.rs:30-L58](../../crates/engine/src/tls.rs#L30-L58), [tls.rs:92-L102](../../crates/engine/src/tls.rs#L92-L102)). No insecure certificate bypass is configured in the engine client source.
- FTPS uses the platform verifier and optional additional roots; implicit and explicit TLS are implemented ([ftp.rs:60-L68](../../crates/engine/src/ftp.rs#L60-L68), [ftp.rs:468-L489](../../crates/engine/src/ftp.rs#L468-L489)). Plain FTP is selected for `ftp://` and does not install a TLS connector ([ftp.rs:105-L133](../../crates/engine/src/ftp.rs#L105-L133)).
- HTTP credential scoping removes Cookie, Authorization and Proxy-Authorization when a manually followed page hop changes host/port or downgrades HTTPS; same-party site hosts are an explicit exception. Cookies are added only for allowed same-host/site hops ([download.rs:536](../../crates/engine/src/download.rs#L536), [download.rs:2420](../../crates/engine/src/download.rs#L2420)). reqwest-managed automatic redirect details are inherited from the dependency and were not separately verified from its implementation.
- HTTPS-to-HTTP redirects are not categorically blocked by the engine's redirect policy. The manually followed page path strips credentials on downgrade, but may still follow and transfer content over HTTP. **SEC-006** is a hardening/transport downgrade gap, not proof that credentials leak through this path.
- Proxy modes include none, system, manual and PAC. Manual proxy basic credentials are passed into reqwest's proxy configuration; FTP explicitly does not use the HTTP proxy ([download.rs:1325](../../crates/engine/src/download.rs#L1325), [ftp.rs:20](../../crates/engine/src/ftp.rs#L20)). Whether all proxy credential storage and operating-system proxy semantics are secure was not dynamically tested.
- Connect and read timeouts default to 30 seconds; retry counts are finite and Retry-After is capped at 5 minutes ([download.rs:212](../../crates/engine/src/download.rs#L212), [download.rs:430](../../crates/engine/src/download.rs#L430)). Redirect count is bounded.
- The configured HTTP protocol is HTTP/1.1 only; no HTTP/2 or HTTP/3 path is active ([download.rs:1472](../../crates/engine/src/download.rs#L1472)).

## Filesystem Security

- Server `Content-Disposition`/URL names are reduced to one component and sanitized for Windows/Unix separators, control characters, reserved device names, trailing dots/spaces and selected Unicode format characters ([naming.rs:12](../../crates/engine/src/naming.rs#L12), [naming.rs:132](../../crates/engine/src/naming.rs#L132)). This is a verified path-traversal mitigation for server-suggested filenames.
- Directory destinations create the part file with `create_new(true)`, avoiding following a pre-existing part file in the ordinary directory path ([download.rs:1764](../../crates/engine/src/download.rs#L1764)).
- **SEC-002:** explicit `Destination::File` instead uses `create(true).truncate(true)` on the `.mdm.part` path, without `create_new` or no-follow semantics ([download.rs:1746-L1752](../../crates/engine/src/download.rs#L1746-L1752)). Directory mode uses exclusive creation ([download.rs:1764-L1768](../../crates/engine/src/download.rs#L1764-L1768)). Standard filesystem behavior follows an existing symlink, so a party able to plant that sidecar in a writable target directory may cause truncation of another file writable by the MDM user. This is verified source behavior with conditional local impact; no exploit was run.
- Resume opens the part path read/write and follows normal filesystem resolution ([download.rs:1969](../../crates/engine/src/download.rs#L1969)). Sidecar ownership, symlink identity and restrictive permissions are not explicitly checked in this code.
- Finalization checks whether the destination exists, picks a numbered path if so, then calls `rename`; the check and rename are separate ([download.rs:2148-L2169](../../crates/engine/src/download.rs#L2148-L2169)). A competing process may create the target between those operations. Whether replacement occurs is platform-specific; a no-replace atomic operation is not shown. **SEC-004** is a local race, not a reproduced overwrite.
- No explicit file-mode policy is visible in the engine's `OpenOptions`; effective permissions inherit OS defaults/umask. Atomic state helper internals and parent-directory durability are not audited.

## HTTP Response Validation

- Fresh HTTP accepts 200 or an initial 206 whose Content-Range starts at zero; a known total is used for planning ([download.rs:1578](../../crates/engine/src/download.rs#L1578)). Ranged requests require 206, expected start offset and a matching total when the server supplies one; 200 responses are treated as no-range, stale-file or range-unavailable cases ([download.rs:2960](../../crates/engine/src/download.rs#L2960)).
- `Content-Range` syntax is parsed, but the parsed end is discarded at call sites; the helper does not validate `start <= end` or compare the declared end against the requested end ([download.rs:3383](../../crates/engine/src/download.rs#L3383)). Stream completion/body length and planner commits provide additional protection. No corrupt output was demonstrated. **REL-002** requests explicit boundary validation and malformed-response tests.
- For a ranged response, a supplied total must match the planned size; `*` is accepted. `Content-Length` is used for a 200's expected size and checked in the single-stream path. A 416 has no dedicated case and falls through generic status/error handling; retry/final behavior is bounded but not specially classified.
- Strong ETag and strong Last-Modified validators are used for If-Range. Weak/malformed ETags are not accepted; dates qualify only when Date is at least one second later ([state.rs:118](../../crates/engine/src/state.rs#L118)). A 64-byte default overlap check verifies bytes at resumed/taken-over segment boundaries ([download.rs:99](../../crates/engine/src/download.rs#L99)).
- The engine manifest enables reqwest rustls, stream, system-proxy and socks features, not its compression features ([Cargo.toml:90](../../Cargo.toml#L90)). Exact Content-Encoding negotiation/response semantics were not runtime-tested; do not assume transparent decompression.

## Authentication and Sensitive Data

- Request headers (including cookies and Authorization) are deliberately not stored in `ResumeState`; users provide them again on resume ([state.rs:1](../../crates/engine/src/state.rs#L1)). Debrid account secrets are stored in system keychains by the app design documented in [ARCHITECTURE.md](../../docs/ARCHITECTURE.md).
- FTP removes the password before creating resume state (`without_password`) ([ftp.rs:50](../../crates/engine/src/ftp.rs#L50), [download.rs:1851](../../crates/engine/src/download.rs#L1851)).
- **SEC-003:** HTTP `ResumeState::new` receives `req.url.clone()` as `url` and the response's final URL; `Url` serialization includes user-info and query parameters ([download.rs:1655](../../crates/engine/src/download.rs#L1655), [state.rs:20](../../crates/engine/src/state.rs#L20)). Therefore HTTP URL credentials or signed/query tokens can be persisted in the `.mdm.json` sidecar even though header credentials are omitted. Exposure is local to the download folder and depends on URL contents and file permissions.
- Engine event messages generally record the destination or host rather than the full URL; error formatting and crash-report paths were not exhaustively audited. Treat sensitive query strings as persisted until explicitly redacted.

## Resource Exhaustion

- Per-download connections clamp to 1..128; normal engine maximum is 32, and per-host learned limits can lower it. The app queue limit is per queue, not global ([download.rs:63](../../crates/engine/src/download.rs#L63), [queue.rs:155](../../crates/app/src/queue.rs#L155)).
- Per-download buffered data has the nominal 256 MiB hold budget, but there is no global memory/connection admission budget. This is **PERF-001**.
- Unknown-size HTTP responses stream to disk rather than being accumulated wholly in memory. Server-provided pages read for redirects are capped at 256 KiB and page waits at 10 seconds ([download.rs:2524](../../crates/engine/src/download.rs#L2524)).
- Resume JSON size and segment count are unbounded before parsing (**SEC-005** hardening gap). Disk preflight helps with known sizes but can be skipped if volume probing fails and is subject to races.
- Retry attempts and redirect chains are finite. A retry delay can keep an individual worker occupied up to the configured/capped delay, but cancellation is propagated.

## Security Recommendations

Prioritize **ROAD-001** atomic/no-follow part-file handling and **ROAD-002** a caller-aware outbound destination policy. Then redact URL user-info and secret query keys before state persistence (**ROAD-004**), add resume-state size/count caps (**ROAD-005**), decide whether HTTPS downgrades and plaintext FTP need explicit warnings or policy controls (**ROAD-006**), and verify FTP passive data endpoint behavior against the locked library revision. Related details: [Findings and Risks](06_FINDINGS_AND_RISKS.md) and [Improvement Roadmap](07_IMPROVEMENT_ROADMAP.md).

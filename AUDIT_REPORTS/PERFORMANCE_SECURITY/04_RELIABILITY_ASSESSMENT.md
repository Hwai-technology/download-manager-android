# Reliability Assessment

## Download Lifecycle

`Download::start` creates shared observable state and spawns one Tokio task; `resume` loads a saved state and re-enters the same run wrapper. `cancel()` signals a `CancellationToken`, while `wait()` joins the task and reports the final outcome ([download.rs:943](../../crates/engine/src/download.rs#L943)). The exposed state machine includes Probing, Downloading, Verifying, Completing, Done, Failed and Cancelled ([download.rs:675](../../crates/engine/src/download.rs#L675)).

## Segmentation and Correctness

- The planner tracks each half-open byte segment with written cursor and reserved bytes; workers claim bytes before writing and commit afterward. Splits occur at/after reserved offsets, avoiding overlap between concurrent workers ([planner.rs:184-L250](../../crates/engine/src/planner.rs#L184-L250)).
- Responses must start at the requested offset; a supplied total must match the expected file size. HTTP 200 after ranges were established is compared against validators/length to classify changed files or unavailable ranges ([download.rs:2971-L3002](../../crates/engine/src/download.rs#L2971-L3002)).
- **REL-002:** the Content-Range end is parsed but not validated against the request, and the parser accepts syntactically ordered/unordered numeric values without checking `a <= b`; this is incomplete metadata validation. The body length, short-body handling and positional commit provide additional guards, but malformed-response outcomes need targeted tests. No final-file corruption was demonstrated.
- If a server refuses ranges, the engine falls back to one stream or restarts when already assigned bytes cannot be safely reconciled. The e2e suite contains no-range scenarios in source ([download.rs:1664](../../crates/engine/src/download.rs#L1664), [e2e.rs](../../crates/engine/tests/e2e.rs)).

## Resume and Cancellation

- Resume metadata contains URL/final URL, destination, expected size, validators, segment progress, content type and optional checksum. Request headers are omitted. Segment geometry is validated to tile `[0, size)` before restore ([state.rs:20](../../crates/engine/src/state.rs#L20), [planner.rs:111-L132](../../crates/engine/src/planner.rs#L111-L132)).
- The part file must exist and match saved size; HTTP validators are used where strong validators exist. The default 64-byte overlap checks the preceding part bytes before continuing a segment. FTP checks size and modification time and also uses overlap as transfers resume ([download.rs:1949](../../crates/engine/src/download.rs#L1949), [download.rs:3118-L3130](../../crates/engine/src/download.rs#L3118-L3130)).
- Cancellation is selected while waiting for network chunks and bandwidth tokens. On cancellation, already claimed buffered data is flushed; supervisor signals workers and joins them instead of aborting blocking file operations ([download.rs:2773-L2782](../../crates/engine/src/download.rs#L2773-L2782), [download.rs:3084-L3125](../../crates/engine/src/download.rs#L3084-L3125)). This avoids a worker continuing to write after return.
- **REL-001:** without a strong server validator, trusted checksum or whole-file comparison, overlap verifies only a small boundary, not all previously downloaded bytes. A server changing bytes outside the checked overlap can leave a mixed-version file undetected. This is a conditional integrity limitation, not evidence that ordinary resumes are corrupt.

## State Persistence and Crash Recovery

- Before saving segment positions, the part file is synchronized with `sync_data`; JSON is written through `craft_atomic::atomic_write` ([state.rs:86-L101](../../crates/engine/src/state.rs#L86-L101)). State saves are triggered on progress/time policy and at stop; saves do not claim bytes that were not synchronized ([download.rs:2176-L2208](../../crates/engine/src/download.rs#L2176-L2208)).
- Completion synchronizes the final file with `sync_all`, renames `.mdm.part` into place, and removes the state file afterward. This ordering intentionally leaves a state marker through the rename window ([download.rs:2127](../../crates/engine/src/download.rs#L2127)).
- If the part is absent but the destination exists at the expected size, resume treats the file as already complete and removes state without rechecking an optional saved checksum ([download.rs:1927-L1934](../../crates/engine/src/download.rs#L1927-L1934)). This is a narrow crash-recovery shortcut and assumes the destination is the just-renamed part. A replacement during that window could be misidentified; no runtime reproduction was performed.
- `craft-atomic` is pinned to a Git revision, but its implementation was not available locally in the inspected source tree. Atomic replacement semantics, directory metadata sync and power-loss guarantees are therefore **PARTIALLY VERIFIED**, not independently proven.

## Final File, Naming and Integrity

- Server-suggested names are sanitized into a single component and unique numbered names are used for existing destinations. Directory-based part creation is exclusive. Explicit-file sidecar creation is not exclusive and can follow a sidecar symlink; see **SEC-002**.
- Finish sets the known size, clears sparse mode where possible, synchronizes, then renames. There is a time-of-check/time-of-use interval between the destination existence check and rename; see **SEC-004** in [Findings and Risks](06_FINDINGS_AND_RISKS.md).
- Checksums can be user-supplied or read from supported response headers and are calculated on a blocking thread. Algorithms include CRC32, MD5, SHA-1, SHA-256 and SHA-512; the strongest supported announced value is selected ([verify.rs:12](../../crates/engine/src/verify.rs#L12), [download.rs:2102](../../crates/engine/src/download.rs#L2102)). A checksum is optional, and server-provided hashes are only as trustworthy as the transport/server that supplied them.

## Retry and Error Handling

Connection retries are finite by default (five retries after initial attempt); exponential delays cap at 30 seconds, while 429/503 Retry-After is capped at 300 seconds ([download.rs:430](../../crates/engine/src/download.rs#L430)). The app queue separately moves failed downloads to the end and retries every 30 seconds subject to queue settings ([queue.rs:1](../../crates/app/src/queue.rs#L1)). Errors distinguish cancellation, status, changed file, short body, bad range, disk full, checksum mismatch and network failures in engine code.

## Overall Reliability Rating

**Good foundations, conditional integrity guarantees.** Resume-state ordering, segment claims, cancellation joins, final file sync, and fault-injection tests are meaningful protections. The main gaps are untrusted metadata size, missing explicit range-end validation, lack of full integrity when no trusted hash/validator exists, size-only crash-window recognition, and unverified atomic-helper/platform durability semantics. No test suite was executed in this audit.

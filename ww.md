# Task: Audit, Correct, and Complete the Docker Build Pipeline

**Project:** Mister Download Manager Native Mobile  
**Priority:** P0 — Build Infrastructure  
**Execution:** Repository Inspection → Root Cause Analysis → Implementation → Build Verification

## 1. Objective

Audit and correct the existing Dockerfile supplied for Mister Download Manager Native Mobile, then implement a complete multi-stage Docker Compose pipeline.

The mandatory execution command is:

```bash
docker compose up --build
```

This command must orchestrate the complete default build pipeline in the correct order, terminate completed containers, propagate failures, and persist all required artifacts on the host.

This is an implementation task, not a documentation-only task.

## 2. Critical Rules

- Inspect the actual repository before modifying anything.
- Treat the supplied Dockerfile as an implementation draft, not verified configuration.
- Preserve the existing Rust engine and mobile architecture.
- Do not change download behavior or business logic.
- Do not introduce React Native or Flutter.
- Do not invent Cargo features, Rust targets, Android SDK versions, NDK versions, Gradle tasks, paths, or dependency versions.
- Do not silently ignore build errors.
- Do not use `|| true` to hide missing required artifacts.
- Do not modify unrelated application source files.
- Do not create Git commits.

## 3. Audit the Existing Dockerfile

Inspect every stage and correct the following areas.

### A. Rust Toolchain

Inspect:
- `Cargo.toml`
- `Cargo.lock`
- `rust-toolchain.toml`, if present
- Workspace `rust-version`
- Rust edition requirements

Requirements:
- Pin a compatible Rust version.
- Do not assume Rust 1.85 is sufficient.
- Do not use an unpinned `stable` toolchain for a reproducible release pipeline.
- Install only the Rust targets actually required.
- Preserve desktop build compatibility.

### B. Workspace and Dependency Copying

Inspect all workspace members and their manifests.

Requirements:
- Ensure all required workspace files are copied into the Docker build context.
- Include required configuration files and source directories.
- Account for local path dependencies.
- Preserve lockfile reproducibility.
- Use `cargo fetch --locked` where appropriate.
- Do not copy only `crates/` if other workspace members or local dependencies are required.

### C. rquickjs-sys Patches

Investigate this existing block:

```dockerfile
RUN set -eu; \
    RQUICKJS_SYS_DIR=$(find /usr/local/cargo/registry/src/ -type d -name "rquickjs-sys*" 2>/dev/null | head -n 1 || true); \
    if [ -n "$RQUICKJS_SYS_DIR" ] && [ -d "$RQUICKJS_SYS_DIR/src/bindings" ]; then \
        cp docker/patches/rquickjs-sys/*.rs "$RQUICKJS_SYS_DIR/src/bindings/" 2>/dev/null || true; \
    fi
```

Requirements:
1. Identify why these patches exist.
2. Inspect their actual contents.
3. Identify the exact `rquickjs-sys` version.
4. Determine whether they are still required.
5. Remove the patch mechanism if unnecessary.
6. If required, apply patches explicitly and reproducibly.
7. Fail clearly when a required patch cannot be applied.
8. Never silently mutate arbitrary Cargo registry directories.

Do not remove a required patch without establishing that the build succeeds without it.

### D. Android SDK and NDK

Inspect:
- Android Gradle Plugin version.
- Gradle wrapper version.
- compileSdk and targetSdk.
- NDK requirements.
- Existing Rust Android build scripts.
- Rust target linker configuration.
- Android ABI filters.

Requirements:
- Pin compatible Android command-line tools, SDK platform, build tools, and NDK versions.
- Verify that the installed NDK is actually used by Rust and Gradle.
- Verify `CC`, `AR`, linker, and required environment variables when applicable.
- Build the actual supported ABIs.
- Do not assume that installing an NDK automatically configures Rust cross-compilation.

Expected ABIs, subject to repository verification:

- `arm64-v8a`
- `x86_64`

### E. Native Library Verification

Reuse:

```bash
./tools/build-android-libs.sh
```

if it is the correct existing build entry point.

Verify:
- Both ABI outputs exist.
- Each `.so` is a valid shared library.
- Each binary matches its intended architecture.
- JNI symbols are exported as expected.
- C-ABI symbols are exported as expected.
- Libraries are copied to the actual Android `jniLibs` directory.

Do not treat symbol inspection alone as proof of runtime JNI correctness.

### F. Rust Verification Stage

Correct the existing `rust-verifier` stage.

Inspect the actual workspace and determine which commands are valid and necessary, including:

```bash
cargo test -p mdm-mobile
cargo check -p mdm-app
```

Requirements:
- Verify Cargo package names.
- Verify required features.
- Ensure the correct native or host target is used.
- Ensure verification is connected to the default Docker Compose pipeline.
- Do not allow BuildKit to skip a required verification stage.

### G. Android Gradle Build

Inspect the actual Android project.

Build the appropriate existing Gradle tasks for:

- Debug APK.
- Release APK.
- Release AAB.

Verify:
- Gradle wrapper works.
- Native libraries are packaged.
- ABI filters match the generated libraries.
- Build outputs exist at their actual locations.
- Release signing behavior matches the Gradle configuration.

Do not claim that release artifacts are signed unless signing is actually configured and verified.

### H. Android Signing

Implement secure signing support.

Requirements:
- Accept externally provided credentials.
- Mount the keystore file securely into the build container.
- Pass only the necessary environment variables to Gradle.
- Never COPY keystore files into Docker images.
- Never commit credentials or signing files.
- Fail clearly if production signing is requested but required credentials are missing.
- Support unsigned release artifacts only when explicitly configured and documented.

Do not invent Gradle signing properties. Inspect and adapt the real Android Gradle configuration.

### I. Artifact Collector

Correct the existing collector.

It must collect:

```text
build-output/
├── rust/
│   └── jniLibs/
│       ├── arm64-v8a/
│       └── x86_64/
├── android/
│   ├── debug/
│   ├── release/
│   └── bundle/
└── ios/
    └── build-preparation/
```

Requirements:
- Inspect actual Gradle output names and locations.
- Verify required files exist before declaring success.
- Fail if required APK/AAB or native libraries are missing.
- Do not use `|| true` to conceal missing artifacts.
- Preserve output through a host-mounted volume.
- Avoid copying unrelated intermediate build files.
- Include the iOS header and build script only if they actually exist.

## 4. Implement Docker Compose

Create or correct:

```text
compose.yaml
```

The default command must be:

```bash
docker compose up --build
```

Implement ordered services with responsibilities equivalent to:

1. `rust-check`
2. `android-libs`
3. `android-app`
4. `release-artifacts`

Use:

```yaml
depends_on:
  another-service:
    condition: service_completed_successfully
```

Requirements:
- The default pipeline executes all required stages.
- Every dependent stage waits for successful completion.
- A failed stage stops downstream execution.
- Containers terminate after their tasks finish.
- Artifacts persist on the host.
- BuildKit caching is enabled.
- Cargo and Gradle caches are reused safely.
- No unnecessary duplicate compilation.
- Individual stages remain runnable.

Support commands such as:

```bash
docker compose up --build rust-check
docker compose up --build android-libs
docker compose up --build android-app
docker compose up --build release-artifacts
```

Ensure that running an individual downstream service also executes its required dependencies.

For automated execution and reliable failure reporting, document and verify the appropriate command:

```bash
docker compose up --build --abort-on-container-exit --exit-code-from release-artifacts
```

The standard requested command must still work as the default complete pipeline.

## 5. Dockerfile Architecture

Use a maintainable multi-stage Dockerfile.

Separate logical responsibilities into appropriate stages:

- Base build environment.
- Android SDK/NDK environment.
- Rust verification.
- Android native library compilation.
- Android application compilation.
- Artifact collection.

Avoid unnecessarily duplicating large SDK installations between stages.

Use BuildKit cache mounts for Cargo and Gradle where appropriate.

Pin base images and toolchain dependencies to reproducible versions.

Add an appropriate `.dockerignore` to avoid sending:
- `.git`
- local Gradle caches
- Cargo build outputs
- Android build outputs
- local secrets
- generated artifacts

Do not exclude source files required by the build.

## 6. iOS Preparation

The host environment is Linux/Fedora.

Requirements:
- Prepare iOS build scripts and required configuration.
- Verify actual Rust Apple targets.
- Verify whether `tools/build-ios-libs.sh` exists and works.
- Document the exact macOS/Xcode build process.
- Do not claim Linux Docker can create an official Xcode Archive or signed IPA.
- Do not introduce unverified iOS cross-compilation workarounds.

Final iOS Archive/IPA remains a macOS/Xcode responsibility.

## 7. Documentation

Create or update Docker documentation describing:

- Architecture.
- Prerequisites.
- Dockerfile stages.
- Compose service dependencies.
- Full build command.
- Individual stage commands.
- Cache behavior.
- Signing configuration.
- Artifact locations.
- iOS limitations.
- Troubleshooting.

Document only behavior that has been verified.

## 8. Verification Requirements

Execute the actual checks available in the environment.

At minimum:

```bash
docker --version
docker compose version
docker compose config
docker compose build
docker compose up --build
```

Also verify:
- Rust checks.
- Android native library builds.
- Both ABI architectures.
- Android Debug APK.
- Android Release APK.
- Android Release AAB.
- Native library packaging.
- Artifact persistence.
- Failure propagation.
- Cache reuse on a subsequent build.

Do not claim a command succeeded unless its actual output confirms success.

If a build is blocked by missing Docker, network access, SDK, NDK, or signing credentials, report the exact blocker and continue with independent work.

## 9. Scope Control

Do not:
- Rewrite Rust Mobile Core.
- Modify the download engine.
- Change Kotlin or Swift application behavior.
- Add features unrelated to the build pipeline.
- Change the application's package identifier.
- Generate fake release artifacts.
- Claim iOS build success from Linux.
- Commit changes.

If a source change is genuinely required to make the build work, explain the root cause and request approval before changing application source files.

## 10. Final Engineering Report

Report:

1. Existing Dockerfile defects discovered.
2. Root causes and corrections.
3. Files created or modified.
4. Final Dockerfile stage architecture.
5. Final Compose service dependency graph.
6. Actual commands executed and their results.
7. Rust build status.
8. Android native libraries and ABI verification.
9. APK/AAB build status.
10. Signing status.
11. Artifact paths.
12. iOS/macOS limitations.
13. Remaining blockers.

Use these classifications:

- IMPLEMENTED
- VERIFIED
- BLOCKED
- NOT VERIFIED

**Final Acceptance Criterion:**

The project must support a reproducible multi-stage build through:

```bash
docker compose up --build
```

The pipeline must build, verify, package, and persist the required Android artifacts without hiding errors or changing the application's download engine.
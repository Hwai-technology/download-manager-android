#!/usr/bin/env bash
# ==============================================================================
# Unified Resilient Multi-Platform Build Pipeline for Mister Download Manager
# Builds Android (APK & AAB), Rust Mobile Libs, iOS Libs, Desktop App, and CLI.
# Fault-tolerant: If any single build target fails or lacks toolchains, it logs
# the warning and proceeds with remaining targets without terminating the whole run.
# ==============================================================================
set -u

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${PROJECT_ROOT}/output"
mkdir -p "${OUTPUT_DIR}/logs"

echo "=========================================================="
echo "  Mister Download Manager — Resilient Master Build Engine  "
echo "  Output Directory: ${OUTPUT_DIR}"
echo "=========================================================="

PASSED_TARGETS=()
FAILED_TARGETS=()
SKIPPED_TARGETS=()

record_success() {
  local target="$1"
  echo ">>> [SUCCESS] Target '${target}' completed successfully."
  PASSED_TARGETS+=("${target}")
}

record_failure() {
  local target="$1"
  local reason="$2"
  echo ">>> [WARNING / CONTINUING] Target '${target}' failed: ${reason}"
  FAILED_TARGETS+=("${target} (${reason})")
}

record_skipped() {
  local target="$1"
  local reason="$2"
  echo ">>> [SKIPPED] Target '${target}': ${reason}"
  SKIPPED_TARGETS+=("${target} (${reason})")
}

# ------------------------------------------------------------------------------
# 1. Native Mobile Rust Engine Check / Build
# ------------------------------------------------------------------------------
echo ""
echo "=== [1/6] Building / Checking Rust Mobile Engine (mdm-mobile) ==="
if command -v cargo >/dev/null 2>&1; then
  if cargo check -p mdm-mobile > "${OUTPUT_DIR}/logs/build-mobile-engine.log" 2>&1; then
    record_success "Rust Mobile Engine (check)"
  else
    record_failure "Rust Mobile Engine" "cargo check failed (see output/logs/build-mobile-engine.log)"
  fi
else
  record_skipped "Rust Mobile Engine" "cargo not found"
fi

# ------------------------------------------------------------------------------
# 2. Android Shared Native JNI Libraries (.so)
# ------------------------------------------------------------------------------
echo ""
echo "=== [2/6] Building Android Native JNI Libraries (.so) ==="
if [ -x "${PROJECT_ROOT}/scripts/build-android-libs.sh" ]; then
  if "${PROJECT_ROOT}/scripts/build-android-libs.sh" > "${OUTPUT_DIR}/logs/build-android-libs.log" 2>&1; then
    record_success "Android JNI Libraries"
  else
    record_failure "Android JNI Libraries" "NDK / cross-compilation warning (see output/logs/build-android-libs.log)"
  fi
else
  record_skipped "Android JNI Libraries" "scripts/build-android-libs.sh not found or not executable"
fi

# ------------------------------------------------------------------------------
# 3. Android Application Debug APK
# ------------------------------------------------------------------------------
echo ""
echo "=== [3/6] Building Android Application (Debug APK) ==="
if [ -x "${PROJECT_ROOT}/scripts/build-android-apk.sh" ]; then
  if "${PROJECT_ROOT}/scripts/build-android-apk.sh" > "${OUTPUT_DIR}/logs/build-android-apk.log" 2>&1; then
    record_success "Android Debug APK"
  else
    record_failure "Android Debug APK" "Gradle APK build failed (see output/logs/build-android-apk.log)"
  fi
else
  record_skipped "Android Debug APK" "scripts/build-android-apk.sh not found"
fi

# ------------------------------------------------------------------------------
# 4. Android Google Play App Bundle (AAB)
# ------------------------------------------------------------------------------
echo ""
echo "=== [4/6] Building Android Google Play Bundle (AAB) ==="
if [ -x "${PROJECT_ROOT}/scripts/build-android-bundle.sh" ]; then
  if "${PROJECT_ROOT}/scripts/build-android-bundle.sh" > "${OUTPUT_DIR}/logs/build-android-bundle.log" 2>&1; then
    record_success "Android Release Bundle (AAB)"
  else
    record_failure "Android Release Bundle (AAB)" "Gradle bundle build failed (see output/logs/build-android-bundle.log)"
  fi
else
  record_skipped "Android Release Bundle (AAB)" "scripts/build-android-bundle.sh not found"
fi

# ------------------------------------------------------------------------------
# 5. iOS Native Static Library (.a)
# ------------------------------------------------------------------------------
echo ""
echo "=== [5/6] Building iOS Static Libraries (.a) ==="
if [ -x "${PROJECT_ROOT}/scripts/build-ios-libs.sh" ]; then
  if "${PROJECT_ROOT}/scripts/build-ios-libs.sh" > "${OUTPUT_DIR}/logs/build-ios-libs.log" 2>&1; then
    record_success "iOS Static Library"
  else
    record_failure "iOS Static Library" "Apple cross-compile toolchain missing (see output/logs/build-ios-libs.log)"
  fi
else
  record_skipped "iOS Static Library" "scripts/build-ios-libs.sh not found"
fi

# ------------------------------------------------------------------------------
# 6. Desktop Application & CLI Build (mdm-app, mdm-cli)
# ------------------------------------------------------------------------------
echo ""
echo "=== [6/6] Building Desktop Application & CLI (mdm-app, mdm-cli) ==="
if command -v cargo >/dev/null 2>&1; then
  if cargo build -p mdm-cli --release > "${OUTPUT_DIR}/logs/build-cli.log" 2>&1; then
    mkdir -p "${OUTPUT_DIR}/cli"
    cp -f "${PROJECT_ROOT}/target/release/mdm" "${OUTPUT_DIR}/cli/" 2>/dev/null || true
    record_success "Desktop CLI (mdm)"
  else
    record_failure "Desktop CLI" "cargo build -p mdm-cli failed (see output/logs/build-cli.log)"
  fi

  if cargo check -p mdm-app > "${OUTPUT_DIR}/logs/build-desktop-app.log" 2>&1; then
    record_success "Desktop GUI App (check)"
  else
    record_failure "Desktop GUI App" "Missing desktop graphics dependencies (see output/logs/build-desktop-app.log)"
  fi
else
  record_skipped "Desktop Application" "cargo not found"
fi

# ------------------------------------------------------------------------------
# Final Summary Report
# ------------------------------------------------------------------------------
echo ""
echo "=========================================================="
echo "               MASTER BUILD PIPELINE SUMMARY              "
echo "=========================================================="
echo "Artifacts Directory: ${OUTPUT_DIR}"
echo ""

echo "Successful Targets (${#PASSED_TARGETS[@]}):"
for target in "${PASSED_TARGETS[@]}"; do
  echo "  ✔ ${target}"
done

if [ ${#FAILED_TARGETS[@]} -gt 0 ]; then
  echo ""
  echo "Failed / Warning Targets (${#FAILED_TARGETS[@]}):"
  for target in "${FAILED_TARGETS[@]}"; do
    echo "  ⚠ ${target}"
  done
fi

if [ ${#SKIPPED_TARGETS[@]} -gt 0 ]; then
  echo ""
  echo "Skipped Targets (${#SKIPPED_TARGETS[@]}):"
  for target in "${SKIPPED_TARGETS[@]}"; do
    echo "  - ${target}"
  done
fi

echo ""
echo "Notice: Output artifacts and build logs are saved in ${OUTPUT_DIR}/"
echo "=========================================================="
exit 0

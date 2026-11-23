#!/usr/bin/env bash
# Backward-compatibility wrapper delegating to scripts/build-ios-libs.sh
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec bash "$ROOT_DIR/scripts/build-ios-libs.sh" "$@"

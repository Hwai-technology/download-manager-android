# Changelog

All notable changes to **Mister Download Manager** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.2] - 2026-11-09

### Added
- Native Android foreground service integration with active notification channels.
- Zero-copy JNI and C-ABI bridge architecture in `crates/mobile`.
- Shared queue scheduling and memory-safe snapshot state serialization.
- Multi-platform packaging and Inno Setup automation.

### Changed
- Refactored native mobile targets to reuse upstream Rust engine directly.
- Standardized release artifact collection and SHA-256 checksum generation.

### Fixed
- Fixed race conditions during multi-part file pre-allocation.
- Improved resume reliability when reconnecting dropped HTTP streams.

## [0.5.1] - 2026-11-08

### Added
- Extension integration for Chromium and Firefox browsers.
- Download media detection and URL capture handlers.

## [0.5.0] - 2026-11-08

### Added
- Initial public release of Mister Download Manager architecture.
- Modular workspace crates (`mdm-engine`, `mdm-mobile`, `mdm-app`).

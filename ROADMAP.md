# Project Roadmap

This document outlines key technical milestones and planned features for Mister Download Manager.

## Milestones

### Mobile Native Core (Android & iOS)
- [x] Shared Rust mobile bridge (`crates/mobile`) with Tokio async runtime.
- [x] JNI bindings and Android foreground service implementation.
- [x] C-ABI exports and Swift bindings for iOS.
- [x] Real-time download progress notification with live speed and progress fraction on Android.
- [x] Automatic clipboard detection and branded action notifications on Android and iOS.
- [x] Native Sharesheet (`ACTION_SEND` / `ACTION_VIEW`) and iOS Share Extension support.
- [ ] BitTorrent download engine integration on mobile.
- [ ] Advanced Android Scoped Storage and MediaStore integration.
- [ ] Background URLSession integration on iOS.

### Desktop & Extensions
- [x] Multi-connection accelerated HTTP/HTTPS engine.
- [x] Chromium and Firefox native messaging host integration.
- [x] BitTorrent support with dynamic bandwidth limits.
- [ ] Enhanced scheduler and batch download rules.

### Web & Documentation
- [x] Courier Waybill responsive landing page and live benchmarks.
- [x] Design token synchronization and Dark/Light mode support.
- [x] Unified developer and contribution guides.

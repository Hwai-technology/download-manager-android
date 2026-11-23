# Security Policy

## Reporting Security Issues

We take the security of Mister Download Manager seriously. If you believe you have discovered a vulnerability, please report it responsibly.

### How to Report

Please do **not** open a public issue. Instead, report security vulnerabilities via GitHub's private vulnerability reporting feature on this repository:

- Go to **Security** -> **Advisories** -> **Report a vulnerability**.
- Provide a clear description of the potential vulnerability, including steps to reproduce, affected platforms, and version number.
- We will acknowledge receipt promptly and coordinate a fix before any public announcement.

### Scope

- Core download engine and segmentation logic (`crates/engine`).
- Mobile FFI boundaries (JNI and C-ABI in `crates/mobile`).
- IPC and native messaging host security (`crates/nmhost`, `crates/ipc`).
- Memory safety and process isolation across supported platforms.

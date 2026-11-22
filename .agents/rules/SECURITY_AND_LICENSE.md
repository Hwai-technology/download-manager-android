# Security and License Rules

## 1. Security-First Engineering

- Security is a mandatory engineering requirement, not an optional enhancement.
- Preserve existing security protections unless a verified requirement justifies changing them.
- Never weaken security controls to bypass integration, compatibility, or development problems.
- Investigate the security implications of every change involving networking, authentication, storage, IPC, downloaded content, native libraries, or Android permissions.
- Prefer secure defaults and explicit trust boundaries.
- Minimize exposed attack surfaces, application privileges, and sensitive data access.
- Do not introduce security-sensitive behavior without understanding its threat model and operational consequences.
- Do not claim that the application or a component is secure merely because it uses encryption or a security-related library.
 
## 2. Secrets and Sensitive Information

Never expose, log, commit, or transmit secrets unnecessarily.

Sensitive information includes, but is not limited to:

- Passwords.
- Authentication tokens.
- Refresh tokens.
- Session identifiers.
- Cookies.
- API keys.
- Private URLs containing credentials or secret query parameters.
- Authorization headers.
- Private certificates and private keys.
- Encryption keys.
- Sensitive user data.
- Download URLs containing access credentials.
- Other information that could grant access to protected resources.

Requirements:

- Never write secrets into source code.
- Never hardcode credentials in Rust, Kotlin, Gradle, Cargo manifests, scripts, or configuration files.
- Never include secrets in error messages, panic messages, debug output, or crash reports.
- Never print complete authentication headers or cookie values.
- Never expose sensitive values through application notifications or user-visible error messages.
- Never commit secrets into Git history.
- Do not copy real credentials into examples, test fixtures, screenshots, or documentation.
- Use clearly synthetic placeholders in examples and tests.
- Inspect logging and diagnostic paths whenever modifying authentication or networking behavior.

If a secret is accidentally exposed:

1. Stop further exposure.
2. Identify where it was written or transmitted.
3. Avoid repeating the secret in reports.
4. Assess whether it may have been accessed.
5. Recommend appropriate revocation or rotation.
6. Remove the exposure through an approved process.
7. Report the incident without reproducing the sensitive value.

## 3. Logging, Diagnostics, and Crash Reporting

Logging must support debugging without exposing sensitive information.

Requirements:

- Use structured and appropriately scoped logging.
- Never log passwords, tokens, cookies, private keys, or authentication headers.
- Avoid logging complete private download URLs.
- Redact credentials and sensitive query parameters before logging URLs.
- Do not assume that a URL is safe to log merely because it uses HTTPS.
- Avoid logging unnecessary personal or device information.
- Do not include sensitive request or response bodies in logs.
- Inspect third-party logging and crash-reporting behavior before enabling it.
- Ensure that production logging does not expose development-only diagnostics.
- Preserve enough non-sensitive context to investigate failures.

When logging download activity, prefer non-sensitive identifiers and operational metadata, such as:

- A non-secret download identifier.
- Download state.
- Transfer progress.
- Error category.
- Retry count.
- Timing information.
- Sanitized host information, where appropriate.

Do not introduce verbose logging of sensitive values as a temporary debugging workaround.

## 4. TLS and Certificate Validation

- Preserve secure TLS configuration.
- Never disable certificate validation to make a connection succeed.
- Never accept invalid certificates as a general fallback.
- Never replace hostname verification with an insecure alternative.
- Never downgrade secure protocols or cipher configurations without a verified compatibility requirement and security assessment.
- Do not silently fall back from HTTPS to HTTP.
- Do not introduce custom trust stores without understanding their security implications.
- Do not bypass certificate errors in production.
- Preserve the existing authentication and transport-security guarantees of the upstream engine.

When TLS integration fails, investigate the actual cause, including:

- Android trust store behavior.
- Certificate chain validity.
- Hostname verification.
- System time and certificate validity periods.
- TLS backend compatibility.
- Native library configuration.
- Proxy or VPN behavior.
- Android-specific networking restrictions.
- Dependency versions and feature flags.

A development-only workaround must never silently become production behavior.

Any exceptional certificate or trust configuration requires a documented security justification and explicit approval.

## 5. Authentication and Authorization

- Preserve existing authentication requirements.
- Do not weaken authentication to simplify Android integration.
- Do not bypass authorization checks in the engine, application, or remote-control layer.
- Do not treat possession of a download identifier as proof of authorization.
- Do not assume that an authenticated connection makes every request trustworthy.
- Validate authorization at the appropriate trust boundary.
- Avoid exposing privileged operations through unrestricted IPC or remote-control interfaces.
- Keep credentials out of URLs whenever the existing protocol and API allow a safer mechanism.
- Protect refresh tokens and long-lived credentials more carefully than short-lived operational data.
- Define credential expiration, revocation, and replacement behavior where authentication is supported.

Before changing authentication behavior, inspect the actual upstream implementation and identify every affected consumer.

## 6. Untrusted Input and Trust Boundaries

Treat all external input as untrusted until it has been validated.

This includes:

- User-entered URLs.
- Imported download links.
- Clipboard content.
- Browser extension messages.
- IPC requests.
- Remote-control requests.
- Network responses.
- HTTP headers.
- Redirect destinations.
- Filenames received from servers.
- Downloaded files.
- Archive contents.
- Metadata and content types.
- External configuration.
- Data received from other applications.

Requirements:

- Validate input at the boundary where it enters the system.
- Use established parsing libraries where appropriate.
- Enforce explicit size and resource limits.
- Reject malformed or unsupported input safely.
- Avoid trusting client-supplied metadata.
- Avoid passing unvalidated input directly into system commands, filesystem operations, or privileged APIs.
- Avoid unsafe string interpolation when constructing commands or queries.
- Handle malformed input without panics, uncontrolled allocation, or process termination.
- Preserve clear error reporting without exposing sensitive internal details.

Validation must match the actual protocol and product requirements. Do not invent arbitrary restrictions that break legitimate supported use cases without assessing their impact.

## 7. URL and Network Request Security

Download URLs and network destinations require deliberate security handling.

Investigate:

- URL parsing and normalization.
- Supported schemes.
- Redirect handling.
- Authentication information in URLs.
- Hostname resolution.
- Private and local network destinations.
- DNS rebinding risks where applicable.
- Proxy behavior.
- Redirects across origins.
- Protocol downgrade risks.
- Server-controlled metadata.
- Request header handling.
- Resource exhaustion through malicious responses.

Requirements:

- Accept only protocols supported by the verified implementation and product requirements.
- Do not assume that a URL is safe because it was entered by the user.
- Do not blindly follow redirects to arbitrary destinations.
- Define how authentication headers and cookies are handled across redirects.
- Avoid unintentionally forwarding credentials to a different origin.
- Validate redirect destinations according to the application's actual threat model.
- Do not expose internal services or privileged network resources through an unrestricted remote download interface.
- Avoid introducing server-side request forgery risks when the application or its remote-control component can access privileged networks.
- Do not trust response headers as proof that the downloaded content is safe.

Any restrictions on local, private, or special-purpose addresses must be justified against the actual application use cases and documented explicitly.

## 8. IPC and Inter-Component Communication

IPC is a security boundary, not merely a communication mechanism.

Before implementing or modifying IPC, investigate:

- Existing IPC architecture.
- Authentication and authorization requirements.
- Endpoint exposure.
- Request validation.
- Message size limits.
- Serialization and deserialization behavior.
- Error propagation.
- Connection lifecycle.
- Client identity where supported.
- Concurrent request handling.
- Resource exhaustion risks.
- Sensitive data transmitted between components.

Requirements:

- Treat every IPC request as untrusted input.
- Validate message structure and payloads.
- Enforce appropriate message size and resource limits.
- Do not expose privileged operations to unauthorized clients.
- Do not rely solely on an obscure endpoint or socket path as an authorization mechanism.
- Avoid transmitting secrets unnecessarily.
- Protect IPC communication against unauthorized access according to the platform's capabilities.
- Handle malformed messages and disconnected clients safely.
- Preserve existing IPC authentication and access restrictions.
- Do not invent new IPC protocols or message contracts without inspecting the current implementation.

## 9. Remote-Control Security

If remote-control functionality is present or introduced, investigate its security model before changing it.

Required investigation areas:

- Remote authentication.
- Authorization and operation permissions.
- Transport security.
- Endpoint exposure.
- Network binding behavior.
- Request validation.
- Session management.
- Token storage and expiration.
- Replay protection where applicable.
- Rate limiting and resource limits.
- Remote shutdown and privileged operations.
- Audit logging without secret exposure.

Requirements:

- Do not expose remote-control endpoints unintentionally.
- Do not assume that network accessibility implies authorization.
- Do not permit privileged operations merely because a request is syntactically valid.
- Do not disable authentication for convenience.
- Do not expose administrative operations through unauthenticated interfaces.
- Keep remote-control privileges limited to the required operations.
- Preserve existing security protections when integrating remote control with Android.

Do not assume that remote-control functionality exists in a particular crate or release without verifying the checked-out source.

## 10. Downloaded Content and File Safety

Downloaded content must be treated as untrusted data, regardless of its extension, filename, MIME type, or server response.

Requirements:

- Do not automatically execute downloaded files.
- Do not treat downloaded content as trusted application code.
- Do not assume that a server-provided filename is safe.
- Prevent path traversal and unintended filesystem writes.
- Validate file destinations according to the actual storage API and permissions.
- Handle symbolic links and filesystem edge cases where relevant.
- Avoid overwriting existing files without the intended user-approved behavior.
- Preserve partial-download integrity.
- Validate file boundaries and sizes.
- Handle disk exhaustion and write failures safely.
- Avoid uncontrolled memory allocation based on remote metadata.
- Do not trust remote content length or content type without appropriate validation.
- Keep temporary and partial files within controlled locations.
- Ensure cleanup does not delete unrelated user data.

For archive extraction, if implemented:

- Inspect the actual extraction implementation and supported formats.
- Prevent archive path traversal.
- Validate extraction destinations.
- Apply limits to entry count, expanded size, and resource consumption.
- Handle malformed archives safely.
- Do not overwrite unrelated files without explicit intended behavior.

Do not assume that archive extraction, file scanning, or malware detection exists unless verified in the repository.

## 11. Android Secure Storage

Android storage security must be investigated deliberately.

Before selecting or changing a credential-storage mechanism, inspect:

- Existing upstream secret-storage behavior.
- Android application storage boundaries.
- Android Keystore capabilities.
- Encryption key ownership and lifecycle.
- Backup and restore behavior.
- Device lock and authentication requirements where applicable.
- Credential persistence requirements.
- Process recreation behavior.
- Uninstall and data-clearing behavior.
- Android API-level compatibility.

Requirements:

- Preserve existing secret-storage protections.
- Do not store passwords or long-lived tokens in plaintext.
- Do not assume that ordinary SharedPreferences provides encryption.
- Do not assume that app-private storage alone satisfies every secret-protection requirement.
- Do not introduce custom cryptography when established platform mechanisms are appropriate.
- Do not store encryption keys alongside encrypted data without a justified secure design.
- Do not log decrypted secrets.
- Do not expose secrets through backups or unintended export mechanisms.
- Define what happens when encrypted credentials become unavailable.
- Handle key invalidation and recovery deliberately.
- Avoid silently deleting credentials when decryption or key access fails.

Any proposed storage solution must document the threat model, key lifecycle, supported Android versions, and recovery behavior.

## 12. Permissions and Least Privilege

- Request only Android permissions required by verified product functionality.
- Avoid unnecessary permissions.
- Do not request sensitive permissions preemptively.
- Explain permission requirements through the appropriate application experience.
- Handle permission denial and revocation safely.
- Do not assume that a permission remains granted indefinitely.
- Recheck permissions when required by the relevant Android API.
- Do not bypass Android permission restrictions through hidden or unsupported mechanisms.
- Keep native and application-level privileges limited to the responsibilities they require.

Every permission change must identify:

1. The feature requiring it.
2. The Android API or framework behavior involved.
3. The reason a less privileged alternative is insufficient.
4. The user-facing impact.
5. The behavior when permission is denied.

## 13. Rust, Native Code, and Memory Safety

- Prefer safe Rust APIs.
- Avoid introducing `unsafe` code unless necessary.
- Do not use `unsafe` to bypass an architectural or integration problem without understanding the underlying requirement.
- Inspect existing unsafe blocks before modifying them.
- Document the safety invariants of any new unsafe code.
- Validate raw pointers, buffer lengths, ownership, and lifetimes at FFI boundaries.
- Do not assume Kotlin and Rust share compatible memory ownership.
- Prevent use-after-free, double-free, invalid pointer access, and data races.
- Define callback lifetime and cancellation behavior.
- Ensure native resources are released correctly.
- Avoid panics crossing FFI boundaries.
- Convert errors into defined integration-safe results.

Any new unsafe block or FFI boundary requires focused review and relevant tests.

## 14. Dependency and Supply-Chain Security

External dependencies introduce security, maintenance, and licensing risks.

Before adding or upgrading a dependency:

- Inspect its purpose.
- Inspect its version and maintenance status.
- Inspect its transitive dependencies.
- Review relevant security advisories where practical.
- Review its Android and target compatibility.
- Review its native-code requirements.
- Review its license.
- Determine whether existing project dependencies already provide the required capability.
- Assess whether the dependency expands the application's attack surface.

Requirements:

- Avoid unnecessary dependencies.
- Avoid abandoned or unmaintained dependencies where a maintained alternative is available.
- Do not blindly upgrade dependencies to resolve an unrelated issue.
- Do not blindly pin old vulnerable versions.
- Do not copy dependency code without checking its origin and license.
- Do not introduce dependencies with incompatible licensing obligations without review.
- Keep dependency changes scoped and documented.
- Use available audit tools when appropriate, but do not treat an empty audit result as proof of complete security.

Security advisory checks and dependency audits must be reported with their actual scope and execution results.

## 15. License and Copyright Compliance

Before distribution decisions, inspect the actual licensing obligations of the project and its dependencies.

Required investigation areas:

- Repository `LICENSE` files.
- License notices in individual crates.
- Third-party dependency licenses.
- Transitive dependency licenses.
- Bundled native libraries.
- Vendored source code.
- Fonts, icons, images, and other bundled assets.
- Upstream attribution requirements.
- Copyright notices.
- Source-distribution obligations.
- Binary-distribution obligations.
- Modification and redistribution conditions.

Requirements:

- Do not assume that the repository's main LICENSE covers every included component.
- Do not assume that every dependency uses the same license.
- Do not remove copyright or attribution notices.
- Do not remove or modify license headers without a justified and permitted reason.
- Preserve required third-party notices.
- Identify dependencies whose license terms may affect distribution.
- Review changes to dependency versions because they may change licensing obligations.
- Document the provenance of copied or adapted code.
- Do not claim that the application is legally cleared for distribution without an appropriate review.

Do not provide legal guarantees or present technical license inspection as legal advice.

When licensing obligations are ambiguous or material to distribution, identify the issue and recommend review by a qualified legal professional.

## 16. Release and Distribution Security

Before preparing an Android release, investigate:

- Release signing configuration.
- Keystore protection.
- Signing-key ownership and backup.
- Build artifact integrity.
- Dependency and native-library contents.
- Embedded credentials and secrets.
- Application permissions.
- Debug configuration.
- Logging configuration.
- Network security configuration.
- Exported Android components.
- Backup behavior.
- Required license notices.
- Third-party attribution.
- Distribution-specific requirements.

Requirements:

- Never commit production signing keys or passwords.
- Never embed private signing credentials in the application package.
- Keep debug and release configurations appropriately separated.
- Do not ship development-only insecure settings.
- Inspect the final packaged artifact when release verification requires it.
- Preserve signing and update compatibility.
- Document release security checks actually completed.
- Do not claim release readiness without the relevant evidence.

## 17. Security Testing and Verification

Security-sensitive changes require tests appropriate to the affected risk.

Where applicable, investigate and test:

- Invalid and malformed URLs.
- Redirect handling.
- Authentication failures.
- Credential redaction.
- TLS certificate failures.
- Unauthorized IPC requests.
- Malformed IPC messages.
- Remote-control authorization.
- Path traversal attempts.
- Malicious filenames.
- Oversized responses.
- Disk exhaustion.
- Interrupted writes.
- Archive extraction boundaries.
- Invalid FFI inputs.
- Secret-storage failure and recovery.
- Permission denial and revocation.
- Process recreation involving protected data.

Requirements:

- Do not claim security testing was performed unless it was actually executed.
- Do not claim comprehensive security coverage based on a small number of tests.
- Do not weaken security tests to make an implementation pass.
- Record untested attack scenarios.
- Distinguish automated test results from manual security review.
- Report discovered vulnerabilities and their impact without unnecessarily reproducing exploitable secrets or sensitive data.

## 18. Security Change-Control Requirements

Before changing a security-sensitive implementation:

1. Identify the affected trust boundary.
2. Inspect the current implementation.
3. Identify existing security controls.
4. Trace callers and consumers.
5. Determine the threat or integration problem.
6. Evaluate the security impact of proposed alternatives.
7. Identify compatibility and usability implications.
8. Present the smallest safe change.
9. Obtain approval for changes that weaken, replace, or materially alter existing security guarantees.
10. Implement the approved scope.
11. Execute relevant tests and verification.
12. Report residual risks and untested scenarios.

Never silently remove or weaken an existing security control.

## 19. Security and License Reporting

For significant security or licensing work, report:

### Security Findings
- Affected components and trust boundaries.
- Existing protections identified.
- Verified vulnerabilities or risks.
- Inferred risks.
- Unknown behavior.
- Required mitigations.

### Secrets and Data Handling
- Sensitive data involved.
- Storage and transmission paths.
- Logging and diagnostic implications.
- Redaction and protection behavior.

### Dependencies and Licensing
- Dependencies inspected.
- License information verified.
- Potential compatibility or redistribution concerns.
- Missing license evidence.
- Required follow-up review.

### Verification
- Security checks and tests actually executed.
- Dependency audit commands actually executed.
- Files and configurations inspected.
- Results and failures.
- Untested scenarios.
- Remaining risks.

Classify findings as:

- `VERIFIED`
- `INFERRED`
- `UNKNOWN`
- `BLOCKED`
- `PROPOSED`

Never present an unresolved security concern as resolved.

## 20. Final Security Principles

1. Never expose secrets.
2. Never weaken TLS, authentication, or authorization to bypass integration problems.
3. Treat external input and downloaded content as untrusted.
4. Preserve existing security protections.
5. Investigate Android secure storage deliberately.
6. Follow least-privilege principles.
7. Keep IPC and remote-control boundaries protected.
8. Prefer safe Rust and carefully reviewed FFI.
9. Review dependency security and licensing before adoption and distribution.
10. Preserve copyright and attribution notices.
11. Verify security-sensitive behavior through relevant tests.
12. Never provide legal guarantees.

**Security before convenience. Least privilege before broad access. Verification before trust.**
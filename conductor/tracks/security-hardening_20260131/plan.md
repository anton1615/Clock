# Track: Security & Privacy Hardening (v1.1.3)
**Date**: 2026-01-31

## Context
Following the completion of core synchronization and background service features, a comprehensive security audit was conducted. The goal was to identify and fix vulnerabilities in network communication, data persistence, and system integration.

## Objectives
- [x] Conduct a full SAST and manual security audit.
- [x] Restrict PC-side network exposure (CORS).
- [x] Secure sensitive system integration points (Startup script).
- [x] Comply with OS standards for configuration storage (setting.json migration).
- [x] Enhance data integrity (Hex color validation).
- [x] Protect user privacy on mobile (Notification visibility).
- [x] Optimize resource usage (mDNS scan management).

## Key Changes

### 1. Network Hardening
- **PC CORS**: Removed `AllowAnyOrigin` and `AllowCredentials` in `SyncServerService.cs`. Only basic SignalR/REST connections are now permitted.
- **Android SSRF Prevention**: Added `isValidIpOrHostname` validation in `MainActivity.kt` using a strict regex for allowed network identifiers.

### 2. Data Persistence & Integrity
- **Settings Migration**: Moved `setting.json` from the application binary directory to `%LocalAppData%\Clock`. Added an automatic migration handler in `AppSettings.Load()` that moves existing files to the new location.
- **Hex Color Validation**: Implemented regex-based validation for color strings in `SettingsRepository.kt` (Android) and UI level checks (PC) to prevent parsing crashes.

### 3. Privacy & OS Integration
- **Notification Visibility**: Switched from `VISIBILITY_PUBLIC` to `VISIBILITY_PRIVATE` in `TimerService.kt` to hide countdown details from the Android lock screen when privacy settings are enabled.
- **PowerShell Escaping**: Added `.Replace("'", "''")` to the startup script generator in `StartupService.cs` to prevent command injection or syntax errors on paths containing single quotes.
- **Resource Management**: Triggered `mdnsScanner.stopScan()` in `MainActivity.kt` upon successful SignalR connection to release the WiFi Multicast Lock and save battery.

## Conclusion
The project is now significantly more robust against common local network attacks and better aligned with platform-specific security best practices.

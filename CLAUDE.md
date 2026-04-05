# CLAUDE.md — Instructions for Claude Code

## IMPORTANT: Read this before doing anything if context was compacted.

## Project: PlausiDen-Android
Android client for PlausiDen plausible deniability engine. Tier 1: generates plausible
artifacts (browser history, filesystem entries, contacts) to provide cover traffic.

## Architecture
- **app/**: Kotlin Android app — Material Design 3, ForegroundService, WorkManager
- **engine-jni/**: Rust JNI bridge — cdylib that wraps plausiden-engine for Android
- **scripts/**: Build helpers (cargo ndk, adb install)

## Key Design Decisions
- F-Droid only distribution. NEVER Google Play.
- No root required. All operations use standard Android APIs.
- Foreground service with innocuous notification text ("Device optimization running").
- WorkManager keeps service alive across doze/standby.
- Rust engine compiled via cargo-ndk for aarch64, armv7, x86_64.

## Rust Edition 2024
`gen` is a reserved keyword. Never use it as a variable name.

## Build
- Android app: `./gradlew assembleDebug` (requires Android SDK)
- Rust JNI: `./scripts/build-rust.sh` (requires cargo-ndk + Android NDK)
- Install: `./scripts/install-debug.sh` (requires adb)

## Targets
- compileSdk 34, minSdk 29 (Android 10+), targetSdk 34

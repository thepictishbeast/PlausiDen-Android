# PlausiDen Android

Android client for the PlausiDen plausible deniability engine.

Generates plausible artifacts (browser history, filesystem entries, contacts, etc.)
to provide cover traffic on Android devices. No root required.

## Architecture

```
app/          Kotlin Android app (Material Design 3)
engine-jni/   Rust JNI bridge wrapping plausiden-engine
scripts/      Build and install helpers
```

## Building

### Prerequisites

- Android SDK (API 34)
- Android NDK
- Rust toolchain with `cargo-ndk`
- `just` command runner (optional)

### Rust Engine

```bash
./scripts/build-rust.sh
# or
just build-rust
```

### Android App

```bash
./gradlew assembleDebug
# or
just build-app
```

### Install to Device

```bash
./scripts/install-debug.sh
# or
just install
```

## Distribution

F-Droid only. This app is never distributed via Google Play.

## License

Business Source License 1.1 (BSL-1.1). See [LICENSE](LICENSE).

Change Date: 2030-04-04. After this date the code converts to Apache 2.0.

> # ⚠️ DO NOT USE — UNVERIFIED — UNSAFE ⚠️
>
> This software is **unverified and unsafe for any production use**.
> It is published publicly only for transparency, third-party audit,
> and reproducibility. Treat every commit as guilty until proven
> innocent.
>
> By using this code you accept:
> - **No warranty** of any kind, express or implied.
> - **No fitness** for any particular purpose.
> - **No guarantee** of correctness, safety, or freedom from defects.
> - **Zero liability** on the maintainer for any damages — data loss,
>   security compromise, financial loss, or any consequential damages.
>
> The code is under active engineering development per the
> [Adversarial Validation Protocol v2](https://github.com/thepictishbeast/PlausiDen-AVP-Doctrine/blob/main/AVP2_PROTOCOL.md).
> Every commit's default verdict is **STILL BROKEN**. AVP-2 requires
> a minimum of 36 verification passes before a `SHIP-DECISION:`
> annotation may be considered. **No commit in this repository has
> reached `SHIP-DECISION:` status.**

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

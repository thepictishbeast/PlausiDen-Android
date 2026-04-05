# PlausiDen Android — justfile

default:
    @just --list

# Build the Rust JNI library for all Android targets
build-rust:
    ./scripts/build-rust.sh

# Build the Android app (debug)
build-app:
    ./gradlew assembleDebug

# Build everything: Rust engine then Android app
build-all: build-rust build-app

# Install debug APK to connected device
install:
    ./scripts/install-debug.sh

# Build and install
run: build-all install

# Check Rust code compiles
check-rust:
    cd engine-jni && cargo check

# Run Rust tests
test-rust:
    cd engine-jni && cargo test

# Clean all build artifacts
clean:
    ./gradlew clean
    cd engine-jni && cargo clean

# Format Rust code
fmt:
    cd engine-jni && cargo fmt

# Lint Rust code
clippy:
    cd engine-jni && cargo clippy -- -D warnings

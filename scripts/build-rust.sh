#!/usr/bin/env bash
# Build the Rust JNI library for all Android targets using cargo-ndk.
#
# Prerequisites:
#   cargo install cargo-ndk
#   rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
#   ANDROID_NDK_HOME must be set

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
JNI_DIR="$PROJECT_DIR/engine-jni"
OUTPUT_DIR="$PROJECT_DIR/app/src/main/jniLibs"

TARGETS=(
    "aarch64-linux-android"
    "armv7-linux-androideabi"
    "x86_64-linux-android"
)

# Map Rust target triples to Android ABI directories
declare -A ABI_MAP=(
    ["aarch64-linux-android"]="arm64-v8a"
    ["armv7-linux-androideabi"]="armeabi-v7a"
    ["x86_64-linux-android"]="x86_64"
)

echo "Building plausiden-engine-jni for Android targets..."
echo "JNI source: $JNI_DIR"
echo "Output:     $OUTPUT_DIR"
echo ""

cd "$JNI_DIR"

for target in "${TARGETS[@]}"; do
    abi="${ABI_MAP[$target]}"
    echo "--- Building for $target ($abi) ---"

    cargo ndk \
        --target "$target" \
        --platform 29 \
        -- build --release

    # Copy .so to the correct jniLibs directory
    mkdir -p "$OUTPUT_DIR/$abi"
    cp "$JNI_DIR/target/$target/release/libplausiden_engine_jni.so" \
       "$OUTPUT_DIR/$abi/libplausiden_engine_jni.so"

    echo "    -> $OUTPUT_DIR/$abi/libplausiden_engine_jni.so"
done

echo ""
echo "All targets built successfully."

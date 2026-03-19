#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────
#  build.sh  –  Compile and run the SWIFT Message Generator
#               Package: com.kazhuga.swift
#
#  Usage:
#    chmod +x build.sh
#    ./build.sh          # compile + run demo
#    ./build.sh test     # compile + run unit tests
#    ./build.sh clean    # remove compiled classes
# ──────────────────────────────────────────────────────────────

set -e

SRC_MAIN="src/main/java"
SRC_TEST="src/test/java"
OUT="out"
MAIN_CLASS="com.kazhuga.swift.demo.SwiftDemo"
TEST_CLASS="com.kazhuga.swift.SwiftLibraryTests"

case "${1:-demo}" in

  clean)
    echo "Cleaning..."
    rm -rf "$OUT"
    echo "Done."
    ;;

  test)
    echo "Compiling sources..."
    mkdir -p "$OUT"
    find "$SRC_MAIN" "$SRC_TEST" -name "*.java" | xargs javac -d "$OUT"
    echo ""
    echo "Running tests..."
    java -cp "$OUT" "$TEST_CLASS"
    ;;

  demo|*)
    echo "Compiling sources..."
    mkdir -p "$OUT"
    find "$SRC_MAIN" -name "*.java" | xargs javac -d "$OUT"
    echo ""
    echo "Running demo..."
    java -cp "$OUT" "$MAIN_CLASS"
    ;;
esac

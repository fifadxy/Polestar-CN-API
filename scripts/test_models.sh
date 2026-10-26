#!/bin/sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/android-app/app/src/main/java/io/github/polestarcn/companion"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT HUP INT TERM
javac -encoding UTF-8 -d "$OUT" "$SRC/api/ProtoWire.java" "$SRC/OperationState.java" "$SRC/TelemetryStatus.java" "$SRC/VehicleSnapshot.java" "$ROOT/tests/ModelStateTest.java"
java -cp "$OUT" io.github.polestarcn.companion.ModelStateTest

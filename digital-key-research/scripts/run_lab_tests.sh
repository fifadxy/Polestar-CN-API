#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT HUP INT TERM
javac -encoding UTF-8 -d "$OUT" \
  "$ROOT/lab/src/io/github/polestarcn/digitalkeylab/DigitalKeyCapabilityModel.java" \
  "$ROOT/lab/src/io/github/polestarcn/digitalkeylab/ProvisioningStateMachine.java" \
  "$ROOT/lab/test/io/github/polestarcn/digitalkeylab/DigitalKeyCapabilityModelTest.java" \
  "$ROOT/lab/test/io/github/polestarcn/digitalkeylab/ProvisioningStateMachineTest.java"
java -cp "$OUT" io.github.polestarcn.digitalkeylab.DigitalKeyCapabilityModelTest
java -cp "$OUT" io.github.polestarcn.digitalkeylab.ProvisioningStateMachineTest

#!/usr/bin/env bash
# Build script for the DailyCheck JavaFX desktop app.
# Runs mvn package, which:
#   1) compiles sources
#   2) shades all dependencies (JavaFX + SQLite native libs) into a runnable uber-jar
#   3) invokes Launch4j to wrap the jar as a Windows EXE (bound to the package phase)
#
# Usage:
#   ./build.sh          # full build: shaded jar + Windows EXE
#   ./build.sh run      # run the app directly via javafx:run
#   ./build.sh jar      # build only the shaded jar, skip the EXE step
#
# NOTE: Launch4j supports cross-compilation on Linux/macOS/Windows.
#       No Windows host required to produce the EXE.
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ "$1" = "run" ]; then
    echo "==> Running DailyCheck via javafx:run ..."
    mvn -q javafx:run
    exit 0
fi

echo "==> Cleaning previous build ..."
mvn -q clean

if [ "$1" = "jar" ]; then
    echo "==> Building shaded jar only (skipping EXE) ..."
    mvn -q -DskipTests package -Dlaunch4j.skip=true
    echo ""
    echo "==> Done. Shaded jar: target/dailycheck-windows.jar"
    exit 0
fi

echo "==> Packaging shaded jar + Windows EXE (mvn package) ..."
mvn -q -DskipTests package

echo ""
echo "==> Build complete."
echo "    Shaded jar:  target/dailycheck-windows.jar"
echo "    EXE:         target/DailyCheck.exe"
echo ""
echo "    Distribute the EXE directly to end users (requires Java 17+ on Windows)."

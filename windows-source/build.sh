#!/usr/bin/env bash
# Build script for the DailyCheck JavaFX desktop app.
# Runs mvn package, which:
#   1) compiles sources
#   2) shades all dependencies (JavaFX + SQLite native libs) into a runnable uber-jar
#   3) invokes jpackage to produce a Windows EXE installer (bound to the package phase)
#
# Usage:
#   ./build.sh          # full build: shaded jar + Windows EXE
#   ./build.sh run      # run the app directly via javafx:run
#   ./build.sh jar      # build only the shaded jar, skip the EXE step
#
# NOTE: The EXE step requires running on Windows (or Wine with jpackage).
#       On non-Windows hosts use `./build.sh jar` to produce just the jar.
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
    mvn -q -DskipTests package -Djpackage.skip=true
    echo ""
    echo "==> Done. Shaded jar: target/dailycheck-windows.jar"
    exit 0
fi

echo "==> Packaging shaded jar + Windows EXE (mvn package) ..."
mvn -q -DskipTests package

echo ""
echo "==> Build complete."
echo "    Shaded jar:  target/dailycheck-windows.jar"
echo "    Installer:   target/jpackage/DailyCheck-1.0.0.exe"
echo ""
echo "    Run the jar directly with:"
echo "      java -jar target/dailycheck-windows.jar"

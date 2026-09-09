#!/usr/bin/env bash
# Build a standalone Windows installer EXE that bundles a JRE.
#
# Produces: target/DailyCheck-Setup.exe  (~36 MB, self-extracting, no Java required)
#
# Requirements:
#   - JDK 17 (for jlink)
#   - Maven
#   - 7za (p7zip-full)
#
# Steps:
#   1. mvn package  -> shaded jar + Launch4j EXE
#   2. download Windows JDK 17 (for jmods)
#   3. jlink -> minimal Windows runtime
#   4. packr  -> app dir (exe + jre + jar)
#   5. 7z SFX -> single self-extracting installer EXE
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "==> Step 1: mvn package (shaded jar) ..."
mvn -q -DskipTests package

echo "==> Step 2: Download Windows JDK 17 ..."
JDK_URL="https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"
curl -L -o /tmp/jdk17-win.zip "$JDK_URL"
rm -rf /tmp/jdk17-win && mkdir -p /tmp/jdk17-win
unzip -q /tmp/jdk17-win.zip -d /tmp/jdk17-win
JMODS=$(ls -d /tmp/jdk17-win/jdk-*/jmods)

echo "==> Step 3: jlink -> minimal Windows runtime ..."
JDK17_HOME="$(dirname "$(dirname "$(readlink -f "$(which javac)")")")"
rm -rf /tmp/dc-runtime
"$JDK17_HOME/bin/jlink" \
  --module-path "$JMODS" \
  --add-modules java.base,java.desktop,java.sql,java.logging,java.management,java.xml,jdk.unsupported,jdk.crypto.ec \
  --strip-debug --no-man-pages --no-header-files --compress=2 \
  --output /tmp/dc-runtime

echo "==> Step 4: packr -> app dir ..."
PACKR_URL="https://github.com/libgdx/packr/releases/download/4.0.0/packr-all-4.0.0.jar"
curl -L -o /tmp/packr.jar "$PACKR_URL"
rm -rf /tmp/dc-out
java -jar /tmp/packr.jar \
  --platform Windows64 \
  --jdk /tmp/dc-runtime \
  --executable DailyCheck \
  --classpath target/dailycheck-windows.jar \
  --mainclass com.dailycheck.Main \
  --vmargs Xmx512m \
  --output /tmp/dc-out

echo "==> Step 5: 7z SFX -> installer EXE ..."
# 7z.sfx (GUI self-extractor) is taken from the 7-Zip Windows installer.
7Z_URL="https://www.7-zip.org/a/7z2301-x64.exe"
curl -L -o /tmp/7z-install.exe "$7Z_URL"
7za x /tmp/7z-install.exe -o/tmp/7z-install -y >/dev/null
SFX=/tmp/7z-install/7z.sfx

cat > /tmp/dc-config.txt <<'EOF'
;!@Install@!UTF-8!
Title="每日打卡 DailyCheck"
BeginPrompt="即将安装 每日打卡 DailyCheck，是否继续？"
Progress="yes"
ExtractPath="%LOCALAPPDATA%\DailyCheck"
RunProgram="DailyCheck.exe"
;!@InstallEnd@!
EOF

cd /tmp/dc-out
7za a -t7z -mx=9 -m0=LZMA2 -mmt=on /tmp/app.7z . >/dev/null
cd "$SCRIPT_DIR"
cat "$SFX" /tmp/dc-config.txt /tmp/app.7z > target/DailyCheck-Setup.exe

echo ""
echo "==> Build complete."
echo "    Installer: target/DailyCheck-Setup.exe  ($(du -h target/DailyCheck-Setup.exe | cut -f1))"
echo "    Standalone, bundles JRE 17. No Java installation required on target machine."

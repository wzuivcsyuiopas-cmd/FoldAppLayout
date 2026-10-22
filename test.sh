#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
BT="$SDK/build-tools/36.0.0"
ANDROID="$SDK/platforms/android-36/android.jar"
API=build/deps/xposed-api82.jar
[ -f build/FoldAppLayout.apk ] || ./build.sh
rm -rf build/test-classes build/test-dex
mkdir -p build/test-classes build/test-dex
javac -source 8 -target 8 -Xlint:-options -cp "build/classes:$ANDROID:$API" -d build/test-classes tests/probe/*.java tests/ScannerTest.java
find build/test-classes/probe -name '*.class' > build/probe-classes.txt
"$BT/d8" --min-api 34 --output build/test-dex @build/probe-classes.txt
python3 - <<'PY'
import zipfile
with zipfile.ZipFile('build/probe.apk','w') as z:z.write('build/test-dex/classes.dex','classes.dex')
PY
java -cp "build/test-classes:build/classes:$ANDROID:$API" local.codex.applayout.ScannerTest build/probe.apk build/FoldAppLayout.apk
python3 tests/check_apk.py "$BT/aapt2"

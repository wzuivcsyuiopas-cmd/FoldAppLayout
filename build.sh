#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
BT="$SDK/build-tools/36.0.0"
ANDROID="$SDK/platforms/android-36/android.jar"
rm -rf build/classes build/dex
mkdir -p build/classes build/dex build/deps
API=build/deps/xposed-api82.jar
if [ ! -f "$API" ]; then
  curl -fL 'https://api.xposed.info/de/robv/android/xposed/api/82/api-82.jar' -o "$API"
fi
python3 - "$API" <<'PY'
import hashlib,sys
p=sys.argv[1]
expected='f48c635f1c7469fdec0e00ad2ea0b7a6b2f5b55065784a35b7ca3a84615e8e25'
if hashlib.sha256(open(p,'rb').read()).hexdigest()!=expected: raise SystemExit('Xposed API dependency hash mismatch')
PY
find src -name '*.java' > build/sources.txt
javac -source 8 -target 8 -Xlint:-options -cp "$ANDROID:$API" -d build/classes @build/sources.txt
find build/classes -name '*.class' > build/classes.txt
"$BT/d8" --min-api 34 --lib "$ANDROID" --classpath "$API" --output build/dex @build/classes.txt
"$BT/aapt2" compile --dir res -o build/resources.zip
"$BT/aapt2" link -I "$ANDROID" --manifest AndroidManifest.xml -A assets -o build/unsigned.apk build/resources.zip
python3 - <<'PY'
import zipfile
with zipfile.ZipFile('build/unsigned.apk','a') as z:z.write('build/dex/classes.dex','classes.dex')
PY
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
SIGNING_KEY="${SIGNING_KEY:-build/local-signing.jks}"
SIGNING_PASSWORD="${SIGNING_PASSWORD:-local-build-only}"
if [ ! -f "$SIGNING_KEY" ]; then
  keytool -genkeypair -keystore "$SIGNING_KEY" -storepass "$SIGNING_PASSWORD" -keypass "$SIGNING_PASSWORD" -alias local -keyalg RSA -validity 3650 -dname 'CN=Fold App Layout Local Build' -noprompt
fi
export SIGNING_PASSWORD
"$BT/apksigner" sign --ks "$SIGNING_KEY" --ks-pass env:SIGNING_PASSWORD --out build/FoldAppLayout.apk build/aligned.apk
"$BT/apksigner" verify build/FoldAppLayout.apk
echo "Built: build/FoldAppLayout.apk"

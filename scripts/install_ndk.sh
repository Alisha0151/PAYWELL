#!/usr/bin/env bash
# Installs the Android NDK version Flutter expects, using the supported `sdkmanager`. macOS/Linux, bash 3.2 compatible.
#   bash scripts/install_ndk.sh [--accept-licenses] [--dry-run] [--version X --allow-mismatch] [--ignore-gradle]
# The NDK version is read from YOUR Flutter SDK (`flutter.ndkVersion`, packages/flutter_tools/gradle/.../FlutterExtension.kt), so the
# installed NDK always matches what the Flutter Gradle plugin will request. `--version X` is refused if it differs from Flutter's value
# unless you also pass --allow-mismatch.
# Safety: never deletes anything, never kills processes. Refuses to start if an NDK install is already running
# (sdkmanager / android-cli / Gradle auto-install) or if another run of this script holds the lock.
# An existing partial folder (no source.properties) is REPORTED, not removed: delete it yourself after checking.
set -u
REQ=""; ACCEPT=0; DRY=0; MISMATCH=0; IGNORE_GRADLE=0
while [ $# -gt 0 ]; do case "$1" in
  --version) REQ="$2"; shift 2;; --accept-licenses) ACCEPT=1; shift;; --dry-run) DRY=1; shift;;
  --allow-mismatch) MISMATCH=1; shift;; --ignore-gradle) IGNORE_GRADLE=1; shift;;
  -h|--help) sed -n 2,12p "$0"; exit 0;; *) echo "unknown option $1"; exit 2;; esac; done
die() { echo "ERROR: $*" >&2; exit 1; }
# --- NDK version Flutter will ask Gradle for
flutter_root() {
  command -v flutter >/dev/null 2>&1 || return 1
  R=$(flutter --version --machine 2>/dev/null | python3 -c 'import sys,json; print(json.load(sys.stdin)["flutterRoot"])' 2>/dev/null)
  [ -n "$R" ] && [ -d "$R" ] && { echo "$R"; return 0; }
  F=$(command -v flutter); F=$(readlink "$F" 2>/dev/null || echo "$F"); (cd "$(dirname "$F")/.." 2>/dev/null && pwd)
}
FR=$(flutter_root); FLUTTER_NDK=""
[ -n "$FR" ] && FLUTTER_NDK=$(grep -rhE "ndkVersion" "$FR/packages/flutter_tools/gradle/src/main" 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]{5,}' | head -1)
if [ -n "$FLUTTER_NDK" ]; then echo "Flutter SDK ($FR) requests NDK $FLUTTER_NDK (flutter.ndkVersion)"; else echo "WARNING: could not read flutter.ndkVersion from the Flutter SDK; falling back to the project's documented 28.2.13676358 - verify with: bash scripts/android_build_doctor.sh"; fi
if [ -n "$REQ" ] && [ -n "$FLUTTER_NDK" ] && [ "$REQ" != "$FLUTTER_NDK" ] && [ "$MISMATCH" != 1 ]; then die "--version $REQ differs from Flutter's $FLUTTER_NDK. Installing a different NDK is usually unnecessary; pass --allow-mismatch only if you really mean it."; fi
VER="${REQ:-${FLUTTER_NDK:-28.2.13676358}}"
HERE_D="$(cd "$(dirname "$0")" && pwd)"; BG="$HERE_D/../mobile/android/app/build.gradle.kts"
if [ -f "$BG" ]; then
  if grep -q "ndkVersion *= *flutter.ndkVersion" "$BG"; then echo "app/build.gradle.kts uses flutter.ndkVersion: consistent"
  else echo "WARNING: app/build.gradle.kts does not use flutter.ndkVersion: $(grep -n ndkVersion "$BG")"; fi
fi
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
[ -d "$SDK" ] || die "Android SDK not found at $SDK (set ANDROID_HOME)"
echo "SDK: $SDK   NDK wanted: $VER"

# 1. Java for sdkmanager
command -v java >/dev/null || die "java not on PATH (JDK 17+ required by sdkmanager)"
java -version 2>&1 | head -1

# 2. sdkmanager
SM="$SDK/cmdline-tools/latest/bin/sdkmanager"
[ -x "$SM" ] || SM="$(ls "$SDK"/cmdline-tools/*/bin/sdkmanager 2>/dev/null | head -1)"
[ -x "${SM:-/nonexistent}" ] || die "sdkmanager not found. Android Studio > Settings > Android SDK > SDK Tools > 'Android SDK Command-line Tools (latest)'."
echo "sdkmanager: $SM"

# 3. already installed and valid?
DIR="$SDK/ndk/$VER"
if [ -f "$DIR/source.properties" ]; then
  REV=$(sed -n 's/^Pkg.Revision *= *//p' "$DIR/source.properties")
  if [ "$REV" = "$VER" ]; then echo "OK: NDK $VER is already installed and valid ($DIR)"; exit 0; fi
  die "$DIR/source.properties reports revision '$REV', expected $VER. Not touching it."
fi
[ -d "$DIR" ] && echo "WARNING: $DIR exists but has no source.properties: a previous install was interrupted. It will NOT be deleted; sdkmanager will be asked to (re)install."

# 4. duplicate-install detection (never start a second install)
RUNNING=$(ps -axo pid,etime,command | grep -E "sdkmanager|android-cli|com.android.sdklib" | grep -E "ndk" | grep -v grep | grep -v "install_ndk.sh")
if [ -n "$RUNNING" ]; then
  echo "An NDK installation already appears to be running:"; echo "$RUNNING" | cut -c1-200
  echo "Not starting another one. Wait for it, or stop it yourself if it has been stuck for many minutes (check the network first)."; exit 3
fi
if [ "$IGNORE_GRADLE" != 1 ]; then
  GB=$(ps -axo pid,etime,command | grep -E "GradleWrapperMain|flutter_tools\.snapshot (build|run)|flutter(\.bat)? (build|run)" | grep -v grep | grep -v "install_ndk.sh")
  if [ -n "$GB" ]; then
    echo "A Gradle/Flutter build is running; it may be auto-installing the NDK itself:"; echo "$GB" | cut -c1-200
    echo "Not starting a second installation. Check ~/Library/Android/sdk/ndk and ~/.gradle/daemon/*/daemon-*.out.log; wait for it, or stop it yourself, or re-run with --ignore-gradle if you are sure it is not installing the NDK."; exit 3
  fi
fi
LOCK="${TMPDIR:-/tmp}/paywell_install_ndk.lock"
if ! mkdir "$LOCK" 2>/dev/null; then die "lock $LOCK exists (another install_ndk.sh run, or a crashed one). If you are sure none is running: rmdir '$LOCK'"; fi
trap 'rmdir "$LOCK" 2>/dev/null' EXIT

# 5. licences
if [ ! -s "$SDK/licenses/android-sdk-license" ]; then
  if [ "$ACCEPT" = 1 ]; then echo "Accepting Android SDK licences (you passed --accept-licenses)"; yes | "$SM" --licenses >/dev/null 2>&1
  else die "Android SDK licences not accepted. Read them with: $SM --licenses  (or re-run this script with --accept-licenses to accept them)"; fi
  [ -s "$SDK/licenses/android-sdk-license" ] || die "licences still not accepted"
fi
echo "licences: accepted"

# 6. network precheck with exact URL and error
URL="https://dl.google.com/android/repository/repository2-3.xml"
if [ "${PAYWELL_SKIP_NET_CHECK:-0}" = 1 ]; then CODE=200; RC=0; : > /tmp/ndk_curl_err; else CODE=$(curl -sS -L --max-time 20 -o /dev/null -w "%{http_code}" "$URL" 2>/tmp/ndk_curl_err); RC=$?; fi
if [ "$RC" != 0 ] || [ "$CODE" != "200" ]; then
  echo "NETWORK PROBLEM reaching $URL  (curl exit $RC, HTTP $CODE): $(cat /tmp/ndk_curl_err)"
  echo "Check VPN/proxy/firewall; if you use a proxy set it in ~/.gradle/gradle.properties and for sdkmanager use --proxy=http --proxy_host=... --proxy_port=..."
  exit 4
fi
echo "network: dl.google.com reachable"
df -h "$SDK" | tail -1; echo "(the NDK needs roughly 2-3 GB free during install)"

[ "$DRY" = 1 ] && { echo "dry run: would run: $SM --install \"ndk;$VER\" --verbose"; exit 0; }

# 7. install (foreground, visible progress). Failures print sdkmanager's own error including the failing URL.
echo "Installing ndk;$VER ... (large download: do not interrupt)"
"$SM" --install "ndk;$VER" --verbose; RC=$?
[ "$RC" = 0 ] || die "sdkmanager exited with $RC. Read its output above for the failing URL/error."

# 8. verify
[ -f "$DIR/source.properties" ] || die "install reported success but $DIR/source.properties is missing"
REV=$(sed -n 's/^Pkg.Revision *= *//p' "$DIR/source.properties")
[ "$REV" = "$VER" ] || die "installed revision '$REV' != $VER"
CLANG=$(find "$DIR/toolchains/llvm/prebuilt" -maxdepth 3 -name clang -type f 2>/dev/null | head -1)
[ -n "$CLANG" ] || die "NDK directory present but no clang found under toolchains/llvm/prebuilt: install looks incomplete"
echo "VERIFIED: NDK $REV at $DIR (clang: $CLANG)"

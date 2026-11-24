#!/usr/bin/env bash
# PAYWELL launcher for macOS/Linux (bash 3.2 compatible). Fails loudly: no step is silently skipped.
#   ./run_paywell.sh --help | --check | --build | --run [device-id]
#   --check  read-only environment checks (Flutter, Java, Android SDK/NDK, devices, backend reachability)
#   --build  check + flutter pub get + analyze + test + build debug APK (+ prints APK path and size)
#   --run    build as above, then install and launch on a device/emulator (first Android device, or the id you pass)
# API_BASE_URL (default http://10.0.2.2:8080 = host Mac from the Android emulator; use http://<mac-lan-ip>:8080 on a phone)
set -u
ROOT="$(cd "$(dirname "$0")" && pwd)"; MOBILE="$ROOT/mobile"
API="${API_BASE_URL:-http://10.0.2.2:8080}"
MODE="${1:-}"; DEVICE="${2:-}"
[ "$MODE" = "-h" ] || [ "$MODE" = "--help" ] || [ -z "$MODE" ] && { sed -n 2,8p "$0"; exit 0; }
case "$MODE" in --check|--build|--run) ;; *) echo "unknown option $MODE (try --help)"; exit 2;; esac
fail() { echo ""; echo "FAILED: $1"; [ -n "${2:-}" ] && echo "FIX: $2"; exit 1; }
LOGDIR="$ROOT/.run_logs"; mkdir -p "$LOGDIR"
# runstep <name> <fix-hint> <command...>: runs the command, shows its output live, saves it to .run_logs/<name>.log;
# on failure prints the exact last lines of that output and stops (nothing after it runs).
runstep() {
  NAME="$1"; HINT="$2"; shift 2
  step "$NAME"; echo "\$ $*"
  "$@" 2>&1 | tee "$LOGDIR/$NAME.log"; RC=${PIPESTATUS[0]}
  if [ "$RC" != 0 ]; then
    echo ""; echo "----- last 40 lines of $LOGDIR/$NAME.log (exit code $RC) -----"; tail -40 "$LOGDIR/$NAME.log"
    fail "step '$NAME' failed with exit code $RC (full output: $LOGDIR/$NAME.log)" "$HINT"
  fi
}
step() { echo ""; echo "== $* =="; }
[ -d "$MOBILE" ] || fail "no mobile/ folder next to this script" "run it from the extracted PAYWELL folder"

step "Flutter"; command -v flutter >/dev/null || fail "flutter not on PATH" "install Flutter and add it to PATH (see documentation/ANDROID_SETUP.md)"
flutter --version | head -2 || fail "flutter --version failed"
step "Java"; command -v java >/dev/null || fail "java not found" "brew install openjdk@21 and set JAVA_HOME (documentation/ANDROID_SETUP.md)"
JV=$(java -version 2>&1 | grep -m1 ' version ' | sed -n 's/.*"\([0-9]*\)[.".].*/\1/p'); echo "java major: ${JV:-?} JAVA_HOME=${JAVA_HOME:-<unset>}"
[ "${JV:-0}" -ge 17 ] 2>/dev/null || fail "Java $JV is too old" "use JDK 21"
[ "$JV" = "21" ] || echo "WARNING: verified with JDK 21, found $JV (Flutter may use its own: flutter config --jdk-dir)"
step "Android SDK / NDK"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"; [ -d "$SDK" ] || fail "Android SDK not found at $SDK" "install via Android Studio, or set ANDROID_HOME"
echo "SDK: $SDK"; [ -s "$SDK/licenses/android-sdk-license" ] || fail "Android licences not accepted" "flutter doctor --android-licenses"
NDKDIR=$(ls "$SDK/ndk" 2>/dev/null | head -5 | tr '\n' ' '); echo "NDKs installed: ${NDKDIR:-none}"
[ -n "$NDKDIR" ] || echo "NOTE: no NDK yet. The first build asks Gradle to install one; to avoid a silent stall run: bash scripts/install_ndk.sh --accept-licenses"
step "Devices"
flutter devices 2>&1 | head -20
IDS=$(flutter devices --machine 2>/dev/null | python3 -c "import sys,json; print('\n'.join(d['id'] for d in json.load(sys.stdin) if d.get('targetPlatform','').startswith('android')))" 2>/dev/null)
echo "Android devices: ${IDS:-none}"
EMU=$(flutter emulators 2>/dev/null | grep -i "android" | head -3); [ -n "$EMU" ] && echo "Available emulators:" && echo "$EMU"
step "Backend"
if curl -sS -m 5 -o /dev/null -w "%{http_code}" "http://localhost:8080/api/v1/meta" 2>/dev/null | grep -q 200; then echo "backend reachable at http://localhost:8080 (app uses $API)"
else echo "WARNING: backend not reachable on localhost:8080. Start it: ./scripts/dev-up.sh (needs .env). The app will open but sign-in/payments will fail."; fi
[ "$MODE" = "--check" ] && { echo ""; echo "check finished (no build performed)."; exit 0; }

cd "$MOBILE" || exit 1
runstep "1-pub-get" "check network access to pub.dev and the pubspec.yaml error above" flutter pub get
runstep "2-analyze" "fix every analyzer issue listed above (do not disable lints); re-run" flutter analyze
runstep "3-test" "see the failing test output above" flutter test
runstep "4-build-debug-apk" "run: bash ../scripts/android_build_doctor.sh  and read documentation/BUILD_TROUBLESHOOTING.md" flutter build apk --debug --dart-define=API_BASE_URL="$API"
step "5-verify-apk"
APK="$MOBILE/build/app/outputs/flutter-apk/app-debug.apk"
[ -s "$APK" ] || fail "build reported success but $APK is missing/empty"
echo "APK: $APK ($(du -h "$APK" | cut -f1))"
[ "$MODE" = "--build" ] && exit 0

step "install and launch"
[ -n "$DEVICE" ] || DEVICE=$(echo "$IDS" | head -1)
[ -n "$DEVICE" ] || fail "no Android device/emulator available" "start an emulator (Android Studio > Device Manager, or: flutter emulators --launch <id>) or connect a phone with USB debugging, then re-run"
echo "device: $DEVICE"
flutter run -d "$DEVICE" --dart-define=API_BASE_URL="$API"

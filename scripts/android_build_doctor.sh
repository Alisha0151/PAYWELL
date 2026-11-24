#!/usr/bin/env bash
# PAYWELL Android build doctor: read-only diagnostics. macOS/Linux, bash 3.2 compatible.
#   cd mobile && bash ../scripts/android_build_doctor.sh [--fetch] [--ndk]
# Writes ./android_doctor.log (no secrets: local.properties / key.properties contents are never printed).
# It NEVER kills processes, deletes caches, edits project files or uninstalls SDK components.
#   --fetch  Downloads the Gradle distribution the wrapper wants into ~/Downloads (resumable `curl -C -`), verifies its SHA-256 and
#            prints the file:// distributionUrl you could use offline. Skips if a verified copy already exists or if a curl for the
#            same URL is already running (no duplicate downloads). Does not edit gradle-wrapper.properties.
#   --ndk    Runs scripts/install_ndk.sh (sdkmanager; licence check; refuses if an NDK install is already running).
set -u
LOG="android_doctor.log"; : > "$LOG"
FETCH=0; NDK=0; for a in "$@"; do case "$a" in --fetch) FETCH=1;; --ndk) NDK=1;; -h|--help) sed -n 2,12p "$0"; exit 0;; esac; done
say() { echo "$@" | tee -a "$LOG"; }
run() { say ""; say "\$ $*"; ( "$@" 2>&1 | head -80 ) | tee -a "$LOG"; }
warn=0; bad() { say "  [PROBLEM] $*"; warn=$((warn+1)); }; ok() { say "  [ok] $*"; }
[ -f pubspec.yaml ] && [ -d android ] || { echo "Run from the Flutter project folder (mobile/)."; exit 1; }
HERE="$(cd "$(dirname "$0")" && pwd)"

say "== 1. Flutter / Java =="
run flutter --version
command -v flutter >/dev/null || bad "flutter not on PATH"
run java -version
say "JAVA_HOME=${JAVA_HOME:-<unset>}"
JV=$(java -version 2>&1 | grep -m1 ' version ' | sed -n 's/.*"\([0-9]*\)[.".].*/\1/p')
[ "${JV:-0}" = "21" ] && ok "java $JV" || bad "java major is '${JV:-?}', project is verified with JDK 21. Flutter's JDK: run 'flutter config --jdk-dir <jdk21>'"
command -v /usr/libexec/java_home >/dev/null && run /usr/libexec/java_home -V
run flutter config --list

say ""; say "== 2. Android SDK =="
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -z "$SDK" ] && [ -f android/local.properties ] && SDK=$(sed -n 's/^sdk.dir=//p' android/local.properties | head -1)
[ -z "$SDK" ] && SDK="$HOME/Library/Android/sdk"
say "SDK path: $SDK"; [ -d "$SDK" ] && ok "exists" || bad "SDK directory missing"
say "platforms:"; ls "$SDK/platforms" 2>&1 | tee -a "$LOG"
say "build-tools:"; ls "$SDK/build-tools" 2>&1 | tee -a "$LOG"
say "cmdline-tools:"; ls "$SDK/cmdline-tools" 2>&1 | tee -a "$LOG"
say "system-images:"; find "$SDK/system-images" -maxdepth 2 -mindepth 2 2>/dev/null | tee -a "$LOG"
[ -s "$SDK/licenses/android-sdk-license" ] && ok "SDK licences accepted" || bad "SDK licences NOT accepted (flutter doctor --android-licenses)"
say "free disk:"; df -h "$SDK" "$HOME/.gradle" 2>&1 | tee -a "$LOG"

say ""; say "== 3. NDK =="
FR="$(command -v flutter)"; FR="$(cd "$(dirname "$(readlink "$FR" 2>/dev/null || echo "$FR")")/.." 2>/dev/null && pwd)"
WANT=$(grep -rhn "ndkVersion" "$FR/packages/flutter_tools/gradle/src/main" 2>/dev/null | head -3)
say "Flutter's ndkVersion definition:"; say "${WANT:-<not found>}"
say "app/build.gradle.kts: $(grep -n ndkVersion android/app/build.gradle.kts)"
NV=$(echo "$WANT" | grep -o '[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*' | head -1); NV="${NV:-28.2.13676358}"
say "NDK version used in checks: $NV"
ls -la "$SDK/ndk" 2>&1 | tee -a "$LOG"
if [ -f "$SDK/ndk/$NV/source.properties" ]; then ok "NDK $NV valid: $(grep Pkg.Revision "$SDK/ndk/$NV/source.properties")"
elif [ -d "$SDK/ndk/$NV" ]; then bad "NDK folder $SDK/ndk/$NV exists but is incomplete (no source.properties): interrupted install. Do not delete blindly; run scripts/install_ndk.sh"
else bad "NDK $NV not installed: run  bash $HERE/install_ndk.sh --accept-licenses  (or this script with --ndk)"; fi
say "SDK/NDK/Gradle related processes (read-only listing):"
ps -axo pid,etime,command | grep -E "android-cli|sdkmanager|GradleDaemon|GradleWrapperMain|KotlinCompileDaemon|flutter_tools" | grep -v grep | cut -c1-220 | tee -a "$LOG"
say "partial SDK downloads (.temp / .downloadIntermediates):"; du -sh "$SDK/.temp" "$SDK/.downloadIntermediates" "$SDK/.sdk" 2>&1 | tee -a "$LOG"
for u in https://dl.google.com/android/repository/repository2-3.xml https://dl.google.com/android/repository/android-ndk-r28c-darwin.zip https://dl.google.com/dl/android/maven2/index.html https://repo.maven.apache.org/maven2/ https://plugins.gradle.org/m2/; do
  say "$(curl -sI -L --max-time 20 -o /dev/null -w "HTTP %{http_code} (%{time_total}s)" "$u" 2>&1)  $u"; done
say "proxy: HTTPS_PROXY=${HTTPS_PROXY:-<unset>} https_proxy=${https_proxy:-<unset>}"
grep -iE "proxy" ~/.gradle/gradle.properties 2>/dev/null | sed 's/\(assword=\).*/\1***/' | tee -a "$LOG"

say ""; say "== 4. Gradle wrapper and distribution =="
WP=android/gradle/wrapper/gradle-wrapper.properties
[ -f "$WP" ] && cat "$WP" | tee -a "$LOG" || bad "$WP missing (Flutter tool recreates it on first build)"
GV=$(sed -n 's#.*gradle-\([0-9][0-9.]*\)-\(bin\|all\)\.zip.*#\1#p' "$WP" 2>/dev/null | head -1)
GT=$(sed -n 's#.*gradle-[0-9][0-9.]*-\(bin\|all\)\.zip.*#\1#p' "$WP" 2>/dev/null | head -1)
say "wrapper Gradle: ${GV:-?} (distribution type: ${GT:-?}; project standard is 'all' - see documentation/ANDROID_SETUP.md)"
for f in android/gradlew android/gradlew.bat android/gradle/wrapper/gradle-wrapper.jar; do [ -s "$f" ] && ok "wrapper file present: $f" || bad "wrapper file MISSING or empty: $f"; done
[ -x android/gradlew ] && ok "android/gradlew is executable" || bad "android/gradlew is not executable: chmod +x android/gradlew"
ls -la ~/.gradle/wrapper/dists 2>&1 | tee -a "$LOG"
CACHED=$(find ~/.gradle/wrapper/dists -maxdepth 3 -type d -name "gradle-${GV}-${GT}" 2>/dev/null | head -1)
OTHER=$(find ~/.gradle/wrapper/dists -maxdepth 3 -type d -name "gradle-${GV}-*" 2>/dev/null | grep -v -- "-${GT}\$" | head -1)
[ -n "$CACHED" ] && ok "Gradle $GV-$GT cached: $CACHED (no new download needed)" || bad "Gradle $GV-$GT not cached yet (first build downloads it from services.gradle.org)"
[ -z "$CACHED" ] && [ -n "$OTHER" ] && say "NOTE: a different distribution type of the same version is cached ($OTHER). The wrapper keys its cache by URL, so it will NOT reuse it. Make gradle-wrapper.properties match what you already downloaded (this project's default is -all). Nothing is modified by this script."
find ~/.gradle/wrapper/dists \( -name "*.part" -o -name "*.lck" \) 2>/dev/null | tee -a "$LOG"
say "Gradle daemons: $(ls ~/.gradle/daemon 2>/dev/null | tr '\n' ' ')"
say "$(curl -sI -L --max-time 20 -o /dev/null -w "HTTP %{http_code} (%{time_total}s)" "https://services.gradle.org/distributions/gradle-${GV}-${GT}.zip" 2>&1)  services.gradle.org gradle-${GV}-${GT}.zip"

say ""; say "== 5. Project configuration consistency =="
cat android/settings.gradle.kts android/app/build.gradle.kts android/gradle.properties 2>/dev/null | sed 's/^/    /' >> "$LOG"
grep -q 'namespace = "com.paywell.app"' android/app/build.gradle.kts && ok "namespace com.paywell.app" || bad "namespace is not com.paywell.app"
grep -q 'applicationId = "com.paywell.app"' android/app/build.gradle.kts && ok "applicationId com.paywell.app" || bad "applicationId is not com.paywell.app"
AGP=$(sed -n 's/.*com.android.application") version "\([^"]*\)".*/\1/p' android/settings.gradle.kts); KGP=$(sed -n 's/.*kotlin.android") version "\([^"]*\)".*/\1/p' android/settings.gradle.kts)
say "AGP=${AGP:-?} Kotlin plugin=${KGP:-?} Gradle=${GV:-?}"
BK=$(sed -n 's/^android.builtInKotlin=//p' android/gradle.properties); ND=$(sed -n 's/^android.newDsl=//p' android/gradle.properties)
say "android.builtInKotlin=${BK:-<unset>} android.newDsl=${ND:-<unset>}"
HASKA=$(grep -c 'id("kotlin-android")' android/app/build.gradle.kts)
[ "$BK" = "true" ] && [ "$HASKA" != 0 ] && bad "builtInKotlin=true but app still applies kotlin-android: remove that plugin line"
[ "$BK" != "true" ] && [ "$HASKA" = 0 ] && bad "kotlin-android removed but builtInKotlin is not true"
grep -q "kotlinOptions" android/app/build.gradle.kts && bad "kotlinOptions is removed in newer Kotlin Gradle plugins: use kotlin { compilerOptions { jvmTarget } }"
[ "$ND" = "false" ] && ok "newDsl=false (required by Flutter's AGP 9 migration guide until Flutter migrates)" || bad "android.newDsl not false"
grep -q "usesCleartextTraffic" android/app/src/debug/AndroidManifest.xml 2>/dev/null && ok "debug manifest allows http to 10.0.2.2" || bad "debug cleartext not enabled: http://10.0.2.2:8080 will be blocked"
grep -q "android.permission.INTERNET" android/app/src/main/AndroidManifest.xml && ok "INTERNET permission" || bad "INTERNET permission missing"
[ -f android/local.properties ] && ok "local.properties present (contents not logged)" || say "  (local.properties absent: created by 'flutter pub get')"
say "plugin versions (pubspec.lock):"; grep -E "^  (flutter_secure_storage|path_provider_android|jni|dio|go_router|flutter_riverpod|intl)[a-z_]*:|^    version:" pubspec.lock 2>/dev/null | paste - - 2>/dev/null | tee -a "$LOG"

if [ "$FETCH" = 1 ] && [ -n "$GV" ]; then
  say ""; say "== 6. --fetch Gradle $GV =="
  U="https://services.gradle.org/distributions/gradle-${GV}-${GT}.zip"; OUT="$HOME/Downloads/gradle-${GV}-${GT}.zip"
  WANTSHA=$(curl -sL --max-time 20 "$U.sha256")
  if [ -f "$OUT" ] && [ "$(shasum -a 256 "$OUT" | cut -d' ' -f1)" = "$WANTSHA" ] && [ -n "$WANTSHA" ]; then say "verified copy already at $OUT: not downloading again"
  elif ps -axo command | grep -v grep | grep -q "curl.*gradle-${GV}-${GT}.zip"; then say "a curl download of this file is already running: not starting another"
  else curl -L -C - --retry 3 --retry-delay 5 --connect-timeout 20 -o "$OUT" "$U" 2>&1 | tail -3 | tee -a "$LOG"; fi
  HAVE=$(shasum -a 256 "$OUT" 2>/dev/null | cut -d' ' -f1); say "expected sha256: ${WANTSHA:-<could not fetch>}"; say "actual   sha256: ${HAVE:-<no file>}"
  [ -n "$WANTSHA" ] && [ "$WANTSHA" = "$HAVE" ] && say "CHECKSUM OK. Offline option: set distributionUrl=file\\:///Users/$USER/Downloads/gradle-${GV}-${GT}.zip in $WP (your decision; the script does not edit it)." || say "CHECKSUM NOT VERIFIED: do not use this file."
fi
if [ "$NDK" = 1 ]; then say ""; say "== 7. --ndk =="; bash "$HERE/install_ndk.sh" --version "$NV" 2>&1 | tee -a "$LOG"; fi
say ""; say "Done: $warn problem(s) flagged. Log: $(pwd)/$LOG"

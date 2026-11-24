# Build troubleshooting
First run `cd mobile && bash ../scripts/android_build_doctor.sh` (read-only; writes `mobile/android_doctor.log`, no secrets).

| Symptom | Cause / what to check | Action |
|---|---|---|
| `Gradle wrapper download: ConnectException / timed out` | `services.gradle.org` blocked/slow (VPN, proxy, firewall) | doctor shows the HTTP status. `bash ../scripts/android_build_doctor.sh --fetch` downloads Gradle with resume + SHA-256 check (skips if a verified copy exists or a curl is already running). Offline option: `distributionUrl=file\:///Users/<you>/Downloads/gradle-9.5.0-all.zip` in `gradle-wrapper.properties` (manual edit). Proxy: `systemProp.https.proxyHost/Port` in `~/.gradle/gradle.properties` |
| Stuck at `Running Gradle task 'assembleDebug'` with `Downloading Android CLI` / NDK | AGP auto-installing `ndk;28.2.13676358` over a slow/blocked `dl.google.com` | check `ps` for a running install (don't start another); `ls ~/Library/Android/sdk/ndk`; then `bash scripts/install_ndk.sh --accept-licenses` which shows progress and the exact failing URL. Stop a stuck process yourself only after confirming it makes no progress; scripts never kill anything |
| Java 25 selected | `JAVA_HOME` or Flutter's JDK points to a newer JDK | `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`; `flutter config --jdk-dir "$JAVA_HOME"` |
| AGP/Kotlin plugin conflict | old versions in `settings.gradle.kts` | this repo ships AGP 9.3.1 / Kotlin 2.4.20 |
| Gradle too old | wrapper < Flutter minimum | wrapper is 9.5.0 |
| `kotlinOptions` unresolved/error | removed in newer Kotlin Gradle plugins | already migrated to `kotlin { compilerOptions }` |
| Plugin "not compatible with built-in Kotlin" | you enabled `android.builtInKotlin=true` before all plugins migrated | set it back to `false` |
| `flutter.sdk not set in local.properties` | file not generated | `flutter pub get` in `mobile/` |
| Cleartext traffic blocked | release build or missing debug manifest | debug manifest allows it; use HTTPS in release |
| App opens but cannot sign in | backend not running / wrong URL | `./run_paywell.sh --check`; emulator needs `10.0.2.2`, phone needs LAN IP |
| Pub/analyzer/test failures | genuine code issues (the Dart was never compiled by the author) | send the first errors; fix them, do not disable lints |
Gradle daemon log: `~/.gradle/daemon/<version>/daemon-*.out.log`. Verbose build: `flutter build apk --debug -v`.

## `Removing unused resources requires unused code shrinking to be turned on` (build.gradle.kts line 7)
Cause: `app/build.gradle.kts` forced `isMinifyEnabled = false` on the `release` build type, while Flutter's Gradle plugin turns resource shrinking on for release. AGP 9 validates this when `:app` is configured, which `android/build.gradle.kts` triggers via `evaluationDependsOn(":app")` (the line-7 reference), so even `assembleDebug` failed. Fix (V5): removed the override so Flutter manages release shrinking consistently; debug builds never shrink. If it persists, send `flutter build apk --debug --stacktrace` output.
`Upgrading build.gradle.kts` is printed by the Flutter tool's own project migrator, which rewrites `android/build.gradle.kts` in your copy. Compare with `diff <(unzip -p ~/Downloads/PAYWELL_FINAL_FIXED_V5.zip paywell/mobile/android/build.gradle.kts) mobile/android/build.gradle.kts`.

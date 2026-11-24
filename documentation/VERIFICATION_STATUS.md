# Verification status (honest, updated 2026-11-18)

The authoring environment has JDK 21, Maven, PostgreSQL 16 and Redis binaries, but **no Flutter/Dart SDK, no Android SDK,
no Xcode**, and its network policy blocks Maven Central, Docker Hub and Flutter's download host (HTTP 403). So dependency
resolution, Java compilation, `mvn test`, `flutter pub get/analyze/test/build` and image pulls were impossible.

## Actually run and passed
| Check | Result |
|---|---|
| fraud-service `pytest` | 7 passed, 1 skipped |
| Flyway SQL V1 + V2 applied to a real PostgreSQL 16 | Applied with no errors; 17 tables created |
| DB immutability triggers (`audit_events`) | UPDATE and DELETE are rejected by the database |
| JPA entity columns vs the real schema (heuristic script over 17 entities) | 0 mismatches |
| Redis server start + `PING` | PONG (Redis is only provisioned in compose; the backend does not use it yet) |
| All 108+12 Java files parsed with `javac` (JDK 21) | 0 syntax errors; every error is a missing third-party dependency (Spring, JPA, JUnit...). No missing symbol refers to a class defined in this project |
| `docker compose config` on `infrastructure/docker-compose.yml` | Valid |
| Mobile -> backend routes (static comparison) | All 15 distinct call sites in the app (including payments/send, wallet/top-up, payments/requests) match a backend route; 0 unmatched |
| Dart import resolution | All relative/package imports resolve to existing files |
| YAML / XML / Python syntax | Pass |

## NOT verified (could not be done here)
* `mvn test` / backend compile with real dependencies; Spring context startup; Hibernate `ddl-auto: validate`; Testcontainers tests.
* Docker Compose stack start (Docker daemon starts, but images cannot be pulled; Postgres/Redis were run natively instead).
* Backend running end-to-end: login, wallet, simulated payments, transaction history, Money Radar. (Only the SQL schema was exercised.)
* `flutter pub get`, `flutter analyze`, `flutter test`. All Dart is uncompiled.
* Navigation, buttons, forms and screens (never run).
* Android debug/release APK and AAB: not built.
* iOS: `Runner.xcodeproj` is NOT included (cannot be generated without Flutter; a hand-written one could not be validated). Run `flutter create . --platforms=ios` on a Mac.
* Launch animation and logo in Flutter (a port of the owner-supplied striped-P animation, `brand/reference/`) on a device/emulator: never run.

## Changes this round
No source code needed changing: the checks above found no defects. They do not prove the code compiles.
Expect compile/analyzer fixes on the first real `mvn test` and `flutter analyze`.

## Verified vs untested at a glance
* **Verified (executed):** SQL migrations on PostgreSQL 16, audit/ledger immutability trigger behaviour (audit table tested), fraud-service tests,
  Redis start/ping, compose file validity, Java syntax parse, route matching between app and backend, YAML/XML/Python syntax.
* **Untested:** everything that needs a compile of Java/Dart, a running backend, a device, or a build: see "NOT verified" above.
* The application is a demo and is **not production-ready**. See `KNOWN_GAPS.md` and `FINTECH_LAUNCH_READINESS.md`.

## Android build repair round (2026-11-21)
Environment: no Flutter SDK, Android SDK, emulator or Mac access. **No Gradle/Flutter command was run.**
* Configuration verified only by reading/static checks: Gradle/AGP/Kotlin settings written to the owner's stated toolchain and Flutter's official AGP 9 / built-in Kotlin pages; the doctor script's consistency checks pass on the edited files (namespace, applicationId, newDsl, Kotlin plugin vs `builtInKotlin`, cleartext debug manifest, INTERNET permission).
* Scripts tested here with fake SDK folders: `install_ndk.sh` (licence gate, network failure message, success + verification, already-installed, partial folder not deleted, lock refusal); `bash -n` syntax on all scripts; `run_paywell.sh --help/--check` failure path.
* Contract checks (static): app default API URL `10.0.2.2:8080`; backend FraudClient calls `/v1/risk/assess` with `X-Service-Token`, matching fraud-service; compose service names/ports consistent with `.env.example`.
* NOT tested: Gradle sync, `flutter pub get/analyze/test`, APK build, NDK installation, emulator install/launch, Android Studio run configuration.

## V2 round (2026-11-22): what was and was not run
**Actually executed here**
* Gradle wrapper files generated with a local Gradle 8.14.3 and exercised end-to-end against a locally built `-all` test distribution (wrapper jar + script run). This proves the wrapper files work; it does **not** prove Gradle 9.5.0 / AGP 9.3.1 work.
* `run_paywell.sh` step order and stop-on-failure, tested with a *fake* `flutter` (not the real one).
* `install_ndk.sh` tested with fake SDK/Flutter folders: version from `flutter.ndkVersion`, existing-NDK detection, mismatch refusal, active android-cli/sdkmanager and Gradle detection. No real NDK was downloaded.
* `android_build_doctor.sh` wrapper-file and `-all`/`-bin` cache checks tested with a fake HOME.
* `bash -n` on all scripts; `PAYWELL.xml` parsed as well-formed XML; ZIP integrity checks.

**Static only (not compiled)**: 13 deprecated `withOpacity` calls replaced by `withValues(alpha:)` and 2 unused imports removed. `flutter analyze` was never run because no Flutter SDK is available here.

**NOT run**: `flutter pub get/analyze/test`, any Gradle build with 9.5.0/AGP 9.3.1, `flutter build apk`, APK generation, real NDK install, Android Studio import of `PAYWELL.xml`, emulator install/launch. Your earlier report (backend 47 tests, Flutter 13 tests passing) came from your machine, not from this session.

## V3 round (2026-11-23): first real run on the owner's Mac (results reported by the owner, not executed in this session)
Environment (owner): Flutter 3.50.0, Dart 3.14.0, JDK 21.0.13, Android SDK 37.0.0, NDK 28.2.13676358 installed and validated, Gradle wrapper 9.5.0, AGP 9.3.1, Kotlin 2.4.20.
* `flutter pub get`: PASSED (owner-reported).
* `flutter analyze` on V2: FAILED with 3 issues (owner-reported): unused field `tagLh`; `prefer_const_declarations` at `paywell_logo.dart:122`; `Money.==` parameter should be named `other`.
* V3 fixes (source edits; **`flutter analyze` / `flutter test` have NOT been re-run in this session - no Flutter SDK here**): removed `tagLh` (also from `brand/gen_flutter.py`), `final top` -> `const top`, `operator ==(Object other)`.
* Not yet confirmed by anyone: `flutter analyze` exit 0, `flutter test`, `flutter build apk --debug`, APK, emulator launch.
* Note: `analysis_options.yaml` still contains `deprecated_member_use: ignore` (pre-existing). All known `withOpacity` uses were already migrated, so it can be removed; left untouched pending a green analyze.

## V4 round (2026-11-23)
* Owner-reported on a real Mac with V3: `flutter pub get` PASSED; `flutter analyze` reported 1 remaining info: `paywell_logo.dart:135:7 prefer_const_declarations` (the three V2 issues were cleared).
* V4 fix (source edit, **not yet analyzed**): `final ruleY` -> `const ruleY` (it derives from the const `top`). The generator template `brand/gen_flutter.py` now emits `const top` and `const ruleY`; regenerating produces a file identical to the shipped one.
* Still unconfirmed: `flutter analyze` exit 0, `flutter test`, `flutter build apk --debug`, APK, emulator launch. Owner will verify on the Mac.

## V5 round (2026-11-23)
Owner-reported on a real Mac with V4: pub get PASSED, `flutter analyze` PASSED (no issues), `flutter test` PASSED (13 tests). `flutter build apk --debug` FAILED during Gradle configuration ("Removing unused resources requires unused code shrinking...").
V5 change (not yet built): removed `isMinifyEnabled = false` from the release build type in `app/build.gradle.kts`. No SDK/NDK/AGP/Kotlin/Gradle versions changed. Still unconfirmed: debug APK build, APK, emulator launch.

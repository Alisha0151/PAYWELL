# PAYWELL

A demo payments app: Flutter mobile client, Java 21 / Spring Boot 3 backend, Python FastAPI fraud-risk demo service.

> **Honest scope.** All payments are **simulated**. No real money moves. PAYWELL is not a bank, not licensed, and makes no
> UPI/RBI/PCI claims. OTP delivery is not claimed unless you configure a real `OtpSender`.

## Layout
| Path | What |
|---|---|
| `mobile/` | Flutter app (Material 3, Riverpod, GoRouter, Dio) |
| `backend/` | Spring Boot API, Flyway migrations, tests |
| `fraud-service/` | FastAPI risk-scoring demo (rule-based, not ML) |
| `infrastructure/` | docker-compose (Postgres, Redis, fraud-service, backend, optional Prometheus) |
| `brand/` | Logo generator, SVG/PNG assets, app-icon sources |
| `documentation/` | Architecture, ERD, API, security, launch readiness, build/iOS notes |

## Quick start
```
cp .env.example .env          # fill in secrets yourself (openssl rand -base64 48)
./scripts/dev-up.sh           # Postgres + Redis + fraud-service + backend on :8080
cd mobile && flutter create . --platforms=android,ios   # regenerates missing native files (keeps lib/ and assets)
flutter pub get && flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```
See `documentation/BUILD_AND_INSTALL.md`.

## Verification status (be aware)
See `documentation/VERIFICATION_STATUS.md`. In short: the fraud-service tests were run and pass; the Java and Dart code
No APK/AAB/IPA was built.

## Where to start
* `documentation/ANDROID_SETUP.md` / `BUILD_TROUBLESHOOTING.md` - Android Studio, NDK, Gradle (start here for Android)
* `./run_paywell.sh --help` - check / build / run helper;  `scripts/android_build_doctor.sh`, `scripts/install_ndk.sh`
* Gradle wrapper is bundled (`mobile/android/gradlew`, Gradle 9.5.0 `-all`); see ANDROID_SETUP.md
* `documentation/LOCAL_DEVELOPMENT_HANDOVER.md` - macOS setup, iOS project generation, run/build/test steps
* `documentation/ENVIRONMENT_VARIABLES.md` - configuration reference
* `documentation/KNOWN_GAPS.md` - prioritised remaining work
* `documentation/VERIFICATION_STATUS.md` - what was actually verified vs not

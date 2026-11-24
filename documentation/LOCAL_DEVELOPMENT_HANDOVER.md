# Local development handover (macOS)

> **Status: demo / not production-ready.** Payments are simulated. Read `VERIFICATION_STATUS.md` first: the Java and Dart
> code has **never been compiled or run**. Treat step 6 ("first build") as the point where real compile errors may appear.
> Commands below are standard tooling commands, but this exact sequence has not been executed against this repo.

## 1. Install the toolchain (macOS, Apple Silicon or Intel)
```bash
# 1.1 Xcode (App Store), then:
sudo xcodebuild -license accept
sudo xcodebuild -runFirstLaunch
xcode-select --install            # command line tools (ignore "already installed")

# 1.2 Homebrew (https://brew.sh), then:
brew install openjdk@21 maven postgresql@16 redis cocoapods git
brew install --cask docker android-studio flutter

# 1.3 Make JDK 21 visible to macOS and Maven
sudo ln -sfn "$(brew --prefix)/opt/openjdk@21/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-21.jdk
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc && source ~/.zshrc
java -version && mvn -version        # both must report Java 21

# 1.4 Android SDK: open Android Studio once -> More Actions -> SDK Manager -> install
#     "Android SDK Platform (latest)", "Android SDK Command-line Tools", "Platform-Tools". Then:
flutter doctor --android-licenses
flutter doctor -v                    # fix every item it flags for Android toolchain and Xcode
```
* Docker: open Docker Desktop once and wait until it says "running". PostgreSQL/Redis can come from Docker (recommended) or
  from Homebrew; you do not need both. If you use Docker, you can skip `brew install postgresql@16 redis`.
* Flutter must satisfy `mobile/pubspec.yaml`: Dart >= 3.6, Flutter >= 3.27 (current stable satisfies this).

> **Android:** the Gradle files are now shipped and edited for AGP 9.3.1 / Gradle 9.5.0 / Kotlin 2.4.20: see `ANDROID_SETUP.md` and `BUILD_TROUBLESHOOTING.md`. Do NOT run `flutter create` for Android unless those files are broken (use `--platforms=ios` instead).

## 2. Generate the missing iOS (and refresh Android) native projects
`mobile/ios/Runner.xcodeproj` is intentionally absent: it cannot be hand-written reliably. Generate it with Flutter:
```bash
cd PAYWELL/mobile
git init && git add -A && git commit -m "baseline"        # so you can see exactly what Flutter changes
flutter create . --platforms=ios --org com.paywell --project-name paywell
git status && git diff                                      # review
```
`flutter create .` in an existing project adds missing files and keeps `lib/`, `assets/`, `test/`. **Check the diff** for any
PAYWELL-specific file it replaced and restore it with `git checkout -- <file>`:
`ios/Runner/Info.plist`, `ios/Runner/AppDelegate.swift`, `ios/Podfile`, `ios/Runner/Assets.xcassets/AppIcon.appiconset/*`,
`android/app/src/main/AndroidManifest.xml`, `android/app/src/main/res/**`, `android/app/build.gradle.kts`.
If Android Gradle fails because the hand-written Gradle files are incompatible with your Flutter version, the fallback is:
move `android/` aside, run `flutter create . --platforms=android`, then copy back only `res/` (icons, colors), the
manifest permissions/label, and `applicationId`/`minSdk 23`. (Untested path.)

Then for iOS:
```bash
cd ios && pod install --repo-update && cd ..
open ios/Runner.xcworkspace          # always the .xcworkspace, not .xcodeproj
```
In Xcode -> Runner target -> Signing & Capabilities: choose your Team. **Change the bundle identifier** from the
placeholder `com.paywell.app` to one unique to your Apple account. Set Deployment Target to iOS 13.0 or higher
(`flutter_secure_storage` needs it; also set `platform :ios, '13.0'` in `ios/Podfile` if `pod install` complains).
Check `ios/Runner/Assets.xcassets/AppIcon.appiconset` still contains the PAYWELL icon (only a 1024 px icon is supplied;
Xcode generates the smaller sizes from it when the asset catalogue uses the single-size format, otherwise provide the sizes).

## 3. Configure environment
```bash
cd PAYWELL
cp .env.example .env
# edit .env; generate real secrets:
openssl rand -base64 48      # use for PAYWELL_JWT_SECRET (>= 32 bytes) and FRAUD_SERVICE_TOKEN
```
All variables are listed in `ENVIRONMENT_VARIABLES.md`. `.env` is git-ignored; never commit it.

## 4. Run database + backend
**Option A - everything in Docker (backend, Postgres, Redis, fraud service):**
```bash
cd PAYWELL
./scripts/dev-up.sh                         # = docker compose --env-file .env -f infrastructure/docker-compose.yml up --build
curl http://localhost:8080/api/v1/meta      # honesty flags (demo mode, simulated provider, OTP configured?)
open http://localhost:8080/swagger-ui.html
```
**Option B - Postgres/Redis/fraud in Docker, backend from Maven (faster edit loop):**
```bash
cd PAYWELL
docker compose --env-file .env -f infrastructure/docker-compose.yml up -d postgres redis fraud-service
set -a; source .env; set +a
export DB_URL=jdbc:postgresql://localhost:5432/${POSTGRES_DB:-paywell} DB_USER=${POSTGRES_USER:-paywell} DB_PASSWORD=$POSTGRES_PASSWORD
export FRAUD_SERVICE_URL=http://localhost:8000 SPRING_PROFILES_ACTIVE=local
cd backend && mvn test                      # FIRST: unit tests (no Docker needed). Fix compile errors here.
mvn spring-boot:run
```
Flyway creates the schema (V1, V2) on startup; `ddl-auto` is `validate`, so a mismatch between entities and schema fails startup
with a clear Hibernate message (possible, never tested). Integration tests: `mvn verify -Pintegration` (needs Docker).
Fraud service alone: `./scripts/test-fraud-service.sh` (verified: 7 passed, 1 skipped).

## 5. Run the Flutter app
```bash
cd PAYWELL/mobile
flutter pub get
flutter analyze                       # FIRST: expect to fix some errors/warnings
flutter test
# Android emulator (host machine is 10.0.2.2 from the emulator):
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
# iOS simulator (shares the Mac's localhost):
open -a Simulator && flutter run --dart-define=API_BASE_URL=http://localhost:8080
```
Android blocks cleartext HTTP in release builds; a debug manifest allows it (`android/app/src/debug`). For a real device use
the Mac's LAN IP (`ipconfig getifaddr en0`) in `API_BASE_URL` and keep phone and Mac on the same Wi-Fi. iOS App Transport
Security also blocks plain HTTP to non-localhost hosts: add an ATS exception for development or serve the API over HTTPS.

**Using the app:** there is no fixed demo account. Sign up in the app (password 10-72 chars with a letter and a digit); with
`paywell.demo.enabled=true` the wallet is seeded with clearly-flagged synthetic demo data. Forgot-password OTP: with
`SPRING_PROFILES_ACTIVE=local` the code is **printed in the backend console** (dev-echo sender); no SMS/email is sent.

## 6. Android APK and testing on a phone
```bash
cd PAYWELL/mobile
flutter build apk --debug             # build/app/outputs/flutter-apk/app-debug.apk
flutter build apk --release           # currently signed with the DEBUG key (see below): for testing only
flutter build appbundle --release     # AAB for Play Console: requires a real upload key
```
**Test on a device:** enable Developer options + USB debugging on the phone, connect by USB, accept the prompt, then
`flutter devices` -> `flutter run -d <id> --dart-define=API_BASE_URL=http://<mac-lan-ip>:8080`.
Or install an APK: `adb install -r build/app/outputs/flutter-apk/app-debug.apk`.
**Release signing** (not configured): create a keystore (`keytool -genkey -v -keystore ~/paywell-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`),
add `android/key.properties` (git-ignored) and replace the `signingConfig = signingConfigs.getByName("debug")` TODO in
`android/app/build.gradle.kts`. Never commit the keystore or passwords.

## 7. iOS build and test (Xcode)
1. Complete section 2. Simulator: select an iPhone simulator in Xcode and press Run, or `flutter run`.
2. Real iPhone: connect it, trust the Mac, enable Developer Mode (Settings -> Privacy & Security), pick it as the run target;
   a free Apple ID works for 7-day development builds; set your Team under Signing.
3. Archive for distribution: `flutter build ipa --release` (needs a paid Apple Developer account, a unique bundle id and
   provisioning) or Xcode -> Product -> Archive -> Distribute (TestFlight).
4. Launch-animation check: confirm the striped P builds, the PAYWELL letters rise, the green accent and rule appear and a light shine passes (~2.4 s), then it reaches the login/home route; then enable
   Settings -> Accessibility -> Motion -> Reduce Motion and confirm it shows the finished logo briefly instead.

## 8. Suggested first-pass test checklist (none of this has been executed yet)
Sign up -> log in -> home shows wallet -> top-up (simulated) -> send to a handle (check Idempotency by retrying) -> history
shows the transaction -> Money Radar loads -> rewards -> insights -> profile/change password -> log out/in (token refresh) ->
forgot password with the console OTP -> reduced-motion launch -> airplane-mode error states.

## 9. Android Studio one-click run and Android build troubleshooting
* Open the **`mobile`** folder (not `mobile/android`) in Android Studio. A shared run configuration named **PAYWELL**
  (`mobile/.idea/runConfigurations/PAYWELL.xml`, entry point `lib/main.dart`, `API_BASE_URL=http://10.0.2.2:8080`) appears in the
  run drop-down. Pick an emulator/device, press Run. For a phone, edit the configuration's "Additional run args" to your Mac's LAN IP.
  (Written by hand, untested: if Android Studio does not show it, create a Flutter run configuration with the same values.)
* Terminal alternative: `./run_paywell.sh [device-id]`.
* If `flutter build apk` hangs at `Running Gradle task 'assembleDebug'` while installing an NDK, or the Gradle wrapper download times out:
  `cd mobile && bash ../scripts/android_build_doctor.sh --fetch --ndk` and keep the generated `android_doctor.log`.
  The script is diagnostic first; it does not kill processes or delete caches.

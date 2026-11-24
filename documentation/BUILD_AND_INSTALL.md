# Build and install
## Backend / stack
`cp .env.example .env`, fill secrets, then `./scripts/dev-up.sh`. Tests: `cd backend && mvn test` (unit), `mvn verify -Pintegration` (Docker).
Fraud service tests: `./scripts/test-fraud-service.sh`.
## Mobile (Flutter, stable channel)
```
cd mobile
flutter create . --platforms=android,ios     # fills in any missing native scaffolding; keep existing lib/, assets/, test/
flutter pub get
flutter analyze && flutter test
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080     # Android emulator -> host
flutter build apk --debug                                        # debug APK
flutter build appbundle --release                                # needs your own signing config (key.properties, not committed)
```
No APK/AAB is included in this package: none was built (no Flutter SDK was available when this was authored).

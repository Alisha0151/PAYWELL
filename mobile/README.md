# PAYWELL mobile (Flutter)

Status: **source only — written without a Flutter SDK, never compiled or run.** Expect to fix small analyzer errors.

```bash
cd mobile
flutter create . --project-name paywell --org com.paywell --platforms android,ios   # regenerates missing platform files (gradle wrapper, Xcode project)
flutter pub get
flutter analyze && flutter test
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080     # Android emulator -> host backend
```

`flutter create .` will not overwrite existing files you keep (it may ask); the hand-written `android/` and `ios/` files here are
minimal configs (manifest, gradle, Info.plist, icons). The Xcode project (`Runner.xcodeproj`) is NOT included: generate it with `flutter create`.

All payment screens are simulated. No real money moves; PAYWELL is not a bank and holds no banking licence.

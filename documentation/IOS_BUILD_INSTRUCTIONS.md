# iOS
Requires macOS + Xcode + an Apple developer account. No IPA is included; none was built.
```
cd mobile && flutter create . --platforms=ios   # generates Runner.xcodeproj (not hand-written here)
cd ios && pod install
open Runner.xcworkspace    # set Team + bundle id, then Product > Archive
flutter build ipa --release
```
The 1024 px app icon is in `ios/Runner/Assets.xcassets/AppIcon.appiconset`; regenerate other sizes with Xcode or a tool of your choice.

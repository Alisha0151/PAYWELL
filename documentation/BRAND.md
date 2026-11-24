# Brand
Logo: a "P" built from 12 horizontal data-stream bands (one band in a green accent), with a vertical divider, the PAYWELL
wordmark in Poppins Medium (letter-spaced) and the tagline "SIMPLE. FAST. SECURE." Design and animation were supplied by the
owner (`brand/reference/`: original HTML, still, GIF and MP4). Dark scheme: white-to-blue bands on #060C1B, green accent #35E0B0.
Light scheme: navy-to-blue bands, accent #0FB98A.

* SVG (editable, text converted to outlines) and PNG exports: `brand/assets/svg`, `brand/assets/png`. Transparent unless the variant
  includes a background (`logo-on-dark`, `app-icon`). Variants: on-dark, on-dark-transparent, on-light, white mono, royal mono, symbol-only versions, app icon, Android adaptive foreground.
* Regenerate: `python3 brand/gen_brand.py` then `python3 brand/gen_flutter.py` (needs fontTools and the Poppins TTFs).
* Flutter: `mobile/lib/core/brand/paywell_logo.dart` (`PaywellLogo`, `PaywellLaunchLogo`). It is a port of the supplied animation:
  same keyframe fractions and easing, played once over ~2.4 s (the supplied loop is 6.5 s) and skipped to the finished logo when
  reduced motion is on. Fonts: `mobile/assets/fonts` (Poppins Light/Medium, SIL OFL; add the full OFL.txt before distribution).
* Honest note: the Flutter port has never been compiled or run; compare it against `brand/reference/paywell-logo-animation.mp4` on first run.
* The logo's green accent is outside the earlier blue-only palette; the app UI stays blue.

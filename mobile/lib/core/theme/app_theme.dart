import 'package:flutter/material.dart';

class Brand {
  static const royal = Color(0xFF10266F);
  static const navy = Color(0xFF091B4D);
  static const electric = Color(0xFF2457F5);
  static const cyan = Color(0xFF68DFFF);
  static const white = Color(0xFFFFFFFF);
  static const text = Color(0xFF111827);
  static const textMuted = Color(0xFF525B6B);
  static const surfaceTint = Color(0xFFF4F7FF);
  static const danger = Color(0xFFD92D20);
  static const warn = Color(0xFFB54708);
  static const ok = Color(0xFF067647);

  static const heroGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [royal, navy],
  );
}

ThemeData buildTheme() {
  final scheme = ColorScheme.fromSeed(seedColor: Brand.electric, brightness: Brightness.light).copyWith(
    primary: Brand.electric,
    onPrimary: Brand.white,
    secondary: Brand.royal,
    tertiary: Brand.cyan,
    surface: Brand.white,
    onSurface: Brand.text,
    error: Brand.danger,
  );
  final base = ThemeData(useMaterial3: true, colorScheme: scheme, scaffoldBackgroundColor: Brand.white);
  return base.copyWith(
    textTheme: base.textTheme.apply(bodyColor: Brand.text, displayColor: Brand.text),
    appBarTheme: const AppBarTheme(
      backgroundColor: Brand.white,
      foregroundColor: Brand.text,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: false,
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: Brand.white,
      indicatorColor: Brand.electric.withValues(alpha: .12),
      labelTextStyle: WidgetStateProperty.all(const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: Brand.electric,
        foregroundColor: Brand.white,
        minimumSize: const Size.fromHeight(52),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: Brand.royal,
        minimumSize: const Size.fromHeight(52),
        side: const BorderSide(color: Brand.royal, width: 1.2),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: Brand.surfaceTint,
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide.none),
      focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: const BorderSide(color: Brand.electric, width: 1.5)),
      errorBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: const BorderSide(color: Brand.danger)),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
    ),
    cardTheme: CardThemeData(
      color: Brand.white,
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18), side: BorderSide(color: Brand.royal.withValues(alpha: .08))),
    ),
  );
}

import 'package:flutter/material.dart';

// Ocean + sunlight brand palette. Keep status colors separate from the
// primary blue so route selection is never confused with barrier severity.
const _ocean = Color(0xFF176B9B);
const _paper = Color(0xFFF6F8FA);
const _ink = Color(0xFF172B3A);

ThemeData buildAppTheme({Brightness brightness = Brightness.light}) {
  final scheme = ColorScheme.fromSeed(
    seedColor: _ocean,
    brightness: brightness,
    surface: brightness == Brightness.light ? _paper : const Color(0xFF101B26),
  );
  final text = ThemeData(brightness: brightness).textTheme.apply(
        fontFamily: 'Atkinson Hyperlegible',
        bodyColor: brightness == Brightness.light ? _ink : const Color(0xFFE7EFF6),
        displayColor: brightness == Brightness.light ? _ink : const Color(0xFFE7EFF6),
      );

  return ThemeData(
    colorScheme: scheme,
    useMaterial3: true,
    fontFamily: 'Atkinson Hyperlegible',
    textTheme: text,
    scaffoldBackgroundColor: scheme.surface,
    appBarTheme: const AppBarTheme(centerTitle: false, elevation: 0),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: brightness == Brightness.light ? Colors.white : const Color(0xFF172635),
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        minimumSize: const Size.fromHeight(56),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        textStyle: const TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        minimumSize: const Size.fromHeight(56),
        foregroundColor: brightness == Brightness.light ? _ocean : const Color(0xFF8AC8EF),
        side: BorderSide(color: brightness == Brightness.light ? _ocean : const Color(0xFF8AC8EF)),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
    ),
    cardTheme: CardThemeData(
      elevation: 0,
      color: brightness == Brightness.light ? Colors.white : const Color(0xFF172635),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
    ),
  );
}

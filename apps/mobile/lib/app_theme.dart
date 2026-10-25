import 'package:flutter/material.dart';

// Forest palette. Keep status colors separate from the brand green so route
// selection is never confused with barrier severity.
const _mist = Color(0xFFF7FBF4);
const _sage = Color(0xFFAEC3B0);
const _leaf = Color(0xFF6B9071);
const _forest = Color(0xFF375534);
const _deepForest = Color(0xFF0F2A1D);
const _lightSurface = Color(0xFFD8E5CD);
const _darkSurface = Color(0xFF193523);
const _errorLight = Color(0xFF9B2C2C);
const _errorDark = Color(0xFFFFB4AB);

ThemeData buildAppTheme({Brightness brightness = Brightness.light}) {
  final isLight = brightness == Brightness.light;
  final scheme = ColorScheme(
    brightness: brightness,
    primary: isLight ? _forest : _sage,
    onPrimary: isLight ? _mist : _deepForest,
    primaryContainer: isLight ? _sage : _forest,
    onPrimaryContainer: isLight ? _deepForest : _mist,
    secondary: isLight ? _leaf : _sage,
    onSecondary: isLight ? _deepForest : _deepForest,
    secondaryContainer: isLight ? _sage : _forest,
    onSecondaryContainer: isLight ? _deepForest : _mist,
    tertiary: _leaf,
    onTertiary: _mist,
    tertiaryContainer: isLight ? _sage : _forest,
    onTertiaryContainer: isLight ? _deepForest : _mist,
    error: isLight ? _errorLight : _errorDark,
    onError: isLight ? _mist : _deepForest,
    errorContainer: isLight ? const Color(0xFFFFDAD6) : const Color(0xFF93000A),
    onErrorContainer: isLight ? const Color(0xFF410002) : const Color(0xFFFFDAD6),
    surface: isLight ? _mist : _deepForest,
    onSurface: isLight ? _deepForest : _mist,
    surfaceContainerHighest: isLight ? _lightSurface : _darkSurface,
    onSurfaceVariant: isLight ? _forest : _sage,
    outline: isLight ? _forest : _leaf,
    outlineVariant: isLight ? _sage : _forest,
    inverseSurface: isLight ? _deepForest : _mist,
    onInverseSurface: isLight ? _mist : _deepForest,
    inversePrimary: isLight ? _sage : _forest,
    shadow: Colors.black,
    scrim: Colors.black,
  );
  final text = ThemeData(brightness: brightness).textTheme.apply(
        fontFamily: 'Source Serif 4',
        bodyColor: isLight ? _deepForest : _mist,
        displayColor: isLight ? _deepForest : _mist,
      );
  final serif = text.copyWith(
    displayLarge: text.displayLarge?.copyWith(fontFamily: 'Source Serif 4', fontSize: 48, fontWeight: FontWeight.w600, height: 1.1),
    displayMedium: text.displayMedium?.copyWith(fontFamily: 'Source Serif 4', fontSize: 42, fontWeight: FontWeight.w600, height: 1.15),
    displaySmall: text.displaySmall?.copyWith(fontFamily: 'Source Serif 4', fontSize: 40, fontWeight: FontWeight.w600, height: 1.15),
    headlineLarge: text.headlineLarge?.copyWith(fontFamily: 'Source Serif 4', fontSize: 32, fontWeight: FontWeight.w600, height: 1.15),
    headlineMedium: text.headlineMedium?.copyWith(fontFamily: 'Source Serif 4', fontSize: 28, fontWeight: FontWeight.w600, height: 1.2),
    headlineSmall: text.headlineSmall?.copyWith(fontFamily: 'Source Serif 4', fontSize: 26, fontWeight: FontWeight.w600, height: 1.2),
    titleLarge: text.titleLarge?.copyWith(fontFamily: 'Source Serif 4', fontSize: 24, fontWeight: FontWeight.w600, height: 1.25),
    titleMedium: text.titleMedium?.copyWith(fontFamily: 'Source Serif 4', fontSize: 22, fontWeight: FontWeight.w600, height: 1.3),
    titleSmall: text.titleSmall?.copyWith(fontFamily: 'Source Serif 4', fontSize: 20, fontWeight: FontWeight.w600, height: 1.3),
    bodyLarge: text.bodyLarge?.copyWith(fontFamily: 'Source Serif 4', fontSize: 20, fontWeight: FontWeight.w400, height: 1.5),
    bodyMedium: text.bodyMedium?.copyWith(fontFamily: 'Source Serif 4', fontSize: 18, fontWeight: FontWeight.w400, height: 1.45),
    bodySmall: text.bodySmall?.copyWith(fontFamily: 'Source Serif 4', fontSize: 16, fontWeight: FontWeight.w400, height: 1.4),
    labelLarge: text.labelLarge?.copyWith(fontFamily: 'Source Serif 4', fontSize: 18, fontWeight: FontWeight.w800),
    labelMedium: text.labelMedium?.copyWith(fontFamily: 'Source Serif 4', fontSize: 17, fontWeight: FontWeight.w800),
    labelSmall: text.labelSmall?.copyWith(fontFamily: 'Source Serif 4', fontSize: 16, fontWeight: FontWeight.w800),
  );

  return ThemeData(
    colorScheme: scheme,
    useMaterial3: true,
    fontFamily: 'Source Serif 4',
    textTheme: serif,
    scaffoldBackgroundColor: scheme.surface,
    appBarTheme: AppBarTheme(centerTitle: false, elevation: 0, backgroundColor: scheme.surface, foregroundColor: scheme.onSurface, titleTextStyle: serif.titleLarge),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: isLight ? _lightSurface : _darkSurface,
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide(color: scheme.outline)),
      enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide(color: scheme.outline)),
      focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide(color: scheme.primary, width: 2)),
      labelStyle: serif.bodyLarge?.copyWith(color: scheme.onSurfaceVariant),
      floatingLabelStyle: serif.bodyLarge?.copyWith(color: scheme.primary),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        minimumSize: const Size.fromHeight(56),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        backgroundColor: scheme.primary,
        foregroundColor: scheme.onPrimary,
      textStyle: serif.labelLarge,
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        minimumSize: const Size.fromHeight(56),
        foregroundColor: scheme.primary,
        side: BorderSide(color: scheme.outline),
        textStyle: serif.labelLarge,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
    ),
    textButtonTheme: TextButtonThemeData(style: TextButton.styleFrom(textStyle: serif.labelLarge)),
    cardTheme: CardThemeData(
      elevation: 0,
      color: isLight ? _lightSurface : _darkSurface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
    ),
  );
}

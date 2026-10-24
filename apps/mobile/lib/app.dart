import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'app_theme.dart';
import 'features/home/home_screen.dart';
import 'features/onboarding/onboarding_screen.dart';
import 'features/onboarding/onboarding_store.dart';

class MaoNaRodaApp extends StatelessWidget {
  const MaoNaRodaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Mão na Roda',
      debugShowCheckedModeBanner: false,
       theme: buildAppTheme(),
       darkTheme: buildAppTheme(brightness: Brightness.dark),
       themeMode: ThemeMode.system,
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ],
      supportedLocales: const [Locale('pt', 'BR')],
       home: const _StartupGate(),
    );
  }
}

class _StartupGate extends StatefulWidget {
  const _StartupGate();
  @override
  State<_StartupGate> createState() => _StartupGateState();
}

class _StartupGateState extends State<_StartupGate> {
  late final Future<bool> _completed = OnboardingStore()
      .isCompleted()
      .timeout(const Duration(seconds: 2), onTimeout: () => false);

  @override
  Widget build(BuildContext context) => FutureBuilder<bool>(
        future: _completed,
        builder: (context, snapshot) {
          if (!snapshot.hasData) {
            return const Scaffold(body: Center(child: CircularProgressIndicator()));
          }
          return snapshot.data! ? const HomeScreen() : OnboardingScreen(onComplete: _showHome);
        },
      );

  void _showHome() => Navigator.of(context).pushReplacement(MaterialPageRoute<void>(builder: (_) => const HomeScreen()));
}

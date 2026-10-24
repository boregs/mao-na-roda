import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mao_na_roda/features/onboarding/onboarding_screen.dart';

void main() {
  testWidgets('apresenta a proposta e ações principais', (tester) async {
    await tester.pumpWidget(MaterialApp(home: OnboardingScreen(onComplete: _noop)));
    expect(find.text('Seu caminho,\nsuas condições.'), findsOneWidget);
    expect(find.text('Continuar'), findsOneWidget);
    expect(find.text('Como funciona'), findsOneWidget);
  });

  testWidgets('abre a explicação do produto', (tester) async {
    await tester.pumpWidget(MaterialApp(home: OnboardingScreen(onComplete: _noop)));
    await tester.tap(find.text('Como funciona'));
    await tester.pumpAndSettle();
    expect(find.text('Como funciona'), findsOneWidget);
    expect(find.text('Seu perfil orienta a busca'), findsOneWidget);
  });
}

void _noop() {}

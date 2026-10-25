import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mao_na_roda/features/auth/auth_screen.dart';

void main() {
  testWidgets('cadastro apresenta os quatro campos obrigatórios', (tester) async {
    await tester.pumpWidget(const MaterialApp(home: AuthScreen(mode: AuthMode.createAccount)));

    expect(find.byType(TextFormField), findsNWidgets(4));
    expect(find.text('Nome'), findsOneWidget);
    expect(find.text('Sobrenome'), findsOneWidget);
    expect(find.text('E-mail'), findsOneWidget);
    expect(find.text('Senha'), findsOneWidget);
  });

  testWidgets('valida campos vazios antes do envio', (tester) async {
    await tester.pumpWidget(const MaterialApp(home: AuthScreen(mode: AuthMode.signIn)));
    await tester.tap(find.text('Entrar'));
    await tester.pump();

    expect(find.text('Informe seu e-mail.'), findsOneWidget);
    expect(find.text('Informe sua senha.'), findsOneWidget);
  });

  testWidgets('permite mostrar e ocultar a senha', (tester) async {
    await tester.pumpWidget(const MaterialApp(home: AuthScreen(mode: AuthMode.signIn)));
    final field = tester.widget<TextFormField>(find.byType(TextFormField).last);
    expect(field.obscureText, isTrue);
    await tester.tap(find.byTooltip('Mostrar senha'));
    await tester.pump();
    expect(tester.widget<TextFormField>(find.byType(TextFormField).last).obscureText, isFalse);
  });
}

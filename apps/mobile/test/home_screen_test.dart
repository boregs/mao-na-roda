import 'package:flutter_test/flutter_test.dart';
import 'package:mao_na_roda/app.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  testWidgets('exibe a mensagem inicial do aplicativo', (tester) async {
    SharedPreferences.setMockInitialValues({'onboarding_completed': true});
    await tester.pumpWidget(const MaoNaRodaApp());
    await tester.pumpAndSettle();

    expect(find.text('Mão na Roda'), findsOneWidget);
    expect(find.text('Rotas mais acessíveis para você'), findsOneWidget);
    expect(find.text('Consultar rota'), findsOneWidget);
  });
}

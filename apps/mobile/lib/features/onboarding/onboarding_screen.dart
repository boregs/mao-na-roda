import 'package:flutter/material.dart';

import '../../design_system/brand_mark.dart';
import 'how_it_works_sheet.dart';
import 'onboarding_store.dart';
import '../auth/auth_screen.dart';

class OnboardingScreen extends StatelessWidget {
  const OnboardingScreen({super.key, required this.onComplete});

  final VoidCallback onComplete;

  Future<void> _continue() async {
    await OnboardingStore().complete();
    onComplete();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(24, 32, 24, 24),
              sliver: SliverFillRemaining(
                hasScrollBody: false,
                child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(children: [const BrandMark(), const SizedBox(width: 10), Text('Mão na Roda', style: theme.textTheme.titleLarge)]),
                const SizedBox(height: 64),
                Text(
                  'Seu caminho,\nsuas condições.',
                  style: theme.textTheme.displaySmall,
                ),
                const SizedBox(height: 16),
                Text('Rotas considerando suas necessidades de mobilidade e as condições do caminho.', style: theme.textTheme.bodyLarge?.copyWith(color: theme.colorScheme.onSurfaceVariant, height: 1.4)),
                const Spacer(),
                const SizedBox(height: 32),
                ElevatedButton(onPressed: _continue, child: const Text('Continuar sem conta')),
                const SizedBox(height: 12),
                OutlinedButton(
                  onPressed: () => Navigator.of(context).push<void>(
                    MaterialPageRoute<void>(builder: (_) => AuthScreen(mode: AuthMode.createAccount, onUseAsGuest: _continue)),
                  ),
                  child: const Text('Criar conta'),
                ),
                const SizedBox(height: 4),
                TextButton(
                  onPressed: () => Navigator.of(context).push<void>(
                    MaterialPageRoute<void>(builder: (_) => AuthScreen(mode: AuthMode.signIn, onUseAsGuest: _continue)),
                  ),
                  child: const Text('Já tenho conta · Entrar'),
                ),
                const SizedBox(height: 8),
                TextButton(onPressed: () => showHowItWorks(context), child: const Text('Como funciona')),
                const SizedBox(height: 24),
                Text('Versão inicial em desenvolvimento', textAlign: TextAlign.center, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
              ],
            ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

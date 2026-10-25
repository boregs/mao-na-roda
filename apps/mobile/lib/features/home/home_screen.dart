import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';

import '../onboarding/how_it_works_sheet.dart';
import '../onboarding/onboarding_screen.dart';
import '../onboarding/onboarding_store.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Mão na Roda')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            Semantics(
              header: true,
              child: Text(
                'Rotas mais acessíveis para você',
                style: Theme.of(context).textTheme.headlineMedium,
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'Encontre caminhos considerando as condições de acessibilidade e as barreiras relatadas por outras pessoas.',
            ),
            const SizedBox(height: 32),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                   crossAxisAlignment: CrossAxisAlignment.start,
                   children: [
                     Text(
                       'Comece sua busca',
                       style: Theme.of(context).textTheme.titleLarge,
                     ),
                     const SizedBox(height: 12),
                    const Text(
                      'A consulta de rotas estará disponível nesta etapa do desenvolvimento.',
                    ),
                    const SizedBox(height: 20),
                    ElevatedButton.icon(
                      onPressed: null,
                      icon: const Icon(Icons.map_outlined),
                      label: const Text('Consultar rota'),
                    ),
                  ],
                ),
              ),
             ),
             const SizedBox(height: 12),
             TextButton.icon(
               onPressed: () => showHowItWorks(context),
               icon: const Icon(Icons.info_outline_rounded),
               label: const Text('Sobre o Mão na Roda'),
             ),
             if (kDebugMode)
               TextButton.icon(
                 onPressed: () async {
                   await OnboardingStore().reset();
                   if (!context.mounted) return;
                   Navigator.of(context).pushReplacement(
                     MaterialPageRoute<void>(
                       builder: (_) => OnboardingScreen(onComplete: () {
                         Navigator.of(context).pushReplacement(
                           MaterialPageRoute<void>(builder: (_) => const HomeScreen()),
                         );
                       }),
                     ),
                   );
                 },
                 icon: const Icon(Icons.restart_alt_rounded),
                 label: const Text('Resetar onboarding (debug)'),
               ),
             const SizedBox(height: 24),
            Text(
              'Versão inicial em desenvolvimento',
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ),
      ),
    );
  }
}

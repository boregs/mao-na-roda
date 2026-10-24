import 'package:flutter/material.dart';

Future<void> showHowItWorks(BuildContext context) => showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (_) => const _HowItWorksSheet(),
    );

class _HowItWorksSheet extends StatelessWidget {
  const _HowItWorksSheet();
  @override
  Widget build(BuildContext context) => SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 8, 24, 24),
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            Text('Como funciona', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w700)),
            const SizedBox(height: 24),
            const _Info(icon: Icons.accessible_forward_rounded, title: 'Seu perfil orienta a busca', text: 'As necessidades de mobilidade ajudam a identificar as barreiras relevantes.'),
            const _Info(icon: Icons.route_rounded, title: 'Conheça as condições do caminho', text: 'Confira barreiras, fontes e datas das informações disponíveis.'),
            const _Info(icon: Icons.people_alt_outlined, title: 'Entenda os relatos', text: 'Relatos de outras pessoas aparecem como não validados.'),
            const SizedBox(height: 16),
            OutlinedButton(onPressed: () => Navigator.pop(context), child: const Text('Fechar')),
          ]),
        ),
      );
}

class _Info extends StatelessWidget {
  const _Info({required this.icon, required this.title, required this.text});
  final IconData icon; final String title; final String text;
  @override
  Widget build(BuildContext context) => Padding(padding: const EdgeInsets.only(bottom: 20), child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [Icon(icon, size: 28, color: Theme.of(context).colorScheme.primary), const SizedBox(width: 16), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(title, style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700)), const SizedBox(height: 4), Text(text, style: Theme.of(context).textTheme.bodyLarge)]))]));
}

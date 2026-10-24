import 'package:flutter/material.dart';

import '../../design_system/brand_mark.dart';
import 'how_it_works_sheet.dart';
import 'onboarding_store.dart';

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
                const Row(children: [BrandMark(), SizedBox(width: 10), Text('Mão na Roda')]),
                const SizedBox(height: 40),
                Semantics(
                  label: 'Ilustração de uma rota acessível',
                  child: _PathIllustration(color: theme.colorScheme.primary),
                ),
                const SizedBox(height: 36),
                Text('Seu caminho,\nsuas condições.', style: theme.textTheme.displaySmall?.copyWith(fontWeight: FontWeight.w700)),
                const SizedBox(height: 16),
                Text('Rotas considerando suas necessidades de mobilidade e as condições do caminho.', style: theme.textTheme.bodyLarge?.copyWith(color: theme.colorScheme.onSurfaceVariant, height: 1.4)),
                const Spacer(),
                const SizedBox(height: 32),
                ElevatedButton(onPressed: _continue, child: const Text('Continuar')),
                const SizedBox(height: 12),
                OutlinedButton(onPressed: () => showHowItWorks(context), child: const Text('Como funciona')),
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

class _PathIllustration extends StatelessWidget {
  const _PathIllustration({required this.color});
  final Color color;
  @override
  Widget build(BuildContext context) => Container(
        height: 220,
        decoration: BoxDecoration(color: color.withOpacity(.1), borderRadius: BorderRadius.circular(28)),
        child: CustomPaint(painter: _PathPainter(color)),
      );
}

class _PathPainter extends CustomPainter {
  const _PathPainter(this.color);
  final Color color;
  @override
  void paint(Canvas canvas, Size size) {
    final path = Path()..moveTo(35, size.height - 45)..cubicTo(90, 80, 125, 180, 185, 70)..cubicTo(225, 0, 275, 155, size.width - 35, 42);
    final paint = Paint()..color = color..style = PaintingStyle.stroke..strokeWidth = 12..strokeCap = StrokeCap.round;
    canvas.drawPath(path, paint);
    final dot = Paint()..color = const Color(0xFFF2C75C);
    canvas.drawCircle(const Offset(35, 175), 13, dot);
    canvas.drawCircle(Offset(size.width - 35, 42), 13, dot);
  }
  @override
  bool shouldRepaint(covariant _PathPainter oldDelegate) => oldDelegate.color != color;
}

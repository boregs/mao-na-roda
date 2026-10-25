import 'package:flutter/material.dart';

enum AuthMode { signIn, createAccount }

class AuthScreen extends StatelessWidget {
  const AuthScreen({super.key, required this.mode, this.onUseAsGuest});

  final AuthMode mode;
  final Future<void> Function()? onUseAsGuest;

  bool get isSignIn => mode == AuthMode.signIn;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: Text(isSignIn ? 'Entrar' : 'Criar conta')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(24, 24, 24, 32),
          children: [
            Text(isSignIn ? 'Que bom ter você de volta.' : 'Faça parte de uma mobilidade mais acessível.', style: theme.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w700)),
            const SizedBox(height: 12),
            Text(isSignIn ? 'Entre para acessar sua conta.' : 'Uma conta permite enviar relatos e guardar suas preferências.', style: theme.textTheme.bodyLarge?.copyWith(color: theme.colorScheme.onSurfaceVariant, height: 1.4)),
            const SizedBox(height: 32),
            if (!isSignIn) ...[
              const TextField(decoration: InputDecoration(labelText: 'Nome')),
              const SizedBox(height: 16),
              const TextField(decoration: InputDecoration(labelText: 'Sobrenome')),
              const SizedBox(height: 16),
            ],
            const TextField(keyboardType: TextInputType.emailAddress, decoration: InputDecoration(labelText: 'E-mail')),
            const SizedBox(height: 16),
            const TextField(obscureText: true, decoration: InputDecoration(labelText: 'Senha')),
            if (isSignIn) Align(alignment: Alignment.centerRight, child: TextButton(onPressed: () {}, child: const Text('Esqueci minha senha'))),
            const SizedBox(height: 16),
            ElevatedButton(onPressed: () {}, child: Text(isSignIn ? 'Entrar' : 'Criar conta')),
            const SizedBox(height: 16),
            OutlinedButton(
              onPressed: onUseAsGuest == null ? () => Navigator.pop(context) : () { onUseAsGuest!(); },
              child: const Text('Continuar sem conta'),
            ),
            const SizedBox(height: 12),
            TextButton(
              onPressed: () => Navigator.pushReplacement<void, void>(context, MaterialPageRoute<void>(builder: (_) => AuthScreen(mode: isSignIn ? AuthMode.createAccount : AuthMode.signIn, onUseAsGuest: onUseAsGuest))),
              child: Text(isSignIn ? 'Criar uma conta' : 'Já tenho conta · Entrar'),
            ),
            const SizedBox(height: 20),
            Text('A autenticação será conectada ao serviço de contas do app.', textAlign: TextAlign.center, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
          ],
        ),
      ),
    );
  }
}

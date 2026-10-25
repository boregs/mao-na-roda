import 'package:flutter/material.dart';

enum AuthMode { signIn, createAccount }

class AuthScreen extends StatefulWidget {
  const AuthScreen({super.key, required this.mode, this.onUseAsGuest});

  final AuthMode mode;
  final Future<void> Function()? onUseAsGuest;

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _surnameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _passwordVisible = false;
  bool _isSubmitting = false;
  String? _submitError;

  bool get isSignIn => widget.mode == AuthMode.signIn;

  @override
  void dispose() {
    _nameController.dispose();
    _surnameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  String? _required(String? value, String label) {
    if (value == null || value.trim().isEmpty) return 'Informe seu $label.';
    return null;
  }

  String? _email(String? value) {
    if (value == null || value.trim().isEmpty) return 'Informe seu e-mail.';
    if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(value.trim())) {
      return 'Informe um e-mail válido.';
    }
    return null;
  }

  String? _password(String? value) {
    if (value == null || value.isEmpty) return 'Informe sua senha.';
    if (value.length < 8) return 'A senha deve ter pelo menos 8 caracteres.';
    return null;
  }

  Future<void> _submit() async {
    FocusScope.of(context).unfocus();
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _isSubmitting = true;
      _submitError = null;
    });

    // The Auth/API contract will be connected here. Keep the form state and
    // feedback ready without pretending that account creation is complete.
    await Future<void>.delayed(const Duration(milliseconds: 350));
    if (!mounted) return;
    setState(() {
      _isSubmitting = false;
      _submitError = 'A autenticação ainda não está conectada ao serviço.';
    });
  }

  void _switchMode() {
    Navigator.pushReplacement<void, void>(
      context,
      MaterialPageRoute<void>(
        builder: (_) => AuthScreen(
          mode: isSignIn ? AuthMode.createAccount : AuthMode.signIn,
          onUseAsGuest: widget.onUseAsGuest,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: Text(isSignIn ? 'Entrar' : 'Criar conta')),
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(24, 24, 24, 32),
            children: [
              Semantics(
                header: true,
                child: Text(
                  isSignIn ? 'Que bom ter você de volta.' : 'Faça parte de uma mobilidade mais acessível.',
                  style: theme.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w700),
                ),
              ),
              const SizedBox(height: 12),
              Text(
                isSignIn ? 'Entre para acessar sua conta.' : 'Uma conta permite enviar relatos e guardar suas preferências.',
                style: theme.textTheme.bodyLarge?.copyWith(color: theme.colorScheme.onSurfaceVariant, height: 1.4),
              ),
              const SizedBox(height: 32),
              if (!isSignIn) ...[
                TextFormField(controller: _nameController, textInputAction: TextInputAction.next, decoration: const InputDecoration(labelText: 'Nome'), validator: (value) => _required(value, 'nome')),
                const SizedBox(height: 16),
                TextFormField(controller: _surnameController, textInputAction: TextInputAction.next, decoration: const InputDecoration(labelText: 'Sobrenome'), validator: (value) => _required(value, 'sobrenome')),
                const SizedBox(height: 16),
              ],
              TextFormField(controller: _emailController, keyboardType: TextInputType.emailAddress, textInputAction: TextInputAction.next, autofillHints: const [AutofillHints.email], decoration: const InputDecoration(labelText: 'E-mail'), validator: _email),
              const SizedBox(height: 16),
              TextFormField(
                controller: _passwordController,
                obscureText: !_passwordVisible,
                textInputAction: TextInputAction.done,
                autofillHints: const [AutofillHints.password],
                onFieldSubmitted: (_) => _submit(),
                decoration: InputDecoration(
                  labelText: 'Senha',
                  suffixIcon: IconButton(
                    tooltip: _passwordVisible ? 'Ocultar senha' : 'Mostrar senha',
                    onPressed: () => setState(() => _passwordVisible = !_passwordVisible),
                    icon: Icon(_passwordVisible ? Icons.visibility_off_outlined : Icons.visibility_outlined),
                  ),
                ),
                validator: _password,
              ),
              if (isSignIn) Align(alignment: Alignment.centerRight, child: TextButton(onPressed: _isSubmitting ? null : () {}, child: const Text('Esqueci minha senha'))),
              if (_submitError != null) ...[
                const SizedBox(height: 8),
                Text(_submitError!, style: TextStyle(color: theme.colorScheme.error)),
              ],
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: _isSubmitting ? null : _submit,
                child: _isSubmitting ? const SizedBox(height: 24, width: 24, child: CircularProgressIndicator(strokeWidth: 2)) : Text(isSignIn ? 'Entrar' : 'Criar conta'),
              ),
              const SizedBox(height: 16),
              OutlinedButton(onPressed: _isSubmitting ? null : (widget.onUseAsGuest ?? () async => Navigator.pop(context)), child: const Text('Continuar sem conta')),
              const SizedBox(height: 12),
              TextButton(onPressed: _isSubmitting ? null : _switchMode, child: Text(isSignIn ? 'Criar uma conta' : 'Já tenho conta · Entrar')),
              const SizedBox(height: 20),
              Text('A autenticação usa um serviço seguro de contas do app.', textAlign: TextAlign.center, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
            ],
          ),
        ),
      ),
    );
  }
}

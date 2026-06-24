import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../shared/utils/validators.dart';
import '../../../shared/widgets/feedback.dart';
import '../data/auth_repository.dart';

class ForgotPasswordScreen extends ConsumerStatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  ConsumerState<ForgotPasswordScreen> createState() =>
      _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends ConsumerState<ForgotPasswordScreen> {
  int _step = 0; // 0 e-mail, 1 código, 2 nova senha
  bool _loading = false;

  final _email = TextEditingController();
  final _codigo = TextEditingController();
  final _novaSenha = TextEditingController();

  @override
  void dispose() {
    _email.dispose();
    _codigo.dispose();
    _novaSenha.dispose();
    super.dispose();
  }

  AuthRepository get _repo => ref.read(authRepositoryProvider);

  Future<void> _run(Future<void> Function() action) async {
    setState(() => _loading = true);
    try {
      await action();
    } on ApiException catch (e) {
      if (mounted) showErrorSnack(context, e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _solicitar() => _run(() async {
        if (Validators.email(_email.text) != null) {
          showErrorSnack(context, 'Informe um e-mail válido.');
          return;
        }
        final debugCode = await _repo.solicitarResetSenha(_email.text);
        if (!mounted) return;
        if (debugCode != null && debugCode.isNotEmpty) {
          _codigo.text = debugCode; // ambiente expõe o código
        }
        showSuccessSnack(context, 'Código enviado para o seu e-mail.');
        setState(() => _step = 1);
      });

  Future<void> _verificar() => _run(() async {
        if (Validators.codigo6(_codigo.text) != null) {
          showErrorSnack(context, 'O código tem 6 dígitos.');
          return;
        }
        await _repo.verificarCodigoReset(_email.text, _codigo.text.trim());
        if (mounted) setState(() => _step = 2);
      });

  Future<void> _redefinir() => _run(() async {
        if (Validators.senha(_novaSenha.text) != null) {
          showErrorSnack(context, 'A senha deve ter ao menos 8 caracteres.');
          return;
        }
        await _repo.redefinirSenha(
            _email.text, _codigo.text.trim(), _novaSenha.text);
        if (!mounted) return;
        showSuccessSnack(context, 'Senha redefinida! Faça login.');
        context.pop();
      });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Recuperar senha')),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    switch (_step) {
                      0 => 'Informe seu e-mail para receber um código.',
                      1 => 'Digite o código de 6 dígitos enviado.',
                      _ => 'Defina sua nova senha.',
                    },
                    style: Theme.of(context).textTheme.bodyLarge,
                  ),
                  const SizedBox(height: 24),
                  if (_step == 0)
                    TextField(
                      controller: _email,
                      keyboardType: TextInputType.emailAddress,
                      decoration: const InputDecoration(
                        labelText: 'E-mail',
                        prefixIcon: Icon(Icons.email_outlined),
                      ),
                    ),
                  if (_step == 1)
                    TextField(
                      controller: _codigo,
                      keyboardType: TextInputType.number,
                      maxLength: 6,
                      decoration: const InputDecoration(
                        labelText: 'Código',
                        prefixIcon: Icon(Icons.pin_outlined),
                      ),
                    ),
                  if (_step == 2)
                    TextField(
                      controller: _novaSenha,
                      obscureText: true,
                      decoration: const InputDecoration(
                        labelText: 'Nova senha',
                        prefixIcon: Icon(Icons.lock_outline),
                      ),
                    ),
                  const SizedBox(height: 16),
                  FilledButton(
                    onPressed: _loading
                        ? null
                        : switch (_step) {
                            0 => _solicitar,
                            1 => _verificar,
                            _ => _redefinir,
                          },
                    child: _loading
                        ? const SizedBox(
                            height: 22,
                            width: 22,
                            child: CircularProgressIndicator(strokeWidth: 2.5),
                          )
                        : Text(switch (_step) {
                            0 => 'Enviar código',
                            1 => 'Verificar',
                            _ => 'Redefinir senha',
                          }),
                  ),
                  if (_step > 0)
                    TextButton(
                      onPressed:
                          _loading ? null : () => setState(() => _step -= 1),
                      child: const Text('Voltar'),
                    ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

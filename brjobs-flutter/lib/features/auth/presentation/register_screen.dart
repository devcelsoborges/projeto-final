import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../shared/utils/formatters.dart';
import '../../../shared/utils/validators.dart';
import '../../../shared/widgets/feedback.dart';
import '../application/auth_controller.dart';
import '../domain/cadastro.dart';
import '../domain/tipo_usuario.dart';

class RegisterScreen extends ConsumerStatefulWidget {
  const RegisterScreen({super.key});

  @override
  ConsumerState<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends ConsumerState<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  TipoUsuario _tipo = TipoUsuario.contratante;
  bool _loading = false;
  bool _obscure = true;
  DateTime? _dataNascimento;
  String? _genero;

  final _nome = TextEditingController();
  final _email = TextEditingController();
  final _senha = TextEditingController();
  final _confirma = TextEditingController();
  final _telefone = TextEditingController();
  final _cpf = TextEditingController();
  final _cep = TextEditingController();
  final _endereco = TextEditingController();
  final _cidade = TextEditingController();
  final _estado = TextEditingController();
  final _bio = TextEditingController();
  // Prestador
  final _funcao = TextEditingController();
  final _especialidades = TextEditingController();
  final _experiencia = TextEditingController();

  @override
  void dispose() {
    for (final c in [
      _nome, _email, _senha, _confirma, _telefone, _cpf, _cep,
      _endereco, _cidade, _estado, _bio, _funcao, _especialidades, _experiencia,
    ]) {
      c.dispose();
    }
    super.dispose();
  }

  CadastroContratante _baseDto() => CadastroContratante(
        nome: _nome.text.trim(),
        email: _email.text.trim(),
        senha: _senha.text,
        confirmacaoSenha: _confirma.text,
        telefone: _empty(_telefone.text),
        cpf: _empty(_cpf.text),
        dataNascimento: _dataNascimento,
        genero: _genero,
        cep: _empty(_cep.text),
        endereco: _empty(_endereco.text),
        cidade: _empty(_cidade.text),
        estado: _empty(_estado.text),
        bio: _empty(_bio.text),
      );

  String? _empty(String v) => v.trim().isEmpty ? null : v.trim();

  Future<void> _cadastrar() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _loading = true);
    try {
      final notifier = ref.read(authControllerProvider.notifier);
      if (_tipo == TipoUsuario.prestador) {
        await notifier.registrarPrestador(CadastroPrestador(
          base: _baseDto(),
          funcao: _empty(_funcao.text),
          especialidades: _empty(_especialidades.text),
          experienciaProfissional: _empty(_experiencia.text),
        ));
      } else {
        await notifier.registrarContratante(_baseDto());
      }
      // Router redireciona ao autenticar.
    } on ApiException catch (e) {
      if (!mounted) return;
      // 409 = e-mail/CPF em uso; 400 = validação por campo.
      showErrorSnack(context, e.firstFieldError ?? e.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _pickData() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _dataNascimento ?? DateTime(now.year - 25),
      firstDate: DateTime(1920),
      lastDate: now,
      helpText: 'Data de nascimento',
    );
    if (picked != null) setState(() => _dataNascimento = picked);
  }

  @override
  Widget build(BuildContext context) {
    final isPrestador = _tipo == TipoUsuario.prestador;
    return Scaffold(
      appBar: AppBar(title: const Text('Criar conta')),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 480),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    SegmentedButton<TipoUsuario>(
                      segments: const [
                        ButtonSegment(
                          value: TipoUsuario.contratante,
                          label: Text('Contratante'),
                          icon: Icon(Icons.person_search),
                        ),
                        ButtonSegment(
                          value: TipoUsuario.prestador,
                          label: Text('Prestador'),
                          icon: Icon(Icons.handyman),
                        ),
                      ],
                      selected: {_tipo},
                      onSelectionChanged: (s) =>
                          setState(() => _tipo = s.first),
                    ),
                    const SizedBox(height: 20),
                    _field(_nome, 'Nome completo *',
                        icon: Icons.badge_outlined, validator: Validators.nome),
                    _field(_email, 'E-mail *',
                        icon: Icons.email_outlined,
                        keyboard: TextInputType.emailAddress,
                        validator: Validators.email),
                    _field(_senha, 'Senha *',
                        icon: Icons.lock_outline,
                        obscure: _obscure,
                        suffix: IconButton(
                          icon: Icon(_obscure
                              ? Icons.visibility_outlined
                              : Icons.visibility_off_outlined),
                          onPressed: () => setState(() => _obscure = !_obscure),
                        ),
                        validator: (v) => Validators.senha(v)),
                    _field(_confirma, 'Confirmar senha *',
                        icon: Icons.lock_outline,
                        obscure: _obscure,
                        validator: (v) =>
                            Validators.confirmacaoSenha(v, _senha.text)),
                    _field(_telefone, 'Telefone',
                        icon: Icons.phone_outlined,
                        keyboard: TextInputType.phone),
                    _field(_cpf, 'CPF',
                        icon: Icons.assignment_ind_outlined,
                        keyboard: TextInputType.number),
                    ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: const Icon(Icons.cake_outlined),
                      title: Text(_dataNascimento == null
                          ? 'Data de nascimento'
                          : Fmt.data(_dataNascimento)),
                      trailing: const Icon(Icons.calendar_today, size: 18),
                      onTap: _pickData,
                    ),
                    DropdownButtonFormField<String>(
                      initialValue: _genero,
                      decoration: const InputDecoration(
                        labelText: 'Gênero',
                        prefixIcon: Icon(Icons.wc_outlined),
                      ),
                      items: const [
                        DropdownMenuItem(
                            value: 'Masculino', child: Text('Masculino')),
                        DropdownMenuItem(
                            value: 'Feminino', child: Text('Feminino')),
                        DropdownMenuItem(value: 'Outro', child: Text('Outro')),
                      ],
                      onChanged: (v) => setState(() => _genero = v),
                    ),
                    const SizedBox(height: 16),
                    _field(_cep, 'CEP',
                        icon: Icons.location_on_outlined,
                        keyboard: TextInputType.number),
                    _field(_endereco, 'Endereço',
                        icon: Icons.home_outlined,
                        validator: (v) => (v == null || v.trim().isEmpty)
                            ? null
                            : (v.trim().length < 5
                                ? 'Endereço muito curto.'
                                : null)),
                    Row(
                      children: [
                        Expanded(
                          child: _field(_cidade, 'Cidade',
                              icon: Icons.location_city_outlined),
                        ),
                        const SizedBox(width: 12),
                        SizedBox(
                          width: 90,
                          child: _field(_estado, 'UF'),
                        ),
                      ],
                    ),
                    _field(_bio, 'Bio',
                        icon: Icons.notes_outlined,
                        maxLines: 3,
                        validator: (v) => Validators.tamanhoMaximo(v, 600,
                            campo: 'A bio')),
                    if (isPrestador) ...[
                      const SizedBox(height: 8),
                      Align(
                        alignment: Alignment.centerLeft,
                        child: Text('Dados profissionais',
                            style: Theme.of(context).textTheme.titleSmall),
                      ),
                      const SizedBox(height: 8),
                      _field(_funcao, 'Função / profissão',
                          icon: Icons.work_outline),
                      _field(_especialidades, 'Especialidades',
                          icon: Icons.star_outline, maxLines: 2),
                      _field(_experiencia, 'Experiência profissional',
                          icon: Icons.history_edu_outlined, maxLines: 3),
                    ],
                    const SizedBox(height: 24),
                    FilledButton(
                      onPressed: _loading ? null : _cadastrar,
                      child: _loading
                          ? const SizedBox(
                              height: 22,
                              width: 22,
                              child: CircularProgressIndicator(strokeWidth: 2.5),
                            )
                          : const Text('Criar conta'),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Text('Já tem conta?'),
                        TextButton(
                          onPressed: _loading ? null : () => context.pop(),
                          child: const Text('Entrar'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _field(
    TextEditingController controller,
    String label, {
    IconData? icon,
    bool obscure = false,
    int maxLines = 1,
    TextInputType? keyboard,
    Widget? suffix,
    String? Function(String?)? validator,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: TextFormField(
        controller: controller,
        obscureText: obscure,
        maxLines: obscure ? 1 : maxLines,
        keyboardType: keyboard,
        decoration: InputDecoration(
          labelText: label,
          prefixIcon: icon == null ? null : Icon(icon),
          suffixIcon: suffix,
        ),
        validator: validator,
      ),
    );
  }
}

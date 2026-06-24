import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/router/app_router.dart';
import '../../../shared/widgets/feedback.dart';
import '../../auth/application/auth_controller.dart';
import '../../auth/domain/usuario.dart';
import '../../avaliacoes/presentation/avaliacoes_section.dart';
import '../data/usuario_repository.dart';
import '../domain/prestador.dart';

class MeuPerfilScreen extends ConsumerStatefulWidget {
  const MeuPerfilScreen({super.key});

  @override
  ConsumerState<MeuPerfilScreen> createState() => _MeuPerfilScreenState();
}

class _MeuPerfilScreenState extends ConsumerState<MeuPerfilScreen> {
  final _formKey = GlobalKey<FormState>();

  final _nome = TextEditingController();
  final _telefone = TextEditingController();
  final _endereco = TextEditingController();
  final _cep = TextEditingController();
  final _cidade = TextEditingController();
  final _estado = TextEditingController();
  final _bio = TextEditingController();

  bool _salvando = false;
  bool _enviandoFoto = false;
  bool _carregouCampos = false;
  PrestadorResponse? _prestador;

  @override
  void dispose() {
    for (final c in [
      _nome,
      _telefone,
      _endereco,
      _cep,
      _cidade,
      _estado,
      _bio,
    ]) {
      c.dispose();
    }
    super.dispose();
  }

  void _preencher(Usuario user) {
    _nome.text = user.nome;
    _telefone.text = user.telefone ?? '';
    _endereco.text = user.endereco ?? '';
    _cep.text = user.cep ?? '';
    _cidade.text = user.cidade ?? '';
    _estado.text = user.estado ?? '';
    _bio.text = user.bio ?? '';
    _carregouCampos = true;
    if (user.isPrestador) {
      _carregarPrestador(user.id);
    }
  }

  Future<void> _carregarPrestador(int usuarioId) async {
    try {
      final p =
          await ref.read(usuarioRepositoryProvider).prestadorPorUsuario(usuarioId);
      if (mounted) setState(() => _prestador = p);
    } on ApiException {
      // Silencioso: dados profissionais são complementares.
    }
  }

  String? _empty(String v) => v.trim().isEmpty ? null : v.trim();

  Future<void> _salvar(Usuario user) async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _salvando = true);
    try {
      // O backend (UsuarioService.atualizarDadosBasicos) faz setters diretos:
      // campo ausente no corpo vira NULL no banco. Por isso o payload parte do
      // estado COMPLETO do usuário (preserva cpf, rua, número, gênero, etc.) e
      // sobrepõe apenas o que foi editado; campo limpo no form é removido
      // (= limpar de fato no servidor).
      final dados = user.toJson();
      void aplicar(String chave, String? valor) {
        if (valor == null) {
          dados.remove(chave);
        } else {
          dados[chave] = valor;
        }
      }

      aplicar('nome', _nome.text.trim());
      aplicar('telefone', _empty(_telefone.text));
      aplicar('endereco', _empty(_endereco.text));
      aplicar('cep', _empty(_cep.text));
      aplicar('cidade', _empty(_cidade.text));
      aplicar('estado', _empty(_estado.text));
      aplicar('bio', _empty(_bio.text));
      final atualizado =
          await ref.read(usuarioRepositoryProvider).atualizar(user.id, dados);
      ref.read(authControllerProvider.notifier).setUser(atualizado);
      if (!mounted) return;
      showSuccessSnack(context, 'Perfil atualizado com sucesso.');
    } on ApiException catch (e) {
      if (!mounted) return;
      showErrorSnack(context, e.firstFieldError ?? e.message);
    } finally {
      if (mounted) setState(() => _salvando = false);
    }
  }

  Future<void> _trocarFoto(Usuario user) async {
    try {
      final picked = await ImagePicker().pickImage(
        source: ImageSource.gallery,
        maxWidth: 1024,
        imageQuality: 85,
      );
      if (picked == null) return;
      setState(() => _enviandoFoto = true);
      await ref.read(usuarioRepositoryProvider).enviarFoto(user.id, picked.path);
      // Recarrega o usuário para refletir a nova foto.
      final atualizado =
          await ref.read(usuarioRepositoryProvider).porId(user.id);
      ref.read(authControllerProvider.notifier).setUser(atualizado);
      if (!mounted) return;
      showSuccessSnack(context, 'Foto atualizada com sucesso.');
    } on ApiException catch (e) {
      if (!mounted) return;
      showErrorSnack(context, e.message);
    } finally {
      if (mounted) setState(() => _enviandoFoto = false);
    }
  }

  Future<void> _confirmarLogout() async {
    final confirmar = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Sair da conta'),
        content: const Text('Deseja realmente encerrar a sessão?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Sair'),
          ),
        ],
      ),
    );
    if (confirmar == true) {
      await ref.read(authControllerProvider.notifier).logout();
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = ref.watch(authControllerProvider).user;

    if (user == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Meu perfil')),
        body: const Center(child: Text('Sessão não encontrada.')),
      );
    }

    if (!_carregouCampos) {
      _preencher(user);
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Meu perfil'),
        actions: [
          IconButton(
            tooltip: 'Sair',
            icon: const Icon(Icons.logout),
            onPressed: _confirmarLogout,
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 520),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _avatar(user),
                    const SizedBox(height: 8),
                    Center(
                      child: Text(
                        user.email,
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ),
                    const SizedBox(height: 24),
                    _field(_nome, 'Nome completo',
                        icon: Icons.badge_outlined,
                        validator: (v) => (v == null || v.trim().isEmpty)
                            ? 'Informe seu nome.'
                            : null),
                    _field(_telefone, 'Telefone',
                        icon: Icons.phone_outlined,
                        keyboard: TextInputType.phone),
                    _field(_endereco, 'Endereço', icon: Icons.home_outlined),
                    _field(_cep, 'CEP',
                        icon: Icons.location_on_outlined,
                        keyboard: TextInputType.number),
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
                    _field(_bio, 'Bio', icon: Icons.notes_outlined, maxLines: 3),
                    if (user.isPrestador) _dadosProfissionais(),
                    const SizedBox(height: 16),
                    FilledButton.icon(
                      onPressed: _salvando ? null : () => _salvar(user),
                      icon: _salvando
                          ? const SizedBox(
                              height: 18,
                              width: 18,
                              child:
                                  CircularProgressIndicator(strokeWidth: 2.5),
                            )
                          : const Icon(Icons.save_outlined),
                      label: const Text('Salvar alterações'),
                    ),
                    const SizedBox(height: 12),
                    OutlinedButton.icon(
                      onPressed: () => context.push(Routes.minhasPublicacoes),
                      icon: const Icon(Icons.work_outline),
                      label: const Text('Minhas publicações'),
                    ),
                    if (user.isPrestador) ...[
                      const SizedBox(height: 24),
                      const Divider(),
                      const SizedBox(height: 8),
                      Align(
                        alignment: Alignment.centerLeft,
                        child: Text(
                          'Avaliações',
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                      ),
                      const SizedBox(height: 8),
                      AvaliacoesSection(usuarioId: user.id),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _avatar(Usuario user) {
    final foto = user.fotoPerfilUrl;
    final temFoto = foto != null && foto.trim().isNotEmpty;
    return Center(
      child: Stack(
        children: [
          CircleAvatar(
            radius: 52,
            backgroundColor:
                Theme.of(context).colorScheme.primaryContainer,
            backgroundImage: temFoto
                ? CachedNetworkImageProvider(foto)
                : null,
            child: temFoto
                ? null
                : Text(
                    _iniciais(user.nome),
                    style: TextStyle(
                      fontSize: 32,
                      fontWeight: FontWeight.bold,
                      color: Theme.of(context)
                          .colorScheme
                          .onPrimaryContainer,
                    ),
                  ),
          ),
          Positioned(
            right: 0,
            bottom: 0,
            child: Material(
              color: Theme.of(context).colorScheme.primary,
              shape: const CircleBorder(),
              child: InkWell(
                customBorder: const CircleBorder(),
                onTap: _enviandoFoto ? null : () => _trocarFoto(user),
                child: Padding(
                  padding: const EdgeInsets.all(8),
                  child: _enviandoFoto
                      ? const SizedBox(
                          height: 18,
                          width: 18,
                          child: CircularProgressIndicator(
                            strokeWidth: 2.5,
                            color: Colors.white,
                          ),
                        )
                      : const Icon(Icons.camera_alt,
                          size: 18, color: Colors.white),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _dadosProfissionais() {
    final p = _prestador;
    if (p == null) return const SizedBox.shrink();
    final itens = <Widget>[];
    void add(String label, String? valor) {
      if (valor == null || valor.trim().isEmpty) return;
      itens.add(
        Padding(
          padding: const EdgeInsets.only(top: 8),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: Theme.of(context).textTheme.labelMedium),
              const SizedBox(height: 2),
              Text(valor),
            ],
          ),
        ),
      );
    }

    add('Função', p.funcao);
    add('Especialidades', p.especialidades);
    add('Experiência profissional', p.experienciaProfissional);
    add('Descrição', p.descricao);

    if (itens.isEmpty) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SizedBox(height: 16),
        Align(
          alignment: Alignment.centerLeft,
          child: Text(
            'Dados profissionais',
            style: Theme.of(context).textTheme.titleSmall,
          ),
        ),
        ...itens,
      ],
    );
  }

  Widget _field(
    TextEditingController controller,
    String label, {
    IconData? icon,
    int maxLines = 1,
    TextInputType? keyboard,
    String? Function(String?)? validator,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: TextFormField(
        controller: controller,
        maxLines: maxLines,
        keyboardType: keyboard,
        decoration: InputDecoration(
          labelText: label,
          prefixIcon: icon == null ? null : Icon(icon),
        ),
        validator: validator,
      ),
    );
  }

  String _iniciais(String nome) {
    final partes =
        nome.trim().split(RegExp(r'\s+')).where((p) => p.isNotEmpty).toList();
    if (partes.isEmpty) return '?';
    if (partes.length == 1) {
      return partes.first.substring(0, 1).toUpperCase();
    }
    return (partes.first.substring(0, 1) + partes.last.substring(0, 1))
        .toUpperCase();
  }
}

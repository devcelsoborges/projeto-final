import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/router/app_router.dart';
import '../../../shared/widgets/state_views.dart';
import '../../auth/application/auth_controller.dart';
import '../../auth/domain/usuario.dart';
import '../../avaliacoes/presentation/avaliacoes_section.dart';
import '../data/usuario_repository.dart';
import '../domain/prestador.dart';

/// Dados consolidados do perfil público (usuário + prestador, se houver).
class _PerfilPublico {
  const _PerfilPublico(this.usuario, this.prestador);
  final Usuario usuario;
  final PrestadorResponse? prestador;
}

final _perfilPublicoProvider =
    FutureProvider.family<_PerfilPublico, int>((ref, usuarioId) async {
  final repo = ref.watch(usuarioRepositoryProvider);
  final usuario = await repo.porId(usuarioId);
  // `prestadorPorUsuario` retorna null em 404 (usuário não é prestador).
  final prestador = await repo.prestadorPorUsuario(usuarioId);
  return _PerfilPublico(usuario, prestador);
});

class PerfilPublicoScreen extends ConsumerWidget {
  const PerfilPublicoScreen({required this.usuarioId, super.key});

  final int usuarioId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(_perfilPublicoProvider(usuarioId));
    final euId = ref.watch(authControllerProvider).user?.id;
    final souEu = euId != null && euId == usuarioId;

    return Scaffold(
      appBar: AppBar(title: const Text('Perfil')),
      body: async.when(
        loading: () => const LoadingView(),
        error: (e, _) => ErrorView(
          message: e is ApiException
              ? e.message
              : 'Não foi possível carregar o perfil.',
          onRetry: () => ref.invalidate(_perfilPublicoProvider(usuarioId)),
        ),
        data: (dados) => _conteudo(context, dados, souEu),
      ),
    );
  }

  Widget _conteudo(BuildContext context, _PerfilPublico dados, bool souEu) {
    final user = dados.usuario;
    final prestador = dados.prestador;
    final foto = user.fotoPerfilUrl;
    final temFoto = foto != null && foto.trim().isNotEmpty;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 520),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Center(
                child: CircleAvatar(
                  radius: 52,
                  backgroundColor:
                      Theme.of(context).colorScheme.primaryContainer,
                  backgroundImage:
                      temFoto ? CachedNetworkImageProvider(foto) : null,
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
              ),
              const SizedBox(height: 12),
              Text(
                user.nome,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              if (user.cidade != null && user.cidade!.trim().isNotEmpty) ...[
                const SizedBox(height: 4),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(Icons.location_on_outlined, size: 16),
                    const SizedBox(width: 4),
                    Text(_localizacao(user)),
                  ],
                ),
              ],
              if (user.bio != null && user.bio!.trim().isNotEmpty) ...[
                const SizedBox(height: 16),
                Text(
                  user.bio!.trim(),
                  textAlign: TextAlign.center,
                ),
              ],
              if (prestador != null) _dadosProfissionais(context, prestador),
              const SizedBox(height: 24),
              if (souEu)
                OutlinedButton.icon(
                  onPressed: () => context.go(Routes.perfil),
                  icon: const Icon(Icons.edit_outlined),
                  label: const Text('Editar meu perfil'),
                )
              else
                FilledButton.icon(
                  onPressed: () => context.push(
                    '${Routes.conversa(user.id)}'
                    '?nome=${Uri.encodeComponent(user.nome)}',
                  ),
                  icon: const Icon(Icons.chat_bubble_outline),
                  label: const Text('Conversar'),
                ),
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
              AvaliacoesSection(usuarioId: usuarioId),
            ],
          ),
        ),
      ),
    );
  }

  Widget _dadosProfissionais(BuildContext context, PrestadorResponse p) {
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
              Text(valor.trim()),
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
        const SizedBox(height: 20),
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

  String _localizacao(Usuario user) {
    final cidade = user.cidade?.trim() ?? '';
    final estado = user.estado?.trim() ?? '';
    if (cidade.isNotEmpty && estado.isNotEmpty) return '$cidade - $estado';
    return cidade.isNotEmpty ? cidade : estado;
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

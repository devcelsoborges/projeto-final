import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/router/app_router.dart';
import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/feedback.dart';
import '../../../shared/widgets/state_views.dart';
import '../data/publicacao_repository.dart';
import '../domain/publicacao.dart';

/// Lista das publicações do usuário autenticado.
final minhasPublicacoesProvider =
    FutureProvider.autoDispose<List<Publicacao>>((ref) {
  return ref.watch(publicacaoRepositoryProvider).minhas();
});

class MinhasPublicacoesScreen extends ConsumerStatefulWidget {
  const MinhasPublicacoesScreen({super.key});

  @override
  ConsumerState<MinhasPublicacoesScreen> createState() =>
      _MinhasPublicacoesScreenState();
}

class _MinhasPublicacoesScreenState
    extends ConsumerState<MinhasPublicacoesScreen> {
  int? _encerrandoId;

  Future<void> _confirmarEncerrar(Publicacao p) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Encerrar publicação'),
        content: Text(
          'Deseja realmente encerrar "${p.titulo}"? Ela deixará de aparecer no catálogo.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Encerrar'),
          ),
        ],
      ),
    );

    if (ok != true) return;
    setState(() => _encerrandoId = p.id);
    try {
      await ref.read(publicacaoRepositoryProvider).encerrar(p.id);
      if (!mounted) return;
      showSuccessSnack(context, 'Publicação encerrada.');
      ref.invalidate(minhasPublicacoesProvider);
    } on ApiException catch (e) {
      if (mounted) showErrorSnack(context, e.message);
    } finally {
      if (mounted) setState(() => _encerrandoId = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final async = ref.watch(minhasPublicacoesProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Minhas publicações')),
      body: async.when(
        loading: () => const LoadingView(),
        error: (_, __) => ErrorView(
          message: 'Não foi possível carregar suas publicações.',
          onRetry: () => ref.invalidate(minhasPublicacoesProvider),
        ),
        data: (lista) {
          if (lista.isEmpty) {
            return EmptyView(
              message: 'Você ainda não tem publicações.',
              icon: Icons.post_add,
              action: FilledButton.icon(
                onPressed: () => context.go(Routes.publicar),
                icon: const Icon(Icons.add),
                label: const Text('Publicar'),
              ),
            );
          }
          return RefreshIndicator(
            onRefresh: () async => ref.invalidate(minhasPublicacoesProvider),
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: lista.length,
              itemBuilder: (context, index) {
                final p = lista[index];
                return _MinhaPublicacaoCard(
                  publicacao: p,
                  encerrando: _encerrandoId == p.id,
                  onAbrir: () => context.push(Routes.publicacao(p.id)),
                  onEncerrar:
                      p.ativa ? () => _confirmarEncerrar(p) : null,
                );
              },
            ),
          );
        },
      ),
    );
  }
}

class _MinhaPublicacaoCard extends StatelessWidget {
  const _MinhaPublicacaoCard({
    required this.publicacao,
    required this.encerrando,
    required this.onAbrir,
    required this.onEncerrar,
  });

  final Publicacao publicacao;
  final bool encerrando;
  final VoidCallback onAbrir;
  final VoidCallback? onEncerrar;

  String _valor() {
    if (publicacao.isPrestacao) {
      return publicacao.preco != null ? Fmt.moeda(publicacao.preco) : 'A combinar';
    }
    final min = publicacao.orcamentoMin;
    final max = publicacao.orcamentoMax;
    if (min != null && max != null) return '${Fmt.moeda(min)} - ${Fmt.moeda(max)}';
    if (min != null) return 'A partir de ${Fmt.moeda(min)}';
    if (max != null) return 'Até ${Fmt.moeda(max)}';
    return 'A combinar';
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final ativa = publicacao.ativa;

    return Card(
      clipBehavior: Clip.antiAlias,
      margin: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        onTap: onAbrir,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      publicacao.titulo,
                      style: Theme.of(context)
                          .textTheme
                          .titleMedium
                          ?.copyWith(fontWeight: FontWeight.bold),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: (ativa ? scheme.primary : scheme.error)
                          .withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      ativa ? 'Ativa' : 'Encerrada',
                      style: Theme.of(context).textTheme.labelSmall?.copyWith(
                            color: ativa ? scheme.primary : scheme.error,
                            fontWeight: FontWeight.bold,
                          ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 6),
              Text(
                '${publicacao.isPrestacao ? 'Prestação' : 'Contratação'}'
                '${publicacao.categoria.isNotEmpty ? '  •  ${publicacao.categoria}' : ''}',
                style: Theme.of(context).textTheme.bodySmall,
              ),
              const SizedBox(height: 8),
              Text(
                _valor(),
                style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      color: scheme.primary,
                      fontWeight: FontWeight.w600,
                    ),
              ),
              if (onEncerrar != null) ...[
                const SizedBox(height: 8),
                Align(
                  alignment: Alignment.centerRight,
                  child: TextButton.icon(
                    onPressed: encerrando ? null : onEncerrar,
                    style: TextButton.styleFrom(foregroundColor: scheme.error),
                    icon: encerrando
                        ? const SizedBox(
                            height: 16,
                            width: 16,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.block, size: 18),
                    label: const Text('Encerrar'),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/state_views.dart';
import '../application/avaliacoes_providers.dart';
import '../domain/avaliacao.dart';

/// Seção de avaliações (somente leitura) embutida em outras telas.
///
/// Não usa `Scaffold`: retorna um [Card] compacto com o resumo (estrelas +
/// média) e a lista de comentários recebidos.
class AvaliacoesSection extends ConsumerWidget {
  const AvaliacoesSection({required this.usuarioId, super.key});

  final int usuarioId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final statsAsync = ref.watch(statsUsuarioProvider(usuarioId));
    final recebidasAsync = ref.watch(recebidasUsuarioProvider(usuarioId));
    final theme = Theme.of(context);

    return Card(
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _Cabecalho(statsAsync: statsAsync, theme: theme),
            const SizedBox(height: 12),
            const Divider(height: 1),
            const SizedBox(height: 12),
            _ListaComentarios(
              recebidasAsync: recebidasAsync,
              onRetry: () =>
                  ref.invalidate(recebidasUsuarioProvider(usuarioId)),
            ),
          ],
        ),
      ),
    );
  }
}

class _Cabecalho extends StatelessWidget {
  const _Cabecalho({required this.statsAsync, required this.theme});

  final AsyncValue<AvaliacaoStats> statsAsync;
  final ThemeData theme;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Text(
            'Avaliações',
            style: theme.textTheme.titleMedium
                ?.copyWith(fontWeight: FontWeight.w600),
          ),
        ),
        statsAsync.when(
          loading: () => const SizedBox(
            height: 18,
            width: 18,
            child: CircularProgressIndicator(strokeWidth: 2),
          ),
          error: (_, __) => Text(
            'Indisponível',
            style: theme.textTheme.bodySmall
                ?.copyWith(color: theme.colorScheme.error),
          ),
          data: (stats) {
            if (stats.isEmpty) {
              return Text(
                'Sem avaliações',
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              );
            }
            return Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                RatingStars(rating: stats.media, count: stats.total),
                const SizedBox(width: 6),
                Text(
                  _formatarMedia(stats.media),
                  style: theme.textTheme.bodyMedium
                      ?.copyWith(fontWeight: FontWeight.w600),
                ),
              ],
            );
          },
        ),
      ],
    );
  }
}

class _ListaComentarios extends StatelessWidget {
  const _ListaComentarios({
    required this.recebidasAsync,
    required this.onRetry,
  });

  final AsyncValue<List<Avaliacao>> recebidasAsync;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return recebidasAsync.when(
      loading: () => const Padding(
        padding: EdgeInsets.symmetric(vertical: 24),
        child: LoadingView(),
      ),
      error: (error, _) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 8),
        child: ErrorView(
          message: 'Não foi possível carregar as avaliações.',
          onRetry: onRetry,
        ),
      ),
      data: (avaliacoes) {
        if (avaliacoes.isEmpty) {
          return const Padding(
            padding: EdgeInsets.symmetric(vertical: 8),
            child: EmptyView(
              message: 'Ainda sem avaliações.',
              icon: Icons.reviews_outlined,
            ),
          );
        }
        return Column(
          children: [
            for (final a in avaliacoes) _ComentarioTile(avaliacao: a),
          ],
        );
      },
    );
  }
}

class _ComentarioTile extends StatelessWidget {
  const _ComentarioTile({required this.avaliacao});

  final Avaliacao avaliacao;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final comentario = avaliacao.comentario?.trim();
    final data = Fmt.data(avaliacao.dataCriacao);

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              RatingStars(rating: avaliacao.nota.toDouble(), size: 14),
              const Spacer(),
              if (data.isNotEmpty)
                Text(
                  data,
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
            ],
          ),
          if (comentario != null && comentario.isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(comentario, style: theme.textTheme.bodyMedium),
          ],
        ],
      ),
    );
  }
}

/// Formata a média com uma casa decimal e vírgula (ex.: `4,5`).
String _formatarMedia(double media) =>
    media.toStringAsFixed(1).replaceAll('.', ',');

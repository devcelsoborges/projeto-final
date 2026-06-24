import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/router/app_router.dart';
import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/state_views.dart';
import '../../avaliacoes/presentation/avaliacoes_section.dart';
import '../data/publicacao_repository.dart';
import '../domain/publicacao.dart';

/// Carrega o detalhe de uma publicação por id.
final publicacaoDetalheProvider =
    FutureProvider.family<Publicacao, int>((ref, id) {
  return ref.watch(publicacaoRepositoryProvider).detalhe(id);
});

class PublicacaoDetailScreen extends ConsumerWidget {
  const PublicacaoDetailScreen({required this.publicacaoId, super.key});

  final int publicacaoId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(publicacaoDetalheProvider(publicacaoId));

    return Scaffold(
      appBar: AppBar(title: const Text('Detalhes')),
      body: async.when(
        loading: () => const LoadingView(),
        error: (_, __) => ErrorView(
          message: 'Não foi possível carregar a publicação.',
          onRetry: () => ref.invalidate(publicacaoDetalheProvider(publicacaoId)),
        ),
        data: (p) => _Conteudo(publicacao: p),
      ),
    );
  }
}

class _Conteudo extends StatelessWidget {
  const _Conteudo({required this.publicacao});

  final Publicacao publicacao;

  String _localizacao() {
    final partes = <String>[
      if ((publicacao.enderecoPublicacao ?? '').trim().isNotEmpty)
        publicacao.enderecoPublicacao!.trim(),
      if ((publicacao.cidadePublicacao ?? '').trim().isNotEmpty)
        publicacao.cidadePublicacao!.trim(),
      if ((publicacao.estadoPublicacao ?? '').trim().isNotEmpty)
        publicacao.estadoPublicacao!.trim(),
    ];
    return partes.join(', ');
  }

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
    final p = publicacao;
    final scheme = Theme.of(context).colorScheme;
    final local = _localizacao();

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
      children: [
        Row(
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(
                color: (p.isPrestacao ? scheme.primary : scheme.tertiary)
                    .withValues(alpha: 0.12),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                p.tipoLabel,
                style: Theme.of(context).textTheme.labelMedium?.copyWith(
                      color: p.isPrestacao ? scheme.primary : scheme.tertiary,
                      fontWeight: FontWeight.w600,
                    ),
              ),
            ),
            const Spacer(),
            if (!p.ativa)
              Text(
                'Encerrada',
                style: Theme.of(context)
                    .textTheme
                    .labelMedium
                    ?.copyWith(color: scheme.error),
              ),
          ],
        ),
        const SizedBox(height: 12),
        Text(
          p.titulo,
          style: Theme.of(context)
              .textTheme
              .headlineSmall
              ?.copyWith(fontWeight: FontWeight.bold),
        ),
        if (p.categoria.isNotEmpty) ...[
          const SizedBox(height: 4),
          Text(
            p.categoria,
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: scheme.onSurfaceVariant),
          ),
        ],
        const SizedBox(height: 16),
        _InfoLinha(
          icon: Icons.payments_outlined,
          label: p.isPrestacao ? 'Preço' : 'Orçamento',
          value: _valor(),
          emphasis: true,
        ),
        if (local.isNotEmpty)
          _InfoLinha(
            icon: Icons.place_outlined,
            label: 'Local',
            value: local,
          ),
        _InfoLinha(
          icon: Icons.person_outline,
          label: 'Publicado por',
          value: p.usuarioNome,
        ),
        if (p.dataCriacao != null)
          _InfoLinha(
            icon: Icons.schedule,
            label: 'Publicado em',
            value: Fmt.data(p.dataCriacao),
          ),
        const SizedBox(height: 20),
        Text(
          'Descrição',
          style: Theme.of(context)
              .textTheme
              .titleMedium
              ?.copyWith(fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        Text(
          p.descricao.isEmpty ? 'Sem descrição.' : p.descricao,
          style: Theme.of(context).textTheme.bodyMedium,
        ),
        const SizedBox(height: 24),
        Row(
          children: [
            Expanded(
              child: FilledButton.icon(
                onPressed: () => context.push(
                  '${Routes.conversa(p.usuarioId)}?nome=${Uri.encodeComponent(p.usuarioNome)}',
                ),
                icon: const Icon(Icons.chat_bubble_outline),
                label: const Text('Conversar'),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: OutlinedButton.icon(
                onPressed: () => context.push(Routes.perfilPublico(p.usuarioId)),
                icon: const Icon(Icons.person_outline),
                label: const Text('Ver perfil'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 28),
        Text(
          'Avaliações de ${p.usuarioNome}',
          style: Theme.of(context)
              .textTheme
              .titleMedium
              ?.copyWith(fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        AvaliacoesSection(usuarioId: p.usuarioId),
      ],
    );
  }
}

class _InfoLinha extends StatelessWidget {
  const _InfoLinha({
    required this.icon,
    required this.label,
    required this.value,
    this.emphasis = false,
  });

  final IconData icon;
  final String label;
  final String value;
  final bool emphasis;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 20, color: scheme.onSurfaceVariant),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: Theme.of(context)
                      .textTheme
                      .bodySmall
                      ?.copyWith(color: scheme.onSurfaceVariant),
                ),
                const SizedBox(height: 2),
                Text(
                  value,
                  style: emphasis
                      ? Theme.of(context).textTheme.titleSmall?.copyWith(
                            color: scheme.primary,
                            fontWeight: FontWeight.bold,
                          )
                      : Theme.of(context).textTheme.bodyMedium,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

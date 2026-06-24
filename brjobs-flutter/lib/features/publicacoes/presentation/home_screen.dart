import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/router/app_router.dart';
import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/state_views.dart';
import '../application/home_controller.dart';
import '../domain/publicacao.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  final _busca = TextEditingController();
  final _scroll = ScrollController();
  Timer? _debounce;

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_onScroll);
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _busca.dispose();
    _scroll
      ..removeListener(_onScroll)
      ..dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scroll.position.pixels >=
        _scroll.position.maxScrollExtent - 320) {
      ref.read(homeControllerProvider.notifier).carregarMais();
    }
  }

  void _onTermoChanged(String value) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 450), () {
      ref.read(homeControllerProvider.notifier).setTermo(value);
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(homeControllerProvider);
    final notifier = ref.read(homeControllerProvider.notifier);

    return Scaffold(
      appBar: AppBar(title: const Text('BRJobs')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: TextField(
              controller: _busca,
              textInputAction: TextInputAction.search,
              onChanged: _onTermoChanged,
              decoration: InputDecoration(
                hintText: 'Buscar serviços, categorias...',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _busca.text.isEmpty
                    ? null
                    : IconButton(
                        icon: const Icon(Icons.close),
                        onPressed: () {
                          _busca.clear();
                          _debounce?.cancel();
                          notifier.setTermo('');
                        },
                      ),
                isDense: true,
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: SegmentedButton<String>(
              showSelectedIcon: false,
              segments: const [
                ButtonSegment(value: '', label: Text('Todos')),
                ButtonSegment(value: 'PRESTACAO', label: Text('Prestação')),
                ButtonSegment(value: 'CONTRATACAO', label: Text('Contratação')),
              ],
              selected: {state.tipo ?? ''},
              onSelectionChanged: (sel) {
                final value = sel.first;
                notifier.setTipo(value.isEmpty ? null : value);
              },
            ),
          ),
          const SizedBox(height: 8),
          Expanded(
            child: RefreshIndicator(
              onRefresh: notifier.refresh,
              child: _buildBody(state, notifier),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBody(HomeState state, HomeController notifier) {
    if (state.loading && state.isEmpty) {
      return const LoadingView();
    }
    if (state.erro != null && state.isEmpty) {
      return ErrorView(message: state.erro!, onRetry: notifier.refresh);
    }
    if (state.isEmpty) {
      return ListView(
        // Mantém o pull-to-refresh funcional mesmo vazio.
        children: const [
          SizedBox(height: 120),
          EmptyView(
            message: 'Nenhuma publicação encontrada.',
            icon: Icons.search_off,
          ),
        ],
      );
    }

    return ListView.builder(
      controller: _scroll,
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      itemCount: state.itens.length + (state.hasMore ? 1 : 0),
      itemBuilder: (context, index) {
        if (index >= state.itens.length) {
          return const Padding(
            padding: EdgeInsets.symmetric(vertical: 24),
            child: Center(
              child: SizedBox(
                height: 24,
                width: 24,
                child: CircularProgressIndicator(strokeWidth: 2.5),
              ),
            ),
          );
        }
        return _PublicacaoCard(publicacao: state.itens[index]);
      },
    );
  }
}

class _PublicacaoCard extends StatelessWidget {
  const _PublicacaoCard({required this.publicacao});

  final Publicacao publicacao;

  String _localizacao() {
    final cidade = publicacao.cidadePublicacao?.trim();
    final estado = publicacao.estadoPublicacao?.trim();
    if (cidade != null && cidade.isNotEmpty && estado != null && estado.isNotEmpty) {
      return '$cidade - $estado';
    }
    if (cidade != null && cidade.isNotEmpty) return cidade;
    return estado ?? '';
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
    final scheme = Theme.of(context).colorScheme;
    final local = _localizacao();
    final distancia = Fmt.distancia(publicacao.distanceKm);

    return Card(
      clipBehavior: Clip.antiAlias,
      margin: const EdgeInsets.only(top: 12),
      child: InkWell(
        onTap: () => context.push(Routes.publicacao(publicacao.id)),
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
                  if (publicacao.isHighlighted) ...[
                    const SizedBox(width: 8),
                    _Badge(
                      label: 'Destaque',
                      icon: Icons.star,
                      color: Colors.amber.shade700,
                    ),
                  ],
                ],
              ),
              const SizedBox(height: 6),
              Wrap(
                spacing: 8,
                runSpacing: 4,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  _Tag(
                    label: publicacao.isPrestacao ? 'Prestação' : 'Contratação',
                    color: publicacao.isPrestacao
                        ? scheme.primary
                        : scheme.tertiary,
                  ),
                  if (publicacao.categoria.isNotEmpty)
                    Text(
                      publicacao.categoria,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                ],
              ),
              const SizedBox(height: 10),
              Text(
                _valor(),
                style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      color: scheme.primary,
                      fontWeight: FontWeight.w600,
                    ),
              ),
              if (local.isNotEmpty || distancia.isNotEmpty) ...[
                const SizedBox(height: 8),
                Row(
                  children: [
                    Icon(Icons.place_outlined,
                        size: 16, color: scheme.onSurfaceVariant),
                    const SizedBox(width: 4),
                    Expanded(
                      child: Text(
                        [local, if (distancia.isNotEmpty) distancia]
                            .where((s) => s.isNotEmpty)
                            .join('  •  '),
                        style: Theme.of(context).textTheme.bodySmall,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _Tag extends StatelessWidget {
  const _Tag({required this.label, required this.color});

  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        label,
        style: Theme.of(context)
            .textTheme
            .labelSmall
            ?.copyWith(color: color, fontWeight: FontWeight.w600),
      ),
    );
  }
}

class _Badge extends StatelessWidget {
  const _Badge({required this.label, required this.icon, required this.color});

  final String label;
  final IconData icon;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: color),
          const SizedBox(width: 4),
          Text(
            label,
            style: Theme.of(context)
                .textTheme
                .labelSmall
                ?.copyWith(color: color, fontWeight: FontWeight.bold),
          ),
        ],
      ),
    );
  }
}

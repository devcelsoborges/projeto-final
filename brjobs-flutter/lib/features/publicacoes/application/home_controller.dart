import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_exception.dart';
import '../data/publicacao_repository.dart';
import '../domain/publicacao.dart';

/// Estado imutável da busca paginada do catálogo (home).
class HomeState {
  const HomeState({
    this.itens = const [],
    this.termo = '',
    this.tipo,
    this.page = 0,
    this.loading = false,
    this.loadingMore = false,
    this.hasMore = true,
    this.erro,
  });

  /// Lista acumulada de todas as páginas carregadas.
  final List<Publicacao> itens;

  /// Termo de busca atual.
  final String termo;

  /// Filtro de tipo (`PRESTACAO`/`CONTRATACAO`) ou `null` para "Todos".
  final String? tipo;

  /// Índice da última página carregada (base 0).
  final int page;

  /// Carregamento da primeira página (substitui a lista).
  final bool loading;

  /// Carregamento de páginas seguintes ("carregar mais").
  final bool loadingMore;

  /// Ainda há páginas a carregar.
  final bool hasMore;

  /// Mensagem de erro da primeira página (quando aplicável).
  final String? erro;

  bool get isEmpty => itens.isEmpty;

  HomeState copyWith({
    List<Publicacao>? itens,
    String? termo,
    Object? tipo = _sentinel,
    int? page,
    bool? loading,
    bool? loadingMore,
    bool? hasMore,
    Object? erro = _sentinel,
  }) {
    return HomeState(
      itens: itens ?? this.itens,
      termo: termo ?? this.termo,
      tipo: identical(tipo, _sentinel) ? this.tipo : tipo as String?,
      page: page ?? this.page,
      loading: loading ?? this.loading,
      loadingMore: loadingMore ?? this.loadingMore,
      hasMore: hasMore ?? this.hasMore,
      erro: identical(erro, _sentinel) ? this.erro : erro as String?,
    );
  }

  static const _sentinel = Object();
}

final homeControllerProvider =
    NotifierProvider<HomeController, HomeState>(HomeController.new);

/// Controla a busca paginada do catálogo: filtro por termo/tipo e paginação
/// incremental ("carregar mais").
class HomeController extends Notifier<HomeState> {
  static const _pageSize = 20;

  PublicacaoRepository get _repo => ref.read(publicacaoRepositoryProvider);

  @override
  HomeState build() {
    // Dispara a carga inicial sem bloquear a construção do provider.
    Future.microtask(refresh);
    return const HomeState(loading: true);
  }

  /// Atualiza o termo e recarrega a partir da primeira página.
  Future<void> setTermo(String termo) async {
    if (termo == state.termo) return;
    state = state.copyWith(termo: termo);
    await refresh();
  }

  /// Atualiza o filtro de tipo e recarrega a partir da primeira página.
  Future<void> setTipo(String? tipo) async {
    if (tipo == state.tipo) return;
    state = state.copyWith(tipo: tipo);
    await refresh();
  }

  /// Recarrega a primeira página (pull-to-refresh / mudança de filtro).
  Future<void> refresh() async {
    state = state.copyWith(loading: true, erro: null);
    try {
      final pagina = await _repo.buscarPaginado(
        tipo: state.tipo,
        termo: state.termo,
        page: 0,
        size: _pageSize,
      );
      state = state.copyWith(
        itens: pagina.content,
        page: 0,
        hasMore: !pagina.isLast,
        loading: false,
        loadingMore: false,
        erro: null,
      );
    } on ApiException catch (e) {
      state = state.copyWith(loading: false, loadingMore: false, erro: e.message);
    }
  }

  /// Carrega a próxima página e a anexa à lista atual.
  Future<void> carregarMais() async {
    if (state.loading || state.loadingMore || !state.hasMore) return;
    state = state.copyWith(loadingMore: true);
    try {
      final proxima = state.page + 1;
      final pagina = await _repo.buscarPaginado(
        tipo: state.tipo,
        termo: state.termo,
        page: proxima,
        size: _pageSize,
      );
      state = state.copyWith(
        itens: [...state.itens, ...pagina.content],
        page: proxima,
        hasMore: !pagina.isLast,
        loadingMore: false,
      );
    } on ApiException {
      // Mantém a lista já carregada; apenas para o indicador de "mais".
      state = state.copyWith(loadingMore: false, hasMore: false);
    }
  }
}

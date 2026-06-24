import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_endpoints.dart';
import '../../../core/network/api_guard.dart';
import '../../../core/providers/app_providers.dart';
import '../../../shared/models/page.dart';
import '../domain/publicacao.dart';

final publicacaoRepositoryProvider = Provider<PublicacaoRepository>(
  (ref) => PublicacaoRepository(ref.watch(dioProvider)),
);

/// Acesso às rotas de publicações de serviço.
class PublicacaoRepository {
  PublicacaoRepository(this._dio);

  final Dio _dio;

  /// Catálogo público paginado. `tipo` filtra por `PRESTACAO`/`CONTRATACAO`;
  /// `termo` faz a busca textual; `lat`/`lng` habilitam ordenação por distância.
  Future<Page<Publicacao>> buscarPaginado({
    String? tipo,
    String? termo,
    int page = 0,
    int size = 20,
    double? lat,
    double? lng,
  }) {
    return apiGuard(() async {
      final query = <String, dynamic>{
        'page': page,
        'size': size,
      };
      if (tipo != null && tipo.isNotEmpty) query['tipo'] = tipo;
      if (termo != null && termo.trim().isNotEmpty) query['termo'] = termo.trim();
      if (lat != null) query['lat'] = lat;
      if (lng != null) query['lng'] = lng;

      final res = await _dio.get(
        Api.publicacoesPaginado,
        queryParameters: query,
      );
      return Page<Publicacao>.fromJson(
        asJsonMap(res.data),
        Publicacao.fromJson,
      );
    });
  }

  /// Detalhe público de uma publicação.
  Future<Publicacao> detalhe(int id) {
    return apiGuard(() async {
      final res = await _dio.get(Api.publicacao(id));
      return Publicacao.fromJson(asJsonMap(res.data));
    });
  }

  /// Publicações do usuário autenticado (ativas e encerradas).
  Future<List<Publicacao>> minhas() {
    return apiGuard(() async {
      final res = await _dio.get(Api.minhasPublicacoes);
      return asJsonList(res.data).map(Publicacao.fromJson).toList();
    });
  }

  /// Cria uma nova publicação (requer sessão).
  Future<Publicacao> criar(CriarPublicacao dto) {
    return apiGuard(() async {
      final res = await _dio.post(Api.publicacoes, data: dto.toJson());
      return Publicacao.fromJson(asJsonMap(res.data));
    });
  }

  /// Encerra (remove do catálogo) uma publicação do usuário.
  Future<void> encerrar(int id) {
    return apiGuard(() async {
      await _dio.delete(Api.publicacao(id));
    });
  }
}

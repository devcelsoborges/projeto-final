import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_endpoints.dart';
import '../../../core/network/api_guard.dart';
import '../../../core/providers/app_providers.dart';
import '../domain/avaliacao.dart';

final avaliacaoRepositoryProvider = Provider<AvaliacaoRepository>(
  (ref) => AvaliacaoRepository(ref.watch(dioProvider)),
);

/// Acesso às rotas públicas de avaliações (somente leitura nesta fase).
class AvaliacaoRepository {
  AvaliacaoRepository(this._dio);

  final Dio _dio;

  /// Estatísticas (média + total) de um usuário avaliado.
  Future<AvaliacaoStats> statsUsuario(int usuarioId) {
    return apiGuard(() async {
      final res = await _dio.get(Api.avaliacoesV1UsuarioStats(usuarioId));
      return AvaliacaoStats.fromJson(asJsonMap(res.data));
    });
  }

  /// Avaliações recebidas por um usuário.
  Future<List<Avaliacao>> recebidasUsuario(int usuarioId) {
    return apiGuard(() async {
      final res = await _dio.get(Api.avaliacoesUsuarioRecebidas(usuarioId));
      return asJsonList(res.data).map(Avaliacao.fromJson).toList();
    });
  }

  /// Estatísticas (média + total) de um prestador.
  Future<AvaliacaoStats> statsPrestador(int prestadorId) {
    return apiGuard(() async {
      final res = await _dio.get(Api.avaliacoesV1PrestadorStats(prestadorId));
      return AvaliacaoStats.fromJson(asJsonMap(res.data));
    });
  }
}

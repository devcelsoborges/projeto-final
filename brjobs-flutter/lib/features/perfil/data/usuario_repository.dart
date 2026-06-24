import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_endpoints.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/network/api_guard.dart';
import '../../../core/providers/app_providers.dart';
import '../../auth/domain/usuario.dart';
import '../domain/prestador.dart';

final usuarioRepositoryProvider = Provider<UsuarioRepository>(
  (ref) => UsuarioRepository(ref.watch(dioProvider)),
);

/// Acesso às rotas de usuários e prestadores (perfil).
class UsuarioRepository {
  UsuarioRepository(this._dio);

  final Dio _dio;

  /// Busca o usuário pelo id.
  Future<Usuario> porId(int id) {
    return apiGuard(() async {
      final res = await _dio.get(Api.usuario(id));
      return Usuario.fromJson(asJsonMap(res.data));
    });
  }

  /// Atualiza o usuário (PUT com corpo JSON) e retorna o registro atualizado.
  Future<Usuario> atualizar(int id, Map<String, dynamic> dados) {
    return apiGuard(() async {
      final res = await _dio.put(Api.usuario(id), data: dados);
      return Usuario.fromJson(asJsonMap(res.data));
    });
  }

  /// Envia a foto de perfil (multipart, campo `foto`). Backend responde 204.
  Future<void> enviarFoto(int id, String filePath) {
    return apiGuard(() async {
      final form = FormData.fromMap({
        'foto': await MultipartFile.fromFile(filePath),
      });
      await _dio.post(Api.usuarioFoto(id), data: form);
    });
  }

  /// Dados de prestador do usuário; retorna `null` quando não é prestador (404).
  Future<PrestadorResponse?> prestadorPorUsuario(int usuarioId) async {
    // O catch fica FORA do apiGuard: dentro dele o Dio ainda lança
    // DioException; só após a conversão existe ApiException com statusCode.
    try {
      return await apiGuard(() async {
        final res = await _dio.get(Api.prestadorPorUsuario(usuarioId));
        return PrestadorResponse.fromJson(asJsonMap(res.data));
      });
    } on ApiException catch (e) {
      if (e.statusCode == 404) return null;
      rethrow;
    }
  }
}

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_endpoints.dart';
import '../../../core/network/api_guard.dart';
import '../../../core/providers/app_providers.dart';
import '../domain/cadastro.dart';
import '../domain/usuario.dart';

final authRepositoryProvider = Provider<AuthRepository>(
  (ref) => AuthRepository(ref.watch(dioClientProvider).dio, ref),
);

/// Acesso às rotas de autenticação (fluxo de sessão por cookie).
class AuthRepository {
  AuthRepository(this._dio, this._ref);

  final Dio _dio;
  final Ref _ref;

  /// Pré-carrega o cookie CSRF (chamado no bootstrap).
  Future<void> primeCsrf() =>
      apiGuard(() async => _dio.get(Api.csrf));

  /// Login local. Estabelece a sessão (cookies) e retorna o usuário.
  Future<Usuario> login(String email, String senha) {
    return apiGuard(() async {
      final res = await _dio.post(Api.login, data: {
        'email': email.trim(),
        'senha': senha,
      });
      final data = res.data;
      if (data is Map && data['id'] != null) {
        return Usuario.fromJson(data.cast<String, dynamic>());
      }
      return me();
    });
  }

  /// Usuário autenticado a partir da sessão atual.
  Future<Usuario> me() {
    return apiGuard(() async {
      final res = await _dio.get(Api.me);
      return Usuario.fromJson(asJsonMap(res.data));
    });
  }

  /// Cadastro de contratante via `/api/v1/auth/register` (faz login automático).
  Future<Usuario> registrarContratante(CadastroContratante dto) {
    return apiGuard(() async {
      final res = await _dio.post(Api.register, data: dto.toJson());
      final data = res.data;
      if (data is Map && data['id'] != null) {
        return Usuario.fromJson(data.cast<String, dynamic>());
      }
      return me();
    });
  }

  /// Cadastro de prestador via `/api/usuarios/prestador` (sem sessão),
  /// seguido de login explícito para estabelecer os cookies.
  Future<Usuario> registrarPrestador(CadastroPrestador dto) {
    return apiGuard(() async {
      await _dio.post(Api.cadastroPrestador, data: dto.toJson());
      return login(dto.base.email, dto.base.senha);
    });
  }

  /// Login social com Google. [idToken] vem do `google_sign_in`.
  Future<Usuario> loginGoogle(String idToken) {
    return apiGuard(() async {
      final res = await _dio.post(Api.socialGoogle, data: {
        'idToken': idToken,
        'credential': idToken,
        'token': idToken,
      });
      final data = res.data;
      if (data is Map && data['id'] != null) {
        return Usuario.fromJson(data.cast<String, dynamic>());
      }
      return me();
    });
  }

  Future<void> logout() async {
    try {
      await _dio.post(Api.logout);
    } catch (_) {
      // Best-effort; segue limpando os cookies localmente.
    }
    await _ref.read(dioClientProvider).clearSession();
  }

  // ---- Recuperação de senha ----

  /// Solicita o código. Retorna `debugCode` quando o backend o expõe.
  Future<String?> solicitarResetSenha(String email) {
    return apiGuard(() async {
      final res = await _dio
          .post(Api.forgotPasswordRequest, data: {'email': email.trim()});
      final data = res.data;
      if (data is Map) return data['debugCode']?.toString();
      return null;
    });
  }

  Future<void> verificarCodigoReset(String email, String code) {
    return apiGuard(() async {
      await _dio.post(Api.forgotPasswordVerify,
          data: {'email': email.trim(), 'code': code});
    });
  }

  Future<void> redefinirSenha(String email, String code, String novaSenha) {
    return apiGuard(() async {
      await _dio.post(Api.forgotPasswordReset, data: {
        'email': email.trim(),
        'code': code,
        'newPassword': novaSenha,
      });
    });
  }
}

import 'package:dio/dio.dart';

/// Erro normalizado da API.
///
/// O backend (`GlobalExceptionHandler`) responde de formas diferentes:
/// - `400` validação de campos -> JSON `{ "campo": "mensagem", ... }`
/// - `409` -> string simples, ex.: "Email já está em uso."
/// - `400` regra de negócio -> string simples
/// - `403` -> "Acesso negado"
/// - `429` -> "Limite de mensagens excedido. Tente novamente em instantes."
class ApiException implements Exception {
  ApiException({
    required this.message,
    this.statusCode,
    this.fieldErrors = const {},
  });

  final String message;
  final int? statusCode;

  /// Erros por campo (para `400` de validação). Vazio quando não aplicável.
  final Map<String, String> fieldErrors;

  bool get isUnauthorized => statusCode == 401;
  bool get isForbidden => statusCode == 403;
  bool get isConflict => statusCode == 409;
  bool get isRateLimited => statusCode == 429;
  bool get isNetwork => statusCode == null;

  /// Mensagem do primeiro erro de campo, se houver.
  String? get firstFieldError =>
      fieldErrors.isEmpty ? null : fieldErrors.values.first;

  @override
  String toString() => 'ApiException($statusCode): $message';

  /// Constrói a partir de uma [DioException].
  factory ApiException.fromDio(DioException e) {
    final response = e.response;

    if (response == null) {
      // Sem resposta = timeout, DNS, conexão recusada, etc.
      final msg = switch (e.type) {
        DioExceptionType.connectionTimeout ||
        DioExceptionType.sendTimeout ||
        DioExceptionType.receiveTimeout =>
          'Tempo de conexão esgotado. Verifique sua internet.',
        DioExceptionType.connectionError =>
          'Não foi possível conectar ao servidor.',
        _ => 'Falha de rede. Tente novamente.',
      };
      return ApiException(message: msg);
    }

    final status = response.statusCode;
    final data = response.data;

    // 400 de validação: corpo é um mapa campo -> mensagem.
    if (status == 400 && data is Map) {
      final fields = <String, String>{};
      data.forEach((key, value) {
        if (key is String) fields[key] = value?.toString() ?? 'Inválido';
      });
      if (fields.isNotEmpty) {
        return ApiException(
          message: fields.values.first,
          statusCode: status,
          fieldErrors: fields,
        );
      }
    }

    // Corpo string simples (409, 403, 429, 400 de regra).
    String message;
    if (data is String && data.trim().isNotEmpty) {
      message = data.trim();
    } else if (data is Map && data['message'] is String) {
      message = data['message'] as String;
    } else {
      message = switch (status) {
        401 => 'Sessão expirada. Entre novamente.',
        403 => 'Acesso negado.',
        404 => 'Não encontrado.',
        409 => 'Registro já existe.',
        429 => 'Muitas requisições. Aguarde um instante.',
        500 => 'Erro interno do servidor.',
        _ => 'Erro inesperado ($status).',
      };
    }

    return ApiException(message: message, statusCode: status);
  }
}

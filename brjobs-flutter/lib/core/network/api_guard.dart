import 'package:dio/dio.dart';

import 'api_exception.dart';

/// Executa uma chamada HTTP convertendo qualquer [DioException] em
/// [ApiException] (e repassando [ApiException] já lançadas).
Future<T> apiGuard<T>(Future<T> Function() call) async {
  try {
    return await call();
  } on ApiException {
    rethrow;
  } on DioException catch (e) {
    throw ApiException.fromDio(e);
  }
}

/// Garante que o corpo da resposta é um `Map<String, dynamic>`.
Map<String, dynamic> asJsonMap(dynamic data) {
  if (data is Map<String, dynamic>) return data;
  if (data is Map) return data.cast<String, dynamic>();
  throw ApiException(message: 'Resposta inesperada do servidor.');
}

/// Garante que o corpo da resposta é uma lista de mapas.
List<Map<String, dynamic>> asJsonList(dynamic data) {
  if (data is List) {
    return data
        .whereType<Map>()
        .map((e) => e.cast<String, dynamic>())
        .toList();
  }
  throw ApiException(message: 'Resposta inesperada do servidor.');
}

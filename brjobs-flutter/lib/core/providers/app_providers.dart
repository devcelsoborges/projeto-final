import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../network/dio_client.dart';
import '../storage/cookie_store.dart';

/// Sobrescrito em `main()` com a instância criada de forma assíncrona.
final cookieStoreProvider = Provider<CookieStore>(
  (ref) => throw UnimplementedError('cookieStoreProvider deve ser sobrescrito'),
);

/// Sobrescrito em `main()` com a instância criada de forma assíncrona.
final dioClientProvider = Provider<DioClient>(
  (ref) => throw UnimplementedError('dioClientProvider deve ser sobrescrito'),
);

/// Atalho para o [Dio] configurado (com cookies + CSRF + refresh).
final dioProvider = Provider<Dio>((ref) => ref.watch(dioClientProvider).dio);

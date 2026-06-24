import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';
import 'core/network/dio_client.dart';
import 'core/providers/app_providers.dart';
import 'core/storage/cookie_store.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Sessão por cookie persistida em disco (igual ao navegador do site).
  final cookieStore = await CookieStore.create();
  final dioClient = await DioClient.create(cookieStore);

  runApp(
    ProviderScope(
      overrides: [
        cookieStoreProvider.overrideWithValue(cookieStore),
        dioClientProvider.overrideWithValue(dioClient),
      ],
      child: const BrjobsApp(),
    ),
  );
}

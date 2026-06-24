import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_endpoints.dart';
import '../../../core/network/api_guard.dart';
import '../../../core/providers/app_providers.dart';
import '../../../shared/utils/json_utils.dart';
import '../domain/chat_message.dart';
import '../domain/conversa.dart';

final chatRepositoryProvider = Provider<ChatRepository>(
  (ref) => ChatRepository(ref.watch(dioProvider)),
);

/// Acesso às rotas de chat (mensagens diretas com polling).
class ChatRepository {
  ChatRepository(this._dio);

  final Dio _dio;

  /// Lista as conversas do usuário autenticado.
  Future<List<Conversa>> conversas() {
    return apiGuard(() async {
      final res = await _dio.get(Api.chatConversas);
      return asJsonList(res.data).map(Conversa.fromJson).toList();
    });
  }

  /// Mensagens trocadas com [outroUsuarioId] (mais recentes primeiro no
  /// backend; aqui devolvemos exatamente o que vem da API).
  Future<List<ChatMessage>> mensagens(int outroUsuarioId, {int limit = 50}) {
    return apiGuard(() async {
      final res = await _dio.get(
        Api.chatConversa(outroUsuarioId),
        queryParameters: {'limit': limit},
      );
      return asJsonList(res.data).map(ChatMessage.fromJson).toList();
    });
  }

  /// Envia uma mensagem para [destinatarioId] e devolve a mensagem criada.
  Future<ChatMessage> enviar(int destinatarioId, String conteudo) {
    return apiGuard(() async {
      final res = await _dio.post(
        Api.chatEnviar,
        queryParameters: {'destinatarioId': destinatarioId},
        data: {'conteudo': conteudo},
      );
      return ChatMessage.fromJson(asJsonMap(res.data));
    });
  }

  /// Marca como lidas todas as mensagens recebidas de [outroUsuarioId].
  Future<void> marcarLidas(int outroUsuarioId) {
    return apiGuard(() async {
      await _dio.put(Api.chatMarcarLidas(outroUsuarioId));
    });
  }

  /// Contagem total de mensagens não lidas.
  Future<int> naoLidas() {
    return apiGuard(() async {
      final res = await _dio.get(Api.chatNaoLidas);
      return asInt(res.data) ?? 0;
    });
  }
}

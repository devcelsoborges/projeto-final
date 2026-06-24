import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/config/env.dart';
import '../data/chat_repository.dart';
import '../domain/chat_message.dart';
import '../domain/conversa.dart';

/// Lista de conversas com atualização periódica (polling).
final conversasControllerProvider =
    AsyncNotifierProvider<ConversasController, List<Conversa>>(
  ConversasController.new,
);

class ConversasController extends AsyncNotifier<List<Conversa>> {
  Timer? _timer;

  ChatRepository get _repo => ref.read(chatRepositoryProvider);

  @override
  Future<List<Conversa>> build() async {
    _timer = Timer.periodic(
      const Duration(milliseconds: Env.chatPollIntervalMs),
      (_) => _poll(),
    );
    ref.onDispose(() => _timer?.cancel());
    return _repo.conversas();
  }

  /// Recarrega exibindo o estado de carregamento (pull-to-refresh / retry).
  Future<void> recarregar() async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard(_repo.conversas);
  }

  /// Atualização silenciosa em segundo plano (não pisca a tela).
  Future<void> _poll() async {
    try {
      final dados = await _repo.conversas();
      state = AsyncValue.data(dados);
    } catch (_) {
      // Mantém o estado atual; o próximo ciclo tenta de novo.
    }
  }
}

/// Thread de mensagens de um contato, com polling.
///
/// Indexado por `outroUsuarioId` via `.family`.
final conversaThreadProvider = AsyncNotifierProvider.family<ConversaThreadController,
    List<ChatMessage>, int>(
  ConversaThreadController.new,
);

class ConversaThreadController
    extends FamilyAsyncNotifier<List<ChatMessage>, int> {
  Timer? _timer;
  late int _outroUsuarioId;

  ChatRepository get _repo => ref.read(chatRepositoryProvider);

  @override
  Future<List<ChatMessage>> build(int arg) async {
    _outroUsuarioId = arg;
    _timer = Timer.periodic(
      const Duration(milliseconds: Env.chatPollIntervalMs),
      (_) => _poll(),
    );
    ref.onDispose(() => _timer?.cancel());
    final mensagens = await _repo.mensagens(_outroUsuarioId);
    await _marcarLidasSilencioso();
    return mensagens;
  }

  /// Recarrega exibindo carregamento (usado no retry).
  Future<void> recarregar() async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      final mensagens = await _repo.mensagens(_outroUsuarioId);
      await _marcarLidasSilencioso();
      return mensagens;
    });
  }

  /// Envia uma mensagem e atualiza a thread com a resposta do backend.
  Future<void> enviar(String conteudo) async {
    final texto = conteudo.trim();
    if (texto.isEmpty) return;
    await _repo.enviar(_outroUsuarioId, texto);
    final mensagens = await _repo.mensagens(_outroUsuarioId);
    state = AsyncValue.data(mensagens);
  }

  /// Atualização silenciosa em segundo plano.
  Future<void> _poll() async {
    try {
      final mensagens = await _repo.mensagens(_outroUsuarioId);
      state = AsyncValue.data(mensagens);
      await _marcarLidasSilencioso();
    } catch (_) {
      // Mantém o estado atual; o próximo ciclo tenta de novo.
    }
  }

  Future<void> _marcarLidasSilencioso() async {
    try {
      await _repo.marcarLidas(_outroUsuarioId);
    } catch (_) {
      // Best-effort; não atrapalha a exibição das mensagens.
    }
  }
}

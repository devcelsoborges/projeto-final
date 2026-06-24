import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/avaliacao_repository.dart';
import '../domain/avaliacao.dart';

/// Estatísticas (média + total) das avaliações recebidas por um usuário.
final statsUsuarioProvider =
    FutureProvider.family<AvaliacaoStats, int>((ref, usuarioId) {
  return ref.watch(avaliacaoRepositoryProvider).statsUsuario(usuarioId);
});

/// Avaliações recebidas por um usuário.
final recebidasUsuarioProvider =
    FutureProvider.family<List<Avaliacao>, int>((ref, usuarioId) {
  return ref.watch(avaliacaoRepositoryProvider).recebidasUsuario(usuarioId);
});

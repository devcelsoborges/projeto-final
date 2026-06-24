import '../../../shared/utils/json_utils.dart';

/// Espelha `AvaliacaoDTO` do backend (somente leitura nesta fase).
class Avaliacao {
  const Avaliacao({
    required this.id,
    required this.nota,
    this.comentario,
    this.solicitacaoId,
    this.usuarioId,
    this.usuarioAvaliadoId,
    this.prestadorId,
    this.dataCriacao,
  });

  final int id;

  /// Nota de 1 a 5.
  final int nota;

  /// Comentário opcional (máx. 200 caracteres no backend).
  final String? comentario;
  final int? solicitacaoId;
  final int? usuarioId;
  final int? usuarioAvaliadoId;
  final int? prestadorId;
  final DateTime? dataCriacao;

  factory Avaliacao.fromJson(Map<String, dynamic> json) {
    return Avaliacao(
      id: asInt(json['id']) ?? 0,
      nota: asInt(json['nota']) ?? 0,
      comentario: json['comentario']?.toString(),
      solicitacaoId: asInt(json['solicitacaoId']),
      usuarioId: asInt(json['usuarioId']),
      usuarioAvaliadoId: asInt(json['usuarioAvaliadoId']),
      prestadorId: asInt(json['prestadorId']),
      dataCriacao: asDate(json['dataCriacao']),
    );
  }
}

/// Estatísticas agregadas de avaliação de um usuário/prestador.
///
/// Lê as chaves `media_avaliacao` e `total_avaliacoes` retornadas pelo
/// backend, tolerando valores nulos (padrão para zero).
class AvaliacaoStats {
  const AvaliacaoStats({required this.media, required this.total});

  final double media;
  final int total;

  bool get isEmpty => total <= 0;

  factory AvaliacaoStats.fromJson(Map<String, dynamic> json) {
    return AvaliacaoStats(
      media: asDouble(json['media_avaliacao']) ?? 0,
      total: asInt(json['total_avaliacoes']) ?? 0,
    );
  }
}

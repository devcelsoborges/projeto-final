import '../../../shared/utils/json_utils.dart';

/// Espelha `ChatConversationDTO` do backend.
class Conversa {
  const Conversa({
    required this.id,
    required this.contatoId,
    this.contatoNome,
    this.ultimaMensagem,
    this.ultimaMensagemEm,
    this.ultimaMensagemRemetenteId,
    this.naoLidas = 0,
    this.atualizadaEm,
  });

  final int id;
  final int contatoId;
  final String? contatoNome;
  final String? ultimaMensagem;
  final DateTime? ultimaMensagemEm;
  final int? ultimaMensagemRemetenteId;
  final int naoLidas;
  final DateTime? atualizadaEm;

  factory Conversa.fromJson(Map<String, dynamic> json) {
    return Conversa(
      id: asInt(json['id']) ?? 0,
      contatoId: asInt(json['contatoId']) ?? 0,
      contatoNome: json['contatoNome']?.toString(),
      ultimaMensagem: json['ultimaMensagem']?.toString(),
      ultimaMensagemEm: asDate(json['ultimaMensagemEm']),
      ultimaMensagemRemetenteId: asInt(json['ultimaMensagemRemetenteId']),
      naoLidas: asInt(json['naoLidas']) ?? 0,
      atualizadaEm: asDate(json['atualizadaEm']),
    );
  }
}

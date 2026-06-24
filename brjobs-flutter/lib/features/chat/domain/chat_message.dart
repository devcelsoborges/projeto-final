import '../../../shared/utils/json_utils.dart';

/// Espelha `ChatMessageDTO` do backend.
class ChatMessage {
  const ChatMessage({
    required this.id,
    required this.remetenteId,
    this.remetenteName,
    required this.destinatarioId,
    required this.conteudo,
    this.lido = false,
    this.criadoEm,
  });

  final int id;
  final int remetenteId;
  final String? remetenteName;
  final int destinatarioId;
  final String conteudo;
  final bool lido;
  final DateTime? criadoEm;

  factory ChatMessage.fromJson(Map<String, dynamic> json) {
    return ChatMessage(
      id: asInt(json['id']) ?? 0,
      remetenteId: asInt(json['remetenteId']) ?? 0,
      remetenteName: json['remetenteName']?.toString(),
      destinatarioId: asInt(json['destinatarioId']) ?? 0,
      conteudo: json['conteudo']?.toString() ?? '',
      lido: asBool(json['lido']),
      criadoEm: asDate(json['criadoEm']),
    );
  }
}

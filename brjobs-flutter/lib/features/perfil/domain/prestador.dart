import '../../../shared/utils/json_utils.dart';

/// Espelha `PrestadorResponseDTO` do backend.
class PrestadorResponse {
  const PrestadorResponse({
    required this.id,
    required this.usuarioId,
    this.funcao,
    this.experienciaProfissional,
    this.especialidades,
    this.descricao,
    this.ativo = true,
    this.dataCadastro,
  });

  final int id;
  final int usuarioId;
  final String? funcao;
  final String? experienciaProfissional;
  final String? especialidades;
  final String? descricao;
  final bool ativo;
  final DateTime? dataCadastro;

  factory PrestadorResponse.fromJson(Map<String, dynamic> json) {
    return PrestadorResponse(
      id: asInt(json['id']) ?? 0,
      usuarioId: asInt(json['usuarioId']) ?? 0,
      funcao: json['funcao']?.toString(),
      experienciaProfissional: json['experienciaProfissional']?.toString(),
      especialidades: json['especialidades']?.toString(),
      descricao: json['descricao']?.toString(),
      ativo: asBool(json['ativo'], fallback: true),
      dataCadastro: asDate(json['dataCadastro']),
    );
  }
}

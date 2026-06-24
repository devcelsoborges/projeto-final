import '../../../shared/utils/json_utils.dart';

/// Espelha `PublicacaoServicoDTO` do backend.
///
/// Uma publicação pode ser uma oferta de serviço (`PRESTACAO`) ou um pedido de
/// contratação (`CONTRATACAO`). O status controla se ela ainda aparece no
/// catálogo (`ATIVA`) ou foi encerrada (`ENCERRADA`).
class Publicacao {
  const Publicacao({
    required this.id,
    required this.tipoPublicacao,
    required this.titulo,
    required this.descricao,
    required this.categoria,
    this.enderecoPublicacao,
    this.cepPublicacao,
    this.cidadePublicacao,
    this.estadoPublicacao,
    this.latitude,
    this.longitude,
    this.geocodeProvider,
    this.geocodePrecision,
    this.distanceKm,
    this.preco,
    this.orcamentoMin,
    this.orcamentoMax,
    required this.status,
    required this.usuarioId,
    required this.usuarioNome,
    this.usuarioBairro,
    this.usuarioCidade,
    this.usuarioEndereco,
    this.isHighlighted = false,
    this.highlightExpiresAt,
    this.highlightPlanId,
    this.highlightPlanName,
    this.highlightPriority,
    this.dataCriacao,
  });

  final int id;

  /// `PRESTACAO` (oferta de serviço) ou `CONTRATACAO` (pedido de serviço).
  final String tipoPublicacao;
  final String titulo;
  final String descricao;
  final String categoria;
  final String? enderecoPublicacao;
  final String? cepPublicacao;
  final String? cidadePublicacao;
  final String? estadoPublicacao;
  final double? latitude;
  final double? longitude;
  final String? geocodeProvider;
  final String? geocodePrecision;
  final double? distanceKm;
  final double? preco;
  final double? orcamentoMin;
  final double? orcamentoMax;

  /// `ATIVA` ou `ENCERRADA`.
  final String status;
  final int usuarioId;
  final String usuarioNome;
  final String? usuarioBairro;
  final String? usuarioCidade;
  final String? usuarioEndereco;
  final bool isHighlighted;
  final DateTime? highlightExpiresAt;
  final int? highlightPlanId;
  final String? highlightPlanName;
  final int? highlightPriority;
  final DateTime? dataCriacao;

  /// `true` quando é uma oferta de serviço (prestador divulgando-se).
  bool get isPrestacao => tipoPublicacao.toUpperCase() == 'PRESTACAO';

  /// `true` enquanto a publicação ainda está no catálogo.
  bool get ativa => status.toUpperCase() == 'ATIVA';

  /// Rótulo amigável do tipo, em pt-BR.
  String get tipoLabel => isPrestacao ? 'Prestação de serviço' : 'Contratação';

  /// `true` quando há faixa de orçamento informada (pedido de contratação).
  bool get temOrcamento => orcamentoMin != null || orcamentoMax != null;

  factory Publicacao.fromJson(Map<String, dynamic> json) {
    return Publicacao(
      id: asInt(json['id']) ?? 0,
      tipoPublicacao: json['tipoPublicacao']?.toString() ?? 'PRESTACAO',
      titulo: json['titulo']?.toString() ?? '',
      descricao: json['descricao']?.toString() ?? '',
      categoria: json['categoria']?.toString() ?? '',
      enderecoPublicacao: json['enderecoPublicacao']?.toString(),
      cepPublicacao: json['cepPublicacao']?.toString(),
      cidadePublicacao: json['cidadePublicacao']?.toString(),
      estadoPublicacao: json['estadoPublicacao']?.toString(),
      latitude: asDouble(json['latitude']),
      longitude: asDouble(json['longitude']),
      geocodeProvider: json['geocodeProvider']?.toString(),
      geocodePrecision: json['geocodePrecision']?.toString(),
      distanceKm: asDouble(json['distanceKm']),
      preco: asDouble(json['preco']),
      orcamentoMin: asDouble(json['orcamentoMin']),
      orcamentoMax: asDouble(json['orcamentoMax']),
      status: json['status']?.toString() ?? 'ATIVA',
      usuarioId: asInt(json['usuarioId']) ?? 0,
      usuarioNome: json['usuarioNome']?.toString() ?? '',
      usuarioBairro: json['usuarioBairro']?.toString(),
      usuarioCidade: json['usuarioCidade']?.toString(),
      usuarioEndereco: json['usuarioEndereco']?.toString(),
      isHighlighted: asBool(json['isHighlighted']),
      highlightExpiresAt: asDate(json['highlightExpiresAt']),
      highlightPlanId: asInt(json['highlightPlanId']),
      highlightPlanName: json['highlightPlanName']?.toString(),
      highlightPriority: asInt(json['highlightPriority']),
      dataCriacao: asDate(json['dataCriacao']),
    );
  }
}

/// Payload de criação de publicação (`POST /api/v1/publicacoes`).
///
/// `latitude`/`longitude` e os campos de geocode ficam nulos nesta fase
/// (geocode e destaque pago via Stripe são fase 2).
class CriarPublicacao {
  const CriarPublicacao({
    required this.tipoPublicacao,
    required this.titulo,
    required this.descricao,
    required this.categoria,
    this.enderecoPublicacao,
    this.cepPublicacao,
    this.cidadePublicacao,
    this.estadoPublicacao,
    this.latitude,
    this.longitude,
    this.geocodeProvider,
    this.geocodePrecision,
    this.preco,
    this.orcamentoMin,
    this.orcamentoMax,
  });

  final String tipoPublicacao;
  final String titulo;
  final String descricao;
  final String categoria;
  final String? enderecoPublicacao;
  final String? cepPublicacao;
  final String? cidadePublicacao;
  final String? estadoPublicacao;
  final double? latitude;
  final double? longitude;
  final String? geocodeProvider;
  final String? geocodePrecision;
  final double? preco;
  final double? orcamentoMin;
  final double? orcamentoMax;

  Map<String, dynamic> toJson() => pruneNulls({
        'tipoPublicacao': tipoPublicacao,
        'titulo': titulo,
        'descricao': descricao,
        'categoria': categoria,
        'enderecoPublicacao': enderecoPublicacao,
        'cepPublicacao': cepPublicacao,
        'cidadePublicacao': cidadePublicacao,
        'estadoPublicacao': estadoPublicacao,
        'latitude': latitude,
        'longitude': longitude,
        'geocodeProvider': geocodeProvider,
        'geocodePrecision': geocodePrecision,
        'preco': preco,
        'orcamentoMin': orcamentoMin,
        'orcamentoMax': orcamentoMax,
      });
}

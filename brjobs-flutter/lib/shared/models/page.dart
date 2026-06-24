import '../utils/json_utils.dart';

/// Espelha a estrutura `Page<T>` do Spring Data (paginação).
class Page<T> {
  const Page({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.number,
    required this.size,
    required this.isLast,
  });

  final List<T> content;
  final int totalElements;
  final int totalPages;

  /// Índice da página atual (base 0 no backend de publicações).
  final int number;
  final int size;
  final bool isLast;

  bool get isEmpty => content.isEmpty;

  factory Page.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) itemFromJson,
  ) {
    final rawContent = (json['content'] as List?) ?? const [];
    final items = rawContent
        .whereType<Map>()
        .map((e) => itemFromJson(e.cast<String, dynamic>()))
        .toList();
    final totalPages = asInt(json['totalPages']) ?? 1;
    final number = asInt(json['number']) ?? 0;
    return Page<T>(
      content: items,
      totalElements: asInt(json['totalElements']) ?? items.length,
      totalPages: totalPages,
      number: number,
      size: asInt(json['size']) ?? items.length,
      isLast: asBool(json['last'], fallback: number + 1 >= totalPages),
    );
  }
}

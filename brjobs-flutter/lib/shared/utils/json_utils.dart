/// Helpers de parsing tolerantes para conversão de JSON da API.
library;

int? asInt(dynamic v) {
  if (v == null) return null;
  if (v is int) return v;
  if (v is num) return v.toInt();
  return int.tryParse(v.toString());
}

double? asDouble(dynamic v) {
  if (v == null) return null;
  if (v is double) return v;
  if (v is num) return v.toDouble();
  return double.tryParse(v.toString());
}

bool asBool(dynamic v, {bool fallback = false}) {
  if (v == null) return fallback;
  if (v is bool) return v;
  final s = v.toString().toLowerCase();
  return s == 'true' || s == '1';
}

/// Faz parse de `LocalDate` (yyyy-MM-dd) ou `LocalDateTime` (ISO-8601).
DateTime? asDate(dynamic v) {
  if (v == null) return null;
  if (v is DateTime) return v;
  return DateTime.tryParse(v.toString());
}

/// Formata um [DateTime] como `yyyy-MM-dd` (formato `LocalDate` do backend).
String? toLocalDate(DateTime? d) {
  if (d == null) return null;
  final y = d.year.toString().padLeft(4, '0');
  final m = d.month.toString().padLeft(2, '0');
  final day = d.day.toString().padLeft(2, '0');
  return '$y-$m-$day';
}

/// Remove chaves com valor nulo de um mapa (para PUT/POST mais limpos).
Map<String, dynamic> pruneNulls(Map<String, dynamic> map) {
  map.removeWhere((_, value) => value == null);
  return map;
}

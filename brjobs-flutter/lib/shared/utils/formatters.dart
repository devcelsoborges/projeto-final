/// Formatadores pt-BR sem dependência de dados de locale (determinísticos).
class Fmt {
  Fmt._();

  /// `1234.5` -> `R$ 1.234,50`
  static String moeda(num? valor) {
    final v = (valor ?? 0).toDouble();
    final negativo = v < 0;
    final fixed = v.abs().toStringAsFixed(2);
    final partes = fixed.split('.');
    final inteiro = partes[0];
    final centavos = partes[1];
    final buffer = StringBuffer();
    for (var i = 0; i < inteiro.length; i++) {
      if (i > 0 && (inteiro.length - i) % 3 == 0) buffer.write('.');
      buffer.write(inteiro[i]);
    }
    return '${negativo ? '-' : ''}R\$ $buffer,$centavos';
  }

  /// `DateTime` -> `dd/MM/yyyy`
  static String data(DateTime? d) {
    if (d == null) return '';
    final dd = d.day.toString().padLeft(2, '0');
    final mm = d.month.toString().padLeft(2, '0');
    return '$dd/$mm/${d.year}';
  }

  /// `DateTime` -> `dd/MM HH:mm`
  static String dataHora(DateTime? d) {
    if (d == null) return '';
    final dd = d.day.toString().padLeft(2, '0');
    final mm = d.month.toString().padLeft(2, '0');
    final hh = d.hour.toString().padLeft(2, '0');
    final min = d.minute.toString().padLeft(2, '0');
    return '$dd/$mm $hh:$min';
  }

  /// Tempo relativo curto: `agora`, `há 5 min`, `há 2 h`, `há 3 d`.
  static String relativo(DateTime? d, {DateTime? agora}) {
    if (d == null) return '';
    final now = agora ?? DateTime.now();
    final diff = now.difference(d);
    if (diff.inMinutes < 1) return 'agora';
    if (diff.inMinutes < 60) return 'há ${diff.inMinutes} min';
    if (diff.inHours < 24) return 'há ${diff.inHours} h';
    if (diff.inDays < 7) return 'há ${diff.inDays} d';
    return data(d);
  }

  static String distancia(double? km) {
    if (km == null) return '';
    if (km < 1) return '${(km * 1000).round()} m';
    return '${km.toStringAsFixed(1).replaceAll('.', ',')} km';
  }
}

/// Tipo de usuário no backend (`TipoUsuario`).
enum TipoUsuario {
  contratante('CONTRATANTE'),
  prestador('PRESTADOR');

  const TipoUsuario(this.wire);

  /// Valor exato esperado/retornado pela API.
  final String wire;

  bool get isPrestador => this == TipoUsuario.prestador;
  bool get isContratante => this == TipoUsuario.contratante;

  static TipoUsuario? fromWire(String? value) {
    if (value == null) return null;
    final v = value.toUpperCase();
    for (final t in TipoUsuario.values) {
      if (t.wire == v) return t;
    }
    return null;
  }

  String get label => switch (this) {
        TipoUsuario.contratante => 'Contratante',
        TipoUsuario.prestador => 'Prestador de serviço',
      };
}

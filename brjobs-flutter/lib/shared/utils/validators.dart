/// Validações reutilizáveis para formulários (combinam com as regras do
/// backend: e-mail válido, nome 3-150, senha obrigatória, etc.).
class Validators {
  Validators._();

  static final _emailRe =
      RegExp(r'^[\w.+-]+@[\w-]+\.[\w.-]+$', caseSensitive: false);

  static String? obrigatorio(String? v, {String campo = 'Este campo'}) {
    if (v == null || v.trim().isEmpty) return '$campo é obrigatório.';
    return null;
  }

  static String? email(String? v) {
    if (v == null || v.trim().isEmpty) return 'Informe o e-mail.';
    if (!_emailRe.hasMatch(v.trim())) return 'E-mail inválido.';
    return null;
  }

  static String? nome(String? v) {
    final base = obrigatorio(v, campo: 'O nome');
    if (base != null) return base;
    final len = v!.trim().length;
    if (len < 3) return 'O nome deve ter ao menos 3 caracteres.';
    if (len > 150) return 'O nome deve ter no máximo 150 caracteres.';
    return null;
  }

  static String? senha(String? v, {int min = 8}) {
    if (v == null || v.isEmpty) return 'Informe a senha.';
    if (v.length < min) return 'A senha deve ter ao menos $min caracteres.';
    return null;
  }

  static String? confirmacaoSenha(String? v, String senha) {
    if (v == null || v.isEmpty) return 'Confirme a senha.';
    if (v != senha) return 'As senhas não conferem.';
    return null;
  }

  static String? tamanhoMaximo(String? v, int max, {String campo = 'O campo'}) {
    if (v != null && v.length > max) {
      return '$campo deve ter no máximo $max caracteres.';
    }
    return null;
  }

  static String? codigo6(String? v) {
    if (v == null || v.trim().length != 6) return 'O código tem 6 dígitos.';
    return null;
  }
}

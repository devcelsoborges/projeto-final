/// Caminhos da API, extraídos diretamente dos controllers do backend
/// (`brjobs-java`). Mantenha sincronizado com o backend de produção.
///
/// Convenção observada no backend:
/// - Fluxo de sessão por cookie (usado pelo app): prefixo `/api/v1/auth/...`
/// - Recuperação de senha: prefixo `/api/auth/forgot-password/...`
/// - Recursos: `/api/v1/...` (com tenant) e `/api/...` (alguns públicos)
class Api {
  Api._();

  // ---- Auth (sessão por cookie) ----
  static const csrf = '/api/v1/auth/csrf';
  static const login = '/api/v1/auth/login';
  static const register = '/api/v1/auth/register'; // contratante
  static const refresh = '/api/v1/auth/refresh';
  static const logout = '/api/v1/auth/logout';
  static const me = '/api/v1/auth/me';
  static const socialGoogle = '/api/v1/auth/social/google';
  static const socialFacebook = '/api/v1/auth/social/facebook';

  // ---- Recuperação de senha ----
  static const forgotPasswordRequest = '/api/auth/forgot-password/request';
  static const forgotPasswordVerify = '/api/auth/forgot-password/verify';
  static const forgotPasswordReset = '/api/auth/forgot-password/reset';

  // ---- Usuários ----
  static const usuarios = '/api/usuarios';
  static const cadastroContratante = '/api/usuarios/contratante';
  static const cadastroPrestador = '/api/usuarios/prestador';
  static String usuario(int id) => '/api/usuarios/$id';
  static String usuarioPorEmail(String email) => '/api/usuarios/email/$email';
  static String usuarioSenha(int id) => '/api/usuarios/$id/senha';
  static String usuarioFoto(int id) => '/api/usuarios/$id/foto';

  // ---- Prestadores ----
  static const prestadores = '/api/prestadores';
  static String prestador(int id) => '/api/prestadores/$id';
  static String prestadorPorUsuario(int usuarioId) =>
      '/api/prestadores/usuario/$usuarioId';
  static String prestadorCriar(int usuarioId) => '/api/prestadores/$usuarioId';
  static String prestadorCurriculo(int id) => '/api/prestadores/$id/curriculo';

  // ---- Publicações de serviço ----
  static const publicacoes = '/api/v1/publicacoes';
  static const publicacoesPaginado = '/api/v1/publicacoes/paginado';
  static const minhasPublicacoes = '/api/v1/publicacoes/minhas';
  static String publicacao(int id) => '/api/v1/publicacoes/$id';

  // ---- Serviços ----
  static const servicos = '/api/v1/servicos';
  static const servicosTodos = '/api/v1/servicos/todos';
  static String servico(int id) => '/api/v1/servicos/$id';

  // ---- Chat ----
  static const chatEnviar = '/api/v1/chat/enviar';
  static const chatConversas = '/api/v1/chat/conversas';
  static const chatNaoLidas = '/api/v1/chat/nao-lidas';
  static String chatConversa(int outroUsuarioId) =>
      '/api/v1/chat/conversa/$outroUsuarioId';
  static String chatMarcarLida(int mensagemId) =>
      '/api/v1/chat/marcar-lida/$mensagemId';
  static String chatMarcarLidas(int outroUsuarioId) =>
      '/api/v1/chat/marcar-lidas/$outroUsuarioId';

  // ---- Avaliações ----
  static const avaliacoesV1 = '/api/avaliacoes/v1';
  static const avaliacoesV1Recebidas = '/api/avaliacoes/v1/recebidas';
  static String avaliacoesV1PrestadorStats(int prestadorId) =>
      '/api/avaliacoes/v1/prestador/$prestadorId/stats';
  static String avaliacoesV1UsuarioStats(int usuarioId) =>
      '/api/avaliacoes/v1/usuario/$usuarioId/stats';
  static String avaliacoesPrestador(int prestadorId) =>
      '/api/avaliacoes/prestador/$prestadorId';
  static String avaliacoesUsuarioRecebidas(int usuarioId) =>
      '/api/avaliacoes/usuario/$usuarioId/recebidas';

  // ---- Config pública ----
  static const configPublica = '/api/v1/config/public';
}

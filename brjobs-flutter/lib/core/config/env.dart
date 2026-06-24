/// Configuração de ambiente do app.
///
/// O backend de produção roda em `https://api.brjobs.com.br` (sem context-path;
/// as rotas começam em `/api/...` e `/api/v1/...`).
///
/// Para apontar para um backend local em desenvolvimento, rode o app com:
///   flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
/// (No emulador Android, `localhost` da máquina host é acessível via 10.0.2.2.)
class Env {
  Env._();

  /// URL base da API. Default = produção.
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'https://api.brjobs.com.br',
  );

  /// Client ID OAuth Web do Google (usado como `serverClientId` no mobile para
  /// obter um ID Token que o backend valida). É o mesmo ID usado pelo site.
  static const String googleServerClientId = String.fromEnvironment(
    'GOOGLE_SERVER_CLIENT_ID',
    defaultValue:
        '562205988451-gu1vcc47c8nffla5eabhk6o3p00k8s1n.apps.googleusercontent.com',
  );

  // Parâmetros de chat (espelham environment.ts do site).
  static const int chatPollIntervalMs = 15000;
  static const int chatMaxMessageLength = 500;
  static const int chatHeaderBadgeMax = 99;
}

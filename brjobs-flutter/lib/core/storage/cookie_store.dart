import 'package:cookie_jar/cookie_jar.dart';
import 'package:path_provider/path_provider.dart';

/// Cookie jar persistente em disco.
///
/// É o coração da autenticação por sessão: os cookies httpOnly
/// `ACCESS_TOKEN` (2h) e `REFRESH_TOKEN` (7 dias) emitidos por
/// `/api/v1/auth/login` são guardados aqui e reenviados automaticamente
/// pelo Dio em cada requisição — exatamente como o navegador faz no site.
///
/// O cookie `XSRF-TOKEN` (não-httpOnly) também é guardado e lido para montar
/// o header `X-XSRF-TOKEN` nas mutações.
class CookieStore {
  CookieStore._(this.jar);

  final PersistCookieJar jar;

  static Future<CookieStore> create() async {
    final dir = await getApplicationSupportDirectory();
    final jar = PersistCookieJar(
      ignoreExpires: false,
      storage: FileStorage('${dir.path}/.brjobs_cookies'),
    );
    return CookieStore._(jar);
  }

  Future<void> clear() => jar.deleteAll();
}

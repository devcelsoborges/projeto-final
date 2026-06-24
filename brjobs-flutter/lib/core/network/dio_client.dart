import 'package:dio/dio.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';

import '../config/env.dart';
import '../storage/cookie_store.dart';
import 'api_endpoints.dart';

/// Cliente HTTP central. Reproduz fielmente o comportamento do site Angular:
///
/// 1. **Sessão por cookie** — os cookies `ACCESS_TOKEN`/`REFRESH_TOKEN` são
///    persistidos e reenviados automaticamente ([CookieManager]).
/// 2. **CSRF** — antes de qualquer mutação (POST/PUT/PATCH/DELETE) garante o
///    cookie `XSRF-TOKEN` e injeta o header `X-XSRF-TOKEN`.
/// 3. **Refresh transparente** — em um `401`, chama `/api/v1/auth/refresh` uma
///    única vez (single-flight) e repete a requisição original. Se o refresh
///    falhar, dispara [onSessionExpired].
class DioClient {
  DioClient._(this.dio, this._authDio, this._cookieStore);

  final Dio dio;

  /// Dio "cru" (apenas cookie manager) usado para `/csrf` e `/refresh`,
  /// evitando recursão de interceptors.
  final Dio _authDio;
  final CookieStore _cookieStore;

  /// Chamado quando a sessão expira em definitivo (refresh falhou).
  void Function()? onSessionExpired;

  Future<bool>? _refreshInFlight;

  static const _xsrfCookie = 'XSRF-TOKEN';
  static const _xsrfHeader = 'X-XSRF-TOKEN';

  static Future<DioClient> create(CookieStore cookieStore) async {
    BaseOptions options() => BaseOptions(
          baseUrl: Env.apiBaseUrl,
          connectTimeout: const Duration(seconds: 20),
          receiveTimeout: const Duration(seconds: 30),
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
          },
          // Deixa o Dio lançar em >= 400 para o interceptor de erro tratar.
          validateStatus: (s) => s != null && s < 400,
        );

    final cookieManager = CookieManager(cookieStore.jar);

    final authDio = Dio(options())..interceptors.add(cookieManager);
    final dio = Dio(options())..interceptors.add(cookieManager);

    final client = DioClient._(dio, authDio, cookieStore);
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: client._onRequest,
        onError: client._onError,
      ),
    );
    return client;
  }

  bool _isMutation(String method) {
    final m = method.toUpperCase();
    return m == 'POST' || m == 'PUT' || m == 'PATCH' || m == 'DELETE';
  }

  /// Endpoints isentos de CSRF no backend.
  bool _csrfExempt(String path) =>
      path.startsWith('/api/auth/') ||
      path == Api.login ||
      path == Api.csrf ||
      path.startsWith('/api/v1/auth/social/');

  Future<void> _onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    if (_isMutation(options.method) && !_csrfExempt(options.path)) {
      final token = await _ensureCsrfToken();
      if (token != null) options.headers[_xsrfHeader] = token;
    }
    handler.next(options);
  }

  /// Lê o cookie `XSRF-TOKEN`; se ausente, busca `/csrf` para emiti-lo.
  Future<String?> _ensureCsrfToken() async {
    var token = await _readXsrfCookie();
    if (token != null) return token;
    try {
      await _authDio.get(Api.csrf);
    } catch (_) {
      // Ignora; segue sem CSRF (a chamada pode falhar com 403 e será tratada).
    }
    token = await _readXsrfCookie();
    return token;
  }

  Future<String?> _readXsrfCookie() async {
    final cookies =
        await _cookieStore.jar.loadForRequest(Uri.parse(Env.apiBaseUrl));
    for (final c in cookies) {
      if (c.name == _xsrfCookie) return c.value;
    }
    return null;
  }

  Future<void> _onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final response = err.response;
    final request = err.requestOptions;

    final canRetry = response?.statusCode == 401 &&
        request.extra['__retried'] != true &&
        !_isAuthEndpoint(request.path);

    if (!canRetry) {
      handler.next(err);
      return;
    }

    final refreshed = await _refresh();
    if (!refreshed) {
      onSessionExpired?.call();
      handler.next(err);
      return;
    }

    // Repete a requisição original uma única vez.
    try {
      request.extra['__retried'] = true;
      final clone = await dio.fetch(request);
      handler.resolve(clone);
    } on DioException catch (e) {
      handler.next(e);
    }
  }

  bool _isAuthEndpoint(String path) =>
      path == Api.login ||
      path == Api.refresh ||
      path == Api.csrf ||
      path == Api.me ||
      path.startsWith('/api/v1/auth/social/');

  /// Refresh com single-flight: várias requisições que tomam 401 ao mesmo
  /// tempo aguardam o mesmo refresh.
  Future<bool> _refresh() {
    return _refreshInFlight ??= _doRefresh().whenComplete(() {
      _refreshInFlight = null;
    });
  }

  Future<bool> _doRefresh() async {
    try {
      // CSRF também é exigido no refresh; garante o header.
      final token = await _ensureCsrfToken();
      final res = await _authDio.post(
        Api.refresh,
        options: Options(
          headers: {if (token != null) _xsrfHeader: token},
          validateStatus: (s) => s != null && s < 400,
        ),
      );
      return res.statusCode != null && res.statusCode! < 400;
    } catch (_) {
      return false;
    }
  }

  Future<void> clearSession() => _cookieStore.clear();
}

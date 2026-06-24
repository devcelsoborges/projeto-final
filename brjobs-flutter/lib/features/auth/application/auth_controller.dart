import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/providers/app_providers.dart';
import '../data/auth_repository.dart';
import '../domain/cadastro.dart';
import '../domain/usuario.dart';

enum AuthStatus { unknown, authenticated, unauthenticated }

class AuthState {
  const AuthState(this.status, [this.user]);

  final AuthStatus status;
  final Usuario? user;

  bool get isAuthenticated => status == AuthStatus.authenticated;
  bool get isUnknown => status == AuthStatus.unknown;

  const AuthState.unknown() : this(AuthStatus.unknown);
  const AuthState.unauthenticated() : this(AuthStatus.unauthenticated);
}

final authControllerProvider =
    NotifierProvider<AuthController, AuthState>(AuthController.new);

class AuthController extends Notifier<AuthState> {
  AuthRepository get _repo => ref.read(authRepositoryProvider);

  @override
  AuthState build() {
    // Quando o refresh falha em definitivo, derruba a sessão.
    ref.read(dioClientProvider).onSessionExpired = () {
      state = const AuthState.unauthenticated();
    };
    return const AuthState.unknown();
  }

  /// Tenta restaurar a sessão a partir dos cookies persistidos.
  Future<void> bootstrap() async {
    try {
      await _repo.primeCsrf();
      final user = await _repo.me();
      state = AuthState(AuthStatus.authenticated, user);
    } catch (_) {
      state = const AuthState.unauthenticated();
    }
  }

  Future<void> login(String email, String senha) async {
    final user = await _repo.login(email, senha);
    state = AuthState(AuthStatus.authenticated, user);
  }

  Future<void> registrarContratante(CadastroContratante dto) async {
    final user = await _repo.registrarContratante(dto);
    state = AuthState(AuthStatus.authenticated, user);
  }

  Future<void> registrarPrestador(CadastroPrestador dto) async {
    final user = await _repo.registrarPrestador(dto);
    state = AuthState(AuthStatus.authenticated, user);
  }

  Future<void> loginGoogle(String idToken) async {
    final user = await _repo.loginGoogle(idToken);
    state = AuthState(AuthStatus.authenticated, user);
  }

  /// Atualiza o usuário em memória (após editar o perfil).
  void setUser(Usuario user) {
    state = AuthState(AuthStatus.authenticated, user);
  }

  Future<void> logout() async {
    await _repo.logout();
    state = const AuthState.unauthenticated();
  }
}

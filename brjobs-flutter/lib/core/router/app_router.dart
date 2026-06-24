import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/application/auth_controller.dart';
import '../../features/auth/presentation/forgot_password_screen.dart';
import '../../features/auth/presentation/login_screen.dart';
import '../../features/auth/presentation/register_screen.dart';
import '../../features/auth/presentation/splash_screen.dart';
import '../../features/chat/presentation/conversa_screen.dart';
import '../../features/chat/presentation/conversas_screen.dart';
import '../../features/perfil/presentation/meu_perfil_screen.dart';
import '../../features/perfil/presentation/perfil_publico_screen.dart';
import '../../features/publicacoes/presentation/home_screen.dart';
import '../../features/publicacoes/presentation/minhas_publicacoes_screen.dart';
import '../../features/publicacoes/presentation/publicacao_detail_screen.dart';
import '../../features/publicacoes/presentation/publicar_screen.dart';
import 'scaffold_with_nav_bar.dart';

/// Rotas nomeadas (centralizadas para evitar strings soltas).
class Routes {
  Routes._();
  static const splash = '/splash';
  static const login = '/login';
  static const register = '/register';
  static const forgotPassword = '/forgot-password';
  static const home = '/';
  static const publicar = '/publicar';
  static const conversas = '/conversas';
  static const perfil = '/perfil';
  static const minhasPublicacoes = '/minhas-publicacoes';
  static String publicacao(int id) => '/publicacoes/$id';
  static String perfilPublico(int id) => '/usuarios/$id';
  static String conversa(int outroUsuarioId) => '/conversa/$outroUsuarioId';
}

const _protectedPrefixes = [
  Routes.publicar,
  Routes.conversas,
  Routes.perfil,
  Routes.minhasPublicacoes,
  '/conversa',
];

final _rootNavigatorKey = GlobalKey<NavigatorState>();

final routerProvider = Provider<GoRouter>((ref) {
  // Notifica o GoRouter quando o estado de auth muda.
  final refresh = ValueNotifier<int>(0);
  ref.listen(authControllerProvider, (_, __) => refresh.value++);
  ref.onDispose(refresh.dispose);

  return GoRouter(
    navigatorKey: _rootNavigatorKey,
    initialLocation: Routes.splash,
    refreshListenable: refresh,
    redirect: (context, state) {
      final status = ref.read(authControllerProvider).status;
      final loc = state.matchedLocation;
      final atSplash = loc == Routes.splash;

      if (status == AuthStatus.unknown) {
        return atSplash ? null : Routes.splash;
      }

      if (atSplash) return Routes.home;

      final isAuthRoute = loc == Routes.login ||
          loc == Routes.register ||
          loc == Routes.forgotPassword;
      final isProtected =
          _protectedPrefixes.any((p) => loc == p || loc.startsWith('$p/'));
      final authed = status == AuthStatus.authenticated;

      if (!authed && isProtected) return Routes.login;
      if (authed && isAuthRoute) return Routes.home;
      return null;
    },
    routes: [
      GoRoute(
        path: Routes.splash,
        builder: (_, __) => const SplashScreen(),
      ),
      GoRoute(
        path: Routes.login,
        builder: (_, __) => const LoginScreen(),
      ),
      GoRoute(
        path: Routes.register,
        builder: (_, __) => const RegisterScreen(),
      ),
      GoRoute(
        path: Routes.forgotPassword,
        builder: (_, __) => const ForgotPasswordScreen(),
      ),
      // Rotas empilhadas sobre a casca:
      GoRoute(
        path: '/publicacoes/:id',
        parentNavigatorKey: _rootNavigatorKey,
        builder: (_, state) => PublicacaoDetailScreen(
          publicacaoId: int.tryParse(state.pathParameters['id'] ?? '') ?? 0,
        ),
      ),
      GoRoute(
        path: '/usuarios/:id',
        parentNavigatorKey: _rootNavigatorKey,
        builder: (_, state) => PerfilPublicoScreen(
          usuarioId: int.tryParse(state.pathParameters['id'] ?? '') ?? 0,
        ),
      ),
      GoRoute(
        path: '/conversa/:id',
        parentNavigatorKey: _rootNavigatorKey,
        builder: (_, state) => ConversaScreen(
          outroUsuarioId: int.tryParse(state.pathParameters['id'] ?? '') ?? 0,
          nomeContato: state.uri.queryParameters['nome'],
        ),
      ),
      GoRoute(
        path: Routes.minhasPublicacoes,
        parentNavigatorKey: _rootNavigatorKey,
        builder: (_, __) => const MinhasPublicacoesScreen(),
      ),
      // Casca com bottom navigation (4 seções).
      StatefulShellRoute.indexedStack(
        builder: (_, __, navigationShell) =>
            ScaffoldWithNavBar(navigationShell: navigationShell),
        branches: [
          StatefulShellBranch(routes: [
            GoRoute(path: Routes.home, builder: (_, __) => const HomeScreen()),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(
                path: Routes.publicar,
                builder: (_, __) => const PublicarScreen()),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(
                path: Routes.conversas,
                builder: (_, __) => const ConversasScreen()),
          ]),
          StatefulShellBranch(routes: [
            GoRoute(
                path: Routes.perfil,
                builder: (_, __) => const MeuPerfilScreen()),
          ]),
        ],
      ),
    ],
  );
});

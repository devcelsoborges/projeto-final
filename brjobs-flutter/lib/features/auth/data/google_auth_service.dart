import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_sign_in/google_sign_in.dart';

import '../../../core/config/env.dart';

final googleAuthServiceProvider =
    Provider<GoogleAuthService>((ref) => GoogleAuthService());

/// Encapsula o `google_sign_in`. Retorna o **ID Token** que o backend valida
/// em `/api/v1/auth/social/google`.
///
/// Pré-requisito de produção: no Google Cloud Console é preciso cadastrar um
/// OAuth Client do tipo *Android* com o package `br.com.brjobs.mobile` e a
/// SHA-1 da chave de assinatura. O `serverClientId` (client Web) garante que o
/// ID Token tenha a audiência que o backend espera.
class GoogleAuthService {
  final GoogleSignIn _googleSignIn = GoogleSignIn(
    serverClientId: Env.googleServerClientId,
    scopes: const ['email', 'profile'],
  );

  /// Retorna o ID Token, ou `null` se o usuário cancelar.
  Future<String?> signInGetIdToken() async {
    final account = await _googleSignIn.signIn();
    if (account == null) return null; // cancelado
    final auth = await account.authentication;
    return auth.idToken;
  }

  Future<void> signOut() => _googleSignIn.signOut();
}

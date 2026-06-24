# BRJobs Mobile (Flutter)

App mobile (Android/iOS) do **BRJobs**, integrado ao backend de produção
`https://api.brjobs.com.br` — o mesmo consumido pelo site Angular.

## Stack

- **Flutter** + **Dart 3** (Material 3)
- **Riverpod** — gerência de estado
- **Dio** + **cookie_jar** — HTTP com **sessão por cookie** (igual ao site):
  cookies `ACCESS_TOKEN`/`REFRESH_TOKEN` persistidos, header CSRF `X-XSRF-TOKEN`
  e **refresh automático** no `401`.
- **go_router** — navegação com guarda de autenticação

## Arquitetura (por feature)

```
lib/
  core/            # infraestrutura
    config/        # Env (URL base, client id Google)
    network/       # DioClient (cookies+CSRF+refresh), Api (rotas), ApiException, apiGuard
    storage/       # CookieStore (PersistCookieJar)
    providers/     # dioProvider, dioClientProvider
    router/        # GoRouter + bottom nav (Routes)
    theme/
  shared/          # utils (validators, Fmt), widgets (state views, feedback), models (Page<T>)
  features/
    auth/          # login, registro, recuperação de senha, login Google, sessão
    publicacoes/   # busca/catálogo, detalhe, publicar, minhas publicações
    perfil/        # meu perfil (editar + foto), perfil público
    chat/          # conversas + thread (polling 15s, anti-spam 429)
    avaliacoes/    # leitura: média + comentários recebidos
```

Cada feature: `domain/` (modelos) → `data/` (repositórios HTTP) →
`application/` (controllers Riverpod) → `presentation/` (telas).

## Rodando

> Pré-requisitos: Flutter SDK (`C:\dev\flutter`), e para Android o SDK +
> JDK 17 (este projeto **não** usa o Java 25 do sistema porque o Gradle não o
> suporta — o Flutter foi apontado para `C:\dev\jdk-17`).

```bash
flutter pub get

# Produção (default):
flutter run

# Backend local (emulador Android → host via 10.0.2.2):
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080

# Gerar APK de debug:
flutter build apk --debug
```

## Login social (Google)

O código já está pronto (`features/auth/data/google_auth_service.dart`),
usando o **mesmo client Web** do site como `serverClientId`. Para funcionar em
produção é preciso, no **Google Cloud Console**, criar um **OAuth Client tipo
Android** com:
- Package name: `br.com.brjobs.mobile`
- SHA-1 da chave de assinatura (debug: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`)

Sem esse cadastro, o login por e-mail/senha funciona normalmente; o botão do
Google retornará erro de configuração.

## Escopo

**MVP (implementado):** autenticação (e-mail/senha, Google, recuperação de
senha), catálogo/busca de publicações com paginação, detalhe, publicar e
gerenciar minhas publicações, perfil (editar + foto), chat com polling,
avaliações (somente leitura).

**Fase 2 (não incluído):** fluxo de solicitação/contratação, **criação de
avaliação** (depende de `solicitacaoId`), relatório de ganhos, destaque de
publicação com checkout **Stripe**, geocode no publicar, notificações push.

# Briefing Técnico — Atualização JDK 25, Spring, Angular LTS e autenticação nativa

> **Versão:** 0.1
> **Status:** Rascunho
> **Gerado em:** 2026-06-09
> **Baseado em:**
>   - [`../discovery.md`](../discovery.md) — discovery de 2026-06-09

---

## 1. Contexto e Problema

O BrJobs usa backend Spring Boot 3.3.5 com Java 17 e frontend Angular 20.3.x. A base atual funciona, mas está atrás do alvo técnico definido: JDK 25 LTS, Spring Boot 4.x e Angular 21 LTS.

Além do upgrade, o fluxo de autenticação atual mistura JWT em storage acessível pelo navegador e login social amplo. Esse desenho aumenta exposição em caso de XSS, dificulta revogação real de sessão e mantém integrações sociais fora do escopo desejado.

Se não resolvermos, o projeto continua com dívida de versão, autenticação menos segura e maior custo para evoluir features autenticadas.

---

## 2. Solução Proposta

Atualizar a stack base e substituir a autenticação atual por uma auth nativa própria com cookies seguros, refresh token rotativo e auditoria.

Escopo desta versão:

- migrar backend para JDK 25 LTS;
- migrar Spring Boot para 4.0.x estável;
- corrigir conflitos em Spring Security, JPA/Hibernate, SpringDoc, JWT, Redis, Stripe, Lombok e plugins Maven;
- migrar frontend para Angular 21 LTS e dependências compatíveis;
- trocar armazenamento de token em `localStorage` por cookies `HttpOnly`, `Secure`, `SameSite`;
- implementar access token de 2h e refresh token rotativo de 7d;
- manter login por e-mail/senha, Google e Facebook;
- remover Apple login;
- preservar papéis `CONTRATANTE` e `PRESTADOR`;
- adicionar auditoria e métricas de autenticação.

Não será construído nesta versão:

- MFA;
- compatibilidade com tokens antigos;
- migração de usuários/senhas antigas;
- Apple login;
- reescrita de domínios fora de auth e compatibilidade do upgrade.

Retrocompatibilidade:

- rollout será big-bang;
- dados antigos podem quebrar;
- não há usuários em produção;
- APIs autenticadas passam a depender de cookies e CSRF/XSRF.

---

## 3. Personas e Papéis Afetados

| Papel | Ação que realiza | Impacto da feature |
|-------|------------------|--------------------|
| Visitante | Faz login, registro ou login social | Direto — novo fluxo de autenticação |
| Contratante | Usa áreas autenticadas, publicações e contato | Direto — nova sessão com mesmo papel |
| Prestador | Usa perfil, publicações e áreas autenticadas | Direto — nova sessão com mesmo papel |
| Engenharia Backend | Atualiza Spring/JDK, security e persistência | Direto — alto impacto técnico |
| Engenharia Frontend | Atualiza Angular e fluxo de auth | Direto — remove dependência de token no storage |
| Operação/Segurança | Monitora auth, falhas e eventos críticos | Direto — novos logs, métricas e alertas |
| QA | Valida upgrade e regressão autenticada | Direto — precisa testar fluxo completo |

---

## 4. Premissas, Restrições e Decisões Tomadas

- **Projeto brownfield:** manter arquitetura atual do BrJobs.
- **Backend alvo:** JDK 25 LTS + Spring Boot 4.0.x.
- **Frontend alvo:** Angular 21 LTS.
- **Auth alvo:** cookies seguros + JWT curto + refresh rotativo server-side.
- **Access token:** 2 horas.
- **Refresh token:** 7 dias.
- **Social login:** manter Google e Facebook.
- **Apple login:** remover.
- **RBAC:** preservar `CONTRATANTE` e `PRESTADOR`.
- **Rollout:** big-bang.
- **Dados antigos:** podem ser quebrados/removidos.
- **SLA recomendado:** login p95 <= 1s; refresh p95 <= 500ms.
- **Segurança:** nenhum token sensível deve ficar acessível via JavaScript.
- **CSRF:** obrigatório para requests mutáveis quando auth usar cookies.

---

## 5. Arquitetura e Fluxos

### 5.1 Fluxo principal — login nativo

```text
[Usuário]
   -> POST /api/v1/auth/login { email, senha } com XSRF
   -> AuthController
   -> AuthService valida credenciais
   -> PasswordEncoder compara hash
   -> AccessTokenService gera JWT 2h
   -> RefreshTokenService cria token opaco/hash 7d
   -> BE grava refresh token server-side
   -> BE seta cookies HttpOnly/Secure/SameSite
   -> FE chama /api/v1/auth/me para hidratar estado
```

### 5.2 Fluxo refresh rotativo

```text
[Browser]
   -> POST /api/v1/auth/refresh com cookie refresh
   -> BE localiza hash do refresh token
   -> se válido e não revogado:
        revoga token atual
        emite novo access token
        emite novo refresh token
        seta novos cookies
   -> se token reutilizado:
        revoga família/sessão
        registra auth_refresh_reuse_detected
        retorna 401
```

### 5.3 Fluxo social Google/Facebook

```text
[FE]
   -> recebe credential/access token do provider
   -> POST /api/v1/auth/social/{google|facebook}
   -> BE valida token com provider
   -> BE cria/atualiza usuário local
   -> BE emite mesma sessão nativa
   -> cookies seguros + /me
```

### 5.4 Modelo de dados

| Campo | Entidade | Tipo | Obrigatório | Descrição |
|-------|----------|------|-------------|-----------|
| id | usuarios | BIGINT | Sim | Usuário local |
| email | usuarios | VARCHAR unique | Sim | Identificador de login |
| senha_hash | usuarios | VARCHAR | Não | Hash para login nativo; nulo para social-only se permitido |
| tipo_usuario | usuarios | ENUM | Sim | `CONTRATANTE` ou `PRESTADOR` |
| auth_provider | usuarios | VARCHAR | Não | Provider principal ou `LOCAL` |
| email_verificado | usuarios | BOOLEAN | Sim | Estado de verificação |
| id | auth_refresh_tokens | BIGINT | Sim | Refresh token persistido |
| usuario_id | auth_refresh_tokens | BIGINT FK | Sim | Dono do token |
| token_hash | auth_refresh_tokens | VARCHAR unique | Sim | Hash do refresh token opaco |
| family_id | auth_refresh_tokens | UUID | Sim | Família para rotação/reuse detection |
| expires_at | auth_refresh_tokens | TIMESTAMP | Sim | Expiração em 7 dias |
| revoked_at | auth_refresh_tokens | TIMESTAMP | Não | Revogação |
| replaced_by_token_id | auth_refresh_tokens | BIGINT FK | Não | Token emitido na rotação |
| created_ip | auth_refresh_tokens | VARCHAR | Não | IP de criação, mascarável |
| user_agent_hash | auth_refresh_tokens | VARCHAR | Não | Hash do user-agent |
| id | auth_audit_events | BIGINT | Sim | Evento auditável |
| usuario_id | auth_audit_events | BIGINT | Não | Usuário relacionado, se houver |
| event_type | auth_audit_events | VARCHAR | Sim | Tipo do evento |
| ip | auth_audit_events | VARCHAR | Não | IP mascarado |
| user_agent_hash | auth_audit_events | VARCHAR | Não | Hash do user-agent |
| metadata_json | auth_audit_events | JSON/TEXT | Não | Metadados sem segredo |
| created_at | auth_audit_events | TIMESTAMP | Sim | Momento do evento |

### 5.5 Endpoints

| Método | Path | Auth | Payload resumido | Resposta |
|--------|------|------|------------------|----------|
| GET | `/api/v1/auth/csrf` | Não | sem body | cookie/token XSRF |
| POST | `/api/v1/auth/register` | Não + XSRF | `{ nome, email, senha, tipoUsuario }` | usuário + cookies |
| POST | `/api/v1/auth/login` | Não + XSRF | `{ email, senha }` | usuário + cookies |
| POST | `/api/v1/auth/social/google` | Não + XSRF | `{ idToken }` | usuário + cookies |
| POST | `/api/v1/auth/social/facebook` | Não + XSRF | `{ accessToken }` | usuário + cookies |
| POST | `/api/v1/auth/refresh` | Cookie refresh | sem body | novos cookies |
| POST | `/api/v1/auth/logout` | Cookie auth + XSRF | sem body | `204`, cookies limpos |
| GET | `/api/v1/auth/me` | Cookie auth | sem body | usuário autenticado |

Integrações externas:

- Google identity token validation;
- Facebook token/debug endpoint ou SDK equivalente;
- PostgreSQL para sessão/auditoria;
- Redis opcional se já usado para cache/revogação futura.

---

## 6. UX e Comportamento da Interface

Não há briefing UX dedicado. A UI deve manter o layout atual de login/registro, removendo Apple e alterando comportamento técnico.

### 6.1 Estados da interface

```text
[Login inicial]
-> campos email/senha + botões Google/Facebook

[Carregando]
-> botão acionado desabilitado, sem duplo submit

[Login ok]
-> redireciona para destino anterior ou home

[Sessão expirada]
-> refresh automático; se falhar, redireciona para login

[Erro credencial]
-> mensagem genérica sem revelar se email existe

[Erro provider]
-> mensagem específica por Google/Facebook indisponível
```

### 6.2 Wireframe — login

```text
+------------------------------------------------+
| Entrar                                         |
|                                                |
| Email                                          |
| [__________________________________________]   |
| Senha                                          |
| [__________________________________________]   |
|                                                |
| [Entrar]                                       |
|                                                |
| ou                                             |
| [Continuar com Google]                         |
| [Continuar com Facebook]                       |
|                                                |
| Criar conta | Esqueci minha senha              |
+------------------------------------------------+
```

### 6.3 Comportamento frontend

- `AuthService` não deve salvar JWT/refresh em `localStorage` ou `sessionStorage`.
- Todas as chamadas autenticadas devem usar `withCredentials: true`.
- Interceptor deve lidar com `401` chamando refresh uma vez e repetindo request original.
- Estado autenticado deve ser derivado de `/api/v1/auth/me`.
- Logout deve chamar backend e limpar estado local.
- Componentes existentes que chamam `getUsuarioAtual()` devem funcionar com usuário hidratado via `/me`.

---

## 7. Regras de Negócio

1. Usuário DEVE informar e-mail e senha válidos para login nativo.
2. O sistema N�O DEVE revelar se o e-mail existe quando credencial falhar.
3. O sistema DEVE emitir access token com expiração de 2 horas.
4. O sistema DEVE emitir refresh token com expiração de 7 dias.
5. O sistema DEVE rotacionar refresh token a cada uso.
6. O sistema DEVE revogar a família de refresh tokens se detectar reutilização.
7. Logout DEVE revogar refresh token ativo e limpar cookies.
8. Requests mutáveis autenticados DEVEM validar CSRF/XSRF.
9. Google e Facebook DEVEM criar sessão nativa após validação server-side do provider.
10. Apple login N�O DEVE aparecer na UI nem permanecer como endpoint ativo.
11. Papéis `CONTRATANTE` e `PRESTADOR` DEVEM ser preservados.
12. Usuário social novo PODE ser criado como `CONTRATANTE` por padrão se não houver escolha explícita no fluxo atual.
13. Dados antigos PODEM ser removidos por migração destrutiva.

---

## 8. Segurança e Privacidade

### 8.1 Controle de acesso

| Ação | Papel permitido | Papel bloqueado |
|------|-----------------|-----------------|
| Registrar | Público | N/A |
| Login local/social | Público | N/A |
| Refresh | Sessão com refresh válido | Anônimo sem cookie válido |
| Logout | Sessão válida | Anônimo |
| `/me` | Sessão válida | Anônimo |
| Área contratante | `CONTRATANTE` | Anônimo, papel incompatível |
| Área prestador | `PRESTADOR` | Anônimo, papel incompatível |

### 8.2 Dados sensíveis

| Dado | Onde armazenar | Criptografia | Pode logar? |
|------|---------------|--------------|-------------|
| Senha | Nunca em claro | Hash BCrypt/Argon2 | Não |
| Refresh token | Cookie + hash no DB | Hash server-side | Não |
| Access token | Cookie HttpOnly | Assinado | Não |
| JWT secret/private key | Env/secret manager | Segredo externo | Não |
| Google/Facebook token | Memória durante validação | TLS | Não |
| IP | Audit table/log mascarado | Conforme infra | Sim, mascarado |
| User-Agent | Hash | Hash | Sim, hash |

### 8.3 Cookies

- `ACCESS_TOKEN`: `HttpOnly`, `Secure`, `SameSite=Lax` ou `Strict`, path `/`.
- `REFRESH_TOKEN`: `HttpOnly`, `Secure`, `SameSite=Lax` ou `Strict`, path `/api/v1/auth/refresh`.
- `XSRF-TOKEN`: não `HttpOnly`, `Secure`, `SameSite`, usado pelo Angular no header `X-XSRF-TOKEN`.

### 8.4 Validações obrigatórias

- validar assinatura, issuer, audience e expiração do JWT;
- validar força mínima de senha no backend;
- normalizar e-mail antes de consultar/criar;
- nunca confiar em papel enviado pelo frontend sem validação de regra;
- validar provider social server-side;
- configurar CORS com origem explícita e `allowCredentials=true`.

---

## 9. Tratamento de Erros e Resiliência

| Cenário | Causa | Comportamento esperado | Mensagem ao usuário |
|---------|-------|----------------------|---------------------|
| Credenciais inválidas | email/senha incorretos | `401`, log `auth_login_failed` | E-mail ou senha inválidos. |
| Conta não encontrada | email não cadastrado | `401` genérico | E-mail ou senha inválidos. |
| Senha fraca no registro | validação falhou | `400` | A senha não atende aos requisitos mínimos. |
| E-mail já cadastrado | unique violation | `409` | Este e-mail já está em uso. |
| Access token expirado | expiração 2h | tentar refresh automático | Sem mensagem se refresh funcionar. |
| Refresh expirado | expiração 7d | `401`, limpar estado | Sua sessão expirou. Entre novamente. |
| Refresh reutilizado | token roubado/replay | revogar família, `401`, alerta | Sua sessão foi encerrada por segurança. |
| CSRF inválido | header ausente/incorreto | `403` | Não foi possível validar a requisição. Recarregue a página. |
| Google token inválido | provider rejeitou | `401` | Não foi possível entrar com Google. |
| Facebook indisponível | erro provider/time-out | `503` ou `502` | Login com Facebook indisponível no momento. |
| CORS/cookie bloqueado | config ambiente incorreta | falha frontend + log backend se chegar | Não foi possível iniciar sessão. |
| DB indisponível | PostgreSQL fora | `503` | Serviço temporariamente indisponível. |
| Dependência incompatível no upgrade | build/test falha | bloquear merge/deploy | N/A |

---

## 10. Observabilidade

### 10.1 Eventos a logar

| Evento | Campos obrigatórios | Nível |
|--------|---------------------|-------|
| `auth_login_success` | `user_id`, `provider`, `ip_masked`, `user_agent_hash`, `request_id` | info |
| `auth_login_failed` | `email_hash`, `provider`, `reason`, `ip_masked`, `request_id` | warn |
| `auth_refresh_success` | `user_id`, `family_id`, `request_id` | info |
| `auth_refresh_failed` | `reason`, `ip_masked`, `request_id` | warn |
| `auth_refresh_reuse_detected` | `user_id`, `family_id`, `ip_masked`, `request_id` | error |
| `auth_logout` | `user_id`, `request_id` | info |
| `auth_csrf_rejected` | `path`, `method`, `ip_masked`, `request_id` | warn |
| `social_login_provider_error` | `provider`, `reason`, `request_id` | warn |

### 10.2 Métricas

| Métrica | Tipo | O que mede |
|---------|------|-----------|
| `auth_login_total` | counter | logins por provider e resultado |
| `auth_login_duration_ms` | histogram | latência de login |
| `auth_refresh_total` | counter | refresh por resultado |
| `auth_refresh_duration_ms` | histogram | latência de refresh |
| `auth_csrf_rejected_total` | counter | rejeições CSRF |
| `auth_session_active_estimate` | gauge | sessões/refresh tokens ativos |
| `social_provider_error_total` | counter | erros Google/Facebook |

### 10.3 Alertas

| Condição | Threshold | Ação |
|----------|-----------|------|
| Falha login dispara muito | > 30% falhas por 10 min | investigar credenciais/brute force |
| Reuse refresh detectado | >= 1 evento | revisar sessão e possível roubo |
| Refresh 5xx | > 2% por 5 min | checar DB/security config |
| Login p95 lento | > 1s por 10 min | checar DB/provider |
| CSRF rejeitado alto | > 5% requests mutáveis por 10 min | revisar frontend/config cookie |

---

## 11. Variáveis de Ambiente e Configuração

```env
# Backend - runtime
AUTH_ACCESS_TOKEN_TTL=PT2H                 # BE - access token com 2 horas
AUTH_REFRESH_TOKEN_TTL=P7D                 # BE - refresh token com 7 dias
AUTH_COOKIE_SECURE=true                    # BE - true em HTTPS/prod
AUTH_COOKIE_SAME_SITE=Lax                  # BE - Lax ou Strict
AUTH_COOKIE_DOMAIN=localhost               # BE - domínio do cookie por ambiente
AUTH_COOKIE_ACCESS_NAME=ACCESS_TOKEN       # BE - nome cookie access
AUTH_COOKIE_REFRESH_NAME=REFRESH_TOKEN     # BE - nome cookie refresh

# JWT
JWT_ISSUER=brjobs-api                      # BE - issuer esperado
JWT_AUDIENCE=brjobs-web                    # BE - audience esperado
JWT_SECRET=change-me-min-32-bytes          # BE - segredo HMAC ou trocar por keypair

# CSRF/CORS
CSRF_COOKIE_NAME=XSRF-TOKEN                # BE/FE - cookie XSRF
CSRF_HEADER_NAME=X-XSRF-TOKEN              # BE/FE - header XSRF
APP_CORS_ALLOWED_ORIGINS=http://localhost:4200
APP_CORS_ALLOW_CREDENTIALS=true

# Google
GOOGLE_CLIENT_ID=google-client-id          # BE/FE - client id permitido
GOOGLE_CLIENT_SECRET=google-client-secret  # BE - se fluxo exigir

# Facebook
FACEBOOK_APP_ID=facebook-app-id            # BE/FE - app id permitido
FACEBOOK_APP_SECRET=facebook-app-secret    # BE - validação server-side

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/brjobs
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Frontend
NG_APP_API_URL=http://localhost:8080        # FE - base URL da API
NG_APP_GOOGLE_CLIENT_ID=google-client-id    # FE - Google button
NG_APP_FACEBOOK_APP_ID=facebook-app-id      # FE - Facebook SDK/button
```

---

## 12. Estratégia de Rollout e Rollback

**Rollout:**

- big-bang após validação local/CI;
- aplicar migrações destrutivas permitidas;
- publicar backend e frontend juntos;
- invalidar sessões antigas removendo dependência de tokens legados;
- validar smoke test: `/csrf`, registro, login, `/me`, refresh, logout, Google, Facebook.

**Rollback:**

- reverter deploy backend/frontend para versão anterior;
- se migrações destrutivas forem aplicadas, rollback de dados não é garantido;
- manter backup antes da migração mesmo sem usuários produtivos;
- se problema for só frontend, restaurar build anterior e manter backend compatível apenas se contratos não divergem;
- se auth nova falhar em produção, rollback completo backend + frontend + banco a partir do backup.

---

## 13. Fases de Entrega

### Fase 1 — Inventário e upgrade backend
- [ ] Validar JDK 25 local/CI.
- [ ] Atualizar `pom.xml` para Java 25 e Spring Boot 4.0.x.
- [ ] Ajustar dependências incompatíveis.
- [ ] Rodar `mvn test` e corrigir quebras.

### Fase 2 — Upgrade frontend
- [ ] Atualizar Angular para 21 LTS.
- [ ] Ajustar TypeScript/Node/RxJS/Zone conforme matriz oficial.
- [ ] Rodar `npm install`, `npm run build`, `npm test`.
- [ ] Corrigir APIs removidas/deprecadas.

### Fase 3 — Auth backend nativa
- [ ] Criar entidades/tabelas de refresh token e auditoria.
- [ ] Implementar cookies, CSRF, login, refresh, logout e `/me`.
- [ ] Implementar rotação e reuse detection.
- [ ] Atualizar Spring Security/CORS.

### Fase 4 — Social login Google/Facebook
- [ ] Migrar validação Google/Facebook para emitir sessão nativa.
- [ ] Remover Apple do backend.
- [ ] Remover configs/envs não usados.

### Fase 5 — Frontend auth
- [ ] Remover persistência de JWT em `localStorage`.
- [ ] Adicionar `withCredentials`.
- [ ] Ajustar interceptor para refresh.
- [ ] Ajustar componentes login/registro/header.
- [ ] Remover UI Apple.

### Fase 6 — Observabilidade e hardening
- [ ] Adicionar logs estruturados e métricas.
- [ ] Adicionar testes de CSRF, refresh rotation e logout.
- [ ] Executar smoke test completo.

---

## 14. Fora do Escopo (desta versão)

- MFA — recusado nesta fase.
- Apple login — removido por decisão de escopo.
- Compatibilidade com sessões/tokens antigos — rollout big-bang.
- Migração de usuários existentes — não há usuários em produção.
- Rate limit/lockout avançado — não solicitado; pode entrar em versão futura.
- Painel administrativo de sessões — futuro.
- Troca completa para OIDC próprio — fora do tamanho desta entrega.

---

## 15. Riscos e Pontos em Aberto

| # | Descrição | Probabilidade | Impacto | Mitigação |
|---|-----------|---------------|---------|-----------|
| R01 | Spring Boot 4 quebra APIs/configs atuais de Security/JPA | Média | Alto | Fazer upgrade isolado, corrigir warnings e cobrir auth com testes |
| R02 | Dependências como SpringDoc, jjwt, Stripe, Redis ou Lombok incompatíveis | Média | Médio | Validar versões no build antes de alterar regra de negócio |
| R03 | Angular 21 exige Node/TypeScript diferentes do ambiente atual | Média | Médio | Fixar engines e atualizar lockfile de forma controlada |
| R04 | Cookies `SameSite`/CORS quebram login local ou produção | Média | Alto | Testar com origem real, `withCredentials` e HTTPS |
| R05 | CSRF mal configurado bloqueia requests legítimos | Média | Alto | Testes E2E/smoke em login, refresh, logout e requests mutáveis |
| R06 | Access token de 2h aumenta janela se sessão for comprometida | Baixa | Médio | Cookie HttpOnly, Secure, CSRF e refresh revogável |
| R07 | Social login provider instável afeta entrada Google/Facebook | Baixa | Médio | Mensagem clara e fallback para login e-mail/senha |
| R08 | Rollback com migração destrutiva perde dados antigos | Baixa | Alto | Backup antes do deploy; aceito porque não há usuários em produção |

**Pontos em aberto (bloqueadores):**

- Nenhum ponto em aberto bloqueante identificado.

---

*Documento gerado para alinhamento técnico interno. Revisar com o time antes de iniciar o desenvolvimento.*

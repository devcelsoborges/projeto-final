<a id="spec-01"></a>
## SPEC-01 — Upgrade da plataforma Java/Spring/Angular

**Objetivo**
> Atualizar a base técnica para JDK 25 LTS, Spring Boot 4.0.x e Angular 21 LTS, mantendo o produto buildável e testável.

**Contexto**
> O backend está em Java 17 + Spring Boot 3.3.5. O frontend está em Angular 20.3.x. O briefing define JDK 25 LTS, Spring Boot 4.0.x e Angular 21 LTS.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Engenharia Backend | Atualiza runtime, Maven e dependências Spring | Direto |
> | Engenharia Frontend | Atualiza Angular/TypeScript/Node | Direto |
> | QA | Valida regressão de build e fluxo básico | Direto |

**Comportamento esperado**
> Fluxo principal:
> 1. Backend compila e testa com JDK 25.
> 2. Maven usa Spring Boot 4.0.x e dependências compatíveis.
> 3. Frontend instala e builda com Angular 21 LTS.
> 4. Quebras de APIs/dependências são corrigidas sem alterar domínio fora do escopo.
>
> Fluxo alternativo — dependência incompatível:
> 1. Build falha por lib incompatível.
> 2. Versão compatível é fixada e documentada.

**Regras de negócio**
> - O backend DEVE usar `<java.version>25</java.version>`.
> - O backend DEVE usar Spring Boot 4.0.x estável.
> - O frontend DEVE usar Angular 21 LTS.
> - O sistema N�O DEVE migrar para Angular 22 active.
> - O sistema N�O DEVE migrar para JDK 26.

**Contrato de API**
> Não há novo contrato funcional nesta spec. Contratos existentes devem permanecer acessíveis após upgrade, exceto auth quando coberta pelas specs seguintes.

**SLA e Performance**
> - Build backend deve completar sem falhas.
> - Build frontend deve completar sem falhas.
> - Smoke test local deve iniciar API em até 60s e frontend em até 60s.

**Observabilidade**
> - **Logar:** versão Java/Spring no startup — nível `info`.
> - **Métrica:** não aplicável nesta spec.
> - **Alerta:** falha de build em CI bloqueia merge/deploy.

**Critérios de aceite**
> - DADO o backend atualizado QUANDO `mvn test` rodar ENT�O o build finaliza sem erro.
> - DADO o frontend atualizado QUANDO `npm run build` rodar ENT�O o build finaliza sem erro.
> - DADO a aplica��o iniciada QUANDO consultar health/swagger/endpoints p�blicos ENT�O n�o h� erro de runtime por incompatibilidade.

**Estado atual**
> `brjobs-java/pom.xml` usa Spring Boot 3.3.5 e Java 17. `brjobs-angular/package.json` usa Angular 20.3.x.

**Mudanças necessárias**
> - **Banco de dados:** nenhuma direta.
> - **Backend:** atualizar `pom.xml`, plugins e imports/configs incompatíveis.
> - **Frontend:** atualizar `package.json`, lockfile e eventuais APIs Angular.
> - **Infra/Config:** garantir JDK 25 e Node compatível no ambiente.

**Definição de pronto**
> - [ ] Backend compila/testa com JDK 25.
> - [ ] Frontend builda com Angular 21.
> - [ ] Dependências incompatíveis corrigidas.
> - [ ] Startup local validado.

---

<a id="spec-02"></a>
## SPEC-02 — Sessão nativa com cookies seguros e refresh rotativo

**Objetivo**
> Substituir JWT em storage acessível por sessão nativa com cookies `HttpOnly`, access token 2h, refresh token rotativo 7d e revogação server-side.

**Contexto**
> O fluxo atual usa JWT e refresh com armazenamento no frontend. O briefing exige token inacessível por JavaScript, CSRF e rotação de refresh token.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Visitante | Faz login/registro local | Direto |
> | Contratante | Mantém sessão segura | Direto |
> | Prestador | Mantém sessão segura | Direto |
> | Engenharia Backend | Implementa security/session | Direto |

**Comportamento esperado**
> Fluxo principal:
> 1. Usuário envia e-mail/senha.
> 2. Backend valida credenciais.
> 3. Backend emite access token 2h e refresh token 7d.
> 4. Cookies seguros são enviados.
> 5. `/me` retorna usuário autenticado.
>
> Fluxo refresh:
> 1. Access expira.
> 2. Frontend chama `/refresh`.
> 3. Backend revoga refresh atual e emite novo.
> 4. Reuso de refresh revoga a família inteira.

**Regras de negócio**
> - O sistema DEVE usar cookie `HttpOnly` para access e refresh.
> - O sistema DEVE usar `Secure` em produção.
> - O sistema DEVE validar CSRF em requests mutáveis.
> - O sistema DEVE rotacionar refresh token a cada refresh.
> - O sistema DEVE revogar sessão no logout.
> - O sistema N�O DEVE armazenar JWT em `localStorage`.

**Contrato de API**
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------:|-----------------|
> | GET | `/api/v1/auth/csrf` | não | - | 204 + cookie XSRF | 500 |
> | POST | `/api/v1/auth/register` | XSRF | `{ nome, email, senha, tipoUsuario }` | 201 + cookies | 400, 409 |
> | POST | `/api/v1/auth/login` | XSRF | `{ email, senha }` | 200 + cookies | 400, 401 |
> | POST | `/api/v1/auth/refresh` | cookie refresh | - | 204 + cookies novos | 401 |
> | POST | `/api/v1/auth/logout` | cookie auth + XSRF | - | 204 + cookies limpos | 401, 403 |
> | GET | `/api/v1/auth/me` | cookie auth | - | 200 usuário | 401 |

**SLA e Performance**
> - Login p95 <= 1s.
> - Refresh p95 <= 500ms.
> - `/me` p95 <= 300ms.

**Observabilidade**
> - **Logar:** `auth_login_success`, `auth_login_failed`, `auth_refresh_success`, `auth_refresh_reuse_detected`, `auth_logout`.
> - **Métrica:** `auth_login_total`, `auth_refresh_total`, `auth_login_duration_ms`.
> - **Alerta:** qualquer `auth_refresh_reuse_detected` gera alerta.

**Critérios de aceite**
> - DADO credenciais v�lidas QUANDO login ocorrer ENT�O cookies seguros s�o setados e `/me` retorna usu�rio.
> - DADO refresh v�lido QUANDO `/refresh` ocorrer ENT�O token antigo � revogado e novo token � emitido.
> - DADO refresh reutilizado QUANDO `/refresh` ocorrer ENT�O fam�lia � revogada e resposta � 401.
> - DADO logout QUANDO chamado ENT�O refresh token � revogado e cookies s�o limpos.

**Estado atual**
> Existem classes de auth/JWT/security e DTOs de auth. O frontend depende de estado/token salvo localmente.

**Mudanças necessárias**
> - **Banco de dados:** tabelas `auth_refresh_tokens` e `auth_audit_events`.
> - **Backend:** novos serviços de token, cookies, CSRF, endpoints e filtros security.
> - **Frontend:** coberto na SPEC-04.
> - **Infra/Config:** envs `AUTH_*`, `JWT_*`, `COOKIE_*`, `CSRF_*`.

**Definição de pronto**
> - [ ] Login/register/refresh/logout/me implementados.
> - [ ] Cookies têm flags corretas.
> - [ ] CSRF validado.
> - [ ] Reuse detection testado.
> - [ ] Nenhum token sensível logado.

---

<a id="spec-03"></a>
## SPEC-03 — Social login Google/Facebook integrado à sessão nativa

**Objetivo**
> Manter login Google/Facebook e remover Apple, emitindo a mesma sessão nativa segura após validação server-side.

**Contexto**
> O projeto contém social login amplo e docs/configs para Google, Facebook e Apple. O briefing mantém apenas Google/Facebook.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Visitante | Entra via Google/Facebook | Direto |
> | Backend Engineer | Valida providers e cria sessão | Direto |
> | Frontend Engineer | Remove Apple e ajusta botões | Direto |

**Comportamento esperado**
> Fluxo Google/Facebook:
> 1. Frontend obtém token/credential do provider.
> 2. Backend valida token com provider.
> 3. Usuário local é encontrado ou criado.
> 4. Backend emite cookies da sessão nativa.
> 5. Frontend hidrata estado via `/me`.
>
> Fluxo Apple removido:
> 1. Endpoint/UI Apple não existem.
> 2. Configs Apple não são exigidas.

**Regras de negócio**
> - Google DEVE ser validado server-side.
> - Facebook DEVE ser validado server-side.
> - Apple N�O DEVE estar dispon�vel.
> - Usuário social novo PODE ser criado como `CONTRATANTE` por padrão.
> - Social login DEVE emitir cookies iguais ao login local.

**Contrato de API**
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------:|-----------------|
> | POST | `/api/v1/auth/social/google` | XSRF | `{ idToken }` | 200 + cookies | 400, 401, 502 |
> | POST | `/api/v1/auth/social/facebook` | XSRF | `{ accessToken }` | 200 + cookies | 400, 401, 502 |

**SLA e Performance**
> - Social login p95 <= 2s, incluindo provider.
> - Timeout provider <= 5s.

**Observabilidade**
> - **Logar:** `auth_login_success` com `provider`.
> - **Logar:** `social_login_provider_error`.
> - **Métrica:** `social_provider_error_total`.
> - **Alerta:** erro provider > 10% por 10 min.

**Critérios de aceite**
> - DADO token Google v�lido QUANDO login social ocorrer ENT�O sess�o nativa � emitida.
> - DADO token Facebook v�lido QUANDO login social ocorrer ENT�O sess�o nativa � emitida.
> - DADO Apple QUANDO buscar UI ou endpoint ENT�O ele n�o est� dispon�vel.
> - DADO provider indispon�vel QUANDO login social ocorrer ENT�O usu�rio recebe erro claro.

**Estado atual**
> Existem `SocialAuthService`, DTOs, docs e componentes com Google/Facebook/Apple.

**Mudanças necessárias**
> - **Banco de dados:** manter/ajustar vínculo social se existir; remover necessidade Apple.
> - **Backend:** validar providers e integrar com sessão nativa.
> - **Frontend:** remover botão Apple; manter Google/Facebook.
> - **Infra/Config:** manter `GOOGLE_*`, `FACEBOOK_*`; remover `APPLE_*`.

**Definição de pronto**
> - [ ] Google login emite cookies nativos.
> - [ ] Facebook login emite cookies nativos.
> - [ ] Apple removido da UI/backend/config.
> - [ ] Erros provider tratados.

---

<a id="spec-04"></a>
## SPEC-04 — Frontend auth sem localStorage e com credenciais/csrf

**Objetivo**
> Adaptar Angular para sessão por cookies, `withCredentials`, CSRF e estado derivado de `/me`.

**Contexto**
> Componentes e services atuais dependem de `AuthService`, token local e sincronização manual. A auth nova exige cookies e CSRF.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Usuário autenticado | Navega sem perder sessão | Direto |
> | Frontend Engineer | Ajusta services/interceptors/guards | Direto |
> | QA | Valida login/refresh/logout | Direto |

**Comportamento esperado**
> Fluxo principal:
> 1. App inicia.
> 2. Frontend chama `/me` com credentials.
> 3. Se autenticado, popula usuário atual.
> 4. Requests usam cookies automaticamente.
> 5. Em 401, interceptor tenta refresh uma vez e repete request.
> 6. Se refresh falhar, redireciona para `/login`.

**Regras de negócio**
> - Frontend N�O DEVE ler access/refresh token.
> - Frontend N�O DEVE gravar JWT em `localStorage`.
> - Frontend DEVE usar `withCredentials: true`.
> - Frontend DEVE enviar header XSRF em requests mutáveis.
> - Interceptor DEVE evitar loop infinito em refresh.

**Contrato de API**
> Usa contratos da SPEC-02 e SPEC-03.

**SLA e Performance**
> - Hidratação `/me` p95 <= 300ms.
> - Refresh transparente não deve repetir a mesma request mais de uma vez.

**Observabilidade**
> - **Logar:** no frontend, erros controlados em console apenas em dev.
> - **Métrica:** coberta pelo backend.
> - **Alerta:** coberto pelo backend.

**Critérios de aceite**
> - DADO usu�rio logado QUANDO recarregar a p�gina ENT�O estado autenticado � restaurado via `/me`.
> - DADO access expirado QUANDO request autenticada ocorrer ENT�O refresh roda e request original � repetida.
> - DADO refresh expirado QUANDO request ocorrer ENT�O usu�rio vai para login.
> - DADO logout QUANDO usu�rio clicar ENT�O backend revoga sess�o e frontend limpa estado.

**Estado atual**
> `AuthService` e componentes de login/registro manipulam estado local e social login atual.

**Mudanças necessárias**
> - **Banco de dados:** nenhuma.
> - **Backend:** contratos da SPEC-02 disponíveis.
> - **Frontend:** `AuthService`, interceptors, guards, login/register/header, social login.
> - **Infra/Config:** `NG_APP_API_URL`, ids Google/Facebook.

**Definição de pronto**
> - [ ] `localStorage` não contém tokens.
> - [ ] `withCredentials` aplicado.
> - [ ] Refresh transparente implementado.
> - [ ] UI Apple removida.
> - [ ] Login/logout/regressão manual validada.

---

<a id="spec-05"></a>
## SPEC-05 — Hardening, testes e observabilidade de autenticação

**Objetivo**
> Garantir segurança, rastreabilidade e prontidão de rollout para upgrade e auth nativa.

**Contexto**
> A mudança toca runtime, framework, auth e frontend. O briefing exige logs, métricas, alertas, smoke tests e rollback claro.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Segurança/Operação | Monitora eventos críticos | Direto |
> | QA | Executa matriz de regressão | Direto |
> | Engenharia | Corrige falhas antes do deploy | Direto |

**Comportamento esperado**
> Fluxo principal:
> 1. Testes unitários e integração cobrem auth.
> 2. Smoke test cobre login, `/me`, refresh, logout, Google, Facebook.
> 3. Logs estruturados sem segredos são emitidos.
> 4. Métricas e alertas mínimos ficam definidos.
> 5. Rollback/backup fica documentado.

**Regras de negócio**
> - Testes DEVEM cobrir refresh rotation e reuse detection.
> - Logs N�O DEVEM incluir senha, access token, refresh token ou provider token.
> - Deploy DEVE ter backup antes de migração destrutiva.
> - CI DEVE bloquear build quebrado.

**Contrato de API**
> Não adiciona novos endpoints além dos cobertos em SPEC-02/03.

**SLA e Performance**
> - Login p95 <= 1s.
> - Refresh p95 <= 500ms.
> - Erro 5xx auth < 1% em smoke/homologação.

**Observabilidade**
> - **Logar:** eventos da seção 10 do briefing técnico.
> - **Métrica:** `auth_login_total`, `auth_refresh_total`, `auth_csrf_rejected_total`.
> - **Alerta:** reuse refresh >= 1; login p95 > 1s; CSRF rejeitado > 5%.

**Critérios de aceite**
> - DADO su�te backend QUANDO executar testes ENT�O casos cr�ticos de auth passam.
> - DADO su�te frontend QUANDO build/test rodar ENT�O login/logout n�o quebra.
> - DADO evento cr�tico QUANDO ocorrer ENT�O log estruturado n�o cont�m segredo.
> - DADO plano de rollback QUANDO revisado ENT�O backup e passos est�o documentados.

**Estado atual**
> Há docs de security/social login, mas a nova auth ainda não tem cobertura dedicada.

**Mudanças necessárias**
> - **Banco de dados:** índices e constraints auditáveis.
> - **Backend:** testes service/controller/security.
> - **Frontend:** testes/manual smoke.
> - **Infra/Config:** checklist env + alertas.

**Definição de pronto**
> - [ ] Testes backend passam.
> - [ ] Build/test frontend passam.
> - [ ] Logs/métricas/alertas definidos.
> - [ ] Smoke test executado.
> - [ ] Rollback documentado.

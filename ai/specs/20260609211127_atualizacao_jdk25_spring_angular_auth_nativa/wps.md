### Wp-37 — Upgrade backend JDK 25 e Spring Boot 4

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-37                                              |
| **Spec relacionada**     | [SPEC-01](./specs.md#spec-01)                      |
| **Tipo**                 | backend                                            |
| **Estimativa**           | 3d                                                 |
| **Dependências**         | Nenhuma                                            |
| **Pode paralelizar com** | Wp-38                                              |
| **Testes requeridos**    | unit \| integration                                |
| **Status**               | Concluido                                           |

**Escopo**
> Atualizar `brjobs-java` para JDK 25 e Spring Boot 4.0.x, corrigindo dependências e quebras de compilação.

**Definition of Ready**
> - [ ] JDK 25 disponível local/CI.
> - [ ] Versão Spring Boot 4.0.x final escolhida.

**Passos sugeridos de implementação**
> 1. Atualizar `pom.xml`: parent Spring Boot, `java.version`, plugins e libs incompatíveis.
> 2. Corrigir imports/configs quebrados por Spring Boot 4/Spring Security.
> 3. Validar startup local.
> 4. Rodar `mvn test`.

**Critérios de aceite do pacote**
> - Backend compila com JDK 25.
> - `mvn test` passa ou falhas existentes ficam documentadas.
> - API sobe sem erro de runtime por dependência.

**Rollback**
> Reverter `pom.xml`/lock de dependências e voltar runtime para Java 17.

**Áreas impactadas**
> [backend] | [config/env] | [infra]

---

### Wp-38 — Upgrade frontend Angular 21 LTS

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-38                                              |
| **Spec relacionada**     | [SPEC-01](./specs.md#spec-01)                      |
| **Tipo**                 | frontend                                           |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Nenhuma                                            |
| **Pode paralelizar com** | Wp-37                                              |
| **Testes requeridos**    | unit \| manual                                     |
| **Status**               | Concluido                                           |

**Escopo**
> Atualizar `brjobs-angular` para Angular 21 LTS e dependências compatíveis.

**Definition of Ready**
> - [ ] Node compatível disponível.
> - [ ] Matriz Angular 21 revisada.

**Passos sugeridos de implementação**
> 1. Atualizar pacotes Angular/CLI/build/compiler para 21 LTS.
> 2. Atualizar TypeScript/RxJS/Zone conforme matriz.
> 3. Rodar install e corrigir quebras.
> 4. Rodar `npm run build` e `npm test`.

**Critérios de aceite do pacote**
> - Frontend instala dependências sem conflito.
> - Build Angular 21 passa.
> - Login/rotas públicas abrem em smoke manual.

**Rollback**
> Reverter `package.json` e `package-lock.json` para Angular 20.

**Áreas impactadas**
> [frontend] | [config/env]

---

### Wp-39 — Schema de sessão, refresh token e auditoria

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-39                                              |
| **Spec relacionada**     | [SPEC-02](./specs.md#spec-02) e [SPEC-05](./specs.md#spec-05) |
| **Tipo**                 | data                                               |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Wp-37                                              |
| **Pode paralelizar com** | Wp-40                                              |
| **Testes requeridos**    | integration                                        |
| **Status**               | Concluido                                           |

**Escopo**
> Criar persistência para refresh tokens rotativos e eventos de auditoria.

**Definition of Ready**
> - [ ] Wp-37 concluído.
> - [ ] Nomes de tabelas/campos aprovados.

**Passos sugeridos de implementação**
> 1. Criar migration para `auth_refresh_tokens`.
> 2. Criar migration para `auth_audit_events`.
> 3. Adicionar entidades/repositories.
> 4. Criar índices para `token_hash`, `usuario_id`, `family_id`, `expires_at`.

**Critérios de aceite do pacote**
> - Migrations aplicam em base limpa.
> - Repositories persistem e consultam tokens/auditoria.
> - Nenhum token em claro é persistido.

**Rollback**
> Reverter migrations. Aceito destrutivo porque não há usuários em produção.

**Áreas impactadas**
> [banco] | [backend]

---

### Wp-40 — Auth backend nativa: login, cookies, CSRF e refresh rotation

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-40                                              |
| **Spec relacionada**     | [SPEC-02](./specs.md#spec-02)                      |
| **Tipo**                 | backend                                            |
| **Estimativa**           | 3d                                                 |
| **Dependências**         | Wp-37, Wp-39                                       |
| **Pode paralelizar com** | Wp-41                                              |
| **Testes requeridos**    | unit \| integration                                |
| **Status**               | Concluido                                           |

**Escopo**
> Implementar sessão nativa completa no backend: login, register, `/me`, refresh, logout, cookies seguros e CSRF.

**Definition of Ready**
> - [ ] Wp-39 concluído.
> - [ ] SecurityConfig compatível com Boot 4.

**Passos sugeridos de implementação**
> 1. Implementar serviços de access token e refresh token.
> 2. Configurar cookies e CSRF.
> 3. Criar/ajustar endpoints `/api/v1/auth/*`.
> 4. Implementar rotação e reuse detection.
> 5. Atualizar filtros Spring Security.

**Critérios de aceite do pacote**
> - Login/register emitem cookies.
> - Refresh rotaciona token e detecta reuse.
> - Logout revoga sessão.
> - CSRF bloqueia request mutável inválido.

**Rollback**
> Reverter endpoints/security e migrations do Wp-39 se necessário.

**Áreas impactadas**
> [backend] | [banco] | [config/env]

---

### Wp-41 — Social login Google/Facebook e remoção Apple

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-41                                              |
| **Spec relacionada**     | [SPEC-03](./specs.md#spec-03)                      |
| **Tipo**                 | backend                                            |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Wp-40                                              |
| **Pode paralelizar com** | Wp-42                                              |
| **Testes requeridos**    | unit \| integration                                |
| **Status**               | Concluido                                           |

**Escopo**
> Adaptar social login para emitir sessão nativa e remover Apple do backend/config.

**Definition of Ready**
> - [ ] Wp-40 concluído.
> - [ ] Client IDs/secrets de Google/Facebook definidos por env.

**Passos sugeridos de implementação**
> 1. Ajustar validação Google.
> 2. Ajustar validação Facebook.
> 3. Emitir cookies nativos após social login.
> 4. Remover endpoints/configs Apple.

**Critérios de aceite do pacote**
> - Google cria sessão nativa.
> - Facebook cria sessão nativa.
> - Apple não existe como endpoint ativo.

**Rollback**
> Reverter social login para fluxo anterior junto com Wp-40.

**Áreas impactadas**
> [backend] | [config/env]

---

### Wp-42 — Frontend auth por cookies, interceptor e UI sem Apple

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-42                                              |
| **Spec relacionada**     | [SPEC-04](./specs.md#spec-04)                      |
| **Tipo**                 | frontend                                           |
| **Estimativa**           | 3d                                                 |
| **Dependências**         | Wp-38, Wp-40                                       |
| **Pode paralelizar com** | Wp-41                                              |
| **Testes requeridos**    | unit \| manual                                     |
| **Status**               | Concluido                                           |

**Escopo**
> Remover armazenamento de JWT no frontend e adaptar Angular para cookies, CSRF, refresh transparente e remoção Apple.

**Definition of Ready**
> - [ ] Wp-38 concluído.
> - [ ] Wp-40 endpoints disponíveis.

**Passos sugeridos de implementação**
> 1. Atualizar `AuthService` para `/csrf`, `/login`, `/refresh`, `/logout`, `/me`.
> 2. Aplicar `withCredentials` nos clients.
> 3. Ajustar interceptor para refresh-once.
> 4. Remover Apple da UI.
> 5. Ajustar guards/header/componentes dependentes de usuário atual.

**Critérios de aceite do pacote**
> - Nenhum JWT fica em `localStorage`.
> - Refresh transparente funciona.
> - Logout limpa estado.
> - Google/Facebook seguem visíveis; Apple removido.

**Rollback**
> Reverter services/components de auth para versão anterior junto com backend.

**Áreas impactadas**
> [frontend] | [config/env]

---

### Wp-43 — Observabilidade, testes de segurança e smoke suite

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-43                                              |
| **Spec relacionada**     | [SPEC-05](./specs.md#spec-05)                      |
| **Tipo**                 | fullstack                                          |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Wp-40, Wp-41, Wp-42                                |
| **Pode paralelizar com** | —                                                  |
| **Testes requeridos**    | unit \| integration \| manual                      |
| **Status**               | Concluido                                           |

**Escopo**
> Adicionar logs/métricas mínimos e validar matriz de segurança/rollback da autenticação.

**Definition of Ready**
> - [ ] Wp-40 a Wp-42 concluídos.
> - [ ] Lista de envs por ambiente revisada.

**Passos sugeridos de implementação**
> 1. Implementar eventos estruturados de auth.
> 2. Adicionar testes para refresh rotation/reuse/logout/CSRF.
> 3. Criar checklist de smoke manual.
> 4. Documentar backup/rollback big-bang.

**Critérios de aceite do pacote**
> - Eventos críticos aparecem sem segredos.
> - Testes críticos passam.
> - Smoke login/register/me/refresh/logout/social validado.

**Rollback**
> Desativar deploy e restaurar versão anterior + backup se migração destrutiva aplicada.

**Áreas impactadas**
> [backend] | [frontend] | [infra] | [config/env]

---

### Wp-44 — Hardening final do upgrade e readiness de deploy

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-44                                              |
| **Spec relacionada**     | [SPEC-01](./specs.md#spec-01) e [SPEC-05](./specs.md#spec-05) |
| **Tipo**                 | fullstack                                          |
| **Estimativa**           | 1d                                                 |
| **Dependências**         | Wp-37, Wp-38, Wp-43                                |
| **Pode paralelizar com** | —                                                  |
| **Testes requeridos**    | integration \| manual                              |
| **Status**               | Concluido                                           |

**Escopo**
> Rodar validação final, corrigir ajustes menores e fechar readiness do upgrade completo.

**Definition of Ready**
> - [ ] Wp-37 a Wp-43 concluídos.

**Passos sugeridos de implementação**
> 1. Rodar backend tests/build.
> 2. Rodar frontend build/test.
> 3. Executar smoke full stack.
> 4. Revisar env examples/docs de rollout.

**Critérios de aceite do pacote**
> - Backend e frontend verificados.
> - Fluxos auth principais validados.
> - Rollout/rollback revisado.

**Rollback**
> Rollback completo para versão anterior e restauração de backup caso necessário.

**Áreas impactadas**
> [backend] | [frontend] | [infra] | [docs]

---

### Mapa de Dependências

```text
Wp-37 -> Wp-39 -> Wp-40 -> Wp-41 -> Wp-43 -> Wp-44
Wp-38 -> Wp-42 -> Wp-43 -> Wp-44
Wp-40 -> Wp-42
```

---

### Riscos e Pontos Desconhecidos

| # | Descrição | Probabilidade | Impacto | Mitigação |
|---|-----------|---------------|---------|-----------|
| R01 | Spring Boot 4 quebrar security/JPA | Média | Alto | Wp-37 antes da auth nova |
| R02 | Angular 21 exigir Node diferente | Média | Médio | Validar engines no Wp-38 |
| R03 | Cookies/CORS/CSRF quebrar login | Média | Alto | Wp-40 + Wp-42 com smoke |
| R04 | Social providers instáveis | Baixa | Médio | Testes mockados + fallback local |
| R05 | Rollback destrutivo de schema | Baixa | Alto | Backup obrigatório antes do deploy |

---

### Oportunidades de Paralelização

| Grupo | WPs que podem rodar juntos | Pré-requisito para o grupo |
|-------|---------------------------|---------------------------|
| G1 | Wp-37, Wp-38 | Nenhum |
| G2 | Wp-39 | Wp-37 concluído |
| G3 | Wp-41, Wp-42 | Wp-40 concluído; Wp-42 também requer Wp-38 |
| G4 | Wp-43 | Wp-40, Wp-41, Wp-42 concluídos |
| G5 | Wp-44 | Wp-37, Wp-38, Wp-43 concluídos |

---

### Execucao

- `mvn test` em `brjobs-java`: passou em JDK 25 com Spring Boot 4.0.6.
- `npm run build` em `brjobs-angular`: passou em Angular 21, com avisos de budget preexistentes.
- Auth nativa: cookies HttpOnly, refresh rotativo, auditoria, CSRF em `/api/v1/auth/*`, Google/Facebook ativos, Apple removido da UI ativa e endpoints.

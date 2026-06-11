# Discovery — Atualização JDK 25, Spring, Angular LTS e autenticação nativa

> **Feature:** 20260609211127_atualizacao_jdk25_spring_angular_auth_nativa
> **Data:** 2026-06-09
> **Autor:** Codex (facilitação de discovery)
> **Status:** Completo

---

## Fontes de contexto utilizadas

| # | Fonte | Tipo | Conteúdo principal |
|---|-------|------|--------------------|
| 1 | Entrevista guiada com stakeholder | sessão | Escopo de upgrade, decisão de auth nativa, login social permitido, rollout e premissas de migração |
| 2 | `CLAUDE.md` | arquivo | Stack, arquitetura, comandos e fluxo atual de autenticação |
| 3 | `brjobs-java/pom.xml` | arquivo | Spring Boot 3.3.5, Java 17, dependências de Security, JWT, OAuth2, Redis e Stripe |
| 4 | `brjobs-angular/package.json` | arquivo | Angular 20.3.x, TypeScript 5.9.x e scripts disponíveis |
| 5 | `brjobs-java/src/main/java/ads/uninassau/brjobs/**` | arquivo | Controllers, services, models, security, JWT, usuários e social login |
| 6 | `brjobs-angular/src/app/**` | arquivo | Componentes de login/registro, AuthService, SocialLoginService e uso de estado autenticado |
| 7 | https://www.oracle.com/java/technologies/downloads/ | referência técnica | JDK 25 como release LTS atual da plataforma Java SE; JDK 26 existe, mas não é LTS |
| 8 | https://spring.io/projects/spring-boot | referência técnica | Spring Boot 4.0.x como geração estável atual |
| 9 | https://spring.io/blog/2025/11/20/spring-boot-4-0-0-available-now | referência técnica | Spring Boot 4 com suporte de primeira classe a Java 25 |
| 10 | https://angular.dev/reference/releases | referência técnica | Angular 21 em LTS e Angular 22 em active; decisão por Angular 21 LTS |
| 11 | https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html | referência de segurança | Boas práticas para sessão, cookies seguros e HTTPS |
| 12 | https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html | referência de segurança | Boas práticas de JWT em Java |
| 13 | https://auth0.com/blog/refresh-tokens-what-are-they-and-when-to-use-them/ | referência de segurança | Refresh token rotation para reduzir replay de tokens comprometidos |
| 14 | https://angular.dev/reference/versions | referência técnica | Compatibilidade Angular/Node/TypeScript para planejamento de upgrade |

---

## Tipo de projeto

**Brownfield**

> A plataforma brjobs já existe com backend Spring Boot/Java e frontend Angular. O trabalho altera fundações técnicas, autenticação, contratos HTTP e segurança de sessão em um produto existente.

---

## Resumo do entendimento

Atualizar a base tecnológica da plataforma para JDK 25 LTS, Spring Boot 4.x e Angular 21 LTS, corrigindo conflitos de compatibilidade decorrentes da mudança. Em paralelo, substituir o modelo atual de autenticação por uma autenticação nativa própria, mais segura, mantendo apenas login por e-mail/senha, Google e Facebook.

Como não há usuários em produção, a mudança pode ser big-bang e pode quebrar dados antigos. A solução deve preservar os papéis existentes (`CONTRATANTE` e `PRESTADOR`) e manter o fluxo de produto atual sem compatibilidade com sessões/tokens antigos.

---

## Problema e dor

**Dor principal:** a base atual usa Java 17, Spring Boot 3.3.5 e Angular 20, enquanto a evolução desejada exige versões LTS mais atuais e uma autenticação mais segura que elimine armazenamento sensível em `localStorage`.

**Quem sente:** usuários finais que fazem login/registro; prestadores e contratantes que dependem de sessão ativa; equipe de desenvolvimento que mantém backend, frontend e segurança.

**Frequência e impacto:** ocorre em todo acesso autenticado. Se não resolvido, mantém risco de sessão exposta por XSS, dívida técnica de versões e maior custo para evoluções futuras.

---

## Usuários e papéis afetados

| Papel | Relação com a feature | Impacto |
|-------|-----------------------|---------|
| Visitante | Faz login, registro ou login social | Direto: novo fluxo de autenticação e cookies seguros |
| Contratante | Usa áreas autenticadas e publica/contrata serviços | Direto: sessões novas e preservação do papel atual |
| Prestador | Usa áreas autenticadas, perfil e publicações | Direto: sessões novas e preservação do papel atual |
| Engenharia | Atualiza stack, corrige compatibilidade e mantém auth | Direto: alteração estrutural em backend/frontend |
| Operação/Segurança | Monitora falhas de login, refresh, logout e bloqueios | Direto: novos eventos de auditoria e observabilidade |

---

## Solução proposta (rascunho)

**Construir:**
- Atualizar backend para JDK 25 LTS e Spring Boot 4.x estável, com ajustes em dependências, plugins Maven, Spring Security, SpringDoc, JPA/Hibernate e testes.
- Atualizar frontend para Angular 21 LTS, com versões compatíveis de Angular CLI, TypeScript, RxJS, Zone.js e Node conforme matriz oficial.
- Substituir autenticação atual por auth nativa própria com:
  - login e registro por e-mail/senha;
  - BCrypt/Argon2 conforme compatibilidade definida no briefing técnico;
  - access token com duração de 2 horas;
  - refresh token rotativo com duração de 7 dias;
  - tokens armazenados em cookies `HttpOnly`, `Secure`, `SameSite`;
  - proteção CSRF/XSRF para requests mutáveis;
  - logout com revogação server-side;
  - auditoria de login falho, login bem-sucedido, refresh, logout e bloqueios.
- Manter login social com Google e Facebook, emitindo a mesma sessão nativa após validação do provedor.
- Remover login Apple e dependências/configurações não usadas.
- Preservar papéis atuais (`CONTRATANTE`, `PRESTADOR`) e regras de autorização existentes.
- Ajustar frontend para não depender de token em `localStorage`; usar estado autenticado via endpoint `/me` e cookies enviados automaticamente.
- Criar/alterar migrações de banco para refresh tokens, sessões/auditoria e limpeza de tabelas obsoletas se necessário.

**Fora do escopo desta versão:**
- MFA/autenticação em dois fatores.
- Rate limit avançado ou lockout customizado além do recomendado básico do framework/infra.
- Compatibilidade com tokens antigos ou migração de usuários/senhas existentes.
- Manter Apple login.
- Reescrita completa de domínios fora da autenticação e compatibilidade do upgrade.

---

## Restrições e premissas

- O upgrade alvo de Java é JDK 25 LTS.
- O frontend deve usar Angular 21 LTS, não Angular 22 active.
- Spring deve ir para a geração LTS/estável mais atual compatível com JDK 25; referência inicial: Spring Boot 4.0.x.
- Rollout será big-bang.
- Não há usuários em produção; dados antigos podem ser quebrados/removidos.
- Papéis atuais devem continuar existindo.
- Login social permitido: Google e Facebook.
- Login social removido: Apple.
- Access token: 2 horas.
- Refresh token: 7 dias, rotativo e revogável server-side.
- Variáveis de ambiente devem seguir padrão simples: `AUTH_*`, `JWT_*`, `GOOGLE_*`, `FACEBOOK_*`, `COOKIE_*`, `CSRF_*`.
- SLA recomendado para login: p95 até 1s em ambiente normal; refresh p95 até 500ms.
- Segurança deve evitar armazenamento de token sensível acessível por JavaScript.

---

## Referências de mercado

| Referência | Decisão de design relevante | Ressoa? | Motivo |
|------------|-----------------------------|---------|--------|
| Oracle JDK Downloads | JDK 25 é LTS atual; JDK 26 existe, mas não é LTS | Sim | Confirma alvo Java pedido sem migrar para release não LTS |
| Spring Boot 4.0 release | Spring Boot 4 inicia nova geração com suporte de primeira classe a Java 25 | Sim | Alinha backend com JDK 25 e reduz risco de incompatibilidade futura |
| Angular Releases | Angular 21 está em LTS; Angular 22 está active | Sim | Atende pedido de LTS mais atual no frontend |
| OWASP Session Management | Cookies seguros com `HttpOnly`, `Secure`, `SameSite` e HTTPS | Sim | Base para reduzir exposição de tokens a XSS |
| OWASP JWT for Java | Validação forte de JWT, assinatura e expiração corretas | Sim | Aplica-se diretamente ao backend Java |
| Auth0 Refresh Token Rotation | Refresh token rotativo invalida token anterior a cada uso | Sim | Mitiga replay e melhora revogação |

**Padrões extraídos das referências escolhidas:**
- Preferir cookies seguros a `localStorage` para tokens de sessão.
- Usar access token curto e refresh token rotativo revogável.
- Validar CSRF/XSRF quando sessão usa cookies.
- Alinhar upgrade por matriz oficial de compatibilidade, não apenas por versões isoladas.
- Tratar auditoria de autenticação como requisito de segurança, não detalhe opcional.

---

## Decisões de design tomadas

| Decisão | Alternativas consideradas | Justificativa |
|---------|--------------------------|---------------|
| Angular 21 LTS | Angular 22 active | Usuário pediu LTS mais atual; Angular 21 está em LTS, Angular 22 está active |
| JDK 25 LTS | JDK 21 LTS, JDK 26 | JDK 25 é LTS atual; JDK 26 não atende requisito LTS |
| Spring Boot 4.0.x | Permanecer em 3.3.x/3.5.x | Boot 4 tem suporte moderno a Java 25 e reduz dívida de compatibilidade |
| Auth nativa com cookies `HttpOnly` | JWT em `localStorage`, sessão opaca pura | Cookies reduzem roubo por XSS; JWT curto mantém stateless parcial para APIs |
| Refresh token rotativo server-side | Refresh JWT stateless longo | Rotação e persistência server-side permitem revogação, detecção de replay e logout efetivo |
| Access token 2h, refresh 7d | Access 15min, refresh 30d | Decisão do stakeholder; 2h equilibra UX e segurança aceitável para este contexto |
| Big-bang | Rollout gradual/compatibilidade temporária | Sem usuários em produção; menor complexidade e sem necessidade de suportar tokens antigos |
| Manter Google/Facebook | Remover todos sociais, manter Apple | Stakeholder quer Google/Facebook; Apple fora do escopo |
| Auditoria de auth | Logs mínimos sem eventos estruturados | Stakeholder pediu observabilidade; auth exige trilha de segurança |
| Sem MFA nesta versão | MFA obrigatório/opcional | Stakeholder recusou segurança extra nesta etapa |

---

## Lacunas e pontos em aberto

- Nenhum ponto em aberto bloqueante identificado durante o discovery.

---

## Notas adicionais

- Durante execução, validar a compatibilidade exata de SpringDoc, jjwt, PostgreSQL driver, Lombok, Stripe, Redis e plugins Maven com Spring Boot 4/JDK 25.
- A migração deve remover dependências/configurações de Apple login e qualquer configuração OAuth2 que não seja Google/Facebook.
- Como o frontend deixará de ler tokens, componentes e services devem passar a consultar estado via `/me` e interceptors devem usar `withCredentials`.
- CSRF deve ser testado nos fluxos mutáveis principais: login, refresh, logout, registro, publicação, perfil e endpoints autenticados.
- Testes mínimos esperados: backend auth service/controller/security filter, refresh rotation/reuse detection, logout revocation, social login Google/Facebook mockado, frontend login/logout/estado autenticado.

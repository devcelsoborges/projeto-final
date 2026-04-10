# Discovery: Multi-Tenancy no brjobs

**Data:** 8 de April de 2026  
**Feature:** Implementação completa de multi-tenancy (Tenant = Usuário)  
**Projeto:** brjobs (Angular 20 + Spring Boot 3.3.5)  
**Classificação:** Brownfield — expansão de produto existente

---

## Resumo Executivo

O **brjobs** é um **marketplace bilateral** onde:
- **Prestadores** publicam serviços que oferecem (o que fazem)
- **Contratantes** publicam serviços que precisam (o que desejam)
- Ambos têm **perfil isolado** e acessam dados cada um dentro do seu tenant

A implementação de **multi-tenancy** (Tenant = Usuário) garante que cada usuário veja/modifique apenas seus próprios dados, enquanto buscas são cross-tenant (vejo serviços de outros, mas apenas minhas solicitações).

---

## 1. Contexto & Problema

### Cenário Atual
- Aplicação single-tenant ou inadequadamente isolada
- Prestadores e contratantes compartilham contexto sem isolamento adequado
- Falta de separação clara de dados por usuário

### Dor Principal
Implementar isolamento de dados seguro onde cada usuário (prestador ou contratante) seja um tenant independente, garantindo segurança, performance e escalabilidade.

### Objectivo
Completar a implementação de multi-tenancy no brjobs, já iniciada, com:
- ✅ Isolamento de dados por usuário (tenant_id)
- ✅ Validação de acesso em todas as camadas
- ✅ Chat 1:1 entre prestadores e contratantes isolado por tenant par
- ✅ Avaliações (estrelas + comentários) com filtro de conteúdo maldoso
- ✅ Busca paginada (10 itens/página) com filtros por categoria

---

## 2. Personas & Papéis

| Persona | Descrição | Dados Isolados | Ações |
|---------|-----------|-----------------|-------|
| **Prestador** | Oferece serviços (pintura, reparos, etc.) | Serviços, avaliações recebidas, chat, ganhos | Publicar serviço, responder solicitações, receber avaliações, acessar ganhos |
| **Contratante** | Busca e contrata serviços | Solicitações, avaliações dadas, chat, pagamentos | Buscar serviços, publicar solicitação, avaliar prestador, conversar |
| **Admin** (futuro) | Moderação (opcional) | Dados globais, reports | Bloquear users, remover comentários ofensivos |

---

## 3. Descrição da Feature

### 3.1 Isolamento de Dados (Tenant = Usuário)

Cada usuário logado é um **tenant independente**. O sistema mantém `tenant_id = user_id` a nível de lógica.

**Regra de Isolamento:**
```
SELECT * FROM servicos WHERE prestador_id = :tenant_id       ✅ (meus serviços)
SELECT * FROM servicos WHERE 1=1                            ✅ (busca pública - todos)
SELECT * FROM solicitacoes WHERE contratante_id = :tenant_id ✅ (minhas solicitações)
SELECT * FROM solicitacoes WHERE contratante_id != :tenant_id ❌ (FORBIDDEN)
```

**Dados isolados por tenant:**
- `Servico` — apenas serviços publicados pelo prestador logado
- `SolicitacaoServico` — apenas solicitações criadas pelo contratante logado
- `Avaliacao` — apenas avaliações dadas/recebidas pelo user
- `ChatMessage` — apenas mensagens entre pares específicos
- `RelatorioGanhos` — apenas para prestadores logados (dados privados)

---

### 3.2 Recursos Principales

#### A) Perfis Públicos (Cross-Tenant Read)
- ✅ Contratante **pode visualizar** perfil de prestador
- ✅ Prestador **pode visualizar** perfil de contratante
- ✅ Perfil inclui: nome, foto, avaliações, categoria de serviços
- ❌ Segredos (CPF, número de contas, etc.) **nunca expostos**

#### B) Busca & Listagem de Serviços
- **Paginação:** 10 serviços por página
- **Filtros:** por categoria (pintura, reparos, faxina, construção, marcenaria, otros)
- **Busca:** por texto (nome do serviço, descrição)
- **Ordenação:** recentes, mais avaliados, preço
- **Performance:** < 2-5 segundos (benchmark GetNinjas)

#### C) Chat 1:1
- ✅ Mensagens entre prestador ↔ contratante
- ✅ Restrição: apenas participantes veem o chat
- ✅ Persistência: histórico completo (sem purga automática)
- ⚠️ **Ponto em aberto:** será via websocket (real-time) ou polling?

#### D) Avaliações (Stars + Comments)
- **Estrutura:** 1-5 estrelas + comentário opcional
- **Filtro de Conteúdo:** validar comentários contra lista de palavrões/maldições
- **Quem avalia quem:**
  - Contratante avalia Prestador (após contratação)
  - Prestador avalia Contratante (opcional, após conclusão)
- **Visibilidade:** público no perfil (sem edição após publicação)

#### E) Relatório de Ganhos (Prestador Only)
- Resumo mensal/anual de faturamento
- Breakdown por categoria/cliente
- Apenas prestador vê seus ganhos

---

## 4. Stack Técnica

### Backend: Spring Boot 3.3.5 + Java 17

**Padrão arquitetural:** Controller → Service → Repository → PostgreSQL

**Camadas envolvidas:**
1. **Repository/DAO:** incluir filtro `tenant_id` em todas as queries
2. **Service:** validação de tenant antes de acesso aos dados
3. **Controller/REST:** extrair `tenant_id` do JWT, passar para service
4. **Security/JWT:** guardar `user_id` no token, usar como `tenant_id`

**Dependências:** 
- Spring Security (JWT)
- Spring Data JPA
- PostgreSQL JDBC
- Lombok (@Slf4j, @Data)

### Frontend: Angular 20 + TypeScript 5.9

**Padrão:** Componentes por feature + Services HTTP

**Componentes afetados:**
- `search/` — busca paginada com filtros
- `profile/` — visualização de perfil público + rating
- Chat component (novo)
- Relatório de ganhos (novo)

**Services:**
- `auth.service.ts` — extrair `user_id` do JWT
- `service.service.ts` — listar serviços (cross-tenant)
- `chat.service.ts` — mensagens 1:1
- `rating.service.ts` — avaliações
- `earnings.service.ts` — relatório de ganhos (prestador)

### Banco de Dados: PostgreSQL

**Tabelas afetadas/novas:**
| Tabela | Tenant Field | Novo? | Notas |
|--------|--------------|-------|-------|
| usuarios | - | ❌ | Base de usuários (table "main") |
| servicos | prestador_id (FK Usuario) | ❌ | Existente, adicionar índice em prestador_id |
| solicitacoes_servico | contratante_id (FK Usuario) | ❌ | Existente, adicionar índice |
| avaliacoes | usuario_id (quem avalia) + alvo_id (quem é avaliado) | ✅ Novo | 1-5 stars + comment |
| chat_messages | remetente_id + destinatario_id | ✅ Novo | Mensagens 1:1 |
| conversas_chat | usuario_1_id + usuario_2_id | ✅ Novo | Metadados da conversa |
| relatorio_ganhos | prestador_id | ✅ Novo | Cache/agregado de faturamento |

---

## 5. Estratégia de Identificação do Tenant

**Método:** JWT claim `user_id`

**Fluxo:**
1. Login com email/password
2. Backend valida credentials
3. Gera JWT com claim `user_id` (= tenant_id)
4. Frontend guarda token em localStorage
5. Cada request inclui `Authorization: Bearer <token>`
6. Backend extrai `user_id` do token, valida como tenant

**Válido porque:**
- ✅ Simples (sem overhead de headers customizados)
- ✅ Immutável per request (token não muda)
- ✅ Suporta múltiplas abas/browsers (token único)
- ✖️ Usuário fixo a um tenant per sessão (é OK para brjobs — sem multi-org)

---

## 6. Segurança & Controle de Acesso (RBAC)

### Validação de Tenant em 3 Camadas

#### 1. **JWT/Security Filter (Spring Security)**
```java
// Extrair user_id do token
String tenantId = extractUserIdFromToken(req.getHeader("Authorization"));
req.setAttribute("tenant_id", tenantId);
```

#### 2. **Service Layer**
```java
// Validar antes de buscar
public Servico getServico(Long servicoId, Long tenantId) {
    Servico s = repo.findById(servicoId);
    if (!s.getPrestadorId().equals(tenantId)) {
        throw new AccessDeniedException("Acesso negado");
    }
    return s;
}
```

#### 3. **Repository/Query Level**
```java
// Garantir que query include tenant_id
repo.findByIdAndPrestadorId(servicoId, tenantId);
```

### Casos de Uso de Acesso

| Ação | Tenant A → Dados B | Permitido? | Razão |
|------|-------------------|-----------|-------|
| Listar serviços de todos | A → todos | ✅ | Busca pública |
| Visualizar perfil de B | A → perfil B (público) | ✅ | Perfil é público |
| Editar meu serviço | A → serviço A | ✅ | Tenant = Owner |
| Editar serviço de B | A → serviço B | ❌ | Tenant != Owner |
| Ver minha avaliação dado a B | A → rating A→B | ✅ | Tenant = creator |
| Ver avaliação que B deu a mim | A → rating B→A | ✅ | Público no perfil |
| Enviar msg para B | A → chat A↔B | ✅ | Ambos participantes |
| Ver chat de B com C | A → chat B↔C | ❌ | Tenant não é participante |

---

## 7. Fluxos Principales

### 7.1 Busca de Serviços (Contratante)
```
User (contratante) → GET /api/v1/servicos?categoria=pintura&page=1&size=10
Backend:
  1. Extrair tenant_id do JWT
  2. Query: SELECT * FROM servicos WHERE categoria='pintura' ORDER BY created_at DESC LIMIT 10
     (sem filtro tenant_id — é busca pública)
  3. Enriquecer com: avaliacao_media_prestador, foto_prestador
User vê: 10 serviços com fotos, preços, ratings
```

### 7.2 Publicar Serviço (Prestador)
```
User (prestador) → POST /api/v1/servicos
  Body: { nome, descricao, categoria, preco, ... }
Backend:
  1. Extrair tenant_id do JWT
  2. Criar Servico com prestador_id = tenant_id
  3. Save & return
User vê: seu serviço disponível na busca pública
```

### 7.3 Enviar Mensagem (Chat 1:1)
```
User A → POST /api/v1/chat/{user_b_id}
  Body: { conteudo: "Oi, posso conversar?" }
Backend:
  1. Extrair tenant_id (A) do JWT
  2. Validar que user_b_id existe
  3. Criar ChatMessage(remetente_id=A, destinatario_id=B, conteudo=...)
  4. Criar ou update Conversa(user_1_id=min(A,B), user_2_id=max(A,B), updated_at=now)
  5. [TODO: Notificar B via email / push / websocket?]
User A vê: mensagem enviada
User B vê: notificação + nova msg na conversa
```

### 7.4 Avaliar Prestador
```
User (contratante) → POST /api/v1/avaliacoes
  Body: { alvo_id: prestador_id, stars: 5, comentario: "Adorei!" }
Backend:
  1. Extrair tenant_id (contratante) do JWT
  2. Validar que contratante tem transação com prestador (FK SolicitacaoServico)
  3. Executar filtro_maldizente(comentario) → se matched, reject
  4. Criar Avaliacao(usuario_id=tenant, alvo_id=prestador, ...)
User vê: avaliação publicada no perfil do prestador
```

### 7.5 Relatório de Ganhos (Prestador)
```
User (prestador) → GET /api/v1/ganhos?mes=202604
Backend:
  1. Extrair tenant_id do JWT
  2. Validar que tenant_id é prestador (tipo_usuario = 'PRESTADOR')
  3. Query: SUM(preco) FROM servicos WHERE prestador_id=tenant_id AND data BETWEEN x AND y
  4. Groupby categoria / cliente
User vê: tabela de ganhos detalhada (apenas seus dados)
```

---

## 8. Padrões Internos Identificados

Baseado na leitura de [CLAUDE.md](CLAUDE.md) e exploração do codebase:

### Backend (Spring Boot)
- ✅ **Camadas:** Controller → Service → Repository (strict)
- ✅ **Entidades:** `*Model` suffix (Usuario, Servico, SolicitacaoServico, Prestador, Avaliacao)
- ✅ **DTOs:** `*DTO` suffix
- ✅ **Services:** `*Service` suffix
- ✅ **Repositories:** Spring Data JPA, `*Repository` interface
- ✅ **Security:** JWT em `security/` package
- ✅ **Logging:** SLF4J via `@Slf4j`

### Frontend (Angular)
- ✅ **Componentes:** Feature-based em `src/app/components/feature-name/`
- ✅ **Services:** `src/app/service/*.service.ts`
- ✅ **Routing:** `app.routes.ts`
- ✅ **Selectors:** `app-feature-name` (lowercase hyphens)
- ✅ **Styling:** Tailwind CSS v4
- ✅ **TypeScript:** strict mode (`strict: true`)

---

## 9. Integrações Externas

### Chat (Comunicação)
⚠️ **Ponto em aberto:** 
- Websocket (real-time, mais complexo) ou
- Polling HTTP (simples, menos eficiente)?

**Impacto:** afeta design de backend (Socket.io, Undertow vs. HTTP simples).

### Filtro de Conteúdo Maldoso
⚠️ **Ponto em aberto:** 
- Lista local de palavrões? 
- Serviço externo (ex: OpenAI Moderation API)?
- Regex patterns customizadas?

**Impacto:** custo (API remota), latência, manutenção.

### Notificações (Chat)
⚠️ **Ponto em aberto:** 
- Email simples?
- Push notifications (mobile)?
- In-app notifications?

---

## 10. Performance & SLA

### Requisitos de Latência

Baseado em pesquisa de mercado (GetNinjas, Freelancer):

| Operação | SLA | Justificativa |
|----------|-----|--------------|
| Busca de serviços | < 2s | GetNinjas: resposta em "instantes" para profissionais |
| Listar meus serviços | < 1s | Dados pessoais, baixo volume |
| Enviar mensagem | < 1s | Chat real-time esperado |
| Publicar avaliação | < 2s | Operação de escrita, com validação |
| Relatório de ganhos | < 3s | Agregado, pode ser pré-computado |

### Tratamento de Concorrência

⚠️ **Ponto em aberto — CRÍTICO:**
- Múltiplos prestadores competindo por mesma solicitação
- Múltiplas abas abrindo/fechando chats
- Avaliações simultâneas no mesmo serviço

**Estratégias a explorar:**
1. **Pessimistic Locking:** `SELECT ... FOR UPDATE` em transações críticas
2. **Optimistic Locking:** versioning das entidades (ex: `versao INT`)
3. **Message Queues:** Redis/RabbitMQ para operações assíncronas
4. **Distributed Locks:** Redis SETEX para garantir singletons

---

## 11. Observabilidade

### Logs
- ✅ Todas as operações de tenant (acesso, criação, deleção)
- ✅ Falhas de validação de tenant
- ✅ Operações sensíveis (pagamento, avaliação)
- **Padrão:** SLF4J com context `[tenant_id=X]`

### Métricas
⚠️ **Ponto em aberto:**
- Latência por operação (busca, chat, avaliação)
- Taxa de acesso negado por tenant
- Contagem de mensagens/avaliações por período

### Alertas
⚠️ **Ponto em aberto:**
- Taxa anormal de acesso negado (possível ataque)?
- Latência > 5s em busca (degradação)?

---

## 12. Rollout & Rollback

### Rollout Controlado

1. **Fase 1 — Deploy com feature flag OFF**
   - Código multitenancy pronto, mas desativado
   - Usuários continuam em single-tenant

2. **Fase 2 — Canary: 10% dos usuários**
   - Feature flag permite multitenancy para grupo pequeno
   - Monitor: taxa de erro, latência, logs

3. **Fase 3 — Gradual: 50% → 100%**
   - Aumentar percentual dia a dia
   - Monitorar métricas críticas

4. **Fase 4 — Deprecate single-tenant**
   - Remover código legado após 1 mês de 100%

### Rollback

Se problemas críticos:
```bash
# Feature flag OFF
# Reiniciar aplicação
# Usuários revertidos a single-tenant (sem perda de dados)
# Investigar logs em dev
```

---

## 13. Dados de Referência (Benchmarking)

### GetNinjas (Brasil, mais similar ao brjobs)
- 4 milhões servicos/ano
- 2 milhões profissionais
- Oferta em "instantes" (< 60s)

### Freelancer (global)
- 87.5 milhões profissionais
- "80% de jobs recebem propostas em 60 segundos"
- Indica latência de resposta < 2-5 segundos

### Conclusão
**Esperado:** aplicação robusta que suporte múltiplas requisições simultâneas com latência < 2-5 segundos para operações críticas.

---

## 14. Checklist de Implementação

### Backend (Spring Boot)
- [ ] Adicionar `tenant_id` (= `user_id` do JWT) em todas as queries do repository
- [ ] Criar `TenantValidator` — middleware que extrai e valida tenant
- [ ] Padrão: `getByIdAndTenantId()` em cada repo
- [ ] Criar tabelas: `avaliacoes`, `chat_messages`, `conversas_chat`
- [ ] Criar services: `AvaliacaoService`, `ChatService`, `GanhosService`
- [ ] Criar controllers: `ChatController`, `AvaliacaoController`, `GanhosController`
- [ ] Implementar filtro de palavrões em `AvaliacaoService`
- [ ] Tests: validar que tenant A não acessa dados de B

### Frontend (Angular)
- [ ] `search.component` — paginação + filtros categoria
- [ ] `chat-list.component` — listar conversas
- [ ] `chat-message.component` — enviar/receber mensagens
- [ ] `rating-form.component` — formulário de avaliação
- [ ] `profile-view.component` — visualizar perfil público + ratings
- [ ] `earnings-report.component` — relatório de ganhos (prestador)
- [ ] `chat.service` — HTTP calls para mensagens
- [ ] `rating.service` — HTTP calls para avaliações
- [ ] Tests: validar isolamento de dados por tenant

### Database
- [ ] Migrações: criar tabelas `avaliacoes`, `chat_messages`, `conversas_chat`
- [ ] Índices: `avaliacoes(usuario_id, alvo_id)`, `chat_messages(remetente_id, destinatario_id)`, `servicos(prestador_id)`, `solicitacoes(contratante_id)`
- [ ] Validar que dados históricos não quebram ao adicionar novos campos

---

## 15. Lacunas & Pontos em Aberto

### Críticos (Resolver antes de iniciar código)

⚠️ **Chat — Real-time vs. Polling**
- Decisão necessária: websocket + Socket.io (complexo) ou polling HTTP (simples)?
- Impacto: design backend, dependências, performance

⚠️ **Filtro de Palavrões**
- Solução: lista local, API externa, ML model?
- Custo/latência?

⚠️ **Notificações de Chat**
- Email / push / in-app / nada?
- Quem implementa? (backend, frontend, serviço 3º)

⚠️ **Tratamento de Concorrência**
- Locking pessimista? Otimista? Message queues?
- Precisa teste de carga antes de decidir

⚠️ **Relatório de Ganhos — Pré-Computado?**
- Query ao vivo (mais simples, potencialmente lento)
- Cache/agregado (mais rápido, complexo de manter sincronizado)

### Secundários (Podem ser v2 ou futuro)

- [ ] Admin panel para moderação (bloquear users, remover comentários)
- [ ] Dispute resolution (contratante vs. prestador)
- [ ] Payment integration (Stripe, PagSeguro, etc.)
- [ ] SMS/Whatsapp notifications
- [ ] Analytics & business intelligence
- [ ] Marketplace fees / comissão por serviço

---

## 16. Próximos Passos

1. ✅ **Discovery concluído**

2. 📋 **Executar /lf-new-feature**
   - Gerar `briefing-tech.vN.md` com 15 seções
   - Incluir diagrama ER atualizado com novas tabelas

3. 📐 **Executar /lf-specs** (após UX/design)
   - Gerar `specs.md` com SPEC-XX granulares
   - Gerar `wps.md` com work packages e dependências

4. 🚀 **Executar /lf-exec WP-01**
   - Começar implementação por módulo (ex: WP-01 — Setup DB + Repos)
   - Incrementalmente completar multi-tenancy

---

## Resumo da Discovery

| Campo | Valor |
|-------|-------|
| **Tipo** | Brownfield — Multi-tenancy (Tenant = Usuário) |
| **Escopo** | Isolamento completo: dados, busca, chat, avaliações, ganhos |
| **Stack** | Spring Boot 3.3.5 + Angular 20 + PostgreSQL |
| **SLA Target** | < 2-5s para operações críticas (busca, chat) |
| **Complexidade** | Alta — concorrência, segurança, performance |
| **Timeline** | Estimado 4-8 semanas para código + testes |
| **Riscos Principais** | 1) Concorrência, 2) Performance de busca, 3) Chat real-time |

---

**Próximo passo:** `/lf-new-feature` para detalhar design técnico e trabalhar com UX/design.

# Work Packages: Multi-Tenancy no brjobs

**Data:** 8 April 2026  
**Versão:** v0  
**Projeto:** brjobs — Multi-Tenancy (Tenant = Usuário)  
**Status:** Ready for Execution

---

## Dependency Map

```
┌─────────────────────────────────────────────────────────────┐
│ FASE 1: INFRASTRUCTURE (1-2 semanas)                        │
├─────────────────────────────────────────────────────────────┤
│ WP-01: Setup Database & Migrations                          │
│ WP-02: TenantValidator Filter & Security                    │
│ WP-03: Base Repository Patterns (tenant_id everywhere)      │
└──────────────┬──────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────┐
│ FASE 2: CORE FEATURES (3-4 semanas)                         │
├─────────────────────────────────────────────────────────────┤
│ WP-04: Busca & Listagem de Serviços (REST endpoint)         │
│ WP-05: Chat Real-Time (WebSocket + Socket.io)               │
│ WP-06: Avaliações & Filtro Palavrões                        │
│ WP-07: Relatório de Ganhos (Trigger SQL + Service)          │
│ WP-08: Publicar Serviço (POST endpoint)                      │
└──────────────┬──────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────┐
│ FASE 3: FRONTEND (2-3 semanas)                              │
├─────────────────────────────────────────────────────────────┤
│ WP-09: SearchComponent (busca paginada + filtros)           │
│ WP-10: ChatComponent (lista conversas + mensagens)          │
│ WP-11: RatingComponent (formulário + listagem)              │
│ WP-12: EarningsComponent (relatório)                        │
│ WP-13: ProfileComponent (visualizar público)                │
└──────────────┬──────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────┐
│ FASE 4: INTEGRATION & TESTING (1-2 semanas)                 │
├─────────────────────────────────────────────────────────────┤
│ WP-14: End-to-End Tests (Postman / Selenium)                │
│ WP-15: Load Testing (JMeter)                                │
│ WP-16: Security Tests (Access Denial, Concurrency)          │
│ WP-17: Feature Flags & Rollout Setup                        │
└──────────────────────────────────────────────────────────────┘
```

---

## Work Packages Detalhados

---

## WP-01: Setup Database & Migrations

**Fase:** 1 (Infrastructure)  
**Duração Estimada:** 3-4 dias  
**Dependência:** Nenhuma  
**Owner (Backend):** Eng-Backend-01  

### Descrição
Criar migrações SQL para:
- Tabelas novas: `avaliacoes`, `chat_messages`, `conversas_chat`
- Tabelas de cache: `relatorio_ganhos_cache`, `relatorio_ganhos_categoria`, `relatorio_ganhos_cliente`
- Índices críticos
- Triggers para atualizar agregados

### Tasks

- [ ] **T-01.1** — Criar migração: `0001_create_avaliacoes_table.sql`
  - Tabela `avaliacoes` com campos: id, usuario_id, alvo_id, stars, comentario, created_at
  - Constraints: UNIQUE(usuario_id, alvo_id), CHECK stars 1-5
  - Index: (alvo_id), (usuario_id, alvo_id)

- [ ] **T-01.2** — Criar migração: `0002_create_chat_tables.sql`
  - Tabela `chat_messages`: id, remetente_id, destinatario_id, conteudo, lido, notificado, created_at
  - Tabela `conversas_chat`: id, usuario_1_id, usuario_2_id, ultima_mensagem_id, updated_at
  - Constraints: UNIQUE conversas, CHECK remetente != destinatario
  - Index: (remetente_id, destinatario_id), (destinatario_id, lido), (notificado, created_at)

- [ ] **T-01.3** — Criar migração: `0003_create_ganhos_cache_tables.sql`
  - Tabela `relatorio_ganhos_cache`: id, prestador_id, mes_ano, total_faturado, num_servicos, updated_at
  - Tabela `relatorio_ganhos_categoria`: id, cache_id, categoria, total, num_servicos
  - Tabela `relatorio_ganhos_cliente`: id, cache_id, cliente_id, cliente_nome, total, num_servicos
  - Constraints: UNIQUE(prestador_id, mes_ano)
  - FK on DELETE CASCADE

- [ ] **T-01.4** — Criar migração: `0004_add_indexes.sql`
  - Index em `servicos(prestador_id)`
  - Index em `servicos(categoria, criado_em DESC)`
  - Index em `solicitacoes_servico(contratante_id)`
  - GIST index para full-text search (futuro)

- [ ] **T-01.5** — Criar trigger: `atualizar_relatorio_ganhos()`
  - Disparado em UPDATE servicos SET status='CONCLUIDO'
  - Recalcula cache de relatório para aquele prestador/mês

- [ ] **T-01.6** — Testes
  - Script SQL para validar estrutura (SELECT * FROM avaliacoes LIMIT 0; etc)
  - Teste de trigger: INSERT servico com status='CONCLUIDO', verificar cache atualizado
  - Documento de rollback em caso de erro

### Entregáveis

```
brjobs-java/migrations/
├── 0001_create_avaliacoes_table.sql
├── 0002_create_chat_tables.sql
├── 0003_create_ganhos_cache_tables.sql
├── 0004_add_indexes.sql
└── README.md (instruções de rollback)
```

### Done Criteria
- [ ] Todas as migrations runáveis
- [ ] Prod database structure matches dev
- [ ] Indexes criados e validados (EXPLAIN plan < 10ms)
- [ ] Trigger testado end-to-end

---

## WP-02: TenantValidator Filter & Security

**Fase:** 1 (Infrastructure)  
**Duração Estimada:** 2-3 dias  
**Dependência:** Nenhuma (WP-01 paralelo)  
**Owner (Backend):** Eng-Backend-01  

### Descrição
Implementar:
- JWT Filter que extrai `user_id` como `tenant_id`
- Validator Aspect para validação de isolamento em services
- Custom annotation `@ValidateTenant`
- Logging centralizado de acesso negado

### Tasks

- [ ] **T-02.1** — Classe `TenantFilter extends OncePerRequestFilter`
  - Extrair token de Authorization header
  - Validar assinatura JWT
  - Extrair `user_id` claim
  - Armazenar em `req.setAttribute("tenant_id", userId)`
  - Retornar 401 se inválido

- [ ] **T-02.2** — Classe `TenantValidationAspect` (AOP)
  - Interceptar métodos anotados com `@ValidateTenant`
  - Extrair 1º argumento (tenantId esperado) e HttpRequest attribute (tenant_id real)
  - Validar igualdade
  - Lancar `AccessDeniedException` se não bater
  - Log em caso de negação

- [ ] **T-02.3** — Annotation `@ValidateTenant`
  - Target: METHOD
  - Retention: RUNTIME
  - Documentação de uso

- [ ] **T-02.4** — Registrar no Spring Security Config
  - Adicionar TenantFilter à chain de filters
  - Order: ANTES de outras autentigações

- [ ] **T-02.5** — Tests
  - Test: JWT válido → tenant_id extraído corretamente
  - Test: JWT inválido → 401
  - Test: @ValidateTenant com match → OK
  - Test: @ValidateTenant com mismatch → 403 AccessDeniedException
  - Test: Rate limiting em falhas de auth (5+ tentativas em 5min?)

### Entregáveis

```
brjobs-java/src/main/java/ads/uninassau/brjobs/
├── config/
│   └── SecurityConfig.java (adicionar filter)
├── security/
│   ├── TenantFilter.java
│   ├── TenantValidationAspect.java
│   └── ValidateTenant.java (annotation)
└── src/test/.../
    └── TenantValidationTest.java
```

### Done Criteria
- [ ] Filter funciona em todos os endpoints
- [ ] @ValidateTenant valida isolamento
- [ ] 401 / 403 retornados corretamente
- [ ] Tests passam (unit + integration)
- [ ] Documentação de uso em code comments

---

## WP-03: Base Repository Patterns (Tenant-Aware Queries)

**Fase:** 1 (Infrastructure)  
**Duração Estimada:** 2-3 dias  
**Dependência:** WP-01 (migrations existem)  
**Owner (Backend):** Eng-Backend-01  

### Descrição
Atualizar/criar repositories existentes para incluir `tenant_id` em todas as queries:
- ServicoRepository: findByIdAndPrestadorId
- SolicitacaoServicoRepository: findByIdAndContratanteId
- Novas: AvaliacaoRepository, ChatMessageRepository, ConversaChatRepository

### Tasks

- [ ] **T-03.1** — ServicoRepository
  - Método: `Servico findByIdAndPrestadorId(Long id, Long prestadorId)`
  - Método: `Page<Servico> buscar(String categoria, String search, String sort, Pageable pg)`

- [ ] **T-03.2** — SolicitacaoServicoRepository
  - Método: `SolicitacaoServico findByIdAndContratanteId(Long id, Long contratanteId)`
  - Método: `boolean existsByContratanteIdAndPrestadorId(Long cId, Long pId)`

- [ ] **T-03.3** — AvaliacaoRepository (nova)
  ```java
  Page<Avaliacao> findByAlvoId(Long alvoId, Pageable pg);
  Optional<Avaliacao> findByUsuarioIdAndAlvoId(Long usuarioId, Long alvoId);
  boolean existsByUsuarioIdAndAlvoId(Long usuarioId, Long alvoId);
  Double getAvaliacaoMedia(Long alvoId); // @Query customizada
  ```

- [ ] **T-03.4** — ChatMessageRepository (nova)
  ```java
  List<ChatMessage> findByRemetenteIdAndDestinatarioIdOrderByCreatedAtDesc(Long rem, Long dest);
  List<ChatMessage> findByDestinatarioIdAndLidoFalse(Long destId);
  List<ChatMessage> findByNotificadoFalse(); // para job
  ```

- [ ] **T-03.5** — ConversaChatRepository (nova)
  ```java
  Optional<ConversaChat> findByUsuario1IdAndUsuario2Id(Long u1, Long u2);
  List<ConversaChat> findByUsuario1IdOrUsuario2IdOrderByUpdatedAtDesc(Long u1, Long u2);
  ```

- [ ] **T-03.6** — Tests
  - Test findByIdAndPrestadorId: retorna apenas se tenant match
  - Test findByIdAndPrestadorId: null se tenant != prestador_id
  - Test Chat queries com múltiplas mensagens
  - Test Avaliacao calcular média corretamente

### Entregáveis

```
brjobs-java/src/main/java/ads/uninassau/brjobs/repository/
├── ServicoRepository.java (atualizado)
├── SolicitacaoServicoRepository.java (atualizado)
├── AvaliacaoRepository.java (novo)
├── ChatMessageRepository.java (novo)
└── ConversaChatRepository.java (novo)

brjobs-java/src/test/.../repository/
└── TenantAwareRepositoryTest.java
```

### Done Criteria
- [ ] Todos os métodos implementados
- [ ] Queries usam índices (EXPLAIN plan validado)
- [ ] Tests passam
- [ ] Documentação de padrão em README

---

## WP-04: Busca & Listagem de Serviços (Backend)

**Fase:** 2 (Core Features)  
**Duração Estimada:** 3-4 dias  
**Dependência:** WP-01, WP-02, WP-03  
**Owner (Backend):** Eng-Backend-02  

### Descrição
Implementar endpoint REST:
```
GET /api/v1/servicos?categoria=pintura&search=sp&page=1&size=10&sort=recente
```

Com paginação, filtros, enriquecimento de dados (prestador + rating).

### Tasks

- [ ] **T-04.1** — ServicoService
  - Método: `Page<ServicoDTO> buscar(String categoria, String search, int page, int size, String sort)`
  - Validar inputs (page >= 1, size 1-100, sort válido)
  - Chamar repository
  - Mapear para DTO com dados de prestador + avaliacao_media

- [ ] **T-04.2** — ServicoController
  - Endpoint: `GET /api/v1/servicos`
  - QueryParams: categoria, search, page, size, sort
  - Response: Page<ServicoDTO> com 200 OK

- [ ] **T-04.3** — ServicoDTO
  - Campos: id, nome, descricao, categoria, preco, prestador (nested DTO), criadoEm
  - PrestadorDTO: id, nome, avaliacao_media, num_avaliacoes, foto_url

- [ ] **T-04.4** — Validação & Error Handling
  - 400 se categoria inválida
  - 400 se sort inválido
  - 200 OK com content vazio se nenhum resultado

- [ ] **T-04.5** — Tests
  - Test busca sem filtro retorna 10 primeiros
  - Test filtro categoria funciona
  - Test busca texto funciona
  - Test paginação (page=2 retorna items 11-20)
  - Test ordenação (recente, avaliacoes, preco)
  - Performance (< 2s com 100K items)

### Entregáveis

```
brjobs-java/src/main/java/ads/uninassau/brjobs/
├── controller/ServicoController.java (adicionar buscar endpoint)
├── service/ServicoService.java (adicionar buscar method)
└── dto/
    ├── ServicoDTO.java
    └── PrestadorDTO.java

brjobs-java/src/test/.../
└── ServicoControllerTest.java
```

### Done Criteria
- [ ] Endpoint implementado e funcionalizado
- [ ] Todos os filtros trabalham
- [ ] Performance < 2s
- [ ] Tests passam
- [ ] Swagger docs atualizadas

---

## WP-05: Chat Real-Time (Backend + Socket.io)

**Fase:** 2 (Core Features)  
**Duração Estimada:** 5-6 dias  
**Dependência:** WP-01, WP-02, WP-03, SendGrid + Firebase setup  
**Owner (Backend):** Eng-Backend-02, Owner (DevOps): Eng-DevOps-01  

### Descrição
Implementar WebSocket real-time com Socket.io, mensagens persistidas, e notificações offline (email + push).

### Tasks

- [ ] **T-05.1** — Adicionar dependência Socket.io `pom.xml`
  ```xml
  <dependency>
      <groupId>io.socket</groupId>
      <artifactId>socket.io-server-java</artifactId>
      <version>4.x</version>
  </dependency>
  ```

- [ ] **T-05.2** — Classe `ChatSocketHandler`
  - Registrar handlers para eventos: `mensagem:enviar`, `mensagem:lida`, `usuario:digitando`
  - Autenticar conexão via JWT
  - Emitir eventos para cliente destinatário se online

- [ ] **T-05.3** — ChatService (persistência)
  - Método: `enviarMensagem(Long remetenteId, Long destId, String conteudo)`
  - Validar isolamento (tenant_id)
  - Inserir em chat_messages
  - Atualizar conversas_chat
  - Retornar mensagem

- [ ] **T-05.4** — ChatController (para polling, backup)
  - GET `/api/v1/chat/{user_id}/mensagens?page=1` — histórico
  - POST `/api/v1/chat/enviar` — fallback se WebSocket down

- [ ] **T-05.5** — Background Job (Notifications)
  - Classe: `ChatNotificationJob`
  - Schedule: @Scheduled(fixedDelay = 60000)
  - Logic: SelectBy notificado=false, enviar email via SendGrid + push via Firebase
  - Mark como notificado=true

- [ ] **T-05.6** — SendGrid Integration
  - Service: `SendGridService.sendEmail(...)`
  - Template: "Nova mensagem de {nome}"

- [ ] **T-05.7** — Firebase Integration
  - Service: `FirebaseService.sendPushNotification(...)`
  - Validar firebase tokens armazenados em usuarios table (add column if needed)

- [ ] **T-05.8** — Tests
  - Test WebSocket conexão com JWT válido
  - Test WebSocket 401 com token inválido
  - Test enviar msg User A → User B (online) — entrega < 500ms
  - Test enviar msg User A → User B (offline) — persistido
  - Test Background job envia email + push corretamente
  - Stress test: 100 msgs simultâneas

### Entregáveis

```
brjobs-java/src/main/java/ads/uninassau/brjobs/
├── socket/ChatSocketHandler.java
├── service/ChatService.java
├── controller/ChatController.java
├── job/ChatNotificationJob.java
├── integration/
│   ├── SendGridService.java
│   └── FirebaseService.java
└── dto/ChatMessageDTO.java

brjobs-java/src/test/.../
└── ChatIntegrationTest.java
```

### Configuration
- [ ] Socket.io listening port: 8080 (ou separado?)
- [ ] Firebase service account key armazenado securely (env var)
- [ ] SendGrid API key armazenado securely (env var)

### Done Criteria
- [ ] WebSocket conexão funciona
- [ ] Mensagens persistidas no DB
- [ ] Real-time entrega para online users
- [ ] Email + push para offline users
- [ ] Performance stress test passed
- [ ] Tests passam

---

## WP-06: Avaliações & Filtro Palavrões (Backend)

**Fase:** 2 (Core Features)  
**Duração Estimada:** 3-4 dias  
**Dependência:** WP-01, WP-02, WP-03  
**Owner (Backend):** Eng-Backend-03  

### Descrição
Implementar POST `/api/v1/avaliacoes` com validação de palavrões e isolamento de tenant.

### Tasks

- [ ] **T-06.1** — Criar lista de palavrões (pt-BR)
  - Arquivo: `brjobs-java/config/palabras-proibidas.txt` (~500 palavras)
  - Ou carregar dinamicamente de banco (table `palavras_bloqueadas`)

- [ ] **T-06.2** — AvaliacaoService
  - Método: `criar(Long tenantId, CriarAvaliacaoDTO dto)`
  - Validar: stars 1-5, comentário max 500 chars, obrigatório se stars <= 3
  - Filtrar palavrões: `contemPalavrasProibidas(texto)` → throw ValidationException
  - Validar transação: SELECT existByContratanteIdAndPrestadorId
  - Validar unicidade: SELECT existByUsuarioIdAndAlvoId
  - Salvar Avaliacao
  - Método: `listarPorAlvo(Long alvoId)` — retorna lista pública
  - Método: `getAvaliacaoMedia(Long alvoId)` — agregado

- [ ] **T-06.3** — AvaliacaoController
  - POST `/api/v1/avaliacoes` → criar(tenantId, dto)
  - GET `/api/v1/avaliacoes/usuario/{alvoId}` → listarPorAlvo
  - GET `/api/v1/avaliacoes/media/{alvoId}` → getMedia

- [ ] **T-06.4** — AvaliacaoDTO
  - CriarAvaliacaoDTO: alvo_id, stars, comentario
  - AvaliacaoDTO: id, usuario_id, alvo_id, stars, comentario, criado_em

- [ ] **T-06.5** — Tests
  - Test: 5 stars → 201 Created
  - Test: Comentário com xingamento → 400 Bad Request
  - Test: 2 stars sem comentário → 400
  - Test: 2ª avaliação para mesmo par → 409 Conflict
  - Test: User A tenta avaliar si mesmo → 400
  - Test: User A avalia B (que não conhece) → 400 "sem contrato"
  - Test: Listar avaliações de prestador → retorna array público

### Entregáveis

```
brjobs-java/src/main/java/ads/uninassau/brjobs/
├── config/palabras-proibidas.txt
├── service/AvaliacaoService.java
├── controller/AvaliacaoController.java
└── dto/
    ├── CriarAvaliacaoDTO.java
    └── AvaliacaoDTO.java

brjobs-java/src/test/.../
└── AvaliacaoControllerTest.java
```

### Done Criteria
- [ ] POST /avaliacoes funciona
- [ ] Filtro palavrões bloqueia corretamente
- [ ] Isolamento de tenant validado
- [ ] Tests passam
- [ ] Swagger docs updated

---

## WP-07: Relatório de Ganhos (Trigger SQL + Service)

**Fase:** 2 (Core Features)  
**Duração Estimada:** 3 dias  
**Dependência:** WP-01 (trigger criado), WP-02, WP-03  
**Owner (Backend):** Eng-Backend-03  

### Descrição
Implementar GET `/api/v1/ganhos?mes=4&ano=2026` que retorna agregado pré-computado de ganhos com breakdown.

### Tasks

- [ ] **T-07.1** — Entidades
  - `RelatorioGanhosCache` (JPA)
  - `RelatorioGanhosCategoriaDTO`
  - `RelatorioGanhosClienteDTO`

- [ ] **T-07.2** — Repositories
  - `RelatorioGanhosCacheRepository.findByPrestadorIdAndMesAno(Long, Date)`
  - `RelatorioGanhosCategoriaRepository.findByRelatorioId(Long)`
  - `RelatorioGanhosClienteRepository.findByRelatorioId(Long)`

- [ ] **T-07.3** — GanhosService
  - Método: `gerar(Long tenantId, int mes, int ano)`
  - Validar que é PRESTADOR
  - Validar mes/ano
  - Query no cache
  - Se não existir, return zero-filled DTO
  - Montar resposta com categorias + clientes

- [ ] **T-07.4** — GanhosController
  - GET `/api/v1/ganhos?mes=4&ano=2026` → gerar(tenantId, mes, ano)

- [ ] **T-07.5** — RelatorioDTO
  - Campos: mes, total_faturado, num_servicos, por_categoria[], por_cliente[]

- [ ] **T-07.6** — Tests
  - Test: GET ganhos validado e retorna DTO
  - Test: Contratante tenta acessar → 403
  - Test: Mês sem dados → 200 OK, totals=0
  - Test: Após serviço CONCLUIDO, cache atualizado
  - Test: Breakdown por categoria calculado corretamente
  - Test: Breakdown por cliente calculado corretamente

### Entregáveis

```
brjobs-java/src/main/java/ads/uninassau/brjobs/
├── entity/RelatorioGanhosCache.java
├── service/GanhosService.java
├── controller/GanhosController.java
└── dto/
    ├── RelatorioDTO.java
    ├── RelatorioGanhosCategoriaDTO.java
    └── RelatorioGanhosClienteDTO.java

brjobs-java/src/test/.../
└── GanhosControllerTest.java
```

### Done Criteria
- [ ] GET /ganhos implementado
- [ ] Cache atualizado via trigger
- [ ] Breakdown calculado
- [ ] Tests passam
- [ ] Performance < 1s

---

## WP-08: Publicar Serviço (POST Endpoint)

**Fase:** 2 (Core Features)  
**Duração Estimada:** 2 dias  
**Dependência:** WP-01, WP-02, WP-03  
**Owner (Backend):** Eng-Backend-03  

### Descrição
Implementar POST `/api/v1/servicos` para prestador criar novo serviço.

### Tasks

- [ ] **T-08.1** — CriarServicoDTO
  - Campos: nome, descricao, categoria, preco

- [ ] **T-08.2** — ServicoService.criar()
  - Validar PRESTADOR type
  - Validar campos (nome obrigatório, preco > 0, categoria valid)
  - Crear entidade com prestador_id = tenantId
  - Save & return DTO

- [ ] **T-08.3** — ServicoController.criar()
  - POST `/api/v1/servicos`
  - Return 201 Created com ServicoDTO

- [ ] **T-08.4** — Tests
  - Test: Create serviço como PRESTADOR → 201
  - Test: Create como CONTRATANTE → 403
  - Test: Nome vazio → 400
  - Test: Preco <= 0 → 400
  - Test: Categoria inválida → 400

### Entregáveis

```
brjobs-java/src/main/java/ads/uninassau/brjobs/dto/
└── CriarServicoDTO.java (additions to Controller/Service)

brjobs-java/src/test/.../
└── ServicoCreateTest.java
```

### Done Criteria
- [ ] POST /servicos implementado
- [ ] @ValidateTenant garante isolamento
- [ ] Tests passam

---

## WP-09: SearchComponent (Frontend - Angular)

**Fase:** 3 (Frontend)  
**Duração Estimada:** 3-4 dias  
**Dependência:** WP-04 (backend ready)  
**Owner (Frontend):** Eng-Frontend-01  

### Descrição
Componente Angular para busca paginada de serviços com filtros por categoria.

### Tasks

- [ ] **T-09.1** — SearchComponent
  - Input: categoria (select dropdown), search (text input), page (pagination)
  - Output: lista de cardsServiços
  - Eventos: mudança de filtro chama API
  - Paginação: próxima/anterior página

- [ ] **T-09.2** — ServicoService (Angular)
  - Método: `buscar(categoria, search, page, size)` → Observable<Page>
  - HTTP GET `$API_URL/servicos?...`

- [ ] **T-09.3** — Template
  - `<app-search></app-search>`
  - Filtro dropdown categoria
  - Input busca texto
  - Grid de cards com serviços
  - Pagination controls

- [ ] **T-09.4** — Styling (Tailwind)
  - Responsive grid (mobile: 1 col, tablet: 2, desktop: 3+)
  - Hover effects no card
  - Loading skeleton durante fetch

- [ ] **T-09.5** — Tests
  - Test: Componente renderiza
  - Test: Input muda chama service
  - Test: Lista serviços exibida corretamente
  - Test: Paginação funciona

### Entregáveis

```
brjobs-angular/src/app/components/search/
├── search.component.ts
├── search.component.html
├── search.component.css
└── search.component.spec.ts

brjobs-angular/src/app/service/
└── servico.service.ts (additions)
```

### Done Criteria
- [ ] Componente renderiza
- [ ] Filtros funcionam
- [ ] API chamada corretamente
- [ ] Tests passam

---

## WP-10: ChatComponent (Frontend)

**Fase:** 3 (Frontend)  
**Duração Estimada:** 4-5 dias  
**Dependência:** WP-05 (backend ready)  
**Owner (Frontend):** Eng-Frontend-01  

### Descrição
Componentes Angular para chat 1:1: lista de conversas e interface de mensagens com WebSocket.

### Tasks

- [ ] **T-10.1** — ChatService (Angular)
  - Setup Socket.io client
  - Métodos: `conectar()`, `enviarMensagem(...)`, `ouvir('mensagem:nova')`

- [ ] **T-10.2** — ChatListComponent
  - Renderiza lista de conversas
  - Output: clique em conversa → abre chat detail

- [ ] **T-10.3** — ChatMessageComponent
  - Renderiza histórico de mensagens (scroll)
  - Input para digitar msg
  - Envia via socket.io
  - Recebe em tempo real

- [ ] **T-10.4** — Template
  - Layout 2-colunas (lista | msgs)
  - Histórico com timestamps
  - Input + botão enviar
  - Loading indicator

- [ ] **T-10.5** — Tests
  - Test: Componentes renderizam
  - Test: Socket.io conecta
  - Test: Mensagem enviada via socket

### Entregáveis

```
brjobs-angular/src/app/components/chat/
├── chat-list/
│   ├── chat-list.component.ts
│   ├── chat-list.component.html
│   └── chat-list.component.css
├── chat-message/
│   ├── chat-message.component.ts
│   ├── chat-message.component.html
│   └── chat-message.component.css
└── chat.service.ts

brjobs-angular/src/app/service/
└── (chat.service.ts additions for Http fallback)
```

### Done Criteria
- [ ] WebSocket conecta
- [ ] Mensagens enviadas/recebidas
- [ ] UI responsiva
- [ ] Tests passam

---

## WP-11: RatingComponent (Frontend)

**Fase:** 3 (Frontend)  
**Duração Estimada:** 2-3 dias  
**Dependência:** WP-06 (backend ready)  
**Owner (Frontend):** Eng-Frontend-01  

### Descrição
Componente para avaliar prestador/contratante com stars e comentário.

### Tasks

- [ ] **T-11.1** — RatingFormComponent
  - Input: alvo_id
  - Form: 5 stars (clickable), textarea comentário
  - Validação: comentário obrgatório se stars <= 3, max 500 chars
  - Submit: POST /api/v1/avaliacoes

- [ ] **T-11.2** — RatingListComponent
  - Renderiza lista de avaliações de um user
  - Mostra media de stars
  - Lista comentários

- [ ] **T-11.3** — Template
  - Form com stars (⭐⭐⭐⭐⭐)
  - Textarea pré-validado
  - Submit button
  - Mensagemde sucesso/erro

- [ ] **T-11.4** — Tests
  - Test: Form renderiza
  - Test: Validação comentário obrigatório funciona
  - Test: Submit chama API

### Entregáveis

```
brjobs-angular/src/app/components/rating/
├── rating-form/
│   ├── rating-form.component.ts
│   ├── rating-form.component.html
│   └── rating-form.component.css
├── rating-list/
│   ├── rating-list.component.ts
│   ├── rating-list.component.html
│   └── rating-list.component.css
└── rating.service.ts
```

### Done Criteria
- [ ] Componentes renderizam
- [ ] Validação funciona
- [ ] API chamada corretamente
- [ ] Tests pass

---

## WP-12: EarningsComponent (Frontend)

**Fase:** 3 (Frontend)  
**Duração Estimada:** 2-3 dias  
**Dependência:** WP-07 (backend ready)  
**Owner (Frontend):** Eng-Frontend-02  

### Descrição
Dashboard de ganhos para prestadores (relatório mensal/anual com breakdown).

### Tasks

- [ ] **T-12.1** — EarningsComponent
  - Input: data (mes/ano select)
  - Renderiza: Total, #serviços, breakdown categoria, breakdown cliente
  - Tabelas ou gráficos
  - Botão exportar (opcional v2)

- [ ] **T-12.2** — EarningsService (Angular)
  - GET `/api/v1/ganhos?mes=X&ano=Y`

- [ ] **T-12.3** — Template
  - KPI cards: Total, #Serviços
  - Tabelas (categoria, cliente)
  - Gráfics (opcional)

- [ ] **T-12.4** — Tests
  - Test: Componente renderiza
  - Test: API chamada com mes/ano corretos

### Entregáveis

```
brjobs-angular/src/app/components/earnings/
├── earnings-report.component.ts
├── earnings-report.component.html
├── earnings-report.component.css
└── earnings-report.component.spec.ts

brjobs-angular/src/app/service/
└── earnings.service.ts
```

### Done Criteria
- [ ] Dashboard renderiza
- [ ] Dados corretos obtidos da API
- [ ] Tests pass

---

## WP-13: ProfileComponent (Frontend - Public View)

**Fase:** 3 (Frontend)  
**Duração Estimada:** 2 dias  
**Dependência:** WP-06 (ratings public)  
**Owner (Frontend):** Eng-Frontend-02  

### Descrição
Visualizar perfil público de prestador/contratante com avaliações e statsóricos.

### Tasks

- [ ] **T-13.1** — ProfileViewComponent
  - Input: userId
  - Renderiza: nome, foto, bio, categoria, avaliacao_media, lista de avaliações
  - Botão "Enviar Mensagem" (link para chat)

- [ ] **T-13.2** — Template
  - Card com info básica
  - Seção de avaliações (ratings list)
  - Link enviar msg

- [ ] **T-13.3** — Tests
  - Test: Componente renderiza com userId

### Entregáveis

```
brjobs-angular/src/app/components/profile/
├── profile-view.component.ts
├── profile-view.component.html
├── profile-view.component.css
└── profile-view.component.spec.ts
```

### Done Criteria
- [ ] Perfil renderiza
- [ ] Avaliações exibidas
- [ ] Link chat funciona

---

## WP-14: End-to-End Tests

**Fase:** 4 (Integration)  
**Duração Estimada:** 3-4 dias  
**Dependência:** WP-01 até WP-13  
**Owner:** Eng-QA-01  

### Descrição
Testes E2E full-stack usando Postman / Selenium.

### Tasks

- [ ] **T-14.1** — Postman Collections
  - Collection: Auth (login, refresh token)
  - Collection: Serviços (busca, criar)
  - Collection: Chat (enviar msg, histórico)
  - Collection: Avaliações (criar, listar)
  - Collection: Ganhos (fetch relatório)

- [ ] **T-14.2** — Selenium Tests (Angular)
  - Fluxo: Login → Busca serviço → Abrir perfil → Avaliar → Chat
  - Validar cada step

- [ ] **T-14.3** — Testes de Isolamento (crítico!)
  - User A tenta acessar dados de User B → 403
  - User A tenta editar serviço de User B → 403
  - User A tenta ver chat de User B → 403

- [ ] **T-14.4** — Testes de Concorrência
  - 2+ users tentam aceitar mesma solicitação
  - 100+ msgs simultâneas no chat

### Entregáveis

```
brjobs-java/tests/
├── postman/
│   ├── brjobs-auth.postman_collection.json
│   ├── brjobs-servicos.postman_collection.json
│   ├── brjobs-chat.postman_collection.json
│   └── ...
└── selenium/
    ├── BrJobsE2ETest.java
    └── IsolationTest.java
```

### Done Criteria
- [ ] Todos os E2E tests passam
- [ ] Isolamento validado
- [ ] Concorrência testada
- [ ] Relatório gerado

---

## WP-15: Load Testing (JMeter)

**Fase:** 4 (Integration)  
**Duração Estimada:** 2-3 dias  
**Dependência:** WP-01 até WP-13  
**Owner:** Eng-DevOps-01  

### Descrição
Teste de carga com JMeter para validar SLAs (< 2-5s busca, < 1s chat).

### Tasks

- [ ] **T-15.1** — JMeter Scenario 1: Busca Serviços
  - 100 users, ramp-up 10s
  - Cada user: 10 req de busca
  - Target: P99 < 3s

- [ ] **T-15.2** — JMeter Scenario 2: Chat
  - 50 users (25 pares)
  - Cada par: enviar 10 msgs
  - Target: P99 < 500ms

- [ ] **T-15.3** — JMeter Scenario 3: Mixed Load
  - 100 users fazendo: busca (60%), chat (20%), avaliação (20%)
  - Target: P99 < 3s agregado

- [ ] **T-15.4** — Analyze Results
  - Identificar gargalos (DB? WebSocket? Network?)
  - Propor mitigação (índices? cache? Redis?)
  - Documento: Load Testing Report

### Entregáveis

```
brjobs-java/tests/jmeter/
├── busca-servicos.jmx
├── chat.jmx
├── mixed-load.jmx
└── Load_Testing_Report.md
```

### Done Criteria
- [ ] Testes rodam sem erro
- [ ] P99 < target
- [ ] Relatório gerado com recomendações

---

## WP-16: Security Tests

**Fase:** 4 (Integration)  
**Duração Estimada:** 2-3 dias  
**Dependência:** WP-01 until WP-13  
**Owner:** Eng-Security-01  

### Descrição
Testes de segurança: acesso negado, injeção SQL, XSS, CSRF.

### Tasks

- [ ] **T-16.1** — Access Control Tests
  - User A tenta GET /servicos/1 (owned by B) → 403
  - User A tenta PUT /servicos/1 → 403
  - User A tenta GET /chat/B-to-C → 403
  - User A tenta ver /ganhos/B → 403

- [ ] **T-16.2** — Input Validation
  - SQL injection em search: `' OR 1=1--` → safe
  - XSS em comentário avaliação: `<script>` → filtered
  - Max length violation → 400

- [ ] **T-16.3** — Authentication
  - Expired token → 401
  - Invalid signature → 401
  - Missing token → 401

- [ ] **T-16.4** — Rate Limiting
  - 10 failed login attempts in 5min → 429 Too Many Requests

- [ ] **T-16.5** — CORS
  - Request from wrong origin → 403

### Entregáveis

```
brjobs-java/tests/security/
├── AccessControlTest.java
├── InputValidationTest.java
├── AuthenticationTest.java
├── RateLimitingTest.java
└── Security_Test_Report.md
```

### Done Criteria
- [ ] Access control tests passar
- [ ] Input validation secure
- [ ] AuthSecure
- [ ] Relatório de vulnerabilidades gerado

---

## WP-17: Feature Flags & Rollout Setup

**Fase:** 4 (Integration)  
**Duração Estimada:** 2 dias  
**Dependência:** WP-01 until WP-16  
**Owner:** Eng-DevOps-01, Eng-Backend-Lead  

### Descrição
Setup de feature flag (Spring Cloud Config ou Togglz) para rollout gradual.

### Tasks

- [ ] **T-17.1** — Feature Flag Configuration
  - Config: `feature.multitenancy.enabled` (boolean)
  - Store: Environment variable ou Spring Cloud Config

- [ ] **T-17.2** — Code Integration
  - Todos os endpoints multitenancy guarded por `if (multitenancyEnabled) { ... }`
  - Legacy logic kept as fallback

- [ ] **T-17.3** — Canary Rollout Script
  - Day 0: feature.enabled=false (deploy + validation)
  - Day 1: feature.enabled=true for 10% users (by user_id % 10)
  - Day 3: 50% users
  - Day 7: 100% users

- [ ] **T-17.4** — Monitoring & Alerting
  - Dashboard: % users with feature enabled
  - Alert: Error rate > 1% → rollback feature flag

- [ ] **T-17.5** — Runbook
  - Como rollback imediato?
  - Como monitor canary?
  - Como aumentar %?

### Entregáveis

```
brjobs-java/config/
├── application.yml (feature flags)
└── README-FeatureFlags.md

docs/
└── Rollout_Runbook.md
```

### Done Criteria
- [ ] Feature flag funciona
- [ ] Code pode ser ativado/desativado instantaneamente
- [ ] Monitoring setup
- [ ] Runbook documentado

---

## Summary of Work Packages

| WP | Título | Fase | Dias | Owner | Status |
|----|--------|------|------|-------|--------|
| WP-01 | Database Setup | 1 | 3-4 | Backend Lead | [ ] Not Started |
| WP-02 | TenantValidator | 1 | 2-3 | Backend Lead | [ ] Not Started |
| WP-03 | Repository Patterns | 1 | 2-3 | Backend Lead | [ ] Not Started |
| WP-04 | Busca Serviços | 2 | 3-4 | Backend Dev | [ ] Not Started |
| WP-05 | Chat Real-Time | 2 | 5-6 | Backend Dev + DevOps | [ ] Not Started |
| WP-06 | Avaliações | 2 | 3-4 | Backend Dev | [ ] Not Started |
| WP-07 | Relatório Ganhos | 2 | 3 | Backend Dev | [ ] Not Started |
| WP-08 | Publicar Serviço | 2 | 2 | Backend Dev | [ ] Not Started |
| WP-09 | SearchComponent | 3 | 3-4 | Frontend Dev | [ ] Not Started |
| WP-10 | ChatComponent | 3 | 4-5 | Frontend Dev | [ ] Not Started |
| WP-11 | RatingComponent | 3 | 2-3 | Frontend Dev | [ ] Not Started |
| WP-12 | EarningsComponent | 3 | 2-3 | Frontend Dev | [ ] Not Started |
| WP-13 | ProfileComponent | 3 | 2 | Frontend Dev | [ ] Not Started |
| WP-14 | E2E Tests | 4 | 3-4 | QA | [ ] Not Started |
| WP-15 | Load Testing | 4 | 2-3 | DevOps | [ ] Not Started |
| WP-16 | Security Tests | 4 | 2-3 | Security | [ ] Not Started |
| WP-17 | Feature Flags | 4 | 2 | DevOps + Lead | [ ] Not Started |

**Total Estimado:** 4-8 semanas (4 fases paralelas onde possível)

---

## How to Use This Document

1. **Pick WP by Phase:**
   - Fase 1: WP-01, WP-02, WP-03 (preparação)
   - Fase 2: WP-04 a WP-08 (features backend)
   - Fase 3: WP-09 a WP-13 (features frontend)
   - Fase 4: WP-14 a WP-17 (testing)

2. **Assign to Teams:**
   - Backend leads WP-01 to WP-08
   - Frontend leads WP-09 to WP-13
   - QA/DevOps leads WP-14 to WP-17

3. **Execute com `/lf-exec WP-XX`:**
   ```
   /lf-exec WP-01
   ```
   Isso abre um prompt com task checklist para aquele WP.

4. **Track Progress:**
   - Marcar checkboxes conforme tasks completam
   - Update status WP na tabela final

---

**Status:** Ready to Execute  
**Próximo:** Choose a WP e execute com `/lf-exec WP-01` (recomendado começar com WP-01)

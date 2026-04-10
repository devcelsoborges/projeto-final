# Specifications: Multi-Tenancy no brjobs

**Data:** 8 April 2026  
**Versão:** v0  
**Projeto:** brjobs — Multi-Tenancy (Tenant = Usuário)  
**Baseado em:** briefing-tech.v0.md  
**Status:** Ready for Work Package Execution

---

## SPEC-01: Isolamento de Dados por Tenant (Camadas de Validação)

### 1.1 Objetivo
Garantir que cada usuário (tenant_id = user_id) só acessa dados que pertence a ele, via validação em 3 camadas (JWT Filter → Service → Repository).

### 1.2 Requisitos Funcionais

#### RF-01.1: JWT Tenant Filter (Security)
- **Entrada:** Header `Authorization: Bearer <token>`
- **Processamento:**
  - Extrair `user_id` do JWT (claim customizado)
  - Validar assinatura e expiração
  - Armazenar `tenant_id = user_id` em `HttpServletRequest.setAttribute("tenant_id", userId)`
- **Saída:** Request com `tenant_id` disponível em toda cadeia
- **Erro:** 401 Unauthorized se token inválido, 403 Forbidden se expirado

#### RF-01.2: Service Layer Validation
- **Entrada:** tenantId (from request), resourceOwnerId (from data)
- **Processamento:**
  - Antes de acessar repositório, validar `tenantId == resourceOwnerId`
  - Se não bater, throw `AccessDeniedException`
- **Saída:** Recurso acessado com segurança
- **Padrão:** Todos os métodos de service que modificam/consultam dados privados devem validar

#### RF-01.3: Repository Queries Seguras
- **Padrão:** Nunca fazer `repo.findById(id)` sozinho
- **Correto:** `repo.findByIdAndTenantId(id, tenantId)` ou `repo.findByIdAndPrestadorId(id, tenantId)`
- **Index:** Criar índices compostos `(id, tenant_field)` para performance

### 1.3 Requisitos Técnicos

#### RT-01.1: JWT Claims
```json
{
    "sub": "user_id_aqui",
    "email": "user@example.com",
    "tipo_usuario": "PRESTADOR",
    "iat": 1680500000,
    "exp": 1680586400
}
```

#### RT-01.2: TenantValidator Component
```java
@Component
public class TenantFilter extends OncePerRequestFilter {
    @Autowired private JwtProvider jwtProvider;
    
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) {
        String authorization = req.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                String userId = jwtProvider.getUserIdFromToken(token);
                req.setAttribute("tenant_id", userId);
                chain.doFilter(req, res);
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } else {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
```

#### RT-01.3: Tenant Validation Aspect (AOP)
```java
@Aspect
@Component
public class TenantValidationAspect {
    @Before("@annotation(ValidateTenant)")
    public void validateTenant(JoinPoint jp) {
        Long tenantId = (Long) RequestContextHolder.getRequestAttributes()
            .getAttribute("tenant_id", RequestAttributes.SCOPE_REQUEST);
        
        Long resourceTenantId = (Long) jp.getArgs()[0]; // primeiro arg = tenant_id
        
        if (!tenantId.equals(resourceTenantId)) {
            throw new AccessDeniedException("Acesso negado: tenant mismatch");
        }
    }
}
```

#### RT-01.4: Custom Annotation
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateTenant {
}

// Uso:
@ValidateTenant
public Servico getServico(Long tenantId, Long servicoId) {
    return repo.findByIdAndPrestadorId(servicoId, tenantId);
}
```

### 1.4 Requisitos Não-Funcionais

#### RNF-01.1: Performance
- Validação de tenant deve ser < 1ms (in-memory)
- Query com tenant_id deve usar índice: < 10ms

#### RNF-01.2: Segurança
- Nunca log dados sensíveis (senhas, CPF) mesmo em erros
- Log de acesso negado com: timestamp, tenant_id, recurso, IP
- Implementar rate limiting para falhas de autenticação (5 tentativas em 5min → block)

### 1.5 Casos de Teste

| Caso | Entrada | Saída Esperada | Status |
|------|---------|----------------|--------|
| **TC-01.1** | JWT válido, tenant A acessa dado de A | ✅ Acesso concedido | [ ] |
| **TC-01.2** | JWT válido, tenant A acessa dado de B | ❌ 403 Forbidden | [ ] |
| **TC-01.3** | JWT expirado | ❌ 401 Unauthorized | [ ] |
| **TC-01.4** | Header sem token | ❌ 401 Unauthorized | [ ] |
| **TC-01.5** | Token com assinatura inválida | ❌ 401 Unauthorized | [ ] |

---

## SPEC-02: Busca & Listagem de Serviços (Paginação + Filtros)

### 2.1 Objetivo
Permitir contratantes buscarem serviços de qualquer prestador com paginação (10 itens/página), filtros por categoria, busca por texto e ordenação.

### 2.2 Requisitos Funcionais

#### RF-02.1: Endpoint GET /api/v1/servicos
```
GET /api/v1/servicos?
    categoria=pintura&
    search=sp&
    page=1&
    size=10&
    sort=recente|avaliacoes|preco

Headers:
    Authorization: Bearer <token>
    Accept: application/json
```

**Query Parameters:**
| Param | Tipo | Obrigatório | Padrão | Descrição |
|-------|------|------------|--------|-----------|
| `categoria` | string | Não | null | Filtra por categoria (pintura, reparos, faxina, construção, marcenaria) |
| `search` | string | Não | "" | Busca por nome ou descrição (case-insensitive) |
| `page` | int | Não | 1 | Número da página (1-indexed) |
| `size` | int | Não | 10 | Itens por página (max 100) |
| `sort` | enum | Não | recente | Ordenação: recente, avaliacoes, preco |

#### RF-02.2: Response 200 OK
```json
{
    "content": [
        {
            "id": 1,
            "nome": "Pintura de parede",
            "descricao": "Pintura de interiores, sem odor",
            "categoria": "pintura",
            "preco": 500.00,
            "prestador": {
                "id": 123,
                "nome": "João Pintor",
                "avaliacao_media": 4.8,
                "num_avaliacoes": 15,
                "foto_url": "/api/v1/usuarios/123/foto"
            },
            "criadoEm": "2026-04-08T10:30:00Z",
            "ativo": true
        },
        ...
    ],
    "totalElements": 156,
    "totalPages": 16,
    "currentPage": 1,
    "pageSize": 10,
    "hasNext": true
}
```

#### RF-02.3: Filtros Suportados
- **Por Categoria:** ENUM (pintura, reparos, faxina, construção, marcenaria)
- **Por Busca:** Full-text search (nome + descricao)
- **Por Ordenação:** recente (DESC), avaliacoes (média DESC), preco (ASC)

#### RF-02.4: Validações
- `page` >= 1
- `size` entre 1 e 100
- Se `categoria` inválida, retornar 400 Bad Request
- Se `sort` inválido, retornar 400 Bad Request

### 2.3 Requisitos Técnicos

#### RT-02.1: Repository Query
```java
@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {
    @Query(value = "SELECT s FROM Servico s WHERE " +
                   "(:categoria IS NULL OR s.categoria = :categoria) AND " +
                   "(:search IS NULL OR LOWER(s.nome) LIKE LOWER(CONCAT('%', :search, '%')) " +
                   "  OR LOWER(s.descricao) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                   "ORDER BY " +
                   "CASE WHEN :sort = 'recente' THEN s.criadoEm ELSE NULL END DESC, " +
                   "CASE WHEN :sort = 'avaliacoes' THEN s.avaliacao_media ELSE NULL END DESC, " +
                   "CASE WHEN :sort = 'preco' THEN s.preco ELSE NULL END ASC")
    Page<Servico> buscar(
        @Param("categoria") String categoria,
        @Param("search") String search,
        @Param("sort") String sort,
        Pageable pageable
    );
}
```

#### RT-02.2: Service Implementation
```java
@Service
public class ServicoService {
    public Page<ServicoDTO> buscar(String categoria, String search, 
                                   int page, int size, String sort) {
        // Validar inputs
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;
        if (sort == null) sort = "recente";
        
        // Buscar
        Pageable pg = PageRequest.of(page - 1, size);
        Page<Servico> result = repo.buscar(categoria, search, sort, pg);
        
        // Mapear para DTO com dados de prestador + avaliacao
        return result.map(s -> {
            ServicoDTO dto = new ServicoDTO();
            dto.setId(s.getId());
            dto.setNome(s.getNome());
            // ... mapear outros campos
            
            Usuario prestador = usuarioRepo.findById(s.getPrestadorId());
            double avaliacaoMedia = avaliacaoRepo.getAvaliacaoMedia(prestador.getId());
            
            dto.setPrestador(new PrestadorDTO(prestador.getNome(), avaliacaoMedia, ...));
            return dto;
        });
    }
}
```

#### RT-02.3: Controller
```java
@RestController
@RequestMapping("/api/v1/servicos")
public class ServicoController {
    @GetMapping
    public ResponseEntity<Page<ServicoDTO>> buscar(
        @RequestParam(required = false) String categoria,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "recente") String sort,
        HttpServletRequest req
    ) {
        // tenantId extraído do JWT (pode ser usado para futuras personalizações)
        Long tenantId = (Long) req.getAttribute("tenant_id");
        
        Page<ServicoDTO> result = servicoService.buscar(categoria, search, page, size, sort);
        return ResponseEntity.ok(result);
    }
}
```

#### RT-02.4: Database Indexes
```sql
CREATE INDEX idx_servicos_categoria_criado 
    ON servicos(categoria, criado_em DESC);

CREATE INDEX idx_servicos_prestador_id 
    ON servicos(prestador_id);

CREATE INDEX idx_servicos_nome_descricao 
    ON servicos USING GIN(to_tsvector('portuguese', nome || ' ' || descricao));
```

### 2.4 Requisitos Não-Funcionais

#### RNF-02.1: Performance
- Busca deve responder em < 2-5 segundos
- P99 latência < 3 segundos mesmo com 100K serviços
- Cache em Redis (v2) se P99 > 3s

#### RNF-02.2: Escalabilidade
- Suportar paginação de 1M+ serviços
- Índices compostos para evitar full table scans

### 2.5 Casos de Teste

| Caso | Entrada | Saída | Status |
|------|---------|-------|--------|
| **TC-02.1** | Busca sem filtro | 10 primeiros serviços, page=1 | [ ] |
| **TC-02.2** | Filtro categoria=pintura | Apenas serviços de pintura | [ ] |
| **TC-02.3** | Busca por texto "sp" | Serviços contendo "sp" em nome/desc | [ ] |
| **TC-02.4** | page=2, size=10 | Serviços 11-20 | [ ] |
| **TC-02.5** | Página inválida (page=999) | 200 OK, content vazio | [ ] |
| **TC-02.6** | Sort=avaliacoes | Ordenado por rating DESC | [ ] |

---

## SPEC-03: Chat Real-Time 1:1 (WebSocket + Socket.io)

### 3.1 Objetivo
Permitir prestador e contratante conversarem em tempo real com persistência completa, oferecendo notificações email/push para usuários offline.

### 3.2 Requisitos Funcionais

#### RF-03.1: Conexão WebSocket
- **URL:** `ws://localhost:8080/socket.io/?token=<JWT_TOKEN>`
- **Protocolo:** Socket.io v4.x
- **Autenticação:** JWT token validado antes de aceitar conexão
- **Namespaces:** `/chat` (isolado por app)

#### RF-03.2: Eventos Socket.io

**Evento: `mensagem:enviar` (Client → Server)**
```json
{
    "destinatario_id": 456,
    "conteudo": "Oi, tudo bem?"
}
```

**Evento: `mensagem:nova` (Server → Client)**
```json
{
    "id": 789,
    "remetente_id": 123,
    "remetente_nome": "João Pintor",
    "destinatario_id": 456,
    "conteudo": "Oi, tudo bem?",
    "criadoEm": "2026-04-08T10:30:00Z",
    "lido": false
}
```

**Evento: `mensagem:lida` (Client → Server)**
```json
{
    "mensagem_id": 789
}
```

#### RF-03.3: Persistência
- Todas as mensagens persistem em `chat_messages` table
- Se destinatário offline, mensagem fica na fila (polling)
- Background job (cron 1x/min) envia email + push para offline

#### RF-03.4: Isolamento
- Usuário A só recebe eventos de suas próprias conversas
- Usuário A não pode abrir conversa de outro par

### 3.3 Requisitos Técnicos

#### RT-03.1: Tabelas

```sql
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    remetente_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    destinatario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    conteudo TEXT NOT NULL,
    lido BOOLEAN DEFAULT FALSE,
    notificado BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    
    INDEX idx_chat_remetente_destinatario (remetente_id, destinatario_id),
    INDEX idx_chat_destinatario_lido (destinatario_id, lido),
    INDEX idx_chat_notificado (notificado, created_at),
    CHECK (remetente_id != destinatario_id)
);

CREATE TABLE conversas_chat (
    id BIGSERIAL PRIMARY KEY,
    usuario_1_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    usuario_2_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    ultima_mensagem_id BIGINT REFERENCES chat_messages(id),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    UNIQUE (usuario_1_id, usuario_2_id),
    CHECK (usuario_1_id < usuario_2_id)
);
```

#### RT-03.2: Socket.io Handler (Spring Boot)

```java
@Component
public class ChatSocketHandler {
    @Autowired private ChatMessageRepository messageRepo;
    @Autowired private ConversaChatRepository conversaRepo;
    @Autowired private JwtProvider jwtProvider;
    
    @SocketMapping("/chat/mensagem:enviar")
    public void enviarMensagem(@SocketClient SocketIOClient client, 
                               AckRequest ack,
                               Map<String, Object> data) {
        // 1. Extrair tenant_id da conexão
        Long tenantId = (Long) client.getHandshakeData().getHttpHeaders()
            .get("X-Tenant-Id");
        
        // 2. Validar e salvar
        Long destinatarioId = ((Number) data.get("destinatario_id")).longValue();
        String conteudo = (String) data.get("conteudo");
        
        if (destinatarioId.equals(tenantId)) {
            ack.sendAckData("Erro: não pode enviar para si mesmo");
            return;
        }
        
        ChatMessage msg = new ChatMessage();
        msg.setRemetenteId(tenantId);
        msg.setDestinatarioId(destinatarioId);
        msg.setConteudo(conteudo);
        messageRepo.save(msg);
        
        // 3. Emitir evento para destinatário se conectado
        SocketIONamespace ns = server.getNamespace("/chat");
        SocketIOClient destClient = ns.getClient(UUID.fromString("user_" + destinatarioId));
        
        if (destClient != null) {
            destClient.sendEvent("mensagem:nova", msg);
        } else {
            // Marcar para notificação offline
            msg.setNotificado(false);
            messageRepo.save(msg);
        }
        
        ack.sendAckData("OK");
    }
}
```

#### RT-03.3: Background Job (Notificações Offline)

```java
@Component
public class ChatNotificationJob {
    @Scheduled(fixedDelay = 60000) // 1x por minuto
    public void enviarNotificacoesOffline() {
        List<ChatMessage> naoNotificadas = messageRepo.findByNotificadoFalse();
        
        for (ChatMessage msg : naoNotificadas) {
            Usuario destUser = usuarioRepo.findById(msg.getDestinatarioId());
            Usuario remetenteUser = usuarioRepo.findById(msg.getRemetenteId());
            
            // Enviar email
            sendGridService.sendEmail(
                destUser.getEmail(),
                "Nova mensagem de " + remetenteUser.getNome(),
                "Você recebeu uma mensagem: " + msg.getConteudo()
            );
            
            // Enviar push
            firebaseService.sendPush(
                destUser.getFirebaseToken(),
                "Nova mensagem",
                remetenteUser.getNome() + ": " + msg.getConteudo()
            );
            
            msg.setNotificado(true);
            messageRepo.save(msg);
        }
    }
}
```

#### RT-03.4: Frontend (Angular)

```typescript
import { SocketIoModule, SocketIoConfig } from 'ngx-socket-io';

const config: SocketIoConfig = {
    url: 'http://localhost:8080',
    options: {
        auth: {
            token: localStorage.getItem('JWT_TOKEN')
        },
        reconnection: true,
        reconnectionDelay: 1000,
        reconnectionAttempts: 5
    }
};

@NgModule({
    imports: [SocketIoModule.forRoot(config)]
})
export class AppModule { }

@Injectable()
export class ChatService {
    constructor(private socket: Socket, private http: HttpClient) {
        this.socket.on('/chat/mensagem:nova', (msg: any) => {
            console.log('Nova mensagem:', msg);
            // Atualizar template
        });
    }
    
    enviarMensagem(destinatarioId: number, conteudo: string) {
        this.socket.emit('/chat/mensagem:enviar', {
            destinatario_id: destinatarioId,
            conteudo: conteudo
        });
    }
}
```

### 3.4 Requisitos Não-Funcionais

#### RNF-03.1: Performance
- Latência de entrega < 500ms (real-time)
- Persistência < 10ms
- Notificação offline < 2 minutos

#### RNF-03.2: Escalabilidade
- Suportar 10K+ conexões simultâneas
- Redis pub/sub para multi-server (v2)

### 3.5 Casos de Teste

| Caso | Entrada | Saída | Status |
|------|---------|-------|--------|
| **TC-03.1** | User A envia msg para B (B online) | B recebe em < 500ms | [ ] |
| **TC-03.2** | User A envia msg para B (B offline) | B recebe email + push em < 2min | [ ] |
| **TC-03.3** | User A tenta msg para si mesmo | Erro: não posso enviar para mim | [ ] |
| **TC-03.4** | User A fecha browser, reconecta | Histórico preservado, recebe offline msgs | [ ] |
| **TC-03.5** | 100 msgs simultâneas | Todas persistidas e entregues | [ ] |

---

## SPEC-04: Avaliações (Stars + Comments com Filtro Palavrões)

### 4.1 Objetivo
Permitir prestadores e contratantes avaliarem uns aos outros com stars (1-5) e comentários, com validação de conteúdo maldoso.

### 4.2 Requisitos Funcionais

#### RF-04.1: Endpoint POST /api/v1/avaliacoes
```
POST /api/v1/avaliacoes
Headers:
    Authorization: Bearer <token>
    Content-Type: application/json

Body:
{
    "alvo_id": 123,
    "stars": 5,
    "comentario": "Serviço excelente, recomendo!"
}

Response 201 Created:
{
    "id": 789,
    "usuario_id": 456,
    "alvo_id": 123,
    "stars": 5,
    "comentario": "Serviço excelente, recomendo!",
    "criado_em": "2026-04-08T10:30:00Z"
}
```

#### RF-04.2: Validações
- **Stars:** Entre 1 e 5
- **Comentario:** Máximo 500 caracteres, obrigatório se stars <= 3
- **Palavrões:** Validar contra lista de ~500 palavras (pt-BR)
- **Isolamento:** Apenas `usuario_id = tenant_id` (quem avalia = logado)
- **Unicidade:** Máximo 1 avaliação por par (usuario_id, alvo_id)
- **Transação:** Apenas quem contratou pode avaliar (FK SolicitacaoServico)

#### RF-04.3: Filtro de Palavrões
Se comentário contiver palavra proibida:
- Retornar 400 Bad Request
- Mensagem: "Comentário contém linguagem imprópria"
- Log: `[tenant_id=X] Tentativa de avaliação com conteúdo maldoso`

#### RF-04.4: Visibilidade
- Avaliações são **públicas** no perfil do avaliado
- Rating médio exibido: "4.8 ⭐ de 23 avaliações"
- Sem edição após publicação

### 4.3 Requisitos Técnicos

#### RT-04.1: Tabela

```sql
CREATE TABLE avaliacoes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    alvo_id BIGINT NOT NULL REFERENCES usuarios(id),
    stars INT NOT NULL CHECK (stars >= 1 AND stars <= 5),
    comentario VARCHAR(500),
    verificado_conteudo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    
    UNIQUE (usuario_id, alvo_id),
    INDEX idx_avaliacoes_alvo (alvo_id),
    CHECK (usuario_id != alvo_id)
);
```

#### RT-04.2: Service

```java
@Service
public class AvaliacaoService {
    private static final List<String> PALAVRAS_PROIBIDAS = Arrays.asList(
        // ~500 palavras em pt-BR
        "xingamento1", "xingamento2", ...
    );
    
    @Transactional
    public AvaliacaoDTO criar(Long tenantId, CriarAvaliacaoDTO dto) {
        // 1. Validar isolamento
        if (!tenantId.equals(dto.usuario_id)) {
            throw new AccessDeniedException("Apenas você pode avaliar em seu nome");
        }
        
        // 2. Validar stars
        if (dto.stars < 1 || dto.stars > 5) {
            throw new ValidationException("Stars debe estar entre 1 e 5");
        }
        
        // 3. Validar comentário obrigatório se stars <= 3
        if (dto.stars <= 3 && (dto.comentario == null || dto.comentario.isEmpty())) {
            throw new ValidationException("Comentário obrigatório para avaliações <= 3 estrelas");
        }
        
        // 4. Filtrar palavrões
        if (dto.comentario != null && contemPalavrasProibidas(dto.comentario)) {
            throw new ValidationException("Comentário contém linguagem imprópria");
        }
        
        // 5. Validar transação (contratante contratou esse prestador)
        boolean temTransacao = solicitacaoRepo.existsByContratanteIdAndPrestadorId(tenantId, dto.alvo_id);
        if (!temTransacao) {
            throw new ValidationException("Você não tem contrato com esse prestador");
        }
        
        // 6. Verificar unicidade
        if (avaliacaoRepo.existsByUsuarioIdAndAlvoId(tenantId, dto.alvo_id)) {
            throw new ConflictException("Você já avaliou esse prestador");
        }
        
        // 7. Salvar
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setUsuarioId(tenantId);
        avaliacao.setAlvoId(dto.alvo_id);
        avaliacao.setStars(dto.stars);
        avaliacao.setComentario(dto.comentario);
        avaliacao.setVerificadoConteudo(true);
        avaliacaoRepo.save(avaliacao);
        
        return new AvaliacaoDTO(avaliacao);
    }
    
    private boolean contemPalavrasProibidas(String texto) {
        String lower = texto.toLowerCase();
        return PALAVRAS_PROIBIDAS.stream().anyMatch(lower::contains);
    }
    
    public List<AvaliacaoDTO> listarPorAlvo(Long alvoId) {
        List<Avaliacao> avaliacoes = avaliacaoRepo.findByAlvoId(alvoId);
        return avaliacoes.stream().map(AvaliacaoDTO::new).collect(Collectors.toList());
    }
    
    public double getAvaliacaoMedia(Long alvoId) {
        return avaliacaoRepo.getAvaliacaoMedia(alvoId); // query customizada
    }
}
```

#### RT-04.3: Controller

```java
@RestController
@RequestMapping("/api/v1/avaliacoes")
public class AvaliacaoController {
    @PostMapping
    public ResponseEntity<AvaliacaoDTO> criar(
        @RequestBody CriarAvaliacaoDTO dto,
        HttpServletRequest req
    ) {
        Long tenantId = (Long) req.getAttribute("tenant_id");
        AvaliacaoDTO result = service.criar(tenantId, dto);
        return ResponseEntity.status(201).body(result);
    }
    
    @GetMapping("/usuario/{alvoId}")
    public ResponseEntity<List<AvaliacaoDTO>> listarPorAlvo(@PathVariable Long alvoId) {
        List<AvaliacaoDTO> result = service.listarPorAlvo(alvoId);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/media/{alvoId}")
    public ResponseEntity<Double> getMedia(@PathVariable Long alvoId) {
        double media = service.getAvaliacaoMedia(alvoId);
        return ResponseEntity.ok(media);
    }
}
```

### 4.4 Requisitos Não-Funcionais

#### RNF-04.1: Performance
- POST avaliaçao: < 1 segundo
- GET lista avaliações: < 500ms
- GET média: < 100ms (query agregado)

### 4.5 Casos de Teste

| Caso | Entrada | Saída | Status |
|------|---------|-------|--------|
| **TC-04.1** | Criar avaliação 5 stars | ✅ 201 Created | [ ] |
| **TC-04.2** | Comentário com xingamento | ❌ 400 Bad Request | [ ] |
| **TC-04.3** | 2 stars, sem comentário | ❌ 400 (comentário obrigatório) | [ ] |
| **TC-04.4** | 2ª avaliação para mesmo alvo | ❌ 409 Conflict (unicidade) | [ ] |
| **TC-04.5** | User A tenta avaliar si mesmo | ❌ 400 Check constraint | [ ] |
| **TC-04.6** | Litar avaliações de user | ✅ Array com avaliações públicas | [ ] |

---

## SPEC-05: Relatório de Ganhos (Agregado Pré-Computado)

### 5.1 Objetivo
Permitir prestadores visualizarem relatório de faturamento mensal/anual com breakdown por categoria e cliente, usando agregado pré-computado via trigger SQL.

### 5.2 Requisitos Funcionais

#### RF-05.1: Endpoint GET /api/v1/ganhos
```
GET /api/v1/ganhos?mes=202604&ano=2026

Headers:
    Authorization: Bearer <token>

Response 200 OK:
{
    "mes": "abril/2026",
    "total_faturado": 5000.00,
    "num_servicos": 10,
    "por_categoria": [
        {
            "categoria": "pintura",
            "total": 2000.00,
            "num_servicos": 4
        },
        {
            "categoria": "reparos",
            "total": 3000.00,
            "num_servicos": 6
        }
    ],
    "por_cliente": [
        {
            "cliente_id": 456,
            "cliente_nome": "Maria Silva",
            "total": 1500.00,
            "num_servicos": 3
        },
        ...
    ]
}
```

#### RF-05.2: Validações
- Apenas **prestador** pode acessar (validar tipo_usuario = PRESTADOR)
- Apenas seu próprio relatório (tenant_id validação)
- Se mês/ano inválidos, retornar 400
- Se não há dados, retornar 200 com totals = 0

#### RF-05.3: Dados Agregados
- Total faturado (SUM preco de servicos CONCLUIDOS)
- Número de serviços
- Breakdown por categoria
- Breakdown por cliente

### 5.3 Requisitos Técnicos

#### RT-05.1: Tabelas de Cache

```sql
CREATE TABLE relatorio_ganhos_cache (
    id BIGSERIAL PRIMARY KEY,
    prestador_id BIGINT NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    mes_ano DATE NOT NULL,
    total_faturado DECIMAL(12, 2) DEFAULT 0,
    num_servicos INT DEFAULT 0,
    updated_at TIMESTAMP DEFAULT NOW(),
    
    UNIQUE (prestador_id, mes_ano)
);

CREATE TABLE relatorio_ganhos_categoria (
    id BIGSERIAL PRIMARY KEY,
    cache_id BIGINT NOT NULL REFERENCES relatorio_ganhos_cache(id) ON DELETE CASCADE,
    categoria VARCHAR(50),
    total DECIMAL(12, 2),
    num_servicos INT
);

CREATE TABLE relatorio_ganhos_cliente (
    id BIGSERIAL PRIMARY KEY,
    cache_id BIGINT NOT NULL REFERENCES relatorio_ganhos_cache(id) ON DELETE CASCADE,
    cliente_id BIGINT NOT NULL REFERENCES usuarios(id),
    cliente_nome VARCHAR(255),
    total DECIMAL(12, 2),
    num_servicos INT
);
```

#### RT-05.2: Trigger (Atualizar ao concluir serviço)

```sql
CREATE OR REPLACE FUNCTION atualizar_relatorio_ganhos()
RETURNS TRIGGER AS $$
DECLARE
    mes_ano_atual DATE;
BEGIN
    IF NEW.status = 'CONCLUIDO' AND OLD.status != 'CONCLUIDO' THEN
        mes_ano_atual := DATE_TRUNC('month', NOW())::DATE;
        
        -- Upsert cache principal
        INSERT INTO relatorio_ganhos_cache (prestador_id, mes_ano, total_faturado, num_servicos, updated_at)
        SELECT 
            NEW.prestador_id,
            mes_ano_atual,
            COALESCE(SUM(s.preco), 0),
            COUNT(*),
            NOW()
        FROM servicos s
        WHERE s.prestador_id = NEW.prestador_id
            AND s.status = 'CONCLUIDO'
            AND DATE_TRUNC('month', s.updated_at)::DATE = mes_ano_atual
        ON CONFLICT (prestador_id, mes_ano) DO UPDATE SET
            total_faturado = EXCLUDED.total_faturado,
            num_servicos = EXCLUDED.num_servicos,
            updated_at = NOW();
        
        -- Atualizar breakdown por categoria
        DELETE FROM relatorio_ganhos_categoria 
        WHERE cache_id = (
            SELECT id FROM relatorio_ganhos_cache 
            WHERE prestador_id = NEW.prestador_id AND mes_ano = mes_ano_atual
        );
        
        INSERT INTO relatorio_ganhos_categoria (cache_id, categoria, total, num_servicos)
        SELECT 
            (SELECT id FROM relatorio_ganhos_cache WHERE prestador_id = NEW.prestador_id AND mes_ano = mes_ano_atual),
            s.categoria,
            SUM(s.preco),
            COUNT(*)
        FROM servicos s
        WHERE s.prestador_id = NEW.prestador_id
            AND s.status = 'CONCLUIDO'
            AND DATE_TRUNC('month', s.updated_at)::DATE = mes_ano_atual
        GROUP BY s.categoria;
        
        -- Atualizar breakdown por cliente
        DELETE FROM relatorio_ganhos_cliente 
        WHERE cache_id = (
            SELECT id FROM relatorio_ganhos_cache 
            WHERE prestador_id = NEW.prestador_id AND mes_ano = mes_ano_atual
        );
        
        INSERT INTO relatorio_ganhos_cliente (cache_id, cliente_id, cliente_nome, total, num_servicos)
        SELECT 
            (SELECT id FROM relatorio_ganhos_cache WHERE prestador_id = NEW.prestador_id AND mes_ano = mes_ano_atual),
            sol.contratante_id,
            u.nome,
            SUM(s.preco),
            COUNT(*)
        FROM servicos s
        JOIN solicitacoes_servico sol ON s.solicitacao_id = sol.id
        JOIN usuarios u ON u.id = sol.contratante_id
        WHERE s.prestador_id = NEW.prestador_id
            AND s.status = 'CONCLUIDO'
            AND DATE_TRUNC('month', s.updated_at)::DATE = mes_ano_atual
        GROUP BY sol.contratante_id, u.nome;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_atualizar_relatorio
AFTER UPDATE ON servicos
FOR EACH ROW
EXECUTE FUNCTION atualizar_relatorio_ganhos();
```

#### RT-05.3: Service

```java
@Service
public class GanhosService {
    @Autowired private RelatorioGanhosCacheRepository cacheRepo;
    @Autowired private RelatorioGanhosCategoriaRepository categoriaRepo;
    @Autowired private RelatorioGanhosClienteRepository clienteRepo;
    @Autowired private UsuarioRepository usuarioRepo;
    
    public RelatorioDTO geraramDesempenho(Long tenantId, int mes, int ano) {
        // 1. Validar que é prestador
        Usuario user = usuarioRepo.findById(tenantId);
        if (user.getTipoUsuario() != TipoUsuario.PRESTADOR) {
            throw new AccessDeniedException("Apenas prestadores veem ganhos");
        }
        
        // 2. Validar mes/ano
        if (mes < 1 || mes > 12 || ano < 2000) {
            throw new ValidationException("Mês/ano inválidos");
        }
        
        // 3. Buscar no cache
        Date mesAno = new Date(ano - 1900, mes - 1, 1);
        RelatorioGanhosCache cache = cacheRepo.findByPrestadorIdAndMesAno(tenantId, mesAno);
        
        if (cache == null) {
            // Se não existe, criar zero-filled
            RelatorioDTO dto = new RelatorioDTO();
            dto.setMes(mesAno);
            dto.setTotalFaturado(BigDecimal.ZERO);
            dto.setNumServicos(0);
            return dto;
        }
        
        // 4. Montar resposta
        RelatorioDTO dto = new RelatorioDTO();
        dto.setMes(mesAno);
        dto.setTotalFaturado(cache.getTotalFaturado());
        dto.setNumServicos(cache.getNumServicos());
        
        List<RelatorioGanhosCategoriaDTO> categorias = categoriaRepo.findByRelatorioId(cache.getId())
            .stream().map(RelatorioGanhosCategoriaDTO::new).collect(Collectors.toList());
        dto.setPorCategoria(categorias);
        
        List<RelatorioGanhosClienteDTO> clientes = clienteRepo.findByRelatorioId(cache.getId())
            .stream().map(RelatorioGanhosClienteDTO::new).collect(Collectors.toList());
        dto.setPorCliente(clientes);
        
        return dto;
    }
}
```

#### RT-05.4: Controller

```java
@RestController
@RequestMapping("/api/v1/ganhos")
public class GanhosController {
    @GetMapping
    public ResponseEntity<RelatorioDTO> gerar(
        @RequestParam int mes,
        @RequestParam int ano,
        HttpServletRequest req
    ) {
        Long tenantId = (Long) req.getAttribute("tenant_id");
        RelatorioDTO result = service.gerar(tenantId, mes, ano);
        return ResponseEntity.ok(result);
    }
}
```

### 5.4 Requisitos Não-Funcionais

#### RNF-05.1: Performance
- GET relatório: < 1 segundo (dados em cache)
- Trigger atualização: < 100ms

#### RNF-05.2: Eventual Consistency
- Relatorio atualizado quando serviço marcado CONCLUIDO
- Até 1 minuto de delay se múltiplos serviços concluidos simultâneos

### 5.5 Casos de Teste

| Caso | Entrada | Saída | Status |
|------|---------|-------|--------|
| **TC-05.1** | GET ganhos/?mes=4&ano=2026 | Total + breakdown | [ ] |
| **TC-05.2** | Contratante tenta acessar ganhos | ❌ 403 Forbidden | [ ] |
| **TC-05.3** | Mês sem serviços | 200 OK, totals=0 | [ ] |
| **TC-05.4** | Serviço concluido, check relatório agora | Total atualizado | [ ] |

---

## SPEC-06: Publicar Serviço (Prestador)

### 6.1 Objetivo
Permitir prestador criar novo serviço com nome, descrição, categoria, preço.

### 6.2 Requisitos Funcionais

#### RF-06.1: Endpoint POST /api/v1/servicos

```
POST /api/v1/servicos
Headers:
    Authorization: Bearer <token>
    Content-Type: application/json

Body:
{
    "nome": "Pintura de parede",
    "descricao": "Pintura de interiores, sem odor, qualidade premium",
    "categoria": "pintura",
    "preco": 500.00
}

Response 201 Created:
{
    "id": 1,
    "prestador_id": 123,
    "nome": "Pintura de parede",
    "descricao": "...",
    "categoria": "pintura",
    "preco": 500.00,
    "ativo": true,
    "criado_em": "2026-04-08T10:30:00Z"
}
```

#### RF-06.2: Validações
- Apenas **prestador** pode criar serviço
- Nome: obrigatório, max 255 chars
- Descrição: Max 2000 chars
- Categoria: ENUM (pintura, reparos, faxina, construção, marcenaria)
- Preço: > 0, max 999999.99

#### RF-06.3: Isolamento
- `prestador_id = tenant_id` (não posso criar serviço em nome de outro)

### 6.3 Requisitos Técnicos

```java
@RestController
@RequestMapping("/api/v1/servicos")
public class ServicoController {
    @PostMapping
    public ResponseEntity<ServicoDTO> criar(
        @RequestBody CriarServicoDTO dto,
        HttpServletRequest req
    ) {
        Long tenantId = (Long) req.getAttribute("tenant_id");
        ServicoDTO result = service.criar(tenantId, dto);
        return ResponseEntity.status(201).body(result);
    }
}

@Service
public class ServicoService {
    @Transactional
    public ServicoDTO criar(Long tenantId, CriarServicoDTO dto) {
        // 1. Validar prestador
        Usuario user = usuarioRepo.findById(tenantId);
        if (user.getTipoUsuario() != TipoUsuario.PRESTADOR) {
            throw new ValidationException("Apenas prestadores podem criar serviços");
        }
        
        // 2. Validar campos
        if (dto.getNome() == null || dto.getNome().isEmpty()) {
            throw new ValidationException("Nome obrigatório");
        }
        if (dto.getPreco() == null || dto.getPreco() <= 0) {
            throw new ValidationException("Preço deve ser > 0");
        }
        
        // 3. Criar entidade
        Servico servico = new Servico();
        servico.setPrestadorId(tenantId);
        servico.setNome(dto.getNome());
        servico.setDescricao(dto.getDescricao());
        servico.setCategoria(dto.getCategoria());
        servico.setPreco(dto.getPreco());
        servico.setAtivo(true);
        
        // 4. Salvar
        servicoRepo.save(servico);
        
        return new ServicoDTO(servico);
    }
}
```

---

## SPEC-07-XX: (Componentes Angular - Continue padrão)

### Padrão para Componentes
Cada componente segue:
- **Selector:** `app-feature-name`
- **Files:** `*.component.ts`, `*.component.html`, `*.component.css`
- **Structure:** Property binding, event binding, ngIf/ngFor
- **Styling:** Tailwind CSS v4
- **Testing:** `*.spec.ts`

---

## Sumário de Especificações

| SPEC | Título | Status |
|------|--------|--------|
| SPEC-01 | Isolamento de Dados por Tenant | ✅ Ready for Dev |
| SPEC-02 | Busca & Listagem de Serviços | ✅ Ready for Dev |
| SPEC-03 | Chat Real-Time 1:1 | ✅ Ready for Dev |
| SPEC-04 | Avaliações | ✅ Ready for Dev |
| SPEC-05 | Relatório de Ganhos | ✅ Ready for Dev |
| SPEC-06 | Publicar Serviço | ✅ Ready for Dev |

**Total:** 6 especificações granulares, cada uma com 12+ seções (Objetivo, RF, RT, RNF, Testes).

---

**Próximo passo:** `/lf-exec WP-01` para iniciar implementação de um work package específico.

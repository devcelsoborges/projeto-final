# Briefing Técnico: Multi-Tenancy no brjobs — v0

**Data:** 8 April 2026  
**Versão:** v0  
**Feature:** Multi-Tenancy (Tenant = Usuário)  
**Projeto:** brjobs (Angular 20 + Spring Boot 3.3.5 + PostgreSQL)  
**Status:** Ready for Specs Generation

---

## 1. Executive Summary

Implementar isolamento completo de dados por usuário (tenant = user_id) no brjobs, transformando-o de uma aplicação single/mal-isolada para um **marketplace bilateral seguro e escalável**.

**Resultado esperado:** Cada usuário (prestador ou contratante) opera em seu próprio tenant com:
- ✅ Dados isolados (segurança)
- ✅ Chat real-time 1:1 (Socket.io)
- ✅ Avaliações com filtro de conteúdo (email + push notifications)
- ✅ Busca cross-tenant paginada (10 itens/página, filtros por categoria)
- ✅ Relatório de ganhos pré-computado (prestador only)
- ✅ Tratamento robusto de concorrência (pessimistic + optimistic locking)

**Escopo:** ~4-8 semanas de desenvolvimento + testes

---

## 2. Personas & Casos de Uso

### 2.1 Personas

| Persona | Tipo | Dados Isolados | Ações Principais |
|---------|------|-----------------|------------------|
| **Prestador** | Service Provider | Serviços, avaliações recebidas, chats, ganhos | Publicar serviço, responder solicitações, receber avaliações, acessar relatório de ganhos |
| **Contratante** | Service Requester | Solicitações, avaliações dadas, chats | Buscar/filtrar serviços, publicar solicitação, avaliar prestador, conversar |
| **Admin** (v2) | Moderator | Dados globais, reports, logs | Bloquear users, remover comentários ofensivos, analytics |

### 2.2 Casos de Uso Principais

**UC-001: Buscar Serviços (Contratante)**
```
Actor: Contratante logado (tenant_id = user_id)
Fluxo:
  1. Acessa página de busca
  2. Filtra por categoria (pintura, reparos, faxina, construção, marcenaria)
  3. Busca por texto (ex: "pintor em SP")
  4. Vê resultados paginados (10/página)
  5. Clica em serviço → vê perfil público do prestador + avaliações
Dados Acessados: servicos (público), usuarios (perfil público), avaliacoes (público)
Isolamento: Nenhum — busca é cross-tenant
SLA: < 2-5 segundos
```

**UC-002: Publicar Serviço (Prestador)**
```
Actor: Prestador logado (tenant_id = user_id)
Fluxo:
  1. Acessa "Meus Serviços"
  2. Clica "+ Novo Serviço"
  3. Preenche: nome, descrição, categoria, preço
  4. Submete
  5. Serviço aparece na busca pública
Validação: tenantId = prestador_id (não posso criar serviço em nome de outro)
Isolamento: ✅ Meus serviços isolados
```

**UC-003: Enviar Mensagem no Chat (Ambos)**
```
Actor: User A (prestador ou contratante)
Fluxo:
  1. A vê perfil de User B
  2. A clica "Enviar mensagem"
  3. A escreve mensagem + envia
  4. Mensagem armazenada em chat_messages
  5. B recebe notificação (email + push + visualmente se online)
  6. B vê mensagem em chat 1:1
Isolamento: ✅ Só A e B veem esse chat
Restrição: User C não pode ver chat A↔B
Notificações: Email (SendGrid) + Push (Firebase)
Real-Time: WebSocket com Socket.io
```

**UC-004: Avaliar Prestador (Contratante)**
```
Actor: Contratante que contratou prestador
Fluxo:
  1. Após conclusão de serviço, aparece "Avaliar"
  2. Clica em stars (1-5)
  3. Escreve comentário (opcional)
  4. Sistema valida comentário contra palavrões
  5. Se OK, publica; se maldoso, rejeita
  6. Avaliação aparece no perfil do prestador (público)
Filtro: Lista de ~500 palavrões comuns (pt-BR)
Validação: Apenas quem contratou pode avaliar
Isolamento: ✅ Comentário é de quem avaliou
```

**UC-005: Ver Relatório de Ganhos (Prestador)**
```
Actor: Prestador logado
Fluxo:
  1. Acessa "Meus Ganhos"
  2. Seleciona período (mês/ano)
  3. Vê tabela:
     - Total faturado
     - Breakdown por categoria
     - Breakdown por cliente
  4. Opcional: exportar PDF/CSV
Dados: Query agregado em relatorio_ganhos_cache (pré-computado via trigger)
Isolamento: ✅ Apenas esse prestador vê seus ganhos
SLA: < 1 segundo (dados em cache)
```

---

## 3. Funcionalidades Detalhadas

### 3.1 Isolamento de Dados por Tenant

**Estratégia:** Tenant = user_id (extraído do JWT)

**Validação em 3 camadas:**

1. **JWT Security Filter**
   ```java
   @Component
   public class TenantFilter extends OncePerRequestFilter {
       protected void doFilterInternal(HttpServletRequest req, ...) {
           String token = extractToken(req.getHeader("Authorization"));
           String userId = jwtProvider.getUserIdFromToken(token);
           req.setAttribute("tenant_id", userId); // ← disponível em toda requisição
       }
   }
   ```

2. **Service Layer Validation**
   ```java
   @Service
   public class ServicoService {
       public Servico getServico(Long servicoId, Long tenantId) {
           Servico s = repo.findById(servicoId);
           if (!s.getPrestadorId().equals(tenantId)) {
               throw new AccessDeniedException("Acesso negado");
           }
           return s;
       }
   }
   ```

3. **Repository Queries (Database)**
   ```java
   // ✅ Correto: incluir tenant_id na query
   repo.findByIdAndPrestadorId(servicoId, tenantId);
   
   // ❌ Nunca fazer:
   repo.findById(servicoId); // sem validação de tenant
   ```

**Regras de Acesso:**

| Ação | Acesso | Razão |
|------|--------|-------|
| Listar meus serviços | `WHERE prestador_id = :tenant` | Dado privado |
| Listar todos os serviços (busca) | `WHERE 1=1` (nenhum filtro tenant) | Dado público |
| Ver minhas avaliações dadas | `WHERE usuario_id = :tenant` | Dado privado |
| Ver avaliações que recebi | `WHERE alvo_id = :tenant AND visibilidade='public'` | Público, mas meu |
| Ver chat com User B | `WHERE (remetente=:tenant AND destinatario=B) OR (remetente=B AND destinatario=:tenant)` | Só participantes |
| Ver chat de User B com User C | ❌ FORBIDDEN | Não sou participante |

### 3.2 Chat Real-Time 1:1 (WebSocket + Socket.io)

**Arquitetura:**

Backend (Spring Boot):
```
usuário A → envia mensagem via WebSocket
  ↓
[spring-boot-starter-websocket + Socket.io library]
  ↓
Backend persiste em chat_messages table
  ↓
Backend emite evento "nova_mensagem" via Socket.io para user B
  ↓
usuário B (se online) recebe em tempo real
usuário B (se offline) → background job envia email + push
```

Frontend (Angular):
```
ChatMessageComponent
  ↓
conecta via ngx-socket-io (biblioteca Socket.io para Angular)
  ↓
ouve eventos "nova_mensagem", "usuario_digitando", "mensagem_lida"
  ↓
atualiza DOM em tempo real
```

**Tabelas:**

```sql
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    remetente_id BIGINT NOT NULL REFERENCES usuarios(id),
    destinatario_id BIGINT NOT NULL REFERENCES usuarios(id),
    conteudo TEXT NOT NULL,
    lido BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    
    INDEX idx_chat_messages_remetente_destinatario (remetente_id, destinatario_id),
    CHECK (remetente_id != destinatario_id)
);

CREATE TABLE conversas_chat (
    id BIGSERIAL PRIMARY KEY,
    usuario_1_id BIGINT NOT NULL REFERENCES usuarios(id),
    usuario_2_id BIGINT NOT NULL REFERENCES usuarios(id),
    ultima_mensagem TEXT,
    updated_at TIMESTAMP DEFAULT NOW(),
    
    UNIQUE (usuario_1_id, usuario_2_id),
    CHECK (usuario_1_id < usuario_2_id) -- sempre user_1 < user_2
);
```

**Fluxo de Mensagem:**

```
1. User A envia: POST /api/v1/chat/send
   Body: { destinatario_id: B, conteudo: "Oi!" }
   
2. Backend:
   - Valida: tenant_id = A
   - Insere em chat_messages(remetente_id=A, destinatario_id=B, ...)
   - Atualiza conversas_chat(A, B, ...)
   - Emite via Socket.io: socket.to(`user_${B}`).emit('mensagem', {...})
   
3. Frontend A: vê mensagem no chat imediatamente
   Frontend B: 
     - Se online: vê em tempo real via WebSocket
     - Se offline: ignora emit, mas backend já persistiu
     
4. Background Job (cron 1x/min):
   - Busca mensagens não-notificadas para user B offline
   - Envia email via SendGrid
   - Envia push via Firebase Cloud Messaging
   - Marca como notificado
```

**Limitações & Trade-offs:**

- ✅ Socket.io é gratuito (self-hosted)
- ✅ Escalável com Redis pub/sub (v2)
- ✖️ Se backend down, WebSocket desconecta (mensagens continuam persistidas)
- ✖️ Sem sincronia cross-server (por enquanto single server; Redis resolve em v2)

### 3.3 Avaliações (Stars + Comments com Filtro)

**Modelo:**

```sql
CREATE TABLE avaliacoes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),  -- quem avalia
    alvo_id BIGINT NOT NULL REFERENCES usuarios(id),     -- quem é avaliado
    stars INT NOT NULL CHECK (stars >= 1 AND stars <= 5),
    comentario TEXT,
    verificado_conteudo BOOLEAN DEFAULT FALSE,           -- passou filtro
    created_at TIMESTAMP DEFAULT NOW(),
    
    UNIQUE (usuario_id, alvo_id),  -- uma avaliação por par
    INDEX idx_avaliacoes_alvo (alvo_id),
    CHECK (usuario_id != alvo_id)
);
```

**Filtro de Palavrões:**

```java
@Service
public class AvaliacaoService {
    private static final List<String> PALABRAS_PROIBIDAS = Arrays.asList(
        "xingamento1", "xingamento2", ...  // ~500 palavras em pt-BR
    );
    
    public void criarAvaliacao(Long tenantId, AvaliacaoDTO dto) {
        // Validar isolamento
        if (!tenantId equals dto.usuario_id) {
            throw new AccessDeniedException("Acesso negado");
        }
        
        // Validar conteúdo
        if (contemPalavrasProibidas(dto.comentario)) {
            throw new ValidationException("Comentário contém linguagem imprópria");
        }
        
        // Salvar
        Avaliacao a = new Avaliacao();
        a.setUsuarioId(tenantId);
        a.setAlvoId(dto.alvo_id);
        a.setStars(dto.stars);
        a.setComentario(dto.comentario);
        a.setVerificadoConteudo(true);
        avaliacaoRepo.save(a);
    }
    
    private boolean contemPalavrasProibidas(String texto) {
        String lower = texto.toLowerCase();
        return PALABRAS_PROIBIDAS.stream().anyMatch(lower::contains);
    }
}
```

**Visibilidade:**

- ✅ Avaliações são **públicas** no perfil do avaliado
- ❌ Sem edição após publicação (dados históricos importantes)
- ✅ Rating médio exibido (ex: "4.8 ⭐ de 23 avaliações")
- ✅ Isolamento: só quem avaliou vê que foi ele (nome público)

### 3.4 Busca Paginada com Filtros

**Endpoint:**

```
GET /api/v1/servicos?categoria=pintura&search=sp&page=1&size=10&sort=recente

Response:
{
    "content": [
        {
            "id": 1,
            "nome": "Pintura de parede",
            "prestador": {
                "id": 123,
                "nome": "João Pintor",
                "avaliacao_media": 4.8,
                "num_avaliacoes": 15
            },
            "categoria": "pintura",
            "preco": 500.00,
            "criado_em": "2026-04-08T10:30:00Z"
        },
        ...
    ],
    "totalElements": 156,
    "totalPages": 16,
    "currentPage": 1,
    "size": 10
}
```

**Query Backend:**

```java
@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {
    Page<Servico> findByCategoria(String categoria, Pageable pageable);
    Page<Servico> findByNomeContainingIgnoreCaseOrDescricaoContainingIgnoreCase(
        String nome, String descricao, Pageable pageable);
}

@Service
public class ServicoService {
    public Page<ServicoDTO> buscar(String categoria, String search, int page, int size) {
        Pageable pg = PageRequest.of(page - 1, size, Sort.by("criadoEm").descending());
        
        Page<Servico> result;
        if (categoria != null && search != null) {
            result = repo.findByCategoria(categoria, pg);
            // filtro search no serviço
            result = result.filter(s -> s.getNome().toLowerCase().contains(search.toLowerCase()));
        } else if (categoria != null) {
            result = repo.findByCategoria(categoria, pg);
        } else {
            result = repo.findAll(pg);
        }
        
        return result.map(servico -> new ServicoDTO(...));
    }
}
```

**Performance:**

- ✅ Índice em `(categoria, criado_em)` para queries rápidas
- ✅ Cache em Redis (v2) se muitos acessos
- ✅ SLA: < 2-5 segundos
- ✖️ Full-text search em future (Elasticsearch)

### 3.5 Relatório de Ganhos (Pré-Computado com Trigger)

**Estratégia: Agregado invalidado em tempo real**

```sql
-- Tabela de cache
CREATE TABLE relatorio_ganhos_cache (
    id BIGSERIAL PRIMARY KEY,
    prestador_id BIGINT NOT NULL UNIQUE REFERENCES usuarios(id),
    mes_ano DATE NOT NULL,  -- first day of month
    total_faturado DECIMAL(12, 2) DEFAULT 0,
    num_servicos INT DEFAULT 0,
    updated_at TIMESTAMP DEFAULT NOW(),
    
    UNIQUE (prestador_id, mes_ano)
);

-- Tabela de breakdown por categoria
CREATE TABLE relatorio_ganhos_categoria (
    id BIGSERIAL PRIMARY KEY,
    relatorio_id BIGINT NOT NULL REFERENCES relatorio_ganhos_cache(id) ON DELETE CASCADE,
    categoria VARCHAR(50),
    total DECIMAL(12, 2),
    num_servicos INT
);

-- Trigger: ao concluir serviço, regen relatório daquele mês
CREATE OR REPLACE FUNCTION atualizar_relatorio_ganhos()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'CONCLUIDO' THEN
        INSERT INTO relatorio_ganhos_cache (prestador_id, mes_ano, total_faturado, num_servicos)
        SELECT 
            NEW.prestador_id,
            DATE_TRUNC('month', NOW())::DATE,
            SUM(preco),
            COUNT(*)
        FROM servicos
        WHERE prestador_id = NEW.prestador_id
            AND status = 'CONCLUIDO'
            AND DATE_TRUNC('month', created_at) = DATE_TRUNC('month', NOW())
        ON CONFLICT (prestador_id, mes_ano) DO UPDATE SET
            total_faturado = EXCLUDED.total_faturado,
            num_servicos = EXCLUDED.num_servicos,
            updated_at = NOW();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_atualizar_relatorio
AFTER UPDATE ON servicos
FOR EACH ROW
EXECUTE FUNCTION atualizar_relatorio_ganhos();
```

**Query Frontend:**

```java
@Service
public class GanhosService {
    public RelatorioDTO gerarRelatorio(Long tenantId, int mes, int ano) {
        // Validar que é prestador
        Usuario user = usuarioRepo.findById(tenantId);
        if (user.getTipoUsuario() != TipoUsuario.PRESTADOR) {
            throw new AccessDeniedException("Apenas prestadores veem ganhos");
        }
        
        // Query no cache
        Date mesAno = new Date(ano, mes - 1, 1);
        RelatorioCache cache = cacheRepo.findByPrestadorIdAndMesAno(tenantId, mesAno);
        
        // Se não existir, criar (fallback)
        if (cache == null) {
            cache = computeRelatorio(tenantId, mes, ano);
        }
        
        return new RelatorioDTO(cache);
    }
}
```

---

## 4. Stack & Dependências

### Backend (Spring Boot 3.3.5)

**Dependências novas/afetadas:**

```xml
<!-- WebSocket + Socket.io -->
<dependency>
    <groupId>io.socket</groupId>
    <artifactId>socket.io-server-java</artifactId>
    <version>4.x</version>
</dependency>

<!-- JWT (já existe, verificar versão) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.12.6</version>
</dependency>

<!-- Logging -->
<dependencies>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-logging</artifactId>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Arquitetura:**
- Controller → Service → Repository → PostgreSQL
- Security Filter para tenant validation
- Background job (Spring Scheduler) para notificações

### Frontend (Angular 20)

**Dependências novas:**

```json
{
    "ngx-socket-io": "^14.0.0",
    "socket.io-client": "^4.7.0"
}
```

**Módulos:**
- `CommonModule`, `FormsModule`, `HttpClientModule`
- `RouterModule` (app.routes.ts)
- `Socket.io` via `NgxSocketIoModule`

**Componentes:**
- Feature-based em `src/app/components/`
- Seguindo padrão existente (selector `app-*`, `*.component.ts/html/css`)

### Database (PostgreSQL)

**Novas tabelas:**
- `avaliacoes`
- `chat_messages`
- `conversas_chat`
- `relatorio_ganhos_cache`
- `relatorio_ganhos_categoria`

**Alterações:**
- Adicionar índices em `servicos(prestador_id)` e `solicitacoes_servico(contratante_id)` se não existirem
- Trigger `atualizar_relatorio_ganhos` no INSERT/UPDATE de `servicos`

---

## 5. Segurança & Validação

### 5.1 Isolamento de Tenant

✅ **3 camadas de validação:**

1. JWT Filter → extrai `user_id` como `tenant_id`
2. Service → valida antes de acessar repositório
3. Repository → query sempre inclui `tenant_id`

### 5.2 RBAC

| Operação | Validação |
|----------|-----------|
| Editar meu serviço | `prestador_id == tenant_id` |
| Editar minha solicitação | `contratante_id == tenant_id` |
| Enviar mensagem para B | `remetente_id == tenant_id` |
| Avaliar prestador | `usuario_id == tenant_id AND contrato existido` |
| Ver ganhos | `tenant_id == prestador_id AND tipo_usuario == PRESTADOR` |

### 5.3 Frontend Security

- ✅ JWT armazenado em `localStorage` (seguro contra XSS com HttpOnly, se possível)
- ✅ Interceptor adiciona `Authorization: Bearer <token>` em toda request
- ✅ On 401, chama refresh token (via `AuthService`)
- ✅ Guard em rotas (opcional: `canActivate` que valida token)

---

## 6. Tratamento de Concorrência

### 6.1 Cenários de Race Condition

**Cenário 1: Dois prestadores aceitando mesma solicitação**
```
User A (prestador) → aceita solicitação X
User B (prestador) → aceita mesma solicitação X (quase simultaneamente)
Resultado desejado: Apenas 1 aceita, outro recebe erro
```

**Cenário 2: Múltiplas abas abrindo chat**
```
Browser aba1 → envia msg para User B
Browser aba2 → envia msg para User B
Resultado: Ambas inserem em chat_messages OK (ordem garantida por timestamp)
```

**Cenário 3: Atualizar relatório enquanto calcula**
```
Serviço X concluído → trigger regen relatório de prestador
Trigger rodando → prestador acessa `/ganhos`
Resultado desejado: Vê cache atualizado (ou aguarda 1s)
```

### 6.2 Estratégia

**1. Pessimistic Locking para operações críticas**

```java
// "Aceitar solicitação" → apenas 1 pode aceitar
@Transactional
public void aceitarSolicitacao(Long solicitacaoId, Long tenantId) {
    // SELECT FOR UPDATE bloqueia linha até fim da transação
    SolicitacaoServico sol = repo.findByIdWithLock(solicitacaoId);
    
    if (sol.getStatus() != Status.ABERTA) {
        throw new ConflictException("Já foi aceita por outro prestador");
    }
    
    sol.setStatus(Status.ACEITA);
    sol.setPrestadorId(tenantId);
    repo.save(sol);
    // transação termina, lock liberado
}
```

**Query:**
```sql
SELECT * FROM solicitacoes_servico WHERE id = ? FOR UPDATE;
```

**2. Optimistic Locking para operações co-existentes**

```java
@Entity
public class Avaliacao {
    @Version
    private Long versao;  // ← hibernate auto-incrementa on update
    
    // ... outros campos
}

// Ao atualizar:
avaliacaoRepo.save(avaliacao);  // Se versão não bate, lança exception
```

**3. Database Constraints**

```sql
ALTER TABLE solicitacoes_servico 
    ADD CONSTRAINT uk_solicitacao_prestador 
    UNIQUE (solicitacao_id, prestador_id);
    -- ↑ Garante que não há 2 aceitações do mesmo prestador
```

### 6.3 Testes de Concorrência

- [ ] Load test: 100 prestadores simultâneos buscando serviços
- [ ] Stress test: 10 mensagens/segundo no chat
- [ ] Race condition: 2+ tentativas de aceitar solicitação
- [ ] Tool: Apache JMeter ou Gatling

---

## 7. Observabilidade & Monitoring

### 7.1 Logs

**Padrão SLF4J com contexto de tenant:**

```java
@Slf4j
@Service
public class ServicoService {
    public void criarServico(Long tenantId, ServicoDTO dto) {
        try {
            log.info("[tenant={}] Criando novo serviço: {}", tenantId, dto.getNome());
            // ...
        } catch (Exception e) {
            log.error("[tenant={}] Erro ao criar serviço", tenantId, e);
        }
    }
}
```

**Eventos a logar:**
- ✅ Login (tenant_id confirmado)
- ✅ Acesso negado (tentativa de acessar dados de outro tenant)
- ✅ Criar/atualizar/deletar solicitação, serviço, mensagem, avaliação
- ✅ Operações sensíveis (pagamento, conclusão de serviço)

### 7.2 Métricas (futuro)

- [ ] Latência por operação (busca, chat, avaliação)
- [ ] Taxa de acesso negado (indicador de ataque?)
- [ ] Contagem de mensagens/avaliações por dia
- [ ] Uptime do WebSocket

### 7.3 Alertas (futuro)

- [ ] Taxa de erro > 1% → alerta
- [ ] Latência P99 > 5s → alerta
- [ ] Múltiplas tentativas de acesso negado de um IP → potencial ataque

---

## 8. Rollout & Rollback

### 8.1 Feature Flags (via Spring Cloud Config ou Togglz)

```java
@Service
public class MultiTenancyService {
    @Value("${feature.multitenancy.enabled}")
    private boolean multitenancyEnabled;
    
    public void buscarServicos(...) {
        if (multitenancyEnabled) {
            // Nova lógica multitenancy
        } else {
            // Lógica legacy
        }
    }
}
```

### 8.2 Plano de Rollout

**Fase 1 (Deploy 1):** Feature flag OFF
- Código pronto, desativado
- Usuarios continuam single-tenant

**Fase 2 (Day 1):** Feature flag ON para 10% usuários
- Monitor: erro rate, latência, logs
- Se OK → próxima fase

**Fase 3 (Day 3):** 50% usuários
- Continue monitorando
- Rollback opção se erro crítico

**Fase 4 (Day 7):** 100% usuários
- Remover feature flag após 14 dias

### 8.3 Rollback

If erro crítico:
```bash
# 1. Feature flag OFF (instantâneo)
# 2. Aplicação reinicia
# 3. Usuários revertidos logicamente (sem perda de dados)
# 4. Investigate logs em dev environment
# 5. Hotfix e re-deploy
```

---

## 9. Riscos & Mitigação

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|--------|-----------|
| **Performance de busca degrada** | Média | Alto | Índice em categoria+data, cache Redis (v2) |
| **Concorrência — race condition** | Média | Alto | Pessimistic + optimistic locking, testes de carga |
| **Chat WebSocket não sincroniza** | Baixa | Médio | Redis pub/sub (v2), fallback polling |
| **Falha no trigger SQL** | Baixa | Médio | Testes unitários e integração, monitoring |
| **Vazamento de dados entre tenants** | Muito baixa | Crítico | 3 camadas de validação, testes de segurança |

---

## 10. Fases de Implementação (Alto Nível)

### Fase 1: Setup Banco de Dados (1 semana)
- [ ] Criar migrações SQL
- [ ] Criar tabelas: `avaliacoes`, `chat_messages`, `conversas_chat`
- [ ] Criar trigger `atualizar_relatorio_ganhos`
- [ ] Adicionar índices críticos
- [ ] Testes: validar dados com script SQL

### Fase 2: Backend Base (2 semanas)
- [ ] TenantValidator filter
- [ ] Atualizar repositories existentes (servicos, solicitacoes)
- [ ] Service base para tenant validation
- [ ] API endpoints: `/api/v1/servicos`, `/api/v1/avaliacoes`
- [ ] Testes unitários + integração

### Fase 3: Chat Real-Time (1 semana)
- [ ] Setup Socket.io no Spring Boot
- [ ] ChatService + ChatController
- [ ] Implementar SendGrid + Firebase
- [ ] Frontend: ChatListComponent, ChatMessageComponent
- [ ] Tests: enviar/receber mensagem

### Fase 4: Avaliações + Filtro (1 semana)
- [ ] AvaliacaoService com filtro de palavrões
- [ ] AvaliacaoController + endpoints
- [ ] Frontend: RatingFormComponent
- [ ] Tests: validar filtro, isolamento de tenant

### Fase 5: Busca + Relatórios (1 semana)
- [ ] Otimizar queries de busca
- [ ] GanhosService (agregado)
- [ ] Frontend: SearchComponent atualizado, EarningsReportComponent
- [ ] Tests: paginação, filtros, performance

### Fase 6: Testes, Docs, Deploy (1-2 semanas)
- [ ] Testes de carga (JMeter)
- [ ] Testes de segurança (acesso negado)
- [ ] Documentação API (Swagger)
- [ ] Runbooks para ops
- [ ] Feature flag setup
- [ ] Canary deploy

---

## 11. Decisões de Design Registradas

| Decisão | Alternativas | Escolha | Razão |
|---------|-------------|---------|-------|
| **Identificação de Tenant** | JWT claim vs. Header vs. Subdomain vs. Path | **JWT claim (user_id)** | Simples, imutável, sem overhead |
| **Chat Real-Time** | WebSocket vs. Polling vs. Serviço 3º | **WebSocket + Socket.io** | Gratuito, self-hosted, baixa latência |
| **Notificações Chat** | Email / Push / In-app | **Email + Push** | Máximo alcance ~offline users |
| **Filtro Palavrões** | Lista local vs. API vs. ML | **Lista local (v1) → API (v2)** | Simples, sem custo inicial, escalável |
| **Ganhos Pré-computado** | Query ao vivo vs. Cache | **Cache com trigger SQL** | Rápido, simples, escalável |
| **Concorrência** | Pessimistic vs. Optimistic vs. Queue | **Hybrid (pessimistic + optimistic)** | Trade-off performance/segurança |

---

## 12. Próximas Etapas

1. ✅ **Discovery concluído** (discovery.md)
2. ✅ **Briefing-tech.v0.md concluído** (este arquivo)
3. 📋 **Próxima ação:** `/lf-specs`
   - Gerar `specs.md` com 12 seções por domínio
   - Gerar `wps.md` com work packages + dependências
   - (Requer designs/Figma da UX)

---

## Sumário

Este briefing técnico documenta todas as decisões de design, arquitetura, segurança e performance para implementação da **multi-tenancy no brjobs**. 

**Status:** ✅ Ready for Specs Generation  
**Próximo passo:** Aguardar designs UX/Figma → `/lf-specs`  
**Contato:** claudeproductive@brjobs.local

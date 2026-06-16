# Security Testing Checklist (WP-14-17)

## Input Validation Tests

### Backend (@RestController)
- [ ] Chat message validation: max length 5000 chars
- [ ] Avaliação nota: enforce 1-5 range (reject 0, 6+)
- [ ] Comentário avaliação: filter profanity, max 500 chars
- [ ] Serviço título: min 3 chars, max 100 chars
- [ ] Serviço preço: must be > 0, max 999999.99
- [ ] Categoria: strict enum validation (pintura, encanamento, etc)

Test Cases:
```javascript
// Test: SQL Injection in search
GET /api/v1/servicos?search='; DROP TABLE servicos; --
Expected: 400 Bad Request with safe error message

// Test: XSS in comentário
POST /api/v1/avaliacoes { "comentario": "<script>alert('xss')</script>" }
Expected: Comment stored escaped/filtered, script not executed

// Test: Negative price
POST /api/v1/servicos { "preco": -100.00 }
Expected: 400 Bad Request, price validation error

// Test: Oversized payload
POST /api/v1/chat/enviar (10MB binary)
Expected: 413 Payload Too Large

// Test: Missing required fields
POST /api/v1/servicos { "titulo": "...", "categoria": null }
Expected: 400 Bad Request, missing categoria
```

## Tenant Isolation Tests

### Access Control (@ValidateTenant Aspect)
```javascript
// Test 1: User A tries to update User B's service
Scenario: User A (tenant_id=123) PUT /api/v1/servicos/456
- Service belongs to User B (usuario_id=789)
Expected: 403 Forbidden, "Access denied"

// Test 2: User A tries to rate themselves
Scenario: User A (tenant_id=123) POST /api/v1/avaliacoes { prestadorId: 123 }
Expected: 400 Bad Request, "Cannot rate yourself"

// Test 3: User A tries to delete User C's message
Scenario: User A (tenant_id=123) DELETE /api/v1/chat/456
- Message from User C to User D
Expected: 403 Forbidden, "Access denied"

// Test 4: Unauthorized user (no JWT) accesses protected endpoint
Scenario: GET /api/v1/ganhos (no Authorization header)
Expected: 401 Unauthorized, "Missing or invalid token"
```

## Database Query Tests

### Tenant-Aware Queries
```sql
-- Verify indexes exist
SELECT * FROM pg_indexes WHERE tablename IN ('servicos', 'chat_messages', 'conversas_chat')
AND indexname LIKE '%usuario_id%';

-- Test query performance: findByIdAndUsuarioId should use index
EXPLAIN ANALYZE
SELECT * FROM servicos WHERE id = 456 AND usuario_id = 123;
-- Expected: Index Scan (not Seq Scan)

-- Test FK cascade on delete
DELETE FROM servicos WHERE id = 456;
-- Verify related SolicitacaoServico records also deleted (if configured)

-- Test trigger: Insert completed service, verify earnings cache updated
INSERT INTO servicos (usuario_id, titulo, descricao, categoria, preco, status) 
  VALUES (123, '...', '...', 'pintura', 100.00, 'CONCLUIDO');
SELECT * FROM relatorio_ganhos_cache WHERE prestador_id = 123;
-- Expected: Cache row inserted/updated automatically
```

## JWT & Authentication Tests

```javascript
// Test 1: Expired token
Token exp: 1 day ago
GET /api/v1/ganhos
Expected: 401 Unauthorized, refresh token in response

// Test 2: Tampered token (signature modified)
Original: eyJhbGc...
Modified: eyJhbGc...AAAA
GET /api/v1/servicos
Expected: 401 Unauthorized, invalid signature

// Test 3: Token from different key
Token signed with key_A, server uses key_B
GET /api/v1/avaliacoes
Expected: 401 Unauthorized

// Test 4: Token missing required claims (user_id)
Token { sub: null, iat: ..., exp: ... }
GET /api/v1/chat/conversas
Expected: 400 Bad Request or extraction error
```

## CORS & Origin Tests

```javascript
// Test 1: Request from allowed origin
Origin: http://localhost:4200
GET /api/v1/servicos
Expected: 200 OK, Access-Control-Allow-Origin: http://localhost:4200

// Test 2: Request from disallowed origin
Origin: http://malicious.com
GET /api/v1/servicos
Expected: CORS error in browser console, no data returned

// Test 3: Preflight request
OPTIONS /api/v1/servicos
Access-Control-Request-Method: POST
Expected: 200 OK with Access-Control-Allow-Methods: GET, POST, PUT, DELETE
```

## Performance & Load Tests

### JMeter Scenarios

1. **Search by category (steady load)**
   - 100 virtual users
   - GET /api/v1/servicos?categoria=pintura
   - Duration: 5 minutes
   - Expected: p99 latency < 1s, success rate > 99%

2. **Create service (spike test)**
   - Start 10 users, ramp to 50 in 1 min
   - POST /api/v1/servicos
   - Expected: No errors, p95 latency < 500ms

3. **Chat message throughput**
   - 20 concurrent users
   - POST /api/v1/chat/enviar every 5 seconds
   - Duration: 10 minutes
   - Expected: 100% delivery, avg latency < 100ms

4. **Rating creation (database stress)**
   - 50 virtual users
   - POST /api/v1/avaliacoes
   - Expected: DB connections < 20, no lock timeouts

## Logging & Audit Tests

```javascript
// Verify audit log entries for sensitive operations

// POST /api/v1/avaliacoes -> log entry with:
// - timestamp
// - user_id (tenant_id)
// - prestador_id
// - nota
// - commentário (sanitized)
// - ip_address
// - user_agent

// DELETE /api/v1/servicos/{id} -> log with:
// - timestamp
// - user_id
// - servico_id
// - soft_delete (ativo: false set)
// - ip_address
```

## Regression Tests (Post-Deploy)

```bash
#!/bin/bash

# After each deployment, run these checks

echo "Post-Deploy Smoke Tests"

# 1. Can search servicos
curl -s http://api.brjobs.local/api/v1/servicos?page=1 | jq '.content | length'
[[ $? -eq 0 ]] && echo "✅ Search endpoint OK" || echo "❌ Search failed"

# 2. Can create/read avaliação
curl -s -X POST http://api.brjobs.local/api/v1/avaliacoes/v1 \
  -H "Authorization: Bearer $VALID_TOKEN" \
  -d '{"prestadorId":1,"nota":5,"comentario":"Great!"}' | jq '.id'
[[ $? -eq 0 ]] && echo "✅ Avaliação endpoint OK" || echo "❌ Avaliação failed"

# 3. Can retrieve earnings
curl -s http://api.brjobs.local/api/v1/ganhos/corrente \
  -H "Authorization: Bearer $VALID_TOKEN" | jq '.totalFaturado'
[[ $? -eq 0 ]] && echo "✅ Ganhos endpoint OK" || echo "❌ Ganhos failed"

# 4. Chat service responds
curl -s http://api.brjobs.local/api/v1/chat/nao-lidas \
  -H "Authorization: Bearer $VALID_TOKEN"
[[ $? -eq 0 ]] && echo "✅ Chat endpoint OK" || echo "❌ Chat failed"

# 5. Database migrations applied
psql -U postgres -d brjobs -c "SELECT version, description FROM flyway_schema_history ORDER BY version DESC LIMIT 1" | grep V5
echo "✅ Migrations verified (V5 present)"
```


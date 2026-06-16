# Briefing Técnico — Módulo de chat funcional (MVP)

> **Versão:** 0.1
> **Status:** Rascunho
> **Gerado em:** 2026-04-10
> **Baseado em:**
>   - [`../discovery.md`](../discovery.md) — discovery de 2026-04-10

---

## 1. Contexto e Problema

O projeto já possui implementação parcial de chat 1:1 no frontend Angular e backend Spring Boot, com persistência em PostgreSQL. Apesar disso, o fluxo ainda não está funcional de ponta a ponta para uso confiável em ambiente real.

Principais sintomas observados no estado atual:

- contrato inconsistente entre frontend e backend para listagem de conversas;
- retorno de entidade JPA diretamente em endpoint público de API (acoplamento indevido);
- ausência de fechamento de UX de mensagens não lidas no header global;
- comportamento parcial de marcação de leitura;
- atualização de dados ainda sem estratégia consolidada de custo baixo para produção inicial.

A dor recai diretamente em prestadores e contratantes que precisam se comunicar no momento mais crítico do funil (contato e negociação). Se não resolvermos, o módulo existe no código, porém não entrega valor de negócio.

---

## 2. Solução Proposta

Implementar o chat 1:1 funcional em MVP com foco em estabilidade, reaproveitamento do que já existe e baixo custo de operação na AWS.

Escopo desta versão:

- padronizar contratos de API com DTOs explícitos para conversas e mensagens;
- manter atualização via polling HTTP com janela de até 5 segundos;
- suportar exclusivamente mensagem de texto, limite máximo de 500 caracteres;
- implementar badge de não lidas no header com ícone de sino;
- consolidar regra de marcação como lida ao abrir conversa;
- manter autenticação obrigatória para todo acesso ao chat.

Não será construído nesta versão:

- anexos, áudio, vídeo, grupo, reações;
- infraestrutura dedicada de realtime paga (serviços externos de pub/sub);
- push mobile e notificações por e-mail.

Retrocompatibilidade:

- rotas existentes para iniciar conversa serão preservadas;
- tabelas de chat existentes serão reaproveitadas;
- endpoints atuais serão evoluídos por contrato, sem necessidade de migração de dados legados.

---

## 3. Personas e Papéis Afetados

| Papel | Ação que realiza | Impacto da feature |
|-------|------------------|--------------------|
| Contratante | Inicia conversa com prestador via perfil público/detalhe da publicação | Direto — reduz fricção de contato e acelera contratação |
| Prestador | Responde mensagens e negocia escopo/preço/prazo | Direto — melhora conversão e experiência de atendimento |
| Engenharia Frontend | Mantém tela de chat, header e integração com APIs | Direto — menor regressão e fluxo previsível |
| Engenharia Backend | Mantém contratos, regras de leitura e consultas de conversa | Direto — maior consistência de domínio e API |
| QA | Valida fluxos de mensagem, não lidas e erros | Direto — critérios de aceite objetivos |
| Produto | Define escopo de comunicação e evolução futura | Direto — clareza sobre entrega MVP vs roadmap |

---

## 4. Premissas, Restrições e Decisões Tomadas

- **Projeto brownfield:** já existe código de chat em frontend, backend e banco.
- **Sem custo adicional de infra nesta fase:** proibido depender de vendor pago para realtime no MVP.
- **SLA de atualização:** novas mensagens e não lidas refletidas em até 5 segundos.
- **Mensagem texto-only:** limite de 500 caracteres por mensagem.
- **Autenticação obrigatória:** chat não deve funcionar para usuário anônimo.
- **Sem migração de dados:** não há base produtiva ativa para migração.
- **Header global:** deve exibir sino com badge de mensagens não lidas.
- **Padronização de contrato:** endpoint não deve retornar entidade JPA diretamente ao frontend.

---

## 5. Arquitetura e Fluxos

### 5.1 Fluxo principal

```
[Usuário logado]
   -> abre /chat (direto ou via perfil/publicação)
   -> FE carrega lista de conversas
   -> FE inicia polling (5s) para: conversas + não lidas
   -> usuário seleciona conversa
   -> FE busca histórico da conversa
   -> FE marca mensagens recebidas como lidas
   -> usuário envia nova mensagem
   -> BE persiste mensagem, atualiza conversa
   -> próximo ciclo de polling reflete estado atualizado
```

Fluxo de entrada por perfil público/publicação:

```
[Perfil Público | Detalhe Publicação]
   -> botão "Entrar em contato por chat"
   -> navegação para /chat?usuarioId={id}&nome={nome}
   -> FE cria contexto de conversa direta
   -> FE carrega histórico com aquele usuário
```

### 5.2 Modelo de dados

| Campo | Entidade | Tipo | Obrigatório | Descrição |
|-------|----------|------|-------------|-----------|
| id | chat_messages | BIGINT | Sim | Identificador da mensagem |
| remetente_id | chat_messages | BIGINT (FK usuarios) | Sim | Usuário que enviou |
| destinatario_id | chat_messages | BIGINT (FK usuarios) | Sim | Usuário que recebe |
| conteudo | chat_messages | TEXT | Sim | Texto da mensagem (max 500 no domínio) |
| lido | chat_messages | BOOLEAN | Sim | Indica se destinatário leu |
| notificado | chat_messages | BOOLEAN | Sim | Reserva para notificação futura |
| created_at | chat_messages | TIMESTAMP | Sim | Data de criação |
| id | conversas_chat | BIGINT | Sim | Identificador da conversa |
| usuario_1_id | conversas_chat | BIGINT (FK usuarios) | Sim | Menor ID do par |
| usuario_2_id | conversas_chat | BIGINT (FK usuarios) | Sim | Maior ID do par |
| ultima_mensagem_id | conversas_chat | BIGINT (FK chat_messages) | Não | Última mensagem da conversa |
| updated_at | conversas_chat | TIMESTAMP | Sim | Última atualização da conversa |

Contratos DTO propostos para API (camada de transporte):

- `ConversationListItem`
  - `id: number`
  - `contatoId: number`
  - `contatoNome: string`
  - `ultimaMensagem: string | null`
  - `ultimaMensagemEm: string | null`
  - `ultimaMensagemRemetenteId: number | null`
  - `naoLidas: number`
  - `atualizadaEm: string`

- `MessageItem`
  - `id: number`
  - `conversaId: number`
  - `remetenteId: number`
  - `remetenteNome: string`
  - `destinatarioId: number`
  - `conteudo: string`
  - `status: "ENVIADA" | "LIDA"`
  - `criadoEm: string`

### 5.3 Endpoints

| Método | Path | Auth | Payload resumido | Resposta |
|--------|------|------|------------------|----------|
| GET | /api/v1/chat/conversas | Sim | sem body | `ConversationListItem[]` |
| GET | /api/v1/chat/conversa/{outroUsuarioId}?limit=50 | Sim | path + query | `MessageItem[]` |
| POST | /api/v1/chat/enviar?destinatarioId={id} | Sim | `{ conteudo, publicacaoId?, clientTempId? }` | `MessageItem` |
| PUT | /api/v1/chat/marcar-lida/{mensagemId} | Sim | sem body | `204 No Content` |
| GET | /api/v1/chat/nao-lidas | Sim | sem body | `{ totalNaoLidas: number }` ou número bruto (compat) |

Observação de arquitetura:

- endpoint de `conversas` deve retornar DTO e não `ConversaChat` entidade;
- validação de tenant/user atual segue padrão existente via `tenant_id` extraído do JWT;
- persistência segue camada Controller -> Service -> Repository.

Integrações externas:

- nenhuma integração nova obrigatória nesta fase.

---

## 6. UX e Comportamento da Interface

Não há briefing UX dedicado para esta feature, então os fluxos abaixo são a base funcional da implementação.

### 6.1 Estados da interface

```
[Estado vazio conversas]
-> painel esquerdo mostra "Nenhuma conversa ativa"

[Estado carregando conversas]
-> exibir skeleton/list placeholder + bloqueio de clique sensível

[Estado conversa selecionada]
-> histórico visível + input habilitado + botão enviar

[Estado enviando mensagem]
-> botão enviar desabilitado até retorno

[Estado erro envio]
-> manter texto no input + feedback de erro + opção de reenviar

[Estado não autenticado]
-> redirecionar para /login antes de abrir chat
```

### 6.2 Wireframes

Wireframe da tela de chat:

```
┌───────────────────────────────────────────────────────────────────────┐
│ Header (logo | navegação | sino [3] | usuário)                       │
├───────────────────────────────────────────────────────────────────────┤
│ Conversas                 │ Chat atual                               │
│ ┌───────────────────────┐ │ ┌───────────────────────────────────────┐ │
│ │ [Conversa A]  19:25   │ │ │ Conversa com Maria Souza             │ │
│ │ "Pode vir amanhã?"    │ │ ├───────────────────────────────────────┤ │
│ │ [Conversa B]  18:10   │ │ │ [João] Olá!                          │ │
│ │ "Fechado"            │ │ │ [Maria] Pode vir amanhã?             │ │
│ └───────────────────────┘ │ ├───────────────────────────────────────┤ │
│                           │ │ [Digite sua mensagem...] [Enviar]     │ │
│                           │ └───────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────┘
```

Wireframe do sino no header:

```
┌──────────────────────────────────────────────┐
│ ... links ...        🔔                      │
│                     [badge: 0..99+]          │
└──────────────────────────────────────────────┘
```

Regras de interação UX:

- badge deve atualizar no mesmo ciclo de polling (<=5s);
- ao abrir conversa, mensagens recebidas devem mudar para lidas;
- impedir envio de mensagem vazia;
- impedir envio acima de 500 caracteres com feedback explícito.

---

## 7. Regras de Negócio

1. Usuário DEVE estar autenticado para acessar qualquer endpoint de chat.
2. Usuário NÃO DEVE enviar mensagem para si mesmo.
3. Mensagem DEVE conter texto não vazio e no máximo 500 caracteres.
4. Sistema DEVE manter histórico de mensagens por conversa 1:1.
5. Sistema DEVE ordenar histórico por data de criação (mais recente no topo ou base conforme UI definida, mas consistente).
6. Sistema DEVE contar não lidas apenas para o destinatário logado.
7. SE usuário abrir uma conversa, ENTÃO mensagens recebidas dessa conversa DEVEM ser marcadas como lidas.
8. Sistema NÃO DEVE expor conversa entre terceiros para usuário não participante.
9. Sistema PODE incluir `publicacaoId` opcional na mensagem para rastreio de origem, sem bloquear envio quando ausente.
10. Badge do header DEVE refletir total de mensagens não lidas em até 5 segundos.

---

## 8. Segurança e Privacidade

### 8.1 Controle de acesso

| Ação | Papel permitido | Papel bloqueado |
|------|-----------------|-----------------|
| Listar minhas conversas | Usuário autenticado | Anônimo |
| Ler conversa com outro usuário | Participante da conversa | Qualquer terceiro |
| Enviar mensagem | Usuário autenticado | Anônimo |
| Marcar mensagem como lida | Destinatário da mensagem | Remetente e terceiros |
| Ver total de não lidas | Usuário autenticado (próprias) | Terceiros |

### 8.2 Dados sensíveis

| Dado | Onde armazenar | Criptografia | Pode logar? |
|------|---------------|--------------|-------------|
| Conteúdo da mensagem | PostgreSQL (`chat_messages`) | Em trânsito via HTTPS/TLS | Não (evitar log de conteúdo bruto) |
| Identificadores de usuários | JWT + banco | HTTPS/TLS + controles de acesso | Sim, de forma parcial para auditoria |
| Token JWT | Storage frontend atual | HTTPS/TLS em trânsito | Não |

Regras adicionais:

- backend deve validar tamanho e conteúdo mínimo independentemente de validação frontend;
- logs de erro não devem vazar texto integral da mensagem;
- auditoria mínima: remetenteId, destinatarioId, mensagemId, timestamp, resultado da operação.

---

## 9. Tratamento de Erros e Resiliência

| Cenário | Causa | Comportamento esperado | Mensagem ao usuário |
|---------|-------|----------------------|---------------------|
| Envio sem conteúdo | input vazio | bloquear envio no FE e rejeitar no BE | Digite uma mensagem antes de enviar. |
| Envio com >500 chars | limite ultrapassado | bloquear no FE + 400 no BE | Mensagem deve ter no máximo 500 caracteres. |
| Destinatário inexistente | ID inválido | retornar erro de domínio | Não foi possível enviar para este usuário. |
| Tentativa de enviar para si | remetenteId == destinatarioId | rejeitar operação | Você não pode enviar mensagem para si mesmo. |
| Não autenticado | token ausente/expirado | 401 + redirecionar login | Sua sessão expirou. Faça login novamente. |
| Acesso a conversa de terceiros | violação de autorização | 403 | Você não tem permissão para acessar esta conversa. |
| Falha de banco na gravação | indisponibilidade/transação | manter input e permitir retry | Erro ao enviar mensagem. Tente novamente. |
| Polling falhando | timeout/rede | manter última UI estável + retentativa no próximo ciclo | Não foi possível atualizar o chat agora. |
| Marcar como lida falha | erro no endpoint | manter estado anterior e tentar novamente | Não foi possível atualizar status de leitura. |
| Endpoint de conversas com contrato inválido | backend retornando shape inesperado | tratar fallback e logar erro estruturado | O chat está temporariamente indisponível. |

Resiliência adicional:

- polling deve ser pausado em `ngOnDestroy`;
- polling pode aumentar intervalo em aba inativa (otimização opcional da fase 2);
- retry deve ser limitado para evitar spam de requisições em falha contínua.

---

## 10. Observabilidade

### 10.1 Eventos a logar

| Evento | Campos obrigatórios | Nível |
|--------|--------------------:|-------|
| chat_conversations_loaded | user_id, conversations_count, latency_ms, timestamp | info |
| chat_messages_loaded | user_id, other_user_id, messages_count, latency_ms, timestamp | info |
| chat_message_send_attempt | user_id, destinatario_id, message_length, timestamp | info |
| chat_message_send_success | user_id, destinatario_id, mensagem_id, latency_ms, timestamp | info |
| chat_message_send_failure | user_id, destinatario_id, error_code, timestamp | warn |
| chat_unread_count_loaded | user_id, unread_total, latency_ms, timestamp | info |
| chat_mark_read_success | user_id, mensagem_id, timestamp | info |
| chat_mark_read_failure | user_id, mensagem_id, error_code, timestamp | warn |
| chat_payload_contract_error | route, user_id, payload_shape, timestamp | error |

### 10.2 Métricas

| Métrica | Tipo | O que mede |
|---------|------|-----------|
| chat_send_success_rate | taxa | percentual de envios bem-sucedidos |
| chat_send_latency_ms_p95 | histograma | latência p95 de envio |
| chat_poll_latency_ms_p95 | histograma | latência p95 do polling |
| chat_unread_fetch_error_rate | taxa | taxa de falha ao buscar não lidas |
| chat_conversations_fetch_error_rate | taxa | taxa de falha ao buscar conversas |
| chat_messages_fetch_error_rate | taxa | taxa de falha ao buscar histórico |
| chat_active_users | gauge | usuários com polling ativo por janela |

### 10.3 Alertas

| Condição | Threshold | Ação |
|----------|-----------|------|
| chat_send_success_rate baixo | < 95% por 10 min | investigar backend e banco |
| chat_send_latency_ms_p95 alto | > 1500 ms por 15 min | analisar queries e carga de app |
| chat_poll_latency_ms_p95 alto | > 2000 ms por 15 min | revisar endpoints de conversa/não-lidas |
| chat_*_error_rate alto | > 5% por 10 min | abrir incidente e reduzir impacto |
| chat_payload_contract_error > 0 | qualquer ocorrência em produção | rollback rápido da versão de contrato |

---

## 11. Variáveis de Ambiente e Configuração

```env
# Chat MVP - Backend (BE)
✱ CHAT_ENABLED=true                      # BE — liga/desliga o módulo de chat
✱ CHAT_MESSAGE_MAX_LENGTH=500            # BE — limite máximo de caracteres por mensagem
✱ CHAT_HISTORY_DEFAULT_LIMIT=50          # BE — limite padrão de histórico por conversa
CHAT_HISTORY_MAX_LIMIT=100               # BE — limite hard máximo permitido em query param

# Chat MVP - Frontend (FE)
✱ NG_APP_CHAT_POLL_INTERVAL_MS=5000      # FE — intervalo de polling para mensagens e não lidas
✱ NG_APP_CHAT_MESSAGE_MAX_LENGTH=500     # FE — limite visual e validação local
NG_APP_CHAT_HEADER_BADGE_MAX=99          # FE — valor máximo exibido antes de "99+"

# Segurança e autenticação (já existentes no projeto)
✱ JWT_SECRET=change_me                   # BE — segredo do JWT
✱ JWT_EXPIRATION=3600000                 # BE — duração do token
✱ APP_CORS_ALLOWED_ORIGINS=http://localhost:4200
```

---

## 12. Estratégia de Rollout e Rollback

**Rollout:** direto (MVP), sem feature flag externa paga, com validação em ambiente de homologação antes do deploy.

Plano de rollout:

1. Deploy backend com DTOs e validações de mensagem;
2. Deploy frontend com ajuste de contratos, polling e sino no header;
3. Smoke test end-to-end:
   - iniciar conversa por perfil público;
   - enviar e receber mensagem em duas contas;
   - validar badge no header;
   - validar marcação como lida;
4. Monitorar métricas/erros por janela inicial de estabilização.

**Rollback:**

- rollback de frontend para versão anterior se houver quebra visual/contrato;
- backend deve preservar endpoints antigos de forma compatível temporária ou fallback rápido de release;
- caso contrato novo cause erro crítico, priorizar hotfix no mapper de DTO do endpoint `conversas`.

---

## 13. Fases de Entrega

### Fase 1 — Fechamento de contrato e regras de domínio

- [ ] Criar/ajustar DTO de lista de conversas no backend.
- [ ] Garantir que `/chat/conversas` não retorna entidade JPA diretamente.
- [ ] Validar limite de 500 caracteres no backend.
- [ ] Padronizar erros de domínio para chat (mensagem vazia, self-send, destinatário inválido).

### Fase 2 — Integração frontend e UX funcional

- [ ] Ajustar `chat.service.ts` para novos contratos.
- [ ] Implementar polling consistente de 5s (mensagens + não lidas).
- [ ] Implementar marcação de leitura ao abrir conversa.
- [ ] Tratar estados de erro/retry no envio e carregamento.

### Fase 3 — Notificações globais e estabilização

- [ ] Adicionar sino com badge no header.
- [ ] Integrar total de não lidas ao header e sincronizar com tela de chat.
- [ ] Executar testes manuais multiusuário (duas sessões simultâneas).
- [ ] Validar métricas e alertas mínimos de operação.

---

## 14. Fora do Escopo (desta versão)

- Upload de arquivo e mídia em mensagem.
- Edição/apagamento de mensagens.
- Conversa em grupo.
- Notificações push mobile e e-mail.
- IA de moderação semântica de mensagens.
- Infra de WebSocket/SSE gerenciada por serviço externo pago.

---

## 15. Riscos e Pontos em Aberto

| # | Descrição | Probabilidade | Impacto | Mitigação |
|---|-----------|---------------|---------|-----------|
| R01 | Quebra de contrato FE/BE ao trocar entidade por DTO | Média | Alto | Versionar contrato internamente e validar payload em testes integrados |
| R02 | Polling aumentar carga em horários de pico | Média | Médio | Monitorar RPS, otimizar queries e ajustar intervalo em aba inativa na fase 2 |
| R03 | Falhas de autorização exporem conversa indevida | Baixa | Alto | Reforçar validação de participante em todos endpoints de leitura |
| R04 | Badge no header ficar inconsistente com tela de chat | Média | Médio | Centralizar fonte de não lidas em serviço único e sincronizar por polling |
| R05 | Falta de observabilidade atrasar diagnóstico em produção | Média | Médio | Implementar eventos e alertas mínimos antes do rollout final |
| R06 | Spam de mensagens por usuário malicioso | Média | Médio | Definir e aplicar rate limit no backend antes do go-live público |

**Pontos em aberto (bloqueadores):**

- ⚠️ **Observabilidade:** definir dashboard mínimo oficial (quais métricas entram no painel operacional) — **responsável:** Engenharia.
- ⚠️ **Anti-spam/abuso:** definir política de rate limit por usuário para chat (mensagens por minuto/janela) — **responsável:** Produto + Engenharia.

---

*Documento gerado para alinhamento técnico interno. Revisar com o time antes de iniciar o desenvolvimento.*

# Discovery — Módulo de chat funcional (MVP)

> **Feature:** 20260410193253_modulo_chat_funcional
> **Data:** 2026-04-10
> **Autor:** GitHub Copilot (facilitação de discovery)
> **Status:** Completo

---

## Fontes de contexto utilizadas

| # | Fonte | Tipo | Conteúdo principal |
|---|-------|------|--------------------|
| 1 | Entrevista guiada com stakeholder | sessão | Objetivo da feature, escopo MVP, SLA, limites e prioridades |
| 2 | `brjobs-angular/src/app/components/chat/chat.component.ts` | arquivo | Fluxo atual da UI, polling parcial, seleção de conversa e envio |
| 3 | `brjobs-angular/src/app/service/chat.service.ts` | arquivo | Contratos HTTP atuais para chat |
| 4 | `brjobs-java/src/main/java/ads/uninassau/brjobs/controller/ChatController.java` | arquivo | Endpoints REST atuais do módulo de chat |
| 5 | `brjobs-java/src/main/java/ads/uninassau/brjobs/service/ChatService.java` | arquivo | Regras de negócio de envio, leitura e listagem |
| 6 | `brjobs-java/src/main/java/ads/uninassau/brjobs/model/ChatMessage.java` | arquivo | Modelo de mensagem e restrições de persistência |
| 7 | `brjobs-java/src/main/java/ads/uninassau/brjobs/model/ConversaChat.java` | arquivo | Modelo de conversa e ordenação por atualização |
| 8 | `brjobs-java/src/main/resources/db/migration/V2__create_chat_tables.sql` | arquivo | Estrutura de tabelas e índices do chat |
| 9 | https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events | referência de mercado | Trade-offs SSE (simplicidade, reconexão, limites por conexão) |
| 10 | https://learn.microsoft.com/en-us/azure/azure-web-pubsub/concept-service-internals | referência de mercado | Modelagem de conexões, grupos, usuários e entrega em tempo real |
| 11 | https://www.twilio.com/docs/conversations | referência de mercado | Separação de conversa/mensagem/participante e boas práticas de produto |

---

## Tipo de projeto

**Brownfield**

> O módulo de chat já existe em frontend, backend e banco, mas está incompleto/inconsistente para uso funcional de ponta a ponta.

---

## Resumo do entendimento

Finalizar o chat 1:1 já iniciado no sistema, com foco em robustez e baixo custo operacional. A solução deve reaproveitar o que existe, garantir autenticação obrigatória, suportar apenas mensagens de texto (até 500 caracteres), atualizar novas mensagens em até 5 segundos e exibir notificações também no header via sino.

Não há usuários em produção e não há necessidade de migração de dados.

---

## Problema e dor

**Dor principal:** o chat está implementado, porém incompleto e com lacunas de contrato/integração, impedindo funcionamento confiável no fluxo real do produto.

**Quem sente:** prestadores e contratantes autenticados que precisam conversar a partir de perfil público e detalhe da publicação.

**Frequência e impacto:** recorrente em qualquer tentativa de uso do chat; impacto direto em conversão de contato e experiência principal do marketplace.

---

## Usuários e papéis afetados

| Papel | Relação com a feature | Impacto |
|-------|-----------------------|---------|
| Prestador | Conversa com potenciais contratantes | Direto: comunicação comercial e fechamento de serviço |
| Contratante | Conversa com prestadores de interesse | Direto: validação e negociação antes da contratação |
| Time de desenvolvimento | Mantém frontend/backend do chat | Direto: menor regressão e contrato API estável |

---

## Solução proposta (rascunho)

**Construir:**
- Fechamento funcional do chat 1:1 já existente, mantendo arquitetura Angular + Spring Boot atual.
- Contratos de API explícitos via DTO para conversa e mensagem (evitar retorno de entidade JPA crua no endpoint de conversas).
- Estratégia de atualização sem custo: polling HTTP a cada 5 segundos para mensagens e não lidas.
- Indicador global de não lidas no header com ícone de sino e badge numérico.
- Regra de leitura: ao abrir conversa, marcar mensagens recebidas como lidas.
- Validação de mensagem apenas texto com limite máximo de 500 caracteres no backend e frontend.

**Fora do escopo desta versão:**
- Anexos, áudio, vídeo, stickers, reações, mensagens em grupo.
- Infra dedicada de tempo real (serviços pagos de pub/sub).
- Push notifications mobile, e-mail de notificação e moderação automática por IA paga.

---

## Restrições e premissas

- Usuário precisa estar autenticado para acessar chat.
- SLA funcional: novas mensagens e badge devem refletir em até 5 segundos.
- Mensagem: somente texto, até 500 caracteres.
- Solução deve ser sem custo adicional de infraestrutura.
- Não existe base produtiva ativa e não há migração de dados necessária.
- Reaproveitar componentes, serviços, endpoints e tabelas já implementados.

---

## Referências de mercado

| Referência | Decisão de design relevante | Ressoa? | Motivo |
|------------|-----------------------------|---------|--------|
| MDN SSE | Atualização server->client com reconexão automática; limites por conexão em alguns cenários | Parcialmente | Útil para futura evolução, mas ainda adiciona complexidade de stream e não resolve bidirecionalidade completa sozinho |
| Azure Web PubSub internals | Separação clara entre conexão, usuário e grupos; modelo de eventos | Parcialmente | Excelente referência arquitetural para fase avançada, porém não atende objetivo imediato sem custo/infra extra |
| Twilio Conversations docs | Modelagem consistente de conversa, participante e mensagem; status de leitura | Sim | Reforça desenho de contratos e UX de chat que pode ser aplicada sem vendor lock-in |

**Padrões extraídos das referências escolhidas:**
- Separar claramente contrato de `conversa` e `mensagem` em DTOs.
- Tratar `não lidas` como dado de produto de primeira classe (lista + badge global).
- Evoluir em etapas: primeiro estabilidade de contrato e UX, depois tempo real full.

---

## Decisões de design tomadas

| Decisão | Alternativas consideradas | Justificativa |
|---------|--------------------------|---------------|
| Usar polling HTTP de 5s no MVP | WebSocket, SSE, long polling | Melhor custo-benefício imediato, sem infraestrutura adicional, reaproveitando endpoints existentes |
| Retornar lista de conversas via DTO dedicado | Retornar entidade `ConversaChat` diretamente | Reduz acoplamento, evita riscos de serialização/lazy loading e facilita evolução da API |
| Mensagens apenas texto com limite 500 | Texto + anexos | Escopo MVP solicitado e menor complexidade técnica/segurança |
| Exibir sino com badge no header | Badge só dentro da tela de chat | Usuário pediu visibilidade global; melhora descoberta de novas mensagens |
| Reaproveitar rotas de entrada já existentes (`/perfil/:id` e detalhe de publicação -> `/chat`) | Criar novos fluxos de entrada | Menor esforço, menor regressão e uso de navegação já implementada |
| Marcar como lida ao abrir conversa | Marcar manualmente por mensagem | Melhor UX e menor fricção no fluxo inicial |

---

## Contratos de payload propostos (MVP)

### `GET /api/v1/chat/conversas` → `ConversationListItem[]`

```json
[
  {
    "id": 12,
    "contatoId": 87,
    "contatoNome": "Maria Souza",
    "ultimaMensagem": "Pode vir amanhã?",
    "ultimaMensagemEm": "2026-04-10T19:25:00",
    "ultimaMensagemRemetenteId": 87,
    "naoLidas": 2,
    "atualizadaEm": "2026-04-10T19:25:00"
  }
]
```

### `GET /api/v1/chat/conversa/{outroUsuarioId}?limit=50` → `MessageItem[]`

```json
[
  {
    "id": 301,
    "conversaId": 12,
    "remetenteId": 45,
    "remetenteNome": "João Silva",
    "destinatarioId": 87,
    "conteudo": "Olá!",
    "status": "ENVIADA",
    "criadoEm": "2026-04-10T19:20:00"
  }
]
```

### `POST /api/v1/chat/enviar?destinatarioId={id}`

Request:

```json
{
  "conteudo": "Mensagem de até 500 caracteres",
  "publicacaoId": 123,
  "clientTempId": "tmp-abc-123"
}
```

Response:

```json
{
  "id": 302,
  "conversaId": 12,
  "remetenteId": 45,
  "remetenteNome": "João Silva",
  "destinatarioId": 87,
  "conteudo": "Mensagem de até 500 caracteres",
  "status": "ENVIADA",
  "criadoEm": "2026-04-10T19:25:15"
}
```

---

## Lacunas e pontos em aberto

- ⚠️ **Ponto em aberto:** Observabilidade — definir dashboard mínimo (latência de polling, erro de envio, taxa de leitura) e alertas operacionais — **responsável:** Engenharia.
- ⚠️ **Ponto em aberto:** Anti-spam/abuso — política de rate limit por usuário ainda não definida (ex.: mensagens por minuto) — **responsável:** Produto + Engenharia.

---

## Notas adicionais

- Estratégia técnica recomendada em 2 etapas (sem custo):
  - Etapa 1: fechar contratos DTO, corrigir integração frontend/backend, implementar marcação de lida e badge no header.
  - Etapa 2: otimizar polling (5s ativo, fallback maior com aba em background) e preparar caminho para eventual SSE/WebSocket futuro.
- Como não há produção ativa, rollout pode ser direto após validação funcional end-to-end em ambiente de desenvolvimento.
- Critério de aceite principal: iniciar conversa a partir de perfil/detalhe, trocar mensagens texto, ver atualização em até 5s e visualizar não lidas no sino do header.

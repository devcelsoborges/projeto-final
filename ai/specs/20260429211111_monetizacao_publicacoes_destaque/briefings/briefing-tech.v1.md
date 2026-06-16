# Briefing Técnico — Monetização de publicações em destaque

> **Versão:** 1.1
> **Status:** Rascunho
> **Gerado em:** 2026-04-29
> **Baseado em:**
>   - [`./briefing-tech.v0.md`](./briefing-tech.v0.md) — Briefing técnico v0 (base sem Figma)
>   - [`../discovery.md`](../discovery.md) — discovery de 2026-04-29

---

## 1. Contexto e Problema

O BrJobs já opera como plataforma de publicações e perfis, mas ainda não possui um mecanismo de monetização para aumentar visibilidade de vagas. Hoje, todas as publicações competem no mesmo nível de exposição.

A dor é dupla:

- o autor da publicação não tem um mecanismo simples de promoção paga;
- a listagem pública não diferencia conteúdo promovido de conteúdo orgânico.

Sem isso, a plataforma perde uma fonte direta de receita e deixa de oferecer um recurso comum em marketplaces: pagar para destacar a vaga por tempo limitado.

---

## 2. Solução Proposta

Construir um módulo de monetização para destaque de publicações com checkout via Stripe, confirmação por webhook, expiração automática e ordenação inteligente na listagem.

Escopo desta versão:

- catálogo de planos de destaque com preço, duração e prioridade;
- checkout hospedado pela Stripe com sessão criada no backend;
- webhook como fonte de verdade para ativação do destaque;
- campos de destaque na publicação: `isHighlighted` e `highlightExpiresAt`;
- listagem ordenada por destaque ativo, prioridade do plano e recência;
- paginação obrigatória;
- cache com Redis no MVP;
- scheduler para expiração automática;
- logs estruturados e métricas básicas de operação.

Fora do escopo desta versão:

- outros gateways em produção;
- cobrança recorrente;
- campanhas com CPC, segmentação ou leilão de anúncios;
- painel financeiro avançado;
- automação de moderação do destaque.

Retrocompatibilidade:

- o produto não tem usuários reais em produção;
- não há necessidade de migração histórica;
- a feature pode ser implementada sem preservar contratos legados de monetização, mas deve respeitar o padrão atual de API e autenticação do projeto.

---

## 3. Personas e Papéis Afetados

| Papel | Ação que realiza | Impacto da feature |
|-------|------------------|--------------------|
| Autor da publicação | Seleciona plano, paga e ativa destaque | Direto — promove a vaga e ganha visibilidade |
| Visitante da listagem | Vê a ordenação priorizada | Direto — encontra vagas destacadas primeiro |
| Operação / engenharia | Monitora webhook, cache, expiração e falhas | Direto — garante estabilidade do fluxo |
| Backend engineer | Implementa entidades, integrações e regras | Direto — entrega o domínio monetizado |
| Frontend engineer | Exibe planos, checkout e estados de destaque | Direto — entrega a experiência de compra |
| Produto | Define preço, duração e prioridade dos planos | Direto — controla a estratégia de monetização |
| DBA / infra | Garante persistência, índices e cache | Indireto — suporte à performance e consistência |

---

## 4. Premissas, Restrições e Decisões Tomadas

- **Brownfield:** a solução precisa encaixar no BrJobs atual, que usa Angular, Spring Boot, PostgreSQL e JWT.
- **Gateway principal:** Stripe.
- **Estratégia de integração:** criar uma abstração de gateway para suportar outros meios no futuro.
- **Webhook é a fonte de verdade:** a publicação só entra em destaque após confirmação do evento assíncrono.
- **Redis no MVP:** a listagem destacada deve usar cache desde a primeira versão.
- **Sem migração histórica:** não existem usuários reais na base.
- **Ordenação obrigatória:** destaque ativo sempre vem antes da lista orgânica.
- **Scheduler curto:** remoção de destaque expirado deve ocorrer por varredura periódica curta, com frequência configurável por ambiente.
- **Prioridade de planos:** Básico < Plus < Premium.
- **Stripe keys:** serão fornecidas posteriormente como variáveis de ambiente de produção.

---

## 5. Arquitetura e Fluxos

### 5.1 Fluxo principal

```text
[Autor da publicação]
   -> abre a tela de destaque
   -> escolhe plano
   -> FE chama POST /highlight/checkout/{jobPostId}
   -> BE valida propriedade da publicação
   -> BE cria Payment + Stripe Checkout Session
   -> FE redireciona para Stripe
   -> Stripe confirma pagamento via webhook
   -> BE valida assinatura do webhook
   -> BE marca pagamento como aprovado
   -> BE ativa isHighlighted e define highlightExpiresAt
   -> cache da listagem é invalidado
   -> GET /jobs passa a exibir a publicação destacada
   -> scheduler expira o destaque ao final do período
```

### 5.2 Modelo de dados

| Campo | Entidade | Tipo | Obrigatório | Descrição |
|-------|----------|------|-------------|-----------|
| id | job_posts / publicacao existente | BIGINT | Sim | Identificador da publicação |
| title | job_posts | VARCHAR | Sim | Título da vaga |
| description | job_posts | TEXT | Sim | Descrição da vaga |
| is_highlighted | job_posts | BOOLEAN | Sim | Indica se a publicação está em destaque |
| highlight_expires_at | job_posts | TIMESTAMP | Não | Quando o destaque expira |
| highlight_plan_id | job_posts | BIGINT FK | Não | Plano de destaque vigente |
| id | highlight_plans | BIGINT | Sim | Identificador do plano |
| name | highlight_plans | VARCHAR | Sim | Nome do plano |
| price | highlight_plans | NUMERIC(10,2) | Sim | Preço do plano |
| duration_days | highlight_plans | INT | Sim | Duração do destaque em dias |
| priority | highlight_plans | INT | Sim | Ordem de priorização |
| id | payments | BIGINT | Sim | Identificador do pagamento |
| job_post_id | payments | BIGINT FK | Sim | Publicação associada |
| stripe_session_id | payments | VARCHAR | Sim | Sessão criada na Stripe |
| status | payments | VARCHAR | Sim | `PENDING`, `APPROVED`, `FAILED`, `EXPIRED` |
| amount | payments | NUMERIC(10,2) | Sim | Valor cobrado |
| created_at | payments | TIMESTAMP | Sim | Data de criação |
| updated_at | payments | TIMESTAMP | Não | Data de atualização |

### 5.3 Endpoints

| Método | Path | Auth | Payload resumido | Resposta |
|--------|------|------|------------------|----------|
| GET | `/api/highlight/plans` | Não | sem body | Lista de planos |
| POST | `/api/highlight/checkout/{jobPostId}` | Sim | `{ planId }` | `{ paymentId, stripeSessionId, checkoutUrl }` |
| POST | `/api/webhook/stripe` | Não | evento Stripe | `200 OK` / idempotente |
| GET | `/api/jobs?page=&size=` | Não | filtros/paginação | lista ordenada com destaque |

Integrações externas:

- Stripe Checkout Sessions;
- Stripe Webhooks;
- Redis para cache da listagem;
- Scheduler nativo do Spring para expiração.

---

## 6. UX e Comportamento da Interface

Não existe briefing UX específico para esta feature nesta etapa, então a implementação deve respeitar o padrão atual do BrJobs e os estados técnicos abaixo.

### 6.1 Estados da interface

```text
[Lista de planos]
-> mostra Básico, Plus e Premium com duração, preço e botão de compra

[Checkout iniciando]
-> botão desabilitado e feedback de carregamento

[Pagamento aprovado]
-> mostra confirmação e volta para a publicação com destaque ativo

[Pagamento pendente]
-> mantém estado de espera até o webhook confirmar

[Erro de pagamento]
-> mostra mensagem clara e permite nova tentativa

[Destaque expirado]
-> badge/estado visual removido e publicação volta à ordenação orgânica
```

### 6.2 Wireframe

```text
┌──────────────────────────────────────────────────────────┐
│ Plano de destaque                                       │
├──────────────────────────────────────────────────────────┤
│ Básico    | 5 dias  | R$ 10,00   | [Destacar]          │
│ Plus      | 11 dias | R$ 20,00   | [Destacar]          │
│ Premium   | 25 dias | R$ 40,00   | [Destacar]          │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│ Publicação destacada                                    │
│ [Badge: Destaque ativo]                                 │
│ Vence em: 25/05/2026                                    │
│ Prioridade: Premium                                     │
└──────────────────────────────────────────────────────────┘
```

Regras de interface:

- o usuário não deve conseguir ativar destaque sem pagar;
- o destaque aprovado deve aparecer no catálogo o mais rápido possível após o webhook;
- a listagem deve sempre mostrar o estado atual do destaque;
- erros de pagamento devem ser legíveis e acionáveis.

---

## 7. Regras de Negócio

1. Usuário DEVE ser o autor da publicação para comprar destaque nela.
2. O sistema NÃO DEVE ativar destaque antes da confirmação do webhook.
3. O sistema DEVE impedir pagamentos ativos duplicados para a mesma publicação.
4. SE o pagamento for aprovado ENTÃO a publicação DEVE receber `isHighlighted = true`.
5. SE o período expirar ENTÃO o sistema DEVE remover o destaque.
6. A listagem DEVE priorizar publicações destacadas ativas antes das orgânicas.
7. Entre publicações destacadas, a ordenação DEVE seguir `Premium > Plus > Básico`.
8. Empates de prioridade DEVE ser resolvidos por data de criação mais recente.
9. O sistema DEVE manter a página paginada sempre.
10. O webhook DEVE ser idempotente e não pode duplicar ativação nem pagamento.
11. O sistema PODE permitir reativação após expiração, criando novo pagamento.

### 7.1 Planos e preço

| Plano | Duração | Preço | Prioridade |
|-------|---------|-------|------------|
| Básico | 5 dias | R$ 10,00 | 1 |
| Plus | 11 dias | R$ 20,00 | 2 |
| Premium | 25 dias | R$ 40,00 | 3 |

---

## 8. Segurança e Privacidade

### 8.1 Controle de acesso

| Ação | Papel permitido | Papel bloqueado |
|------|-----------------|-----------------|
| Listar planos | Público | N/A |
| Iniciar checkout | Autor autenticado da publicação | Anônimo, terceiro |
| Receber webhook | Stripe / backend | Usuário final |
| Listar jobs com destaque | Público | N/A |
| Ver estado do pagamento da própria publicação | Autor autenticado | Terceiros |

### 8.2 Dados sensíveis

| Dado | Onde armazenar | Criptografia | Pode logar? |
|------|---------------|--------------|-------------|
| Stripe session id | PostgreSQL | Em trânsito e em repouso conforme infra | Sim, mascarado |
| Stripe webhook signature | Configuração de ambiente | Sim, segredo | Não |
| Dados da publicação | PostgreSQL | Conforme padrão do sistema | Sim, sem expor conteúdo sensível em logs |
| Dados de cartão | Stripe | Gerenciado pela Stripe | Não |

Regras adicionais:

- validar assinatura do webhook com `Stripe-Signature`;
- não registrar payload sensível completo do webhook em logs;
- validar ownership da publicação no backend;
- manter o segredo do webhook fora do repositório.

---

## 9. Tratamento de Erros e Resiliência

| Cenário | Causa | Comportamento esperado | Mensagem ao usuário |
|---------|-------|----------------------|---------------------|
| Usuário tenta destacar publicação de outro autor | ownership inválido | rejeitar checkout | Você não pode destacar esta publicação. |
| Plan inválido | `planId` inexistente | retornar 400 | Plano de destaque inválido. |
| Publicação já destacada com pagamento ativo | duplicidade | impedir novo pagamento ativo | Esta publicação já possui destaque ativo. |
| Stripe indisponível no checkout | falha de integração | não criar destaque, permitir retry | Não foi possível iniciar o pagamento. |
| Webhook com assinatura inválida | payload não confiável | rejeitar com 400 | Evento de pagamento inválido. |
| Webhook duplicado | retry da Stripe | processar uma única vez | Nenhuma ação adicional. |
| Falha ao persistir aprovação | erro de banco | retentar com idempotência | Não foi possível concluir o destaque agora. |
| Scheduler falha | job não executou | destaque expira no próximo ciclo | O destaque será atualizado em breve. |
| Cache do Redis indisponível | indisponibilidade temporária | fazer fallback para banco | A listagem está temporariamente mais lenta. |
| Token ausente/expirado | auth inválida | 401 | Sua sessão expirou. Faça login novamente. |

Resiliência adicional:

- o webhook deve suportar reprocessamento sem duplicar estado;
- o cache deve ser invalidado após aprovação e expiração;
- o scheduler deve ser tolerante a falhas temporárias e reexecutar no próximo ciclo.

---

## 10. Observabilidade

### 10.1 Eventos a logar

| Evento | Campos obrigatórios | Nível |
|--------|--------------------:|-------|
| highlight_checkout_started | user_id, job_post_id, plan_id, amount, timestamp | info |
| highlight_checkout_created | user_id, job_post_id, stripe_session_id, timestamp | info |
| highlight_webhook_received | event_id, event_type, stripe_session_id, timestamp | info |
| highlight_webhook_approved | payment_id, job_post_id, plan_id, amount, timestamp | info |
| highlight_webhook_duplicate | event_id, stripe_session_id, timestamp | warn |
| highlight_activation_failed | payment_id, job_post_id, error_code, timestamp | error |
| highlight_expiration_job_run | processed_count, expired_count, timestamp | info |
| highlight_cache_invalidated | cache_key, reason, timestamp | info |
| highlight_list_fetch | page, size, cache_hit, latency_ms, timestamp | info |

### 10.2 Métricas

| Métrica | Tipo | O que mede |
|---------|------|-----------|
| highlight_checkout_success_rate | taxa | percentagem de checkouts que chegam ao checkout Stripe |
| highlight_webhook_approved_rate | taxa | percentagem de webhooks aprovados |
| highlight_webhook_duplicate_rate | taxa | incidência de eventos repetidos |
| highlight_activation_latency_ms | histograma | tempo entre webhook e ativação do destaque |
| highlight_list_cache_hit_rate | taxa | eficiência do Redis na listagem |
| highlight_expired_count | contador | quantos destaques expiraram no ciclo |
| highlight_checkout_error_rate | taxa | falhas no checkout |

### 10.3 Alertas

| Condição | Threshold | Ação |
|----------|-----------|------|
| Falha de webhook | > 3% em 15 min | investigar Stripe, assinatura e persistência |
| Latência de ativação alta | p95 > 2s em 15 min | revisar persistência e invalidação de cache |
| Cache hit rate baixo | < 70% em 30 min | revisar estratégia de listagem |
| Scheduler não expira | 0 expirados por mais de 1 ciclo esperado | verificar job e relógio do sistema |
| Erro de checkout alto | > 5% em 15 min | revisar conta Stripe, configuração e rede |

---

## 11. Variáveis de Ambiente e Configuração

```env
# Monetização - backend
HIGHLIGHT_ENABLED=true
HIGHLIGHT_DEFAULT_CURRENCY=BRL
HIGHLIGHT_CHECKOUT_SUCCESS_URL=http://localhost:4200/publicacoes/{jobPostId}?highlight=success
HIGHLIGHT_CHECKOUT_CANCEL_URL=http://localhost:4200/publicacoes/{jobPostId}?highlight=cancel
HIGHLIGHT_WEBHOOK_SECRET=whsec_xxx
STRIPE_SECRET_KEY=sk_live_xxx
STRIPE_PUBLISHABLE_KEY=pk_live_xxx

# Planos
HIGHLIGHT_PLAN_BASIC_PRICE=10.00
HIGHLIGHT_PLAN_BASIC_DAYS=5
HIGHLIGHT_PLAN_PLUS_PRICE=20.00
HIGHLIGHT_PLAN_PLUS_DAYS=11
HIGHLIGHT_PLAN_PREMIUM_PRICE=40.00
HIGHLIGHT_PLAN_PREMIUM_DAYS=25

# Scheduler
HIGHLIGHT_EXPIRATION_CRON=0 */1 * * * *

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_CACHE_TTL_SECONDS=300
```

---

## 12. Estratégia de Rollout e Rollback

**Rollout:**

1. publicar backend com entidade/plano/pagamento e endpoints do fluxo;
2. habilitar webhook Stripe em homologação;
3. ativar Redis para listagem destacada;
4. liberar frontend com compra de destaque e badges de visibilidade;
5. monitorar latência de ativação e taxa de falhas por 7 dias.

**Rollback:**

- desabilitar feature por `HIGHLIGHT_ENABLED=false`;
- manter listagem orgânica funcionando mesmo sem destaque;
- preservar dados de pagamento já capturados;
- se necessário, desativar webhook no ambiente até corrigir a causa.

---

## 13. Fases de Entrega

### Fase 1 — Núcleo de domínio e pagamento
- [ ] Criar catálogo de planos.
- [ ] Criar entidade de pagamento e vínculo com publicação.
- [ ] Criar checkout Stripe com metadata.
- [ ] Validar ownership e duplicidade.

### Fase 2 — Ativação e listagem
- [ ] Implementar webhook idempotente.
- [ ] Ativar destaque ao aprovar pagamento.
- [ ] Ordenar listagem com prioridade do destaque.
- [ ] Adicionar paginação.

### Fase 3 — Expiração e performance
- [ ] Implementar scheduler de expiração.
- [ ] Adicionar Redis para cache da listagem.
- [ ] Invalidar cache após aprovação e expiração.
- [ ] Adicionar logs e métricas.

---

## 14. Fora do Escopo (desta versão)

- Outros gateways além da Stripe.
- Renovação automática de destaque.
- Compra por pacote ou assinatura.
- Ranqueamento pago por clique.
- Automação de campanhas publicitárias.
- Painel financeiro avançado.

---

## 15. Riscos e Pontos em Aberto

| # | Descrição | Probabilidade | Impacto | Mitigação |
|---|-----------|---------------|---------|-----------|
| R01 | Webhook mal configurado impedir ativação | Média | Alto | Validar assinatura, testar em homologação e logar eventId |
| R02 | Cache exibir listagem desatualizada | Média | Médio | Invalidação explícita após aprovação e expiração |
| R03 | Scheduler expirar destaque com atraso | Média | Médio | Rodar varredura periódica curta e manter `highlight_expires_at` como fonte de verdade |
| R04 | Pagamento duplicado para a mesma publicação | Baixa | Alto | Índice/validação de unicidade de pagamento ativo |
| R05 | Aumento de custo operacional por Redis e polling de listagem | Baixa | Médio | TTL curto e monitoramento de cache hit rate |
| R06 | Falha de integração Stripe em produção | Média | Alto | Conta de produção separada, segredo em env e fallback de erro claro |
| R07 | Ordenação quebrar regra de negócio | Baixa | Alto | Testes de integração para ordenação e paginação |

**Pontos em aberto (bloqueadores):**

- ⚠️ **Nenhum bloqueador técnico adicional além da entrega das chaves Stripe de produção quando o deploy for feito.**

**Pontos em aberto (não bloqueadores para o briefing):**

- A cadência exata do scheduler pode ser ajustada posteriormente, desde que continue sendo uma varredura periódica curta.

---

*Documento gerado para alinhamento técnico interno. Revisar com o time antes de iniciar o desenvolvimento.*

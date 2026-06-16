# Discovery — Monetização de publicações em destaque

> **Feature:** 20260429211111_monetizacao_publicacoes_destaque
> **Data:** 2026-04-29
> **Autor:** Codex
> **Status:** Completo

---

## Fontes de contexto utilizadas

| # | Fonte | Tipo | Conteúdo principal |
|---|-------|------|--------------------|
| 1 | `input-01.md` | arquivo | Requisitos funcionais e técnicos do módulo de monetização, destaque e pagamento |
| 2 | `CLAUDE.md` | arquivo | Stack do projeto, padrões de arquitetura, banco e convenções do BrJobs |
| 3 | Stripe Checkout Sessions API | documentação | Criação de sessão de checkout, redirecionamento e metadata |
| 4 | Stripe webhooks / idempotency | documentação | Verificação de assinatura, confirmação assíncrona e prevenção de duplicidade |
| 5 | eBay Promoted Listings | documentação | Modelo de destaque em marketplace com priorização de visibilidade |
| 6 | Redis cache-invalidation / cache-aside | documentação | Padrões de cache e invalidação para listagens com leitura frequente |

---

## Tipo de projeto

**Brownfield**

> O BrJobs já possui frontend Angular, backend Spring Boot, PostgreSQL, JWT e catálogo de publicações em desenvolvimento. A feature de monetização precisa se encaixar na modelagem e nas rotas existentes.

---

## Resumo do entendimento

O BrJobs vai permitir que o autor de uma publicação pague para destacá-la por um período limitado. O destaque deve aumentar a visibilidade da vaga na listagem, com prioridade por plano e ordenação de acordo com destaque ativo, nível do plano e recência.

A implementação será baseada em Stripe como gateway principal, com arquitetura preparada para outros gateways no futuro. O fluxo essencial é: selecionar plano, criar sessão de checkout, redirecionar para pagamento, receber webhook confirmado, ativar destaque, expirar automaticamente no fim do período e refletir a ordenação na listagem com paginação.

Como ainda não há usuários reais em produção, não existe necessidade de migração de dados ou compatibilidade retroativa. O time quer Redis já no MVP, além de logs estruturados e observabilidade básica.

---

## Problema e dor

**Dor principal:** publicações comuns têm a mesma visibilidade, o que reduz a capacidade de monetização e impede um modelo simples de promoção paga.

**Quem sente:** usuários que publicam vagas e querem mais alcance.

**Frequência e impacto:** sempre que uma publicação precisa de mais exposição; o impacto é direto em visibilidade, conversão e receita potencial da plataforma.

---

## Usuários e papéis afetados

| Papel | Relação com a feature | Impacto |
|-------|-----------------------|---------|
| Autor da publicação | Escolhe plano, paga e ativa destaque | Direto |
| Visitante da listagem | Vê publicações em ordem priorizada | Direto |
| Operação / engenharia | Monitora webhook, pagamento e expiração | Direto |
| Plataforma | Passa a ter uma linha de receita por destaque | Direto |

---

## Solução proposta (rascunho)

**Construir:**
- Catálogo de planos de destaque com duração, preço e prioridade.
- Checkout via Stripe com sessão criada no backend e confirmação via webhook.
- Persistência de pagamentos, status e vínculo com publicação.
- Sinalização de destaque com `isHighlighted` e `highlightExpiresAt`.
- Ordenação inteligente na listagem: destaque ativo primeiro, depois prioridade do plano e recência.
- Redis no MVP para cache da listagem.
- Scheduler para expirar destaque automaticamente com varredura periódica curta, em linha com a prática de marketplaces de atualizar destaque de forma assíncrona.
- Logs estruturados e eventos de falha/retentativa no webhook.

**Fora do escopo desta versão:**
- Outros gateways em produção.
- Assinatura recorrente.
- Campanhas avançadas com segmentação, anúncios ou CPC.
- Moderação automática de destaque.

---

## Restrições e premissas

- Backend em Spring Boot, banco PostgreSQL, autenticação JWT e arquitetura em camadas.
- Stripe é o gateway principal.
- A ativação do destaque só ocorre após confirmação do webhook.
- Deve haver prevenção de pagamento duplicado ativo para a mesma publicação.
- O webhook precisa ser idempotente.
- Redis entra no MVP.
- Não há base produtiva ativa, então não existe migração histórica.
- O efeito da confirmação deve ser o mais rápido possível no catálogo.

---

## Referências de mercado

| Referência | Decisão de design relevante | Ressoa? | Motivo |
|------------|-----------------------------|---------|--------|
| Stripe Checkout Sessions | Sessão criada no servidor e redirecionamento para checkout hospedado; metadata para reconciliar com IDs internos | Sim | Reduz código customizado e encaixa bem no fluxo “criar sessão -> redirecionar -> confirmar” |
| Stripe webhooks / idempotency | Verificação de assinatura com `Stripe-Signature`; uso de webhook para eventos assíncronos; requests idempotentes evitam duplicidade | Sim | Base correta para ativação segura do destaque e prevenção de retrabalho no pagamento |
| eBay Promoted Listings | Promos competem por visibilidade com estratégia/priority; listagem promove itens de forma explícita | Parcialmente | Confirma a lógica de destaque por prioridade, embora o modelo financeiro aqui seja mais simples |
| Redis cache invalidation | Cache deve ser invalidado quando o dado de origem muda | Sim | Essencial para manter listagem destacada consistente após webhook, expiração ou mudança de status |

**Padrões extraídos das referências escolhidas:**
- Sessão de pagamento deve ser criada no backend com vínculo ao ID interno da publicação.
- Webhook deve ser a fonte de verdade para ativação.
- Listagem com alto volume deve usar cache com invalidação explícita.
- A prioridade de destaque precisa ser tratada como parte do ordenamento de produto.

---

## Decisões de design tomadas

| Decisão | Alternativas consideradas | Justificativa |
|---------|--------------------------|---------------|
| Stripe Checkout hospedado no MVP | Checkout customizado, Payment Element direto | Menor risco, menos código e implementação mais rápida |
| Webhook como gatilho único de ativação | Ativação no frontend após redirect de sucesso | Evita fraude e garante fonte de verdade no backend |
| Arquitetura com provedor de gateway | Stripe-only rígido | Facilita adicionar outro gateway depois sem refatorar domínio |
| Redis já no MVP | Sem cache inicial | Requisito explícito do stakeholder |
| Scheduler periódico para expiração | Expiração apenas sob leitura | Garante limpeza de estado e evita destaque velho permanecer indevidamente |
| Ordenação por destaque ativo, prioridade e data | Ordenação só por data | Mantém a proposta de monetização visível e previsível |
| Pagamento vinculado à publicação do próprio usuário | Qualquer usuário pagar por qualquer vaga | Requisito de ownership e controle do conteúdo |

---

## Contratos de payload propostos (MVP)

### `GET /api/highlight/plans`

```json
[
  {
    "id": 1,
    "name": "Básico",
    "price": 10,
    "durationDays": 5,
    "priority": 1
  },
  {
    "id": 2,
    "name": "Plus",
    "price": 20,
    "durationDays": 11,
    "priority": 2
  },
  {
    "id": 3,
    "name": "Premium",
    "price": 40,
    "durationDays": 25,
    "priority": 3
  }
]
```

### `POST /api/highlight/checkout/{jobPostId}`

Request:

```json
{
  "planId": 2
}
```

Response:

```json
{
  "paymentId": 900,
  "stripeSessionId": "cs_test_123",
  "checkoutUrl": "https://checkout.stripe.com/...",
  "expiresAt": "2026-04-29T21:30:00"
}
```

### `POST /api/webhook/stripe`

Evento relevante no MVP:

```json
{
  "type": "checkout.session.completed",
  "data": {
    "object": {
      "id": "cs_test_123",
      "payment_status": "paid",
      "metadata": {
        "jobPostId": "123",
        "planId": "2"
      }
    }
  }
}
```

### `GET /api/jobs?page=0&size=12`

Resposta deve priorizar:
- publicações destacadas ativas;
- maior prioridade do plano;
- data de criação mais recente.

---

## Lacunas e pontos em aberto

- ⚠️ **Ponto em aberto:** a cadência exata do scheduler deve ser fechada no brief técnico; a direção já está definida como varredura periódica curta.

---

## Notas adicionais

- Como não há usuários reais, a feature pode entrar sem migração histórica.
- O MVP deve preferir consistência sobre sofisticação: webhook como fonte de verdade, cache com invalidação, e ordenação no backend.
- A implementação futura de outros gateways deve reutilizar a mesma abstração de domínio para pagamento e ativação.
- As chaves de Stripe serão fornecidas posteriormente e entram como dependência de ambiente, não como bloqueio de discovery.

# Briefing Técnico — [Nome da Feature]

> **Versão:** 0.1
> **Status:** Rascunho | Em revisão | Aprovado
> **Gerado em:** [YYYY-MM-DD]
> **Baseado em:**
>   - [`../briefings/briefing-ux.vN.md`](../briefings/briefing-ux.vN.md) — Briefing UX/UI vN ([YYYY-MM-DD])
>   - [`../discovery.md`](../discovery.md) — discovery de [YYYY-MM-DD]

<!-- Remover a linha do briefing-ux se não houver Briefing UX/UI para esta feature -->

---

## 1. Contexto e Problema

<!-- Por que isso existe?
     Qual a dor real? Quem sente? Com que frequência? Qual o impacto mensurável?
     O que acontece se não resolvermos? -->

---

## 2. Solução Proposta

<!-- O que vamos construir — em uma frase clara.
     Seguido do que NÃO vamos construir nesta versão.
     Retrocompatibilidade: o sistema atual continua funcionando? Como? -->

---

## 3. Personas e Papéis Afetados

<!-- Quem usa, quem configura, quem é impactado indiretamente.
     Inclua papéis técnicos e de negócio.
     Se houver briefing-ux.vN.md, derivar dali e complementar com papéis técnicos. -->

| Papel | Ação que realiza | Impacto da feature |
|-------|------------------|--------------------|
| [ex: admin] | [configura templates] | [direto — controla a feature] |
| [ex: usuário] | [gera documentos] | [direto — usa o resultado] |

---

## 4. Premissas, Restrições e Decisões Tomadas

<!-- O que já está decidido e não está em discussão.
     Restrições técnicas (ex: não podemos quebrar API pública).
     Restrições legais (ex: LGPD, compliance).
     Restrições de negócio (ex: sem custo adicional de infra nesta fase). -->

- **[Premissa/Restrição]:** [descrição e justificativa]

---

## 5. Arquitetura e Fluxos

<!-- Diagramas ASCII de fluxo de dados (use → para setas).
     Modelo de dados: quais campos, em qual entidade, qual tipo, nullable?.
     Endpoints: método, path, payload resumido, resposta esperada.
     Integrações externas: quais APIs, SDKs, serviços de terceiros. -->

### 5.1 Fluxo principal

```
[Ator] → [Ação] → [Sistema] → [Resultado]
```

### 5.2 Modelo de dados

| Campo | Entidade | Tipo | Obrigatório | Descrição |
|-------|----------|------|-------------|-----------|

### 5.3 Endpoints

| Método | Path | Auth | Payload resumido | Resposta |
|--------|------|------|------------------|----------|

---

## 6. UX e Comportamento da Interface

<!-- Se há briefing-ux.vN.md: resumir os fluxos principais e referenciar o arquivo.
     Adicionar wireframes ASCII apenas para comportamentos técnicos não cobertos no UX
     (estados de erro técnico, fluxos de autenticação, retry, etc.).
     Se não há briefing-ux: incluir wireframes ASCII completos para os fluxos principais.
     Validações visíveis ao usuário (mensagens de erro, tooltips).
     Referências visuais: "seguindo o padrão de X (Slack, Linear, etc.)". -->

<!-- Se briefing-ux existir, adicionar referência: -->
<!-- Ver detalhamento completo de telas e fluxos em: ../briefings/briefing-ux.vN.md -->

### 6.1 Estados da interface

```
[Estado vazio]        → [o que o usuário vê]
[Estado carregando]   → [o que o usuário vê]
[Estado sucesso]      → [o que o usuário vê]
[Estado erro]         → [o que o usuário vê]
```

### 6.2 Wireframe

```
┌──────────────────────────────────────────┐
│  [componente]                            │
│                                          │
└──────────────────────────────────────────┘
```

---

## 7. Regras de Negócio

<!-- Lista numerada. Linguagem prescritiva:
     DEVE     — obrigatório
     NÃO DEVE — proibido
     PODE     — opcional/permitido
     SE...ENTÃO — condicional -->

1. [Papel] DEVE [fazer X] para [contexto].
2. O sistema NÃO DEVE [fazer Y] quando [condição].
3. SE [condição] ENTÃO [comportamento].

---

## 8. Segurança e Privacidade

<!-- RBAC: quem pode fazer o quê — seja explícito por papel.
     Dados sensíveis: o que não pode ser logado, o que precisa criptografar.
     Auditoria: quais ações precisam de log rastreável.
     Validação de entrada: o que o servidor valida independente do frontend. -->

### 8.1 Controle de acesso

| Ação | Papel permitido | Papel bloqueado |
|------|-----------------|-----------------|

### 8.2 Dados sensíveis

| Dado | Onde armazenar | Criptografia | Pode logar? |
|------|---------------|--------------|-------------|

---

## 9. Tratamento de Erros e Resiliência

<!-- Tabela com os cenários de erro esperados.
     Distinguir: erro do usuário vs. erro do sistema vs. erro de terceiro.
     Para integrações externas: o que fazer se a API estiver indisponível? -->

| Cenário | Causa | Comportamento esperado | Mensagem ao usuário |
|---------|-------|----------------------|---------------------|

---

## 10. Observabilidade

<!-- Quais eventos estruturados logar (campos obrigatórios em cada log).
     Quais métricas expor (contadores, histogramas, gauges).
     Quais alertas configurar e com qual threshold.
     Dashboard: o que precisa ser visível para o time de operação? -->

### 10.1 Eventos a logar

| Evento | Campos obrigatórios | Nível |
|--------|--------------------:|-------|
| [ex: google_connect_success] | [user_id, timestamp, scope] | info |

### 10.2 Métricas

| Métrica | Tipo | O que mede |
|---------|------|-----------|

### 10.3 Alertas

| Condição | Threshold | Ação |
|----------|-----------|------|

---

## 11. Variáveis de Ambiente e Configuração

<!-- Bloco .env comentado com todas as variáveis necessárias.
     Indicar: obrigatória (✱) vs opcional, backend (BE) vs frontend (FE).
     Nunca incluir valores reais — apenas exemplos/placeholders. -->

```env
# [Serviço] — obrigatórias
✱ VARIAVEL_1=exemplo          # BE — descrição do que faz
✱ VARIAVEL_2=exemplo          # BE — descrição do que faz

# [Serviço] — frontend (públicas)
✱ NEXT_PUBLIC_VARIAVEL=exemplo  # FE — descrição

# Criptografia / segurança
✱ ENCRYPTION_KEY=base64-32-bytes  # BE — chave AES-256 para tokens
```

---

## 12. Estratégia de Rollout e Rollback

<!-- Feature flag? Se sim, qual o mecanismo (env var, banco, serviço)?
     Rollout gradual: por % de usuários, por tenant, por plano?
     Como desativar em produção sem rollback de banco?
     Rollback: se der errado, como reverter? Quais dados são afetados? -->

**Rollout:** [descrição da estratégia]

**Rollback:** [como desfazer — feature flag off, migration down, etc.]

---

## 13. Fases de Entrega

<!-- Divisão incremental onde cada fase entrega valor observável.
     Cada fase deve ser deployável independentemente. -->

### Fase 1 — [Nome]
- [ ] [Entregável 1]
- [ ] [Entregável 2]

### Fase 2 — [Nome]
- [ ] [Entregável 1]

---

## 14. Fora do Escopo (desta versão)

<!-- Itens explicitamente excluídos — o que NÃO faremos agora.
     Inclui itens que foram considerados e descartados. -->

- [Item 1] — [motivo ou "previsto para versão futura"]
- [Item 2]

---

## 15. Riscos e Pontos em Aberto

<!-- Tabela de riscos com probabilidade, impacto e mitigação.
     ⚠️ Pontos em aberto: o que precisa ser definido antes de iniciar. -->

| # | Descrição | Probabilidade | Impacto | Mitigação |
|---|-----------|---------------|---------|-----------|
| R01 | [risco] | Alta / Média / Baixa | Alto / Médio / Baixo | [ação de mitigação] |

**Pontos em aberto (bloqueadores):**
- ⚠️ [Dimensão]: [o que falta definir] — **responsável:** [nome/papel]

---

*Documento gerado para alinhamento técnico interno. Revisar com o time antes de iniciar o desenvolvimento.*

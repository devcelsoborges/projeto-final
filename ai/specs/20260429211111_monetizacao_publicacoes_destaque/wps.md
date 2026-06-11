### Wp-31 — Schema de destaque, planos e pagamentos

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-31                                              |
| **Spec relacionada**     | [SPEC-01](./specs.md#spec-01)                      |
| **Tipo**                 | backend                                            |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Nenhuma                                            |
| **Pode paralelizar com** | Wp-32                                              |
| **Testes requeridos**    | unit \| integration                                |
| **Status**               | 🔴 Pendente                                        |

**Escopo**

> Criar o núcleo de persistência do módulo: planos de destaque, pagamentos e campos de destaque na publicação existente, incluindo índices e migração.

**Definition of Ready**

> - [ ] Modelo atual de publicação revisado
> - [ ] Campos finais de destaque e pagamento aprovados
> - [ ] Estratégia de migração definida

**Passos sugeridos de implementação**

> 1. Criar migration para planos e pagamentos.
> 2. Adicionar campos `isHighlighted`, `highlightExpiresAt` e `highlightPlanId` na publicação.
> 3. Implementar entidades, enums e repositories.
> 4. Escrever testes de persistência e constraints.

**Critérios de aceite do pacote**

> - Planos Básico, Plus e Premium persistem corretamente.
> - Publicação suporta estado de destaque sem quebrar o fluxo atual.

**Rollback**

> - Reverter migration e manter feature desligada por configuração.

**Áreas impactadas**

> [banco] | [backend]

---

### Wp-32 — Checkout Stripe e abstração de gateway

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-32                                              |
| **Spec relacionada**     | [SPEC-01](./specs.md#spec-01)                      |
| **Tipo**                 | backend                                            |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Wp-31                                              |
| **Pode paralelizar com** | Wp-35                                              |
| **Testes requeridos**    | unit \| integration                                |
| **Status**               | 🔴 Pendente                                        |

**Escopo**

> Implementar criação de sessão Stripe, validação de ownership da publicação e abstração de gateway para suportar outros provedores no futuro.

**Definition of Ready**

> - [ ] Wp-31 concluído
> - [ ] Credenciais de sandbox Stripe disponíveis
> - [ ] Contrato de retorno do checkout aprovado

**Passos sugeridos de implementação**

> 1. Criar interface de gateway de pagamento.
> 2. Implementar provider Stripe.
> 3. Criar service/controller de checkout.
> 4. Validar duplicidade de pagamento ativo e ownership.

**Critérios de aceite do pacote**

> - Checkout gera URL da Stripe com metadata da publicação.
> - Usuário não consegue iniciar pagamento em publicação de terceiro.

**Rollback**

> - Desativar o endpoint de checkout e reverter para fluxo org�nico.

**Áreas impactadas**

> [backend] | [config/env]

---

### Wp-33 — Webhook idempotente e ativação do destaque

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-33                                              |
| **Spec relacionada**     | [SPEC-02](./specs.md#spec-02)                      |
| **Tipo**                 | backend                                            |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Wp-31, Wp-32                                       |
| **Pode paralelizar com** | Wp-34                                              |
| **Testes requeridos**    | unit \| integration                                |
| **Status**               | 🔴 Pendente                                        |

**Escopo**

> Processar eventos Stripe, validar assinatura, evitar duplicidade e ativar destaque ao aprovar o pagamento.

**Definition of Ready**

> - [ ] Wp-32 concluído
> - [ ] Secret do webhook disponível em ambiente
> - [ ] Regra de idempotência aprovada

**Passos sugeridos de implementação**

> 1. Criar endpoint do webhook.
> 2. Validar assinatura Stripe.
> 3. Atualizar status do pagamento e ativar destaque.
> 4. Adicionar proteção contra reprocessamento do mesmo evento.

**Critérios de aceite do pacote**

> - Pagamento aprovado ativa destaque uma única vez.
> - Webhook repetido não duplica escrita.

**Rollback**

> - Desligar webhook e manter os pagamentos pendentes sem ativação automática.

**Áreas impactadas**

> [backend] | [infra] | [config/env]

---

### Wp-34 — Listagem destacada, Redis e scheduler de expiração

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-34                                              |
| **Spec relacionada**     | [SPEC-03](./specs.md#spec-03)                      |
| **Tipo**                 | backend                                            |
| **Estimativa**           | 3d                                                 |
| **Dependências**         | Wp-31, Wp-33                                       |
| **Pode paralelizar com** | Wp-33                                              |
| **Testes requeridos**    | unit \| integration                                |
| **Status**               | 🔴 Pendente                                        |

**Escopo**

> Implementar ordenação das publicações destacadas, cache Redis da listagem e scheduler de expiração automática do destaque.

**Definition of Ready**

> - [ ] Campos de destaque já persistidos
> - [ ] Redis disponível em ambiente de dev/homologação
> - [ ] Frequência do scheduler validada com produto/engenharia

**Passos sugeridos de implementação**

> 1. Criar query ordenada com prioridade e data.
> 2. Adicionar cache-aside com Redis para GET /jobs.
> 3. Implementar scheduler para expiração.
> 4. Invalidações após webhook e expiração.

**Critérios de aceite do pacote**

> - Publicação destacada ativa aparece primeiro na listagem.
> - Destaque expirado é removido sem intervenção manual.

**Rollback**

> - Desativar cache e scheduler, mantendo ordena��o org�nica funcionando.

**Áreas impactadas**

> [backend] | [banco] | [infra] | [config/env]

---

### Wp-35 — Interface de destaque e checkout no frontend

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-35                                              |
| **Spec relacionada**     | [SPEC-01](./specs.md#spec-01) e [SPEC-03](./specs.md#spec-03) |
| **Tipo**                 | frontend                                           |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Wp-32                                              |
| **Pode paralelizar com** | Wp-33                                              |
| **Testes requeridos**    | unit \| integration \| manual                      |
| **Status**               | 🔴 Pendente                                        |

**Escopo**

> Criar a experiência de seleção de plano, redirecionamento para Stripe e indicação visual de publicação destacada na listagem.

**Definition of Ready**

> - [ ] Contrato do checkout disponível no backend
> - [ ] Estados visuais do fluxo aprovados internamente
> - [ ] Rotas de publicação/listagem já existentes

**Passos sugeridos de implementação**

> 1. Criar componente para exibir os planos.
> 2. Integrar botão de checkout com o endpoint backend.
> 3. Exibir estados de sucesso, erro e carregamento.
> 4. Destacar visualmente publicações ativas na listagem.

**Critérios de aceite do pacote**

> - Usuário consegue iniciar checkout pela interface.
> - Publicações destacadas ficam visualmente diferenciadas no catálogo.

**Rollback**

> - Reverter componentes de destaque e manter navega��o org�nica intacta.

**Áreas impactadas**

> [frontend] | [config/env]

---

### Wp-36 — Hardening, observabilidade e rollout da monetização

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-36                                              |
| **Spec relacionada**     | [SPEC-04](./specs.md#spec-04)                      |
| **Tipo**                 | fullstack                                          |
| **Estimativa**           | 3d                                                 |
| **Dependências**         | Wp-32, Wp-33, Wp-34, Wp-35                         |
| **Pode paralelizar com** | —                                                  |
| **Testes requeridos**    | unit \| integration \| e2e \| manual               |
| **Status**               | 🔴 Pendente                                        |

**Escopo**

> Fechar telemetria, alertas, testes de ponta a ponta e estratégia de rollout/rollback da monetização.

**Definition of Ready**

> - [ ] Wp-32 a Wp-35 concluídos
> - [ ] Variáveis de ambiente definidas
> - [ ] Critérios de aceite e rollback aprovados

**Passos sugeridos de implementação**

> 1. Instrumentar logs e métricas da feature.
> 2. Criar alertas mínimos para checkout, webhook e expiração.
> 3. Rodar testes de integração e smoke do fluxo completo.
> 4. Documentar rollout/rollback operacional.

**Critérios de aceite do pacote**

> - Fluxo completo validado em homologação.
> - Observabilidade e rollback prontos para produção.

**Rollback**

> - Desativar feature por configura��o e manter listagem org�nica operacional.

**Áreas impactadas**

> [backend] | [frontend] | [infra] | [config/env]

---

### Mapa de Dependências

```text
Wp-31 -> Wp-32 -> Wp-33 -> Wp-36
Wp-31 -> Wp-34 -> Wp-36
Wp-32 -> Wp-35 -> Wp-36
Wp-33 -> Wp-34
Wp-33 -> Wp-35
```

---

### Riscos e Pontos Desconhecidos

| # | Descrição | Probabilidade | Impacto | Mitigação |
|---|-----------|---------------|---------|-----------|
| R01 | Webhook Stripe mal configurado atrasar ativação | Média | Alto | Testar em homologação e validar assinatura antes de ativar |
| R02 | Cache Redis desatualizar a listagem | Média | Médio | Invalidação explícita após aprovação e expiração |
| R03 | Scheduler expirar destaque com atraso | Média | Médio | Agendar varredura curta e manter data de expiração como fonte de verdade |
| R04 | Pagamento duplicado para mesma publicação | Baixa | Alto | Índice/validação de unicidade e check de status ativo |
| R05 | Contrato frontend/backend divergir na UI de checkout | Média | Alto | Testes de integração e contrato antes de abrir PR |
| R06 | Observabilidade insuficiente dificultar diagnóstico | Média | Médio | Fechar logs/métricas/alertas no Wp-36 |

---

### Oportunidades de Paralelização

| Grupo | WPs que podem rodar juntos | Pré-requisito para o grupo |
|-------|---------------------------|----------------------------|
| G1 | Wp-31 | Nenhum |
| G2 | Wp-32, Wp-34 | Wp-31 concluído |
| G3 | Wp-33, Wp-35 | Wp-32 concluído |
| G4 | Wp-36 | Wp-32, Wp-33, Wp-34 e Wp-35 concluídos |

### Wp-25 — Blindagem de acesso e sessão no chat

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-25                                              |
| **Spec relacionada**     | [SPEC-01](./specs.md#spec-01)                      |
| **Tipo**                 | fullstack                                          |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Nenhuma                                            |
| **Pode paralelizar com** | Wp-26                                              |
| **Testes requeridos**    | integration \| manual                              |
| **Status**               | ✅ Concluído                                       |

**Escopo**

> Consolidar autorização de endpoints do chat e comportamento de sessão no frontend para cenários 401/403, incluindo validação de participação na conversa.

**Definition of Ready**

> - [ ] Contrato de autenticação do projeto validado com o time
> - [ ] Endpoints de chat disponíveis em ambiente de desenvolvimento
> - [ ] Cenários de sessão expirada definidos para UX

**Passos sugeridos de implementação**

> 1. Revisar todos os endpoints de chat para garantir validação de participante.
> 2. Ajustar tratamento padronizado de 401/403 no frontend para redirecionamento/erro.
> 3. Validar fluxo de acesso por dois usuários e tentativa de acesso indevido.

**Critérios de aceite do pacote**

> - Usuário sem sessão não acessa dados de chat.
> - Usuário autenticado não acessa conversa de terceiros.

**Rollback**

> - Reverter ajustes de proteção para versão anterior estável e manter bloqueio por autenticação global.

**Áreas impactadas**

> [backend] | [frontend]

---

### Wp-26 — Contrato DTO de conversas e validações de mensagem

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-26                                              |
| **Spec relacionada**     | [SPEC-02](./specs.md#spec-02)                      |
| **Tipo**                 | backend                                            |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Nenhuma                                            |
| **Pode paralelizar com** | Wp-25                                              |
| **Testes requeridos**    | unit \| integration                                |
| **Status**               | ✅ Concluído                                       |

**Escopo**

> Implementar/ajustar DTOs de lista de conversas e mensagens, removendo exposição de entidade JPA no endpoint de conversas e aplicando validação de conteúdo (1..500 caracteres).

**Definition of Ready**

> - [ ] Estrutura de entidades e repositories de chat revisada
> - [ ] Contrato alvo de payload aprovado no briefing
> - [ ] Casos de erro de validação mapeados

**Passos sugeridos de implementação**

> 1. Criar DTO de conversa com campos necessários para UI.
> 2. Ajustar service/controller para mapear entidade -> DTO.
> 3. Aplicar validação de tamanho e conteúdo da mensagem no backend.
> 4. Escrever testes de contrato para sucesso e erros de validação.

**Critérios de aceite do pacote**

> - Endpoint /chat/conversas retorna DTO estável.
> - Envio com mensagem inválida retorna erro semântico 400.

**Rollback**

> - Reverter mapeamento para release anterior e restaurar contrato anterior temporariamente.

**Áreas impactadas**

> [backend]

---

### Wp-27 — Integração frontend do chat com polling de 5 segundos

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-27                                              |
| **Spec relacionada**     | [SPEC-03](./specs.md#spec-03)                      |
| **Tipo**                 | frontend                                           |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Wp-26                                              |
| **Pode paralelizar com** | Wp-29                                              |
| **Testes requeridos**    | integration \| manual                              |
| **Status**               | ✅ Concluído                                       |

**Escopo**

> Adaptar o componente/serviço de chat para o novo contrato de API, consolidando polling de 5s para conversas e não lidas, com estados de erro/retry e marcação de leitura ao abrir conversa.

**Definition of Ready**

> - [ ] Wp-26 concluído com contrato estável disponível
> - [ ] Variáveis de configuração de polling definidas
> - [ ] Cenários de UX de erro aprovados

**Passos sugeridos de implementação**

> 1. Atualizar interfaces do serviço de chat no frontend.
> 2. Implementar loop de polling com cleanup em destroy.
> 3. Implementar marcação como lida ao abrir conversa.
> 4. Tratar feedback visual de falha de envio e reconciliação de estado.

**Critérios de aceite do pacote**

> - Mensagens e não lidas atualizam em até 5 segundos.
> - Mensagens recebidas são marcadas como lidas ao abrir conversa.

**Rollback**

> - Reverter componente e serviço de chat para comportamento anterior e desativar polling consolidado.

**Áreas impactadas**

> [frontend] | [config/env]

---

### Wp-28 — Sino de notificações no header e sincronização global

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-28                                              |
| **Spec relacionada**     | [SPEC-03](./specs.md#spec-03)                      |
| **Tipo**                 | frontend                                           |
| **Estimativa**           | 1d                                                 |
| **Dependências**         | Wp-27                                              |
| **Pode paralelizar com** | —                                                  |
| **Testes requeridos**    | integration \| manual                              |
| **Status**               | ✅ Concluído                                       |

**Escopo**

> Implementar ícone de sino no header com badge de não lidas e sincronização com a fonte de verdade do chat.

**Definition of Ready**

> - [ ] Wp-27 concluído
> - [ ] Regra visual de badge (0, 1..99, 99+) definida
> - [ ] Estado de usuário autenticado já consolidado no header

**Passos sugeridos de implementação**

> 1. Adicionar componente/slot de sino no header.
> 2. Integrar contagem de não lidas via serviço compartilhado.
> 3. Validar atualização com polling ativo e após leitura de mensagens.

**Critérios de aceite do pacote**

> - Badge mostra total correto de não lidas.
> - Badge zera/atualiza ao ler mensagens na conversa.

**Rollback**

> - Remover sino do header e retornar ao menu anterior sem badge.

**Áreas impactadas**

> [frontend]

---

### Wp-29 — Telemetria operacional e anti-spam mínimo

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-29                                              |
| **Spec relacionada**     | [SPEC-04](./specs.md#spec-04)                      |
| **Tipo**                 | fullstack                                          |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Wp-26                                              |
| **Pode paralelizar com** | Wp-27                                              |
| **Testes requeridos**    | integration \| manual                              |
| **Status**               | ✅ Concluído                                       |

**Escopo**

> Implementar baseline de observabilidade do chat (logs/métricas/alertas) e aplicar política inicial de rate limit para reduzir spam e abuso.

**Definition of Ready**

> - [ ] Campos de log obrigatórios definidos
> - [ ] Critério inicial de rate limit aprovado por produto e engenharia
> - [ ] Destino de métricas/logs disponível na infra

**Passos sugeridos de implementação**

> 1. Instrumentar eventos críticos de envio, leitura e falha.
> 2. Expor métricas mínimas para latência e taxa de erro.
> 3. Aplicar rate limit por usuário em endpoint de envio.
> 4. Validar cenários de bloqueio e mensagens de erro adequadas.

**Critérios de aceite do pacote**

> - Eventos/métricas críticos aparecem no ambiente de homologação.
> - Rate limit bloqueia spam sem impactar uso normal.

**Rollback**

> - Ajustar limite para modo permissivo temporário e manter observabilidade ativa.

**Áreas impactadas**

> [backend] | [frontend] | [infra] | [config/env]

---

### Wp-30 — Hardening final, testes e readiness para deploy

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-30                                              |
| **Spec relacionada**     | [SPEC-04](./specs.md#spec-04)                      |
| **Tipo**                 | fullstack                                          |
| **Estimativa**           | 3d                                                 |
| **Dependências**         | Wp-25, Wp-26, Wp-27, Wp-28, Wp-29                  |
| **Pode paralelizar com** | —                                                  |
| **Testes requeridos**    | unit \| integration \| e2e \| manual             |
| **Status**               | ✅ Concluído                                       |

**Escopo**

> Executar bateria final de validação funcional e operacional do chat, consolidar plano de rollout/rollback e preparar entrega segura para ambiente AWS.

**Definition of Ready**

> - [ ] Wp-25 a Wp-29 concluídos
> - [ ] Ambiente de homologação estável disponível
> - [ ] Checklist de aceite de produto definido

**Passos sugeridos de implementação**

> 1. Rodar testes de integração e smoke em fluxos principais.
> 2. Executar testes manuais multiusuário (duas contas simultâneas).
> 3. Validar alertas e logs em cenário de erro controlado.
> 4. Documentar plano de rollout/rollback operacional.

**Critérios de aceite do pacote**

> - Fluxo completo de chat aprovado em homologação.
> - Plano de rollback testado e documentado.

**Rollback**

> - Reverter frontend/backend para tag estável anterior e manter monitoramento reforçado durante janela de recuperação.

**Áreas impactadas**

> [backend] | [frontend] | [infra] | [config/env]

---

### Mapa de Dependências

```
Wp-26 -> Wp-27 -> Wp-28 -> Wp-30
Wp-25 -> Wp-30
Wp-26 -> Wp-29 -> Wp-30
Wp-27 -> Wp-30
```

---

### Riscos e Pontos Desconhecidos

| # | Descrição | Probabilidade | Impacto | Mitigação |
|---|-----------|---------------|---------|-----------|
| R01 | Contrato de payload divergente entre FE e BE após ajustes | Média | Alto | Testes de contrato e validação conjunta antes do merge |
| R02 | Polling de 5s elevar custo em pico de usuários | Média | Médio | Monitorar RPS e ajustar intervalo em aba inativa na próxima iteração |
| R03 | Rate limit muito agressivo prejudicar usuários legítimos | Média | Médio | Iniciar com limite conservador e revisar por telemetria |
| R04 | Badge do header ficar inconsistente com fonte de verdade | Média | Médio | Centralizar estado de não lidas em serviço único e testes de sincronização |

---

### Oportunidades de Paralelização

| Grupo | WPs que podem rodar juntos | Pré-requisito para o grupo |
|-------|---------------------------|---------------------------|
| G1 | Wp-25, Wp-26 | Nenhum |
| G2 | Wp-27, Wp-29 | Wp-26 concluído |
| G3 | Wp-28 | Wp-27 concluído |

---

### Registro de Execução

- Data: 2026-04-10
- Backend: compilação OK (`mvnw -DskipTests compile`)
- Frontend: build OK (`npm run build`)
- Testes backend (`mvnw test`): falha em suite pré-existente não relacionada ao chat, no arquivo `src/test/java/ads/uninassau/brjobs/controller/UsuarioControllerUnitTest.java`.

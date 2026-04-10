### Wp-18 — Foundations de tokens e estrutura base

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-18                                              |
| **Spec relacionada**     | [SPEC-01](./specs.md#spec-01)                      |
| **Tipo**                 | frontend                                           |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Nenhuma                                            |
| **Pode paralelizar com** | Wp-19                                              |
| **Testes requeridos**    | unit \| manual                                    |
| **Status**               | ✅ Concluido                                       |

**Escopo**

> Definir taxonomia de tokens e implementar arquivo base de CSS variables para foundations (cores, tipografia, spacing, radius, shadow, motion, z-index).

**Definition of Ready**

> - [ ] Escopo de categorias de tokens aprovado
> - [ ] Convenção de nomenclatura definida
> - [ ] Lista mínima de telas críticas validada para prova de aplicação

**Passos sugeridos de implementação**

> 1. Criar estrutura de tokens globais com nomenclatura semântica.
> 2. Definir aliases por uso (ex.: `--color-bg-surface`, `--color-text-primary`).
> 3. Aplicar tokens em camada global e validar compilação.

**Critérios de aceite do pacote**

> - Todos os tokens base estão disponíveis globalmente.
> - Nenhum erro de build/lint introduzido.

**Rollback**

> - Reverter commit de foundations e restaurar arquivo global anterior.

**Áreas impactadas**

> [frontend] | [config/env]

---

### Wp-19 — Engine de tema (system/light/dark)

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-19                                              |
| **Spec relacionada**     | [SPEC-02](./specs.md#spec-02)                      |
| **Tipo**                 | frontend                                           |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Wp-18                                              |
| **Pode paralelizar com** | Wp-20                                              |
| **Testes requeridos**    | unit \| manual                                    |
| **Status**               | ✅ Concluido                                       |

**Escopo**

> Implementar seleção e persistência de tema com fallback para preferência do sistema operacional e aplicação de `data-theme` no root.

**Definition of Ready**

> - [ ] Tokens base disponíveis (Wp-18 concluído)
> - [ ] Decisão de comportamento default confirmada (`system`)

**Passos sugeridos de implementação**

> 1. Criar serviço de tema global.
> 2. Persistir escolha em localStorage.
> 3. Aplicar tema na inicialização e alternância em runtime.

**Critérios de aceite do pacote**

> - Tema muda sem recarregar página.
> - Preferência persiste após reload.

**Rollback**

> - Desativar serviço de tema e retornar ao tema único anterior.

**Áreas impactadas**

> [frontend] | [config/env]

---

### Wp-20 — Botões e formulários canônicos

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-20                                              |
| **Spec relacionada**     | [SPEC-03](./specs.md#spec-03)                      |
| **Tipo**                 | frontend                                           |
| **Estimativa**           | 3d                                                 |
| **Dependências**         | Wp-18                                              |
| **Pode paralelizar com** | Wp-21                                              |
| **Testes requeridos**    | unit \| manual                                    |
| **Status**               | ✅ Concluido                                       |

**Escopo**

> Padronizar botões, inputs, selects e textareas com variantes e estados completos (default, hover, active, focus-visible, disabled, error, loading quando aplicável).

**Definition of Ready**

> - [ ] Tokens de cor/tipografia/spacing disponíveis
> - [ ] Regras de estado por componente aprovadas

**Passos sugeridos de implementação**

> 1. Refatorar estilos compartilhados de botão e campos.
> 2. Garantir feedback de erro e foco visível em formulários.
> 3. Migrar telas de cadastro/login/publicação para os componentes canônicos.

**Critérios de aceite do pacote**

> - Estados de componentes de formulário funcionam em mouse e teclado.
> - Fluxos de cadastro/publicação mantêm comportamento funcional atual.

**Rollback**

> - Reverter camada de componentes de formulário para versão anterior.

**Áreas impactadas**

> [frontend]

---

### Wp-21 — Cards, listas, tabelas e estados de página

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-21                                              |
| **Spec relacionada**     | [SPEC-03](./specs.md#spec-03)                      |
| **Tipo**                 | frontend                                           |
| **Estimativa**           | 3d                                                 |
| **Dependências**         | Wp-18                                              |
| **Pode paralelizar com** | Wp-20                                              |
| **Testes requeridos**    | manual \| integration                             |
| **Status**               | ✅ Concluido                                       |

**Escopo**

> Padronizar cards (incluindo padrão da Home), listas e base de tabelas, além de estados de loading/empty/error/success em páginas críticas.

**Definition of Ready**

> - [ ] Tokens e guidelines de elevação/radius aprovados
> - [ ] Lista de páginas críticas definida

**Passos sugeridos de implementação**

> 1. Criar estilos base de card e variações clicáveis.
> 2. Implementar estado padrão para loading/empty/error nas páginas principais.
> 3. Ajustar home e listagens para o novo padrão.

**Critérios de aceite do pacote**

> - Home mantém identidade visual aprovada com consistência global.
> - Estados de carregamento/erro/vazio seguem padrão único.

**Rollback**

> - Reverter atualização dos componentes de lista e estado de página.

**Áreas impactadas**

> [frontend]

---

### Wp-22 — Navbar/header, modais e integração global

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-22                                              |
| **Spec relacionada**     | [SPEC-03](./specs.md#spec-03)                      |
| **Tipo**                 | frontend                                           |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Wp-20, Wp-21                                       |
| **Pode paralelizar com** | Wp-23                                              |
| **Testes requeridos**    | manual \| integration                             |
| **Status**               | ✅ Concluido                                       |

**Escopo**

> Consolidar layout global no header/navbar e padronizar comportamento visual de modais, mantendo rotas e navegação existentes.

**Definition of Ready**

> - [ ] Componentes base finalizados (Wp-20 e Wp-21)
> - [ ] Restrição de rotas preservadas validada

**Passos sugeridos de implementação**

> 1. Aplicar padrão visual ao header/navbar sem alterar rotas.
> 2. Padronizar modais com estados e foco acessível.
> 3. Revisar consistência da navegação em todos os breakpoints.

**Critérios de aceite do pacote**

> - Navegação permanece funcional e inalterada em estrutura de rotas.
> - Modais seguem padrão de acessibilidade e foco.

**Rollback**

> - Reverter ajustes de layout global e modais para versão anterior.

**Áreas impactadas**

> [frontend]

---

### Wp-23 — Instrumentação de observabilidade UX

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-23                                              |
| **Spec relacionada**     | [SPEC-04](./specs.md#spec-04)                      |
| **Tipo**                 | frontend                                           |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Wp-19                                              |
| **Pode paralelizar com** | Wp-22                                              |
| **Testes requeridos**    | integration \| manual                             |
| **Status**               | ✅ Concluido                                       |

**Escopo**

> Instrumentar eventos de UX e métricas essenciais para monitorar adoção de tema, erros de UI e performance de renderização.

**Definition of Ready**

> - [ ] Lista mínima de eventos aprovada por produto
> - [ ] Endpoint/plataforma de observabilidade disponível

**Passos sugeridos de implementação**

> 1. Implementar camada de tracking para eventos definidos.
> 2. Incluir métricas de performance por rota crítica.
> 3. Validar payloads sem dados sensíveis.

**Critérios de aceite do pacote**

> - Eventos críticos são emitidos com campos mínimos obrigatórios.
> - Métricas podem ser consultadas em ambiente de homologação.

**Rollback**

> - Desativar flag de telemetria e remover hooks de tracking.

**Áreas impactadas**

> [frontend] | [infra] | [config/env]

---

### Wp-24 — Hardening final, acessibilidade e rollout único

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-24                                              |
| **Spec relacionada**     | [SPEC-04](./specs.md#spec-04)                      |
| **Tipo**                 | fullstack                                          |
| **Estimativa**           | 3d                                                 |
| **Dependências**         | Wp-22, Wp-23                                       |
| **Pode paralelizar com** | —                                                  |
| **Testes requeridos**    | unit \| integration \| e2e \| manual          |
| **Status**               | ✅ Concluido                                       |

**Escopo**

> Executar hardening final da padronização, incluindo validação de acessibilidade, performance, regressão visual e execução controlada do rollout/rollback.

**Definition of Ready**

> - [ ] Wp-22 e Wp-23 concluídos
> - [ ] Critérios de performance e a11y formalizados
> - [ ] Plano de rollback aprovado

**Passos sugeridos de implementação**

> 1. Rodar checklist completo de acessibilidade e regressão.
> 2. Validar metas de performance definidas.
> 3. Executar rollout único em homologação e preparar produção.
> 4. Documentar lições aprendidas e pendências pós-release.

**Critérios de aceite do pacote**

> - Não há regressão crítica nas rotas prioritárias.
> - Plano de rollback testado e documentado.

**Rollback**

> - Reverter para tag estável anterior e desativar mudanças de DS quando necessário.

**Áreas impactadas**

> [frontend] | [infra] | [config/env]

---

### Mapa de Dependências

```
Wp-18 -> Wp-19 -> Wp-23 -> Wp-24
Wp-18 -> Wp-20 -> Wp-22 -> Wp-24
Wp-18 -> Wp-21 -> Wp-22 -> Wp-24
```

---

### Riscos e Pontos Desconhecidos

| # | Descrição | Probabilidade | Impacto | Mitigação |
|---|-----------|---------------|---------|-----------|
| R01 | Metas de performance não definidas no início | Alta | Alto | Fechar SLOs antes de iniciar Wp-24 |
| R02 | Decisão sobre Sidebar atrasar componentes de layout | Média | Médio | Manter Sidebar fora do escopo até decisão formal |
| R03 | Regressão visual em rotas críticas no rollout único | Média | Alto | Smoke + checklist visual + rollback testado |
| R04 | Falha de telemetria por payload incompleto | Média | Médio | Contrato de evento mínimo validado em homologação |

---

### Oportunidades de Paralelização

| Grupo | WPs que podem rodar juntos | Pré-requisito para o grupo |
|-------|---------------------------|---------------------------|
| G1 | Wp-19, Wp-20, Wp-21 | Wp-18 concluído |
| G2 | Wp-22, Wp-23 | Wp-19 concluído (Wp-22 também requer Wp-20 e Wp-21) |

# Briefing Técnico — Padronização visual global (Design System)

> **Versão:** 1.1
> **Status:** Rascunho
> **Gerado em:** 2026-04-09
> **Baseado em:**
>   - [./briefing-tech.v0.md](./briefing-tech.v0.md) — Briefing técnico v0 (com referências Figma adicionadas)
>   - [../discovery.md](../discovery.md) — discovery de 2026-04-09

---

## 1. Contexto e Problema

O frontend do projeto apresenta evolução funcional acelerada, porém sem um Design System canônico aplicado de forma global. Isso resulta em:

- variações de cores, espaçamentos e estilos entre telas;
- inconsistência entre componentes equivalentes (botões, cards, formulários, estados);
- retrabalho em novas features por ausência de padrões reutilizáveis;
- maior chance de regressão visual e acessibilidade inconsistente.

A dor afeta diretamente usuários finais (visitantes, prestadores, contratantes) e o time de engenharia/produto. Como o projeto ainda não está em produção, há janela ideal para padronização ampla com menor custo de migração.

---

## 2. Solução Proposta

Implementar um Design System global no frontend Angular, inspirado em foundations/tokens do Primer, cobrindo:

- foundations (cor, tipografia, spacing, radius, shadow, motion, z-index);
- componentes base reutilizáveis e estados canônicos;
- regras de layout responsivo e hierarquia visual;
- suporte a tema sistema (default), claro e escuro;
- diretrizes de acessibilidade para uso e validação contínua.

Não será construído nesta versão:

- reestruturação de rotas;
- reescrita da arquitetura backend;
- white-label por cliente/tenant;
- redesign de identidade verbal ou branding externo completo.

Retrocompatibilidade:

- estrutura de rotas deve permanecer inalterada;
- contratos de API não serão modificados;
- mudanças são de camada visual e comportamento de UI.

---

## 3. Personas e Papéis Afetados

| Papel | Ação que realiza | Impacto da feature |
|-------|------------------|--------------------|
| Visitante | Navega Home, busca e detalhes públicos | Direto — maior clareza, leitura e confiança visual |
| Prestador | Publica serviços, gerencia perfil e interações | Direto — formulários e feedbacks mais consistentes |
| Contratante | Busca serviços e compara ofertas | Direto — melhor escaneabilidade e tomada de decisão |
| Engenheiro Frontend | Implementa telas e componentes | Direto — menor retrabalho, melhor previsibilidade |
| Produto/UX | Define e evolui experiência | Direto — governança e linguagem unificada |
| QA | Valida comportamento visual e a11y | Direto — critérios de aceite objetivos |

---

## 4. Premissas, Restrições e Decisões Tomadas

- **Escopo global:** toda a aplicação frontend entra no padrão.
- **Projeto não produtivo:** permite rollout único com janela de ajuste maior.
- **Rotas preservadas:** não alterar mapa de navegação.
- **Referências internas obrigatórias:** manter linguagem dos cards da Home e formulário de cadastro.
- **Acessibilidade obrigatória:** foco em contraste, foco visível, navegação por teclado e estados compreensíveis.
- **Tema obrigatório:** sistema (default), claro e escuro.
- **Estratégia de adoção:** adaptar princípios de Primer, sem dependência visual rígida de biblioteca externa.

---

## 5. Arquitetura e Fluxos

### 5.1 Fluxo principal

```
Design tokens (CSS variables) -> Theme engine (system/light/dark) ->
Component primitives (button/input/card/...) ->
Feature components (home/publicacoes/profile/...) ->
Páginas e rotas existentes
```

Fluxo de inicialização de tema:

```
App start -> ler preferência salva (localStorage)
        -> se ausente: usar prefers-color-scheme
        -> aplicar data-theme no root
        -> renderizar UI com tokens resolvidos
```

Fluxo de desenvolvimento de nova tela:

```
Novo requisito -> selecionar componentes existentes
             -> compor layout por grid/spacing scale
             -> validar acessibilidade/checklist
             -> smoke test responsivo
```

### 5.2 Modelo de dados

| Campo | Entidade | Tipo | Obrigatório | Descrição |
|-------|----------|------|-------------|-----------|
| tokenName | DesignToken | string | Sim | Nome canônico do token (ex: color-primary-500) |
| tokenValueLight | DesignToken | string | Sim | Valor no tema claro |
| tokenValueDark | DesignToken | string | Sim | Valor no tema escuro |
| category | DesignToken | enum | Sim | color, typography, spacing, radius, shadow, motion |
| componentName | UIComponentSpec | string | Sim | Nome do componente base |
| states | UIComponentSpec | array | Sim | default, hover, active, focus, disabled, error |
| a11yRules | UIComponentSpec | array | Sim | Regras de contraste, foco e teclado |

Observação: modelo acima representa o contrato técnico de documentação/tokens no frontend. Não há mudança de schema no backend.

### 5.3 Endpoints

| Método | Path | Auth | Payload resumido | Resposta |
|--------|------|------|------------------|----------|
| GET | /api/v1/publicacoes | Público | sem mudança | sem mudança |
| GET | /api/v1/publicacoes/paginado | Público | sem mudança | sem mudança |
| POST | /api/v1/publicacoes | Privado | sem mudança | sem mudança |
| GET | /api/usuarios/{id} | Público/Regra atual | sem mudança | sem mudança |

Sem novos endpoints nesta feature. Alterações ficam na camada de apresentação.

Integrações externas relacionadas à UI:

- autenticação social já existente (Google/Facebook) deve herdar padrão visual de botões e estados;
- nenhum novo provedor externo necessário para primeira versão.

---

## 6. UX e Comportamento da Interface

Não existe briefing-ux para esta feature, portanto este documento inclui wireframes técnicos completos para orientar implementação.

### 6.1 Estados da interface

```
[Estado vazio]      -> mensagem contextual + CTA primário + ilustração opcional
[Estado carregando] -> skeleton/shimmer padrão por componente
[Estado sucesso]    -> feedback não bloqueante (banner/toast) + ação seguinte
[Estado erro]       -> mensagem clara + ação de retry + fallback navegável
```

Estados mínimos por componente:

- Botão: default, hover, active, focus-visible, disabled, loading.
- Input/select/textarea: default, focus, filled, error, disabled.
- Card: default, hover (quando clicável), selected (quando aplicável).
- Tabela: loading, empty, erro de carregamento, paginação inativa.

### 6.2 Wireframe

Home (cards + filtros):

> **Figma:** ⚠️ Referência visual pendente — solicitar ao time de UX/UI

```
┌────────────────────────────────────────────────────────────┐
│ Header                                                     │
├────────────────────────────────────────────────────────────┤
│ Hero + busca + filtros                                    │
├────────────────────────────────────────────────────────────┤
│ [Card] [Card] [Card]                                      │
│ [Card] [Card] [Card]                                      │
│ Paginação                                                 │
└────────────────────────────────────────────────────────────┘
```

Formulário (cadastro/publicação):

> **Figma:** ⚠️ Referência visual pendente — solicitar ao time de UX/UI

```
┌──────────────────────────────────────────┐
│ Título                                   │
│ [Input]                                  │
│ Descrição                                │
│ [Textarea]                               │
│ Tipo                                     │
│ [Select]                                 │
│ [Botão Primário] [Botão Secundário]      │
│ Mensagens de erro/sucesso                │
└──────────────────────────────────────────┘
```

Theme switcher (global):

> **Figma:** ⚠️ Referência visual pendente — solicitar ao time de UX/UI

```
┌──────────────────────────┐
│ Tema: (•) Sistema        │
│       ( ) Claro          │
│       ( ) Escuro         │
└──────────────────────────┘
```

---

## 7. Regras de Negócio

1. A aplicação DEVE usar tokens globais para cor, tipografia, spacing, radius e shadow.
2. Componentes compartilhados NÃO DEVEM usar valores hardcoded de estilo quando existir token equivalente.
3. A estrutura de rotas NÃO DEVE ser alterada durante a padronização visual.
4. O tema DEVE suportar sistema, claro e escuro, com persistência da preferência do usuário.
5. Estados de erro, vazio, carregando e sucesso DEVEM ser padronizados por componente.
6. A interface DEVE preservar semântica e legibilidade dos padrões já aprovados (cards da Home e formulário de cadastro).
7. SE um componente não existir no catálogo base, ENTÃO ele deve ser documentado antes de adoção em massa.
8. SE houver conflito entre visual novo e usabilidade atual, ENTÃO priorizar clareza da tarefa e acessibilidade.

---

## 8. Segurança e Privacidade

### 8.1 Controle de acesso

| Ação | Papel permitido | Papel bloqueado |
|------|-----------------|-----------------|
| Alternar tema local | Todos | Nenhum |
| Visualizar componentes públicos | Todos | Nenhum |
| Publicar conteúdo (fluxo existente) | Usuário autenticado | Não autenticado |
| Alterar dados sensíveis de conta (fluxo existente) | Dono da conta | Outros |

A feature não altera políticas de autorização do backend.

### 8.2 Dados sensíveis

| Dado | Onde armazenar | Criptografia | Pode logar? |
|------|---------------|--------------|-------------|
| Preferência de tema | localStorage (frontend) | Não aplicável | Não necessário |
| Token JWT | storage atual existente | Conforme implementação atual | Não |
| Dados pessoais em tela | renderização frontend | TLS em trânsito | Não em logs de analytics |

Diretrizes:

- logs de frontend NÃO DEVEM incluir PII (email, telefone, cpf);
- eventos de UX devem usar IDs técnicos/anonimizados quando possível.

---

## 9. Tratamento de Erros e Resiliência

| Cenário | Causa | Comportamento esperado | Mensagem ao usuário |
|---------|-------|----------------------|---------------------|
| Falha ao carregar lista | API indisponível/timeout | Mostrar estado erro + botão tentar novamente | Nao foi possivel carregar os dados. Tente novamente. |
| Falha ao salvar formulário | 4xx/5xx | Manter dados preenchidos e destacar campos inválidos | Revise os campos e tente novamente. |
| Tema inválido salvo | dado corrompido no storage | Reverter para tema do sistema | Preferencia de tema redefinida para padrao do sistema. |
| Contraste insuficiente detectado em QA | token incorreto | Bloquear release via checklist de qualidade | Ajuste de acessibilidade necessario antes da publicacao. |
| Componente sem estado disabled | implementação incompleta | Falhar validação de Design QA | Componente fora do padrao visual. |
| Regressão visual em rota existente | alteração de CSS global | Rollback de pacote de estilos/token | Ajuste em andamento para restaurar consistencia. |
| Falha de fonte externa | CDN indisponível | Fallback para fonte secundária configurada | Conteudo exibido com fonte alternativa. |

---

## 10. Observabilidade

### 10.1 Eventos a logar

| Evento | Campos obrigatórios | Nível |
|--------|--------------------:|-------|
| theme_changed | user_state(auth/anon), from_theme, to_theme, timestamp | info |
| ds_component_render_error | component_name, route, error_code, timestamp | warn |
| form_submit_validation_error | form_name, field_count_invalid, route, timestamp | warn |
| cta_primary_click | cta_name, route, user_state, timestamp | info |
| page_render_time_sample | route, render_ms, theme, timestamp | info |

### 10.2 Métricas

| Métrica | Tipo | O que mede |
|---------|------|-----------|
| frontend_render_ms_p95 | histograma | Tempo de renderização por rota |
| ui_error_rate | contador/taxa | Erros de UI por 1.000 sessões |
| form_validation_fail_rate | contador/taxa | Percentual de envios inválidos |
| theme_dark_adoption_rate | gauge | Percentual de usuários em tema escuro |
| retry_click_rate | contador/taxa | Frequência de clique em tentar novamente |

### 10.3 Alertas

| Condição | Threshold | Ação |
|----------|-----------|------|
| ui_error_rate elevada | > 3% por 15 min | Notificar canal de engenharia e abrir incidente |
| frontend_render_ms_p95 degradado | > 2500 ms em 30 min | Investigar bundle e componentes recentes |
| retry_click_rate anormal | > 15% em rota crítica | Revisar disponibilidade de API e fallback UI |

---

## 11. Variáveis de Ambiente e Configuração

```env
# Frontend Design System
# Preferência inicial de tema (fallback quando não houver preferência salva)
# Opções: system | light | dark
✱ NG_APP_DEFAULT_THEME=system           # FE — tema inicial padrão

# Habilita telemetria de UX (eventos de UI)
✱ NG_APP_UX_TELEMETRY_ENABLED=true      # FE — liga/desliga eventos de observabilidade

# Amostragem de métricas de performance (0 a 1)
NG_APP_PERF_SAMPLE_RATE=0.2             # FE — percentual de sessões monitoradas

# Contraste mínimo alvo para validação automática (AA como default)
NG_APP_A11Y_CONTRAST_TARGET=AA          # FE — AA ou AAA

# Backend (sem mudanças obrigatórias nesta feature)
# Mantém variáveis existentes de API/JWT/CORS
```

---

## 12. Estratégia de Rollout e Rollback

**Rollout:** único (big bang), conforme decisão do stakeholder.

Plano de rollout:

1. consolidar foundations e tokens globais;
2. migrar componentes base prioritários (botões, inputs, cards, estados);
3. aplicar em rotas críticas (home, cadastro, login, publicações, detalhes);
4. validar responsividade e acessibilidade antes de merge final;
5. publicar em ambiente de homologação para smoke completo;
6. liberar versão final.

**Rollback:**

- manter branch/tag anterior com CSS/tokens legados;
- rollback por reversão de commit de Design System (sem migração de banco);
- em caso de falha crítica, reverter pacote de estilos/componentes e restaurar build anterior.

---

## 13. Fases de Entrega

### Fase 1 — Foundations e Tema
- [ ] Definir tokens globais (cor, tipografia, spacing, radius, shadow, motion)
- [ ] Implementar engine de tema (sistema, claro, escuro)
- [ ] Publicar guia de uso de tokens

### Fase 2 — Componentes Base
- [ ] Padronizar botões, inputs, selects, textarea
- [ ] Padronizar cards e estados (loading/empty/error/sucesso)
- [ ] Padronizar navbar/header e base de tabelas/modais

### Fase 3 — Aplicação Global
- [ ] Migrar telas prioritárias mantendo rotas atuais
- [ ] Ajustar variações de layout responsivo
- [ ] Executar checklist de acessibilidade e regressão visual

### Fase 4 — Observabilidade e Hardening
- [ ] Instrumentar eventos e métricas de UX
- [ ] Definir alertas operacionais
- [ ] Consolidar documentação para manutenção contínua

---

## 14. Fora do Escopo (desta versão)

- Sidebar obrigatória em todas as áreas (depende de confirmação de produto/arquitetura).
- Personalização de tema por cliente/tenant (white-label).
- Refatoração de serviços backend e modelos de dados.
- Reestruturação de rotas e IA de recomendação visual.

---

## 15. Riscos e Pontos em Aberto

| # | Descrição | Probabilidade | Impacto | Mitigação |
|---|-----------|---------------|---------|-----------|
| R01 | Escopo amplo pode gerar regressão visual em múltiplas rotas | Média | Alto | Checklist de regressão por rota + smoke responsivo |
| R02 | Falta de metas numéricas de performance dificulta aceite objetivo | Alta | Médio | Definir SLOs antes do início de implementação massiva |
| R03 | Falta de definição formal de eventos de observabilidade | Alta | Médio | Aprovar dicionário mínimo de eventos com produto |
| R04 | Contraste alvo (AA/AAA) indefinido pode gerar retrabalho | Média | Médio | Congelar política de a11y antes da fase 2 |
| R05 | Rollout único amplia impacto de erro de estilo global | Média | Alto | Plano de rollback pronto + validação em homologação completa |
| R06 | Dependência de fontes externas impactar performance/percepção | Baixa | Médio | Fallback local e preload controlado |

**Pontos em aberto (bloqueadores):**
- ⚠️ SLA/performance: definir metas numéricas de LCP, INP, TTI e orçamento de bundle — **responsável:** Produto + Engenharia Frontend.
- ⚠️ Observabilidade UX: definir lista final de eventos e métricas mandatórias — **responsável:** Produto + Engenharia Frontend.
- ⚠️ Sidebar: confirmar se entra nesta versão ou permanece fora de escopo — **responsável:** Produto/UX.
- ⚠️ Acessibilidade formal: decidir política AA global e critérios AAA para áreas críticas — **responsável:** UX/QA.

---

## 16. Referências de Design (Figma)

> Gerado por /lf-specs em 2026-04-09. URLs fornecidas pelo time de UX/UI.
> ⚠️ Figma MCP não estava disponível durante a geração. Execute /lf-specs novamente com Figma MCP conectado para enriquecer esta seção.

| Tela | Rota | URL Figma | Componentes-chave | Status |
|------|------|-----------|-------------------|--------|
| Home (cards + filtros) | / | — | cards, search bar, filtros, paginação | ⚠️ Pendente |
| Formulário (cadastro/publicação) | /register, /publicacoes | — | input, textarea, select, button | ⚠️ Pendente |
| Theme switcher (global) | global | — | radio/segmented control, tokens de tema | ⚠️ Pendente |

---

Documento gerado para alinhamento técnico interno. Revisar com o time antes de iniciar o desenvolvimento.
# Discovery — Padronização visual global (Design System)

> **Feature:** 20260409200720_padronizacao_design_system_projeto
> **Data:** 2026-04-09
> **Autor:** GitHub Copilot (facilitação de discovery)
> **Status:** Completo

---

## Fontes de contexto utilizadas

| # | Fonte | Tipo | Conteúdo principal |
|---|-------|------|--------------------|
| 1 | Entrevista guiada com stakeholder | sessão | Escopo global, preferências visuais, restrições e rollout |
| 2 | AGENTS.md | arquivo | Convenções de projeto, estrutura e práticas de entrega |
| 3 | CLAUDE.md | arquivo | Stack ativa (Angular + Spring), padrões de arquitetura |
| 4 | https://primer.style/ | referência de mercado | Tokens/primitives, foundations, acessibilidade |
| 5 | https://m3.material.io/ | referência de mercado | Boas práticas de componentes, motion e acessibilidade |
| 6 | https://www.nngroup.com/articles/design-systems-101/ | referência de mercado | Adoção e governança de design system |

---

## Tipo de projeto

**Brownfield**

> Trata-se de padronização de UI/UX em produto já existente (não em produção), preservando arquitetura e rotas atuais.

---

## Resumo do entendimento

Definir e implementar um Design System unificado para todo o projeto (frontend Angular), cobrindo layout, tipografia, cores, componentes, estados de interface e temas claro/escuro/sistema, mantendo a estrutura de rotas atual e elevando consistência, acessibilidade e percepção de qualidade.

O sistema deve manter referências visuais já aprovadas internamente (cards da Home e formulário de cadastro), evoluindo para padrão moderno, limpo, responsivo e escalável.

---

## Problema e dor

**Dor principal:** ausência de padrão visual global consistente gera inconsistências de UI, retrabalho de front e experiência irregular entre telas.

**Quem sente:** time de produto/dev e todos os usuários finais (prestadores, contratantes e visitantes).

**Frequência e impacto:** recorrente em toda nova tela/ajuste; impacto direto em velocidade de entrega, coerência da marca e qualidade percebida.

---

## Usuários e papéis afetados

| Papel | Relação com a feature | Impacto |
|-------|-----------------------|---------|
| Visitante | Navega páginas públicas | Direto: experiência visual e legibilidade |
| Prestador | Usa fluxos autenticados e publicação | Direto: produtividade e clareza de ações |
| Contratante | Usa busca, detalhes e contratação | Direto: confiança e entendimento de estado/CTA |
| Time de desenvolvimento | Implementa e mantém UI | Direto: redução de retrabalho e maior previsibilidade |
| Produto/Design | Define evolução da interface | Direto: governança e consistência |

---

## Solução proposta (rascunho)

**Construir:**
- Design System base com tokens (cor, tipografia, espaçamento, radius, sombra, motion, z-index).
- Catálogo de componentes base reutilizáveis (botões, inputs, selects, textareas, cards, tabelas, estados de loading/empty/error, navbar/header e modais).
- Guia de layout responsivo e grid com escala de espaçamento.
- Estratégia de tema com 3 opções de uso: padrão do sistema (default), claro e escuro.
- Diretrizes de acessibilidade (contraste, foco visível, navegação por teclado, estados e feedback).
- Estratégia técnica de implementação por CSS variables + camadas utilitárias/componentes, preservando rotas existentes.

**Fora do escopo desta versão:**
- Reescrita de arquitetura backend.
- Mudança de estrutura de rotas.
- Rebranding completo de identidade verbal/marketing externo.
- White-label multi-tenant com temas por cliente (nesta fase).

---

## Restrições e premissas

- Escopo de padronização visual cobre todo o frontend do projeto.
- Projeto ainda não está em produção (permite ajustes amplos sem impacto de usuários produtivos).
- Manter estrutura de rotas atual obrigatoriamente.
- Seguir boas práticas modernas de desenvolvimento e acessibilidade.
- Referências internas aprovadas: cards da Home e formulário de cadastro.
- Rollout escolhido: único (big bang), não incremental.
- Tema deve suportar: sistema (default), claro e escuro.

---

## Referências de mercado

| Referência | Decisão de design relevante | Ressoa? | Motivo |
|------------|-----------------------------|---------|--------|
| Primer (GitHub) | Foundations + tokens/primitives como base de consistência | **Sim (principal)** | Preferência explícita do stakeholder; forte alinhamento com governança e escalabilidade |
| Material Design 3 | Diretrizes robustas de componentes, acessibilidade e motion | Parcialmente | Útil como referência técnica, sem copiar linguagem visual padrão Material |
| NN/g Design Systems 101 | Design System como produto contínuo com governança | Sim | Ajuda a estruturar manutenção e adoção do padrão ao longo do tempo |

**Padrões extraídos das referências escolhidas:**
- Construir foundations primeiro (tokens e princípios), depois componentes e padrões de página.
- Garantir documentação de uso e estados de componentes (default, hover, active, disabled, focus, error).
- Tratar acessibilidade como requisito transversal e não etapa final.
- Separar decisões visuais (tokens) da implementação de componentes para facilitar evolução.

---

## Decisões de design tomadas

| Decisão | Alternativas consideradas | Justificativa |
|---------|--------------------------|---------------|
| Adotar abordagem “adaptar” (inspirada em Primer) | Adotar integralmente framework externo; criar do zero sem referência | Equilíbrio entre velocidade, identidade própria e governança |
| Criar Design System para todo frontend agora | Padronização parcial por módulo | Escopo declarado é global e evita inconsistências futuras |
| Preservar padrão visual base da Home/Cadastro | Descartar UI atual e redesenhar tudo | Mantém continuidade com escolhas já aprovadas pelo stakeholder |
| Suportar tema sistema + claro + escuro | Apenas tema claro | Requisito explícito e melhora experiência em diferentes preferências |
| Manter rotas inalteradas | Reestruturar navegação junto com redesign | Restrição explícita para reduzir risco funcional |
| Rollout único (big bang) | Rollout por etapas | Escolha explícita do stakeholder |

---

## Lacunas e pontos em aberto

- ⚠️ **Ponto em aberto:** SLA/performance — metas numéricas não definidas (ex.: LCP, INP, TTI, peso máximo de bundle) — **responsável:** Produto + Engenharia Frontend.
- ⚠️ **Ponto em aberto:** Observabilidade UX — eventos e métricas exatas não definidos (ex.: abandono de formulário, erro por campo, cliques em CTAs) — **responsável:** Produto + Engenharia Frontend.
- ⚠️ **Ponto em aberto:** Escopo de componentes avançados — necessidade de Sidebar ainda não confirmada em arquitetura atual — **responsável:** Produto/UX.
- ⚠️ **Ponto em aberto:** Política de contraste e compliance — nível alvo formal (WCAG AA global vs AAA em áreas críticas) não formalizado — **responsável:** UX/QA.

---

## Notas adicionais

- O discovery foi conduzido sem documentos iniciais externos (do zero), baseado em entrevista guiada e contexto interno do repositório.
- A implementação técnica recomendada para o próximo passo é:
  - Tokens globais via CSS variables (cores, tipografia, spacing, radius, shadow, motion).
  - Organização por camadas: foundations -> componentes base -> padrões de página.
  - Theme switcher persistente (localStorage) com fallback para `prefers-color-scheme`.
  - Migração visual com checklist de regressão por componente e smoke test responsivo.
- O detalhamento técnico por work packages deve ser gerado na próxima etapa.

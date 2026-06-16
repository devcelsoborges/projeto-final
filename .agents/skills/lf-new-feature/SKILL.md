---
name: lf-new-feature
description: "Generates the technical briefing (briefing-tech.vN.md, 15 sections) from an existing discovery.md. Can be called multiple times to iterate versions (v0 → v1 → v2). If briefing-ux.vN.md exists, uses it to populate personas and UX sections without repeating questions. Use when the user asks for /lf-new-feature or needs a technical briefing for a feature. Run /lf-specs after the UX/UI team delivers Figma screens to generate specs.md and wps.md."
---

Você é um engenheiro de software sênior e tech lead, especializado em especificação técnica de features em times AI-native. Seu papel é transformar o contexto de discovery em um briefing técnico canônico de alta qualidade.

Gere documentos com o nível de profundidade e detalhe do exemplo de referência em `ai/specs/20260323142630_google_docs/`. Leia esses arquivos como referência de qualidade antes de gerar.

---

## PASSO 1 — Localizar contexto existente

1. Use a ferramenta Glob para verificar se existe `ai/specs/*/discovery.md` no projeto.

2. **Se discovery.md existir:**
   - Leia `discovery.md` com a ferramenta Read.
   - Leia todos os arquivos em `inputs/` da mesma pasta (use Glob + Read).
   - Confirme: "Encontrei o discovery de **[nome da feature]**. Vou usá-lo para gerar os artefatos."
   - Se houver mais de um discovery recente, liste e pergunte qual usar.

3. **Se discovery.md NÃO existir:**
   - Informe: "Não encontrei um discovery prévio. Vou conduzir o discovery agora antes de gerar os artefatos."
   - Execute as Fases 0 a 6 do comando `/lf-discovery` inline, sem gerar o arquivo `discovery.md` separado.
   - Ao finalizar o discovery inline, prossiga para o Passo 2.

4. **Verificar Briefing UX/UI existente:**
   - Use Glob para verificar se existe `briefings/briefing-ux.v*.md` na mesma pasta do discovery.
   - **Se existir:** identifique a versão mais alta disponível (ex: se há v0 e v1, use v1). Leia o arquivo com Read. Informe: "Encontrei o Briefing UX/UI (briefing-ux.v[N]). Vou usá-lo para popular personas e UX do briefing técnico."
   - **Se não existir:** prossiga normalmente sem bloquear — o Briefing UX/UI é opcional.

5. **Verificar versão existente do briefing-tech:**
   - Use Glob para verificar se existe `briefings/briefing-tech.v*.md` na mesma pasta do discovery.
   - **Se existir:** identifique o número de versão mais alto (ex: se há v0 e v1, próxima é v2). Informe: "Já existe briefing-tech.v[N].md. Vou gerar a versão v[N+1]."
   - **Se não existir:** vou gerar v0.

---

## PASSO 2 — Confirmação de escopo

Leia também os arquivos de referência canônica para calibrar qualidade:
- `ai/specs/20260323142630_google_docs/briefings/briefing.v0.md`

Leia o template em `$CLAUDE_SKILL_DIR/templates/briefing-tech.md`. Se não existir, crie-o com as 15 seções canônicas extraídas de `ai/specs/20260323142630_google_docs/briefings/briefing.v0.md` antes de prosseguir.

Apresente ao usuário:

```
Vou gerar para [nome da feature]:

  • ai/specs/YYYYMMDDHHmmSS_nome/briefings/briefing-tech.v[N].md

Baseado em:
  • [discovery.md existente / inputs/ / discovery inline]
  [• briefings/briefing-ux.v[N].md (se encontrado)]

Pontos em aberto identificados no discovery:
  ⚠️ [lista, se houver]

Confirma? Estes pontos em aberto aparecerão explicitamente nos documentos gerados.
```

Aguarde confirmação antes de gerar.

---

## PASSO 3 — Geração do `briefing-tech.vN.md`

Gere `ai/specs/YYYYMMDDHHmmSS_nome/briefings/briefing-tech.v[N].md` onde N é a próxima versão disponível (identificada no Passo 1, item 5).

**Use `$CLAUDE_SKILL_DIR/templates/briefing-tech.md` como estrutura e `ai/specs/20260323142630_google_docs/briefings/briefing.v0.md` como referência de profundidade.**

**Header obrigatório — popular com valores reais:**

```markdown
> **Versão:** [N].1
> **Status:** Rascunho
> **Gerado em:** [data atual no formato YYYY-MM-DD]
> **Baseado em:**
>   - [`../briefings/briefing-ux.vN.md`](../briefings/briefing-ux.vN.md) — Briefing UX/UI vN ([data]) ← incluir apenas se briefing-ux foi encontrado no Passo 1
>   - [`../discovery.md`](../discovery.md) — discovery de [data do discovery]
```

Se o `briefing-ux.vN.md` não foi encontrado no Passo 1, omitir a linha correspondente.

**Regras de qualidade:**

- **Todas as 15 seções devem estar presentes** — mesmo que uma seção seja "Não aplicável para esta feature", ela deve aparecer com essa nota.
- **Seção 3 (Personas):** se `briefing-ux.vN.md` foi encontrado, derivar desta seção — não repetir perguntas já respondidas. Complementar com papéis técnicos que não aparecem no UX (ex: administrador de sistema, DBA).
- **Seção 5 (Arquitetura):** inclua diagramas ASCII de fluxo, tabela de modelo de dados e tabela de endpoints quando aplicável.
- **Seção 6 (UX):** se `briefing-ux.vN.md` foi encontrado, resumir os fluxos principais já documentados e referenciar o arquivo. Adicionar wireframes ASCII apenas para novos comportamentos técnicos não cobertos no UX (estados de erro técnico, fluxos de autenticação, etc). Se não houver briefing-ux, incluir wireframes ASCII completos para os fluxos principais.
- **Seção 9 (Erros):** tabela completa com todos os cenários de falha identificados.
- **Seção 10 (Observabilidade):** tabela de eventos a logar, métricas e alertas — não deixe vazia.
- **Seção 11 (Env vars):** bloco `.env` comentado com todas as variáveis necessárias.
- **Seção 15 (Riscos):** tabela com pelo menos os riscos identificados no discovery.
- **⚠️ Pontos em aberto** do discovery devem aparecer na Seção 15 e nas seções relevantes.
- Tom: técnico, direto, prescritivo. Sem redundância. Sem frases genéricas.

---

## PASSO 4 — Confirmação final

Após gerar o arquivo, analise se o `briefing-tech.v[N].md` gerado contém decisões técnicas que contradizem ou adicionam restrições ao `briefing-ux.vN.md` existente (se houver). Compare especialmente: seção 5 (arquitetura/limites técnicos), seção 7 (regras de negócio), seção 8 (segurança) e seção 9 (erros) contra as telas e fluxos descritos no briefing UX.

Apresente:

```
Gerado com sucesso ✓

  briefings/briefing-tech.v[N].md — 15 seções · [N pontos em aberto]

[Se houver pontos em aberto:]
Pontos em aberto que precisam de decisão antes de iniciar:
  ⚠️ [item 1]
  ⚠️ [item 2]

[Se houver divergências entre briefing-tech e briefing-ux:]
⚠️ Decisões técnicas que podem impactar o Briefing UX/UI:
  1. [restrição identificada] — afeta [tela/fluxo do briefing-ux] — sugerido: atualizar seção [X] do briefing-ux
  2. [...]

  Para atualizar: peça ao Claude "leia briefing-ux.v[N].md e gere v[N+1] com: [mudanças acima]"

Próximos passos sugeridos:
  1. Revisar o briefing técnico com o time
  2. Resolver os pontos em aberto acima
  3. Para refinar: execute /lf-new-feature novamente → briefing-tech.v[N+1].md

  ── Quando o time de UX/UI entregar as telas no Figma ──
  4. Execute /lf-specs para:
     • Registrar as URLs das telas do Figma no briefing técnico
     • Gerar specs.md (especificações por domínio com referências visuais)
     • Gerar wps.md (work packages com mapa de dependências)
```

---
name: lf-exec
description: "Starts the execution of a specific work package (WP-XX) from a generated wps.md. Interactively lists available spec folders under specs/ and pending work packages (excluding completed ones), guides the developer through project update and branch creation (features/initials/semantic_name or current branch), then fires the execution prompt for the selected WP. No arguments required."
---

Você é um tech lead sênior responsável por conduzir o desenvolvedor no início da implementação de um work package. Seu papel é garantir que o ambiente está preparado (projeto atualizado, branch correta) e disparar o prompt de execução padronizado para o WP selecionado.

---

## PASSO 0 — Listar specs disponíveis

Use a ferramenta Glob para listar todos os arquivos `specs/*/wps.md` no projeto atual.

Se nenhum arquivo for encontrado:

```
❌ Nenhuma especificação com work packages encontrada.

Execute /lf-specs para gerar os work packages de uma feature antes de iniciar a execução.
```

Encerre a execução.

Extraia os nomes das pastas (ex: `20260323142630_google_docs`) a partir dos caminhos encontrados. Ordene em ordem decrescente de timestamp (mais recente primeiro).

Apresente a lista numerada:

```
Qual especificação você deseja executar?

  [1] 20260323142630_google_docs
  [2] 20260301090000_outra_feature

Digite o número e pressione Enter:
```

Aguarde a escolha do usuário. Armazene internamente como `SPEC_FOLDER`.

---

## PASSO 1 — Listar WPs pendentes

Leia `specs/<SPEC_FOLDER>/wps.md` com a ferramenta Read.

Extraia todos os work packages cujo campo `Status` **não** seja `✅ Concluido`. Para cada WP encontrado, normalize o ID para **uppercase** (ex: `Wp-32` → `WP-32`). Extraia também: título do WP, estimativa e dependências.

Se todos os WPs estiverem com status `✅ Concluido`:

```
✅ Todos os work packages desta spec já foram concluídos.

  Spec: <SPEC_FOLDER>

Nada a executar.
```

Encerre a execução.

Apresente a lista numerada dos WPs pendentes, mostrando: ID (uppercase), título, estimativa e dependências (se houver):

```
Qual work package você deseja executar?

  [1] WP-32 — OAuth Google (connect, callback, disconnect) · 2d · Depende de: WP-31
  [2] WP-34 — Seleção de template via Picker · 2d · Depende de: WP-32
  [3] WP-35 — Flags de ativação e bloco de variáveis · 2d · Depende de: WP-32

Digite o número e pressione Enter:
```

Aguarde a escolha do usuário. Armazene internamente como `WP_ID` (sempre uppercase, ex: `WP-32`).

---

## PASSO 2 — Orientar atualização do projeto

Use a ferramenta Glob para verificar se existe o arquivo `.gitmodules` na raiz do projeto atual.

**Se `.gitmodules` existir (projeto com git submodules):**

```
Antes de iniciar, certifique-se de que o projeto está atualizado com as
últimas mudanças da branch principal.

Como este projeto usa git submodules, execute:

  git --no-pager submodule update --init --recursive

Avise quando o projeto estiver atualizado para continuar.
```

**Se `.gitmodules` não existir:**

```
Antes de iniciar, certifique-se de que o projeto está atualizado com as
últimas mudanças da branch principal.

Execute:

  git pull origin main

Avise quando o projeto estiver atualizado para continuar.
```

Aguarde a confirmação do usuário antes de continuar para o próximo passo.

---

## PASSO 3 — Criar ou escolher branch

Extraia o nome semântico da spec a partir de `SPEC_FOLDER`: remova o prefixo timestamp (`YYYYMMDDHHmmSS_`) e use o restante como `NOME_SEMANTICO`, em **lowercase** e **snake_case** (mantenha underscores, sem hífens).

Exemplo: `20260323142630_google_docs` → `NOME_SEMANTICO = google_docs`

Apresente:

```
Deseja criar uma nova branch para este work package?

  [1] Criar nova branch: features/<suas-iniciais>/<NOME_SEMANTICO>
      (serão solicitadas suas iniciais git — máximo 3 letras)

  [2] Usar a branch atual

Escolha 1 ou 2:
```

Aguarde a resposta do usuário.

**Se [1]:**

Pergunte: `Quais são suas iniciais no git? (máximo 3 letras, minúsculas)`

Valide que a resposta tenha no máximo 3 caracteres alfabéticos. Se inválida, peça novamente.

Monte o nome final: `features/<iniciais>/<NOME_SEMANTICO>`

Informe ao usuário:

```
Crie a branch com o comando:

  git checkout -b features/<iniciais>/<NOME_SEMANTICO>

Avise quando estiver na branch correta para continuar.
```

Aguarde a confirmação do usuário.

**Se [2]:** Continue para o Passo 4 sem solicitar criação de branch.

---

## PASSO 4 — Iniciar execução do WP

Leia o template em `$CLAUDE_SKILL_DIR/templates/exec-prompt.md`.

Substitua no conteúdo do template:
- `<PATH_DO_WPS>` → `specs/<SPEC_FOLDER>/wps.md`
- `<WP_ID>` → valor de `WP_ID` em uppercase (ex: `WP-32`)

Emita o prompt preenchido diretamente no chat para que Claude inicie a implementação.

<!-- TEMPLATE DE ESPECIFICAÇÕES — lifters-skills
     Uma entrada SPEC-XX por domínio de responsabilidade.
     Copiar este bloco para cada SPEC e preencher todas as seções.
     Seções marcadas com [NOVO] foram adicionadas ao formato canônico.
-->

<a id="spec-01"></a>
## SPEC-01 — [Nome da Spec]

**Objetivo**
> [Uma frase: o que esta spec entrega e por quê existe.]

**Contexto**
> [O que existe hoje no sistema. De onde veio este requisito.
>  Qual seção do briefing ou decisão de negócio originou esta spec.]

**Personas e Papéis** <!-- [NOVO] -->
> [Quem interage com o que esta spec entrega.]
>
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | [ex: admin] | [configura X] | [direto] |
> | [ex: usuário] | [usa Y] | [indireto] |

**Comportamento esperado**
> [Fluxo numerado detalhado. Inclui fluxo feliz e todos os fluxos alternativos relevantes.]
>
> Fluxo principal:
> 1. [Passo 1]
> 2. [Passo 2]
>
> Fluxo alternativo — [nome do cenário]:
> 1. [Passo 1]

**Regras de negócio**
> - [Papel] DEVE [fazer X] quando [condição].
> - O sistema NÃO DEVE [fazer Y].
> - SE [condição] ENTÃO [comportamento].

**Contrato de API** <!-- [NOVO] — omitir se não aplicável -->
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------:|-----------------|
> | POST | /api/x | admin | `{ campo: tipo }` | `201 { id }` | 400, 401, 409 |

**SLA e Performance** <!-- [NOVO] -->
> - Latência máxima: Xms (P95 em produção)
> - Volume esperado: N req/s no pico
> - Timeout para integrações externas: Xs
> - [Se não aplicável: "Sem requisitos especiais de performance para esta spec."]

**Observabilidade** <!-- [NOVO] -->
> - **Logar:** `[nome_evento]` com campos: `[campo1, campo2, ...]` — nível: `info | warn | error`
> - **Métrica:** `[nome_metrica]` — [o que mede e por quê importa]
> - **Alerta:** SE `[condição]` POR `[duração]` → [ação / notificação]
> - [Se não aplicável: "Coberto pelo logging padrão da aplicação."]

**Critérios de aceite**
> - DADO [contexto inicial] QUANDO [ação do ator] ENTÃO [resultado esperado e observável].
> - DADO [contexto de erro] QUANDO [ação] ENTÃO [comportamento de erro esperado].

**Estado atual**
> [O que existe hoje que esta spec modifica, complementa ou substitui.
>  Se greenfield: "Não há implementação anterior — entrega nova."]

**Mudanças necessárias**
> - **Banco de dados:** [migrações, novos campos, índices, alterações de schema]
> - **Backend:** [novos endpoints, serviços, workers, jobs, regras de negócio]
> - **Frontend:** [novas telas, componentes, fluxos de navegação, estados]
> - **Infra/Config:** [novas variáveis de ambiente, serviços externos, permissões]

**Definição de pronto**
> - [ ] Funcionalidade implementada conforme comportamento esperado
> - [ ] Todos os critérios de aceite validados (automatizados ou manuais)
> - [ ] Casos de erro tratados com feedback adequado ao usuário
> - [ ] Código revisado por outro membro do time
> - [ ] [Item específico desta spec — ex: token criptografado em repouso]
> - [ ] [Item específico desta spec — ex: validado em ambiente de homologação]

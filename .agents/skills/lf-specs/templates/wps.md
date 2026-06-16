<!-- TEMPLATE DE WORK PACKAGES — lifters-skills
     Um Wp-XX por unidade de trabalho coesa (entregável por um dev em 1-3 dias).
     IDs sequenciais globais — continuar a partir do último Wp- existente no repositório.
     Campos marcados com [NOVO] foram adicionados ao formato canônico.
-->

### Wp-XX — [Nome do Work Package]

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-XX                                              |
| **Spec relacionada**     | [SPEC-XX](./specs.md#spec-xx)                      |
| **Tipo**                 | backend \| frontend \| fullstack \| infra \| data  |
| **Estimativa**           | Xd                                                 |
| **Dependências**         | Wp-YY (ou Nenhuma)                                 |
| **Pode paralelizar com** | Wp-ZZ (ou —)                                       |
| **Testes requeridos**    | unit \| integration \| e2e \| manual               |
| **Status**               | 🔲 Pendente                                        |


**Escopo**

> [O que este WP entrega. Uma responsabilidade coesa.
>  Deve ser possível fazer code review isolado deste WP.]


**Definition of Ready** <!-- [NOVO] -->

> [O que deve ser verdade ANTES de um dev começar este WP.]
> - [ ] [Decisão técnica X tomada — ex: schema de banco aprovado]
> - [ ] [Wp-YY concluído — ex: endpoint de autenticação disponível]
> - [ ] [Ambiente Z disponível — ex: credenciais de sandbox configuradas]


**Passos sugeridos de implementação**

> 1. [Passo concreto e acionável — sem ambiguidade]
> 2. [Passo seguinte]
> 3. [...]


**Critérios de aceite do pacote**

> - [Resultado observável e verificável — o que o dev testa antes de abrir o PR]
> - [Resultado 2]


**Rollback** <!-- [NOVO] -->

> [Como desfazer este WP em produção se necessário.]
> - [ex: Feature flag `FEATURE_X_ENABLED=false` desativa sem rollback de banco]
> - [ex: Migration `down` reverte os campos adicionados]
> - [ex: Sem estado persistido — rollback é o deploy da versão anterior]


**Áreas impactadas**

> [banco] | [backend] | [frontend] | [infra] | [config/env]

---

<!-- ═══════════════════════════════════════════════════════════
     SEÇÕES OBRIGATÓRIAS AO FINAL DO ARQUIVO wps.md GERADO
     ═══════════════════════════════════════════════════════════ -->

### Mapa de Dependências

<!-- Representar o grafo de dependências entre WPs.
     Formato: Wp-XX → Wp-YY → Wp-ZZ
     Grupos paralelos na mesma linha separados por vírgula. -->

```
Wp-XX → Wp-YY → Wp-ZZ
Wp-XX → Wp-AA → Wp-BB → Wp-ZZ
```

---

### Riscos e Pontos Desconhecidos

| # | Descrição | Probabilidade | Impacto | Mitigação |
|---|-----------|---------------|---------|-----------|
| R01 | [risco técnico ou de negócio] | Alta / Média / Baixa | Alto / Médio / Baixo | [ação concreta de mitigação] |

---

### Oportunidades de Paralelização

| Grupo | WPs que podem rodar juntos | Pré-requisito para o grupo |
|-------|---------------------------|---------------------------|
| G1 | Wp-XX, Wp-YY | Wp-AA concluído |
| G2 | Wp-BB, Wp-CC | Wp-XX concluído |

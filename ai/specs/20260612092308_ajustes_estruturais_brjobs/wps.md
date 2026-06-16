# Work Packages — Ajustes estruturais no BRJobs

> **Gerado em:** 2026-06-12  
> **Fonte:** [`specs.md`](./specs.md)  
> **Estimativa total:** 20d  
> **Primeiro WP sem dependência:** Wp-45 e Wp-52

---

### Wp-45 — Schema e flags estruturais

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-45                                              |
| **Spec relacionada**     | [SPEC-01](./specs.md#spec-01), [SPEC-03](./specs.md#spec-03), [SPEC-06](./specs.md#spec-06) |
| **Tipo**                 | data / backend / infra                             |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Nenhuma                                            |
| **Pode paralelizar com** | Wp-52                                              |
| **Testes requeridos**    | migration / integration / manual                   |
| **Status**               | ✅ Concluido                                       |

**Escopo**
> Preparar banco e configuração para perfil parcial, social logins, publicações com localização, cache de geocoding e flags independentes.

**Definition of Ready**
> - [ ] Ambiente local com banco disponível.
> - [ ] Estratégia de migration compatível com produção validada.
> - [ ] Nomes finais das flags definidos.

**Passos sugeridos de implementação**
> 1. Criar migrations para tornar campos de perfil opcionais quando ainda não forem.
> 2. Criar/ajustar tabela `social_logins` com chaves únicas por provider/providerId e user/provider.
> 3. Adicionar campos de publicação: `tipo_publicacao`, endereço da publicação, `latitude`, `longitude`, provider/precisão de geocode.
> 4. Criar `geocode_cache` com hash único de endereço normalizado.
> 5. Adicionar leitura de flags `structuralAuth`, `publicationGeo` e `dynamicNotifications`.
> 6. Garantir migrations reversíveis quando possível e não remover `tipoUsuario`.

**Critérios de aceite do pacote**
> - Migrations aplicam em banco limpo e banco com dados existentes.
> - Campos de perfil removidos do cadastro não bloqueiam criação de usuário.
> - Publicações antigas continuam legíveis.
> - Flags existem e podem ser desligadas por config/env.

**Rollback**
> - Desligar flags novas para voltar ao comportamento legado.
> - Manter colunas novas nullable; migration down só deve ser usada antes de persistir dados dependentes.
> - Não remover dados de usuários/publicações em rollback operacional.

**Áreas impactadas**
> banco | backend | infra | config/env

---

### Wp-46 — Cadastro mínimo backend e sessão

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-46                                              |
| **Spec relacionada**     | [SPEC-01](./specs.md#spec-01)                      |
| **Tipo**                 | backend                                            |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Wp-45                                              |
| **Pode paralelizar com** | Wp-47, Wp-51                                       |
| **Testes requeridos**    | unit / integration / manual                        |
| **Status**               | ✅ Concluido                                       |

**Escopo**
> Implementar contrato de cadastro mínimo local, validações server-side e emissão/restauração de sessão sem exigir campos de perfil completo.

**Definition of Ready**
> - [ ] Wp-45 concluído.
> - [ ] Requisitos de senha atuais identificados no backend.
> - [ ] Decisão sobre manter endpoint legado ou criar `/api/v1/auth/register` confirmada no código.

**Passos sugeridos de implementação**
> 1. Criar DTO de cadastro local com `nome`, `email`, `senha` e `confirmacaoSenha`.
> 2. Validar e-mail único, senha, confirmação e nome não vazio.
> 3. Persistir usuário com perfil parcial.
> 4. Emitir sessão/tokens conforme autenticação nativa existente.
> 5. Ajustar respostas de erro para senha fraca, confirmação divergente e e-mail duplicado.
> 6. Adicionar logs `auth_register_success` e `auth_register_failed` sem dados sensíveis.

**Critérios de aceite do pacote**
> - Cadastro local funciona sem CPF, telefone, gênero, data de nascimento ou endereço.
> - E-mail duplicado retorna conflito claro.
> - Senha fraca é rejeitada pelo backend.
> - Usuário recém-criado consegue consultar `/api/v1/auth/me`.

**Rollback**
> - `structuralAuth=false` restaura fluxo legado.
> - Endpoint novo pode permanecer publicado sem ser usado pelo frontend.

**Áreas impactadas**
> backend | auth | config/env

---

### Wp-47 — Auth social/local unificado por e-mail

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-47                                              |
| **Spec relacionada**     | [SPEC-02](./specs.md#spec-02)                      |
| **Tipo**                 | fullstack                                          |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Wp-45                                              |
| **Pode paralelizar com** | Wp-46, Wp-51                                       |
| **Testes requeridos**    | unit / integration / manual                        |
| **Status**               | ✅ Concluido                                       |

**Escopo**
> Unificar login local, Google e Facebook por e-mail verificado, com payload social separado, rotas públicas corretas e interceptor sem Authorization indevido em auth.

**Definition of Ready**
> - [ ] Wp-45 concluído.
> - [ ] Google Client ID e Facebook App ID disponíveis por ambiente.
> - [ ] Contrato atual dos endpoints sociais identificado.

**Passos sugeridos de implementação**
> 1. Garantir `permitAll` para `/api/v1/auth/**`, `/api/auth/**` e `OPTIONS /**`.
> 2. Ajustar filtro JWT para ignorar rotas públicas de auth.
> 3. Implementar validação Google por `idToken` ou `credential` e Facebook por `accessToken`.
> 4. Vincular provider à conta existente apenas com e-mail verificado.
> 5. Criar conta parcial quando e-mail verificado não existir.
> 6. Ajustar interceptor frontend para não anexar `Authorization` inválido em auth.
> 7. Logar falhas com motivo seguro e token fingerprint curto.

**Critérios de aceite do pacote**
> - Conta local e social com mesmo e-mail verificado resultam no mesmo usuário.
> - Social login não exige senha, CPF, telefone ou endereço.
> - Token ausente, audience inválida e e-mail não verificado retornam erro claro.
> - Rotas de auth não retornam 401 por bloqueio de Spring Security/JWT filter.

**Rollback**
> - `structuralAuth=false` desativa fluxo novo no frontend.
> - Backend mantém endpoints, mas pode rejeitar social por flag se necessário.
> - Vínculos já criados permanecem; rollback não remove contas.

**Áreas impactadas**
> backend | frontend | auth | config/env

---

### Wp-48 — UI de cadastro mínimo e perfil completo

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-48                                              |
| **Spec relacionada**     | [SPEC-01](./specs.md#spec-01)                      |
| **Tipo**                 | frontend                                           |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Wp-46                                              |
| **Pode paralelizar com** | Wp-49, Wp-51, Wp-52                                |
| **Testes requeridos**    | manual / lint / typecheck                          |
| **Status**               | ✅ Concluido                                       |

**Escopo**
> Reduzir cadastro para o card "Dados de Acesso", mover campos pessoais/profissionais/endereço para edição de perfil e corrigir validação visual de senha.

**Definition of Ready**
> - [ ] Wp-46 concluído.
> - [ ] Rotas e componentes atuais de cadastro/perfil mapeados.
> - [ ] Padrão visual existente identificado.

**Passos sugeridos de implementação**
> 1. Remover do cadastro os campos de tipo de usuário, dados pessoais, endereço, dados profissionais e foto.
> 2. Manter somente nome completo, e-mail, senha e confirmação.
> 3. Exibir texto fixo de requisitos abaixo do input de senha.
> 4. Exibir erro vermelho apenas após interação/submissão com requisito não atendido.
> 5. Garantir que os campos removidos existam na edição de perfil.
> 6. Ajustar responsividade mobile e textos pt-BR ABNT2.

**Critérios de aceite do pacote**
> - Cadastro mostra apenas o card "Dados de Acesso".
> - Senha fraca exibe feedback correto sem ruído antes de interação.
> - Perfil permite editar dados removidos do cadastro.
> - Não há mojibake nem texto sem acento correto.

**Rollback**
> - `structuralAuth=false` exibe fluxo legado.
> - Reverter componente de cadastro se flag não existir ainda.

**Áreas impactadas**
> frontend | auth | perfil

---

### Wp-49 — Publicação com tipo, endereço e geocoding cacheado

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-49                                              |
| **Spec relacionada**     | [SPEC-03](./specs.md#spec-03)                      |
| **Tipo**                 | backend / data                                     |
| **Estimativa**           | 3d                                                 |
| **Dependências**         | Wp-45                                              |
| **Pode paralelizar com** | Wp-48, Wp-51, Wp-52                                |
| **Testes requeridos**    | unit / integration / manual                        |
| **Status**               | ✅ Concluido                                       |

**Escopo**
> Implementar validação e persistência de `tipoPublicacao`, endereço obrigatório da publicação, coordenadas e geocoding cacheado/rate-limited.

**Definition of Ready**
> - [ ] Wp-45 concluído.
> - [ ] Política de uso do provider gratuito revisada.
> - [ ] Campos atuais de publicação mapeados.

**Passos sugeridos de implementação**
> 1. Ajustar DTO de criação/edição de publicação.
> 2. Validar `tipoPublicacao`, endereço e coordenadas.
> 3. Implementar serviço de geocoding com cache por hash normalizado.
> 4. Adicionar rate limit para Nominatim e User-Agent configurável.
> 5. Persistir provider/precisão de geocode.
> 6. Manter publicações antigas visíveis sem exigir coordenadas retroativas.

**Critérios de aceite do pacote**
> - Criar publicação sem endereço retorna validação.
> - Criar publicação com endereço e coordenadas válidas persiste dados novos.
> - Geocoding repetido usa cache.
> - Publicação antiga sem coordenadas continua listável.

**Rollback**
> - `publicationGeo=false` desativa geocoding/distância.
> - Dados novos permanecem no banco sem afetar leitura antiga.

**Áreas impactadas**
> banco | backend | publicações | config/env

---

### Wp-50 — Listagem com distância e rolagem de 20 cards

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-50                                              |
| **Spec relacionada**     | [SPEC-04](./specs.md#spec-04)                      |
| **Tipo**                 | fullstack                                          |
| **Estimativa**           | 3d                                                 |
| **Dependências**         | Wp-49                                              |
| **Pode paralelizar com** | Wp-51, Wp-52                                       |
| **Testes requeridos**    | integration / manual / lint / typecheck            |
| **Status**               | ✅ Concluido                                       |

**Escopo**
> Exibir "a X km de você" nos cards quando houver localização e limitar a tela principal a 20 cards por página/rolagem.

**Definition of Ready**
> - [ ] Wp-49 concluído.
> - [ ] Endpoints de listagem aceitam `lat`, `lng`, `page` e `size`.
> - [ ] Componentes de card/listagem mapeados.

**Passos sugeridos de implementação**
> 1. Ajustar endpoint de listagem para retornar `distanceKm` opcional.
> 2. Limitar `size` máximo a 20 na tela principal.
> 3. Implementar serviço frontend de localização atual e localização manual.
> 4. Adicionar estado para permissão negada/indisponível.
> 5. Exibir distância nos cards somente quando existir.
> 6. Revisar services para usar `environment.apiUrl` e remover hardcoded localhost.

**Critérios de aceite do pacote**
> - Listagem carrega 20 cards por requisição.
> - Com localização permitida, cards com coordenadas mostram distância.
> - Sem localização, listagem funciona sem distância.
> - Produção não chama `localhost` ou `127.0.0.1`.

**Rollback**
> - `publicationGeo=false` oculta distância e remove envio de coords.
> - Paginação de 20 pode permanecer por ser segura.

**Áreas impactadas**
> backend | frontend | publicações | config/env

---

### Wp-51 — Notificações dinâmicas e dropdown responsivo

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-51                                              |
| **Spec relacionada**     | [SPEC-05](./specs.md#spec-05)                      |
| **Tipo**                 | fullstack                                          |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Wp-45                                              |
| **Pode paralelizar com** | Wp-46, Wp-47, Wp-48, Wp-49                         |
| **Testes requeridos**    | integration / manual / lint / typecheck            |
| **Status**               | ✅ Concluido                                       |

**Escopo**
> Atualizar badge/lista de notificações sem clique, abrir dropdown com 5 últimas notificações e corrigir estados carregando/vazio sem depender de interação.

**Definition of Ready**
> - [ ] Wp-45 concluído.
> - [ ] Endpoints atuais de notificação identificados.
> - [ ] Estratégia polling/SSE escolhida para esta versão.

**Passos sugeridos de implementação**
> 1. Criar/ajustar estado global de notificações iniciado após auth resolvido.
> 2. Consumir contador e recentes com retry/backoff.
> 3. Abrir dropdown no clique do sino sem navegação direta.
> 4. Mostrar até 5 itens e link "Ver todas as notificações".
> 5. Corrigir transição de "Carregando..." para vazio/lista sem clique.
> 6. Ajustar largura/responsividade do dropdown.

**Critérios de aceite do pacote**
> - Nova notificação atualiza badge sem clique.
> - Dropdown mostra até 5 itens e link final.
> - Estado vazio aparece automaticamente.
> - Página de notificações não cai no login durante F5 com sessão válida.

**Rollback**
> - `dynamicNotifications=false` desliga polling novo.
> - Dropdown pode voltar a buscar dados ao abrir, mantendo rota antiga.

**Áreas impactadas**
> backend | frontend | notificações | auth

---

### Wp-52 — Chat responsivo e chamadas de contador

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-52                                              |
| **Spec relacionada**     | [SPEC-05](./specs.md#spec-05)                      |
| **Tipo**                 | frontend                                           |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Nenhuma                                            |
| **Pode paralelizar com** | Wp-45, Wp-46, Wp-47, Wp-49                         |
| **Testes requeridos**    | manual / lint / typecheck                          |
| **Status**               | ✅ Concluido                                       |

**Escopo**
> Corrigir layout mobile/desktop do chat e evitar chamadas contínuas de contador de chat fora do contexto necessário.

**Definition of Ready**
> - [ ] Rotas/componentes de chat mapeados.
> - [ ] Serviços que chamam `/api/v1/chat/nao-lidas` identificados.
> - [ ] Breakpoints existentes do projeto conhecidos.

**Passos sugeridos de implementação**
> 1. Ajustar layout mobile para alternar lista/conversa sem overflow horizontal.
> 2. Garantir desktop com lista e conversa sem sobreposição.
> 3. Revisar polling/chamadas de `/api/v1/chat/nao-lidas`.
> 4. Rodar contador apenas no chat ou por estado global consolidado quando necessário.
> 5. Corrigir textos longos, botões e áreas de toque.

**Critérios de aceite do pacote**
> - Chat não tem overflow horizontal em mobile.
> - Lista/conversa são navegáveis em telas pequenas.
> - Endpoint de não lidas não é chamado toda hora fora do chat sem necessidade.
> - Desktop mantém fluxo atual funcional.

**Rollback**
> - Reverter CSS/componentes de chat.
> - Manter endpoint intacto; mudança é majoritariamente frontend.

**Áreas impactadas**
> frontend | chat | notificações

---

### Wp-53 — Hardening, regressão e rollout

| Campo                    | Valor                                              |
| ------------------------ | -------------------------------------------------- |
| **ID**                   | Wp-53                                              |
| **Spec relacionada**     | [SPEC-06](./specs.md#spec-06)                      |
| **Tipo**                 | fullstack / qa / infra                             |
| **Estimativa**           | 2d                                                 |
| **Dependências**         | Wp-46, Wp-47, Wp-48, Wp-49, Wp-50, Wp-51, Wp-52    |
| **Pode paralelizar com** | —                                                  |
| **Testes requeridos**    | unit / integration / e2e / manual / build          |
| **Status**               | ✅ Concluido                                       |

**Escopo**
> Validar regressão completa, flags, rollback, produção sem localhost, logs seguros, ABNT2/UTF-8 e readiness para deploy.

**Definition of Ready**
> - [ ] Wp-46 até Wp-52 concluídos.
> - [ ] Ambientes local e produção/homologação acessíveis.
> - [ ] Checklist de smoke test definido.

**Passos sugeridos de implementação**
> 1. Rodar testes backend de auth, cadastro, publicações, geocode e notificações.
> 2. Rodar build/lint/typecheck frontend quando aplicável.
> 3. Fazer busca por `localhost`, `localhost:8080`, `http://localhost`, `127.0.0.1`.
> 4. Fazer busca por sequências típicas de mojibake em textos frontend.
> 5. Testar flags on/off por módulo.
> 6. Testar fluxo local, social, cadastro mínimo, publicar, distância, notificações e chat mobile.
> 7. Documentar rollout e rollback.

**Critérios de aceite do pacote**
> - Todos os fluxos críticos passam no smoke test.
> - Nenhuma chamada de produção aponta para localhost.
> - Flags desativam módulos novos sem quebrar navegação.
> - Logs permitem diagnosticar falhas sem expor tokens, senhas, CPF completo ou coordenadas precisas.

**Rollback**
> - Desligar flags por módulo.
> - Se necessário, redeploy da versão anterior.
> - Não executar rollback destrutivo de dados sem plano explícito.

**Áreas impactadas**
> backend | frontend | infra | qa | config/env

---

### Mapa de Dependências

```text
Wp-45 -> Wp-46 -> Wp-48 -> Wp-53
Wp-45 -> Wp-47 -> Wp-53
Wp-45 -> Wp-49 -> Wp-50 -> Wp-53
Wp-45 -> Wp-51 -> Wp-53
Wp-52 -> Wp-53
```

---

### Riscos e Pontos Desconhecidos

| # | Descrição | Probabilidade | Impacto | Mitigação |
|---|-----------|---------------|---------|-----------|
| R01 | Campos obrigatórios antigos ainda bloquearem cadastro mínimo | Alta | Alto | Wp-45 torna schema compatível e Wp-46 testa insert parcial |
| R02 | Vínculo social por e-mail não verificado permitir tomada de conta | Média | Alto | Wp-47 exige `email_verified` e valida token no provedor |
| R03 | `tipoUsuario` ainda influenciar regra de publicação | Alta | Alto | Wp-49 mapeia usos e migra regra para `tipoPublicacao` |
| R04 | Nominatim bloquear por excesso de chamadas | Média | Médio | Cache, rate limit, User-Agent e flag `publicationGeo` |
| R05 | Polling de notificações gerar carga | Média | Médio | Intervalo configurável, backoff, pausa por aba oculta e flag |
| R06 | Responsividade quebrar telas existentes | Média | Médio | Smoke mobile/desktop no Wp-53 |
| R07 | Build de produção manter URL localhost | Média | Alto | Scan obrigatório no Wp-50 e Wp-53 |
| R08 | Falta de Figma gerar divergência visual | Média | Médio | Seção 16 marcada como pendente; revisar com UX antes da implementação visual final |

---

### Oportunidades de Paralelização

| Grupo | WPs que podem rodar juntos | Pré-requisito para o grupo |
|-------|---------------------------|---------------------------|
| G1 | Wp-45, Wp-52 | Nenhum |
| G2 | Wp-46, Wp-47, Wp-49, Wp-51 | Wp-45 concluído |
| G3 | Wp-48, Wp-50, ajustes finais de Wp-51/Wp-52 | Wp-46 para cadastro; Wp-49 para listagem/distância |
| G4 | Wp-53 | Todos os WPs funcionais concluídos |

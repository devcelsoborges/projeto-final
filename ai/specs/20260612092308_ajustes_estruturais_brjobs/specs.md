# Especificações — Ajustes estruturais no BRJobs

> **Gerado em:** 2026-06-12  
> **Fonte primária:** [`briefings/briefing-tech.v1.md`](./briefings/briefing-tech.v1.md)  
> **Figma:** pendente; nenhuma URL visual foi fornecida nesta execução.

---

<a id="spec-01"></a>
## SPEC-01 — Cadastro mínimo e perfil parcial

**Objetivo**
> Entregar um cadastro local com fricção mínima, exigindo apenas nome completo, e-mail, senha e confirmação de senha, sem quebrar usuários existentes nem impedir edição posterior do perfil completo.

**Contexto**
> O cadastro atual exige dados pessoais, endereço, foto e informações profissionais antes da criação da conta. Isso reduz conversão e também conflita com login social, que não fornece CPF, telefone, endereço nem senha local.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Visitante | Cria conta local | Fluxo mais curto e com menos campos obrigatórios |
> | Usuário local | Entra e completa perfil depois | Pode usar a conta antes de preencher dados extras |
> | Frontend dev | Simplifica tela de cadastro | Move campos removidos para edição de perfil |
> | Backend dev | Ajusta validação e persistência | Permite perfil parcial com dados opcionais |

**Comportamento esperado**
> Fluxo principal:
> 1. Visitante abre a tela de cadastro.
> 2. Interface mostra apenas um card "Dados de Acesso".
> 3. Campos visíveis e obrigatórios: nome completo, e-mail, senha e confirmação de senha.
> 4. Texto fixo de requisitos da senha fica sempre abaixo do input de senha.
> 5. Mensagem de erro vermelha aparece somente após interação/submissão quando algum requisito falhar.
> 6. Backend cria usuário com perfil parcial e retorna sessão autenticada ou resposta compatível com o fluxo atual.
>
> Fluxo alternativo — e-mail já cadastrado:
> 1. Visitante informa e-mail existente.
> 2. Backend retorna conflito sem criar nova conta.
> 3. Frontend orienta o usuário a entrar na conta existente.
>
> Fluxo alternativo — perfil completo:
> 1. Usuário acessa edição de perfil.
> 2. Campos pessoais, endereço, foto e dados profissionais ficam disponíveis como opcionais, exceto validações específicas de formato quando preenchidos.

**Regras de negócio**
> - Cadastro local DEVE exigir somente `nome`, `email`, `senha` e `confirmacaoSenha`.
> - Cadastro local NÃO DEVE exigir CPF, telefone, gênero, data de nascimento, endereço, foto, função, especialidades ou resumo profissional.
> - Backend DEVE validar senha no servidor com os requisitos vigentes.
> - Frontend DEVE manter o texto de requisitos de senha sempre visível abaixo do input.
> - Frontend NÃO DEVE exibir erro vermelho antes de interação ou tentativa de submissão.
> - Campos de perfil removidos do cadastro DEVEM existir somente na tela de edição de perfil.
> - `tipoUsuario` DEVE permanecer no modelo como campo legado, sem bloquear cadastro mínimo.
> - E-mail DEVE continuar único.

**Contrato de API**
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------|-----------------|
> | POST | `/api/v1/auth/register` | Público + CSRF quando aplicável | `{ nome, email, senha, confirmacaoSenha }` | `201` usuário + sessão | `400`, `409`, `422` |
> | POST | `/api/usuarios/contratante` | Público + CSRF quando legado ativo | `{ nome, email, senha, confirmacaoSenha }` | `201` usuário + sessão | `400`, `409`, `422` |
> | GET | `/api/v1/auth/me` | Logado | — | `200` usuário autenticado | `401` |
> | PUT | `/api/usuarios/{id}` | Dono/admin | Dados completos do perfil | `200` usuário atualizado | `400`, `401`, `403`, `404` |

**SLA e Performance**
> - Cadastro local P95 até 800 ms sem upload de foto.
> - Validação de e-mail duplicado DEVE usar índice único.
> - Tela de cadastro NÃO DEVE carregar serviços de geolocalização, publicações ou chat.

**Observabilidade**
> - **Logar:** `auth_register_success` com `userId`, `provider=local`, `featureFlagVersion` — nível `info`.
> - **Logar:** `auth_register_failed` com `reason`, `provider=local`, `emailHash` — nível `warn`.
> - **Métrica:** `auth_register_success_total` — cadastros locais concluídos.
> - **Alerta:** sem alerta externo nesta fase; logs devem permitir busca manual.

**Critérios de aceite**
> - DADO um visitante sem conta QUANDO preencher nome, e-mail, senha válida e confirmação igual ENTÃO a conta será criada sem exigir CPF, telefone ou endereço.
> - DADO uma senha fraca QUANDO o usuário submeter o cadastro ENTÃO o frontend exibirá erro vermelho e o backend rejeitará a criação.
> - DADO um e-mail já cadastrado QUANDO o visitante tentar criar nova conta ENTÃO o backend retornará conflito e nenhuma conta duplicada será criada.
> - DADO um usuário recém-cadastrado QUANDO acessar editar perfil ENTÃO os campos pessoais, endereço e profissionais estarão disponíveis para preenchimento opcional.

**Estado atual**
> O cadastro contém campos demais e validações obrigatórias incompatíveis com perfil parcial e login social. Parte das colunas já foi relaxada em produção, mas a feature precisa padronizar contrato, UI e testes.

**Mudanças necessárias**
> - **Banco de dados:** garantir nullable para CPF, telefone, gênero, data de nascimento, endereço e campos profissionais; manter índice único de e-mail.
> - **Backend:** criar ou ajustar endpoint de cadastro mínimo; separar DTO de cadastro local do DTO social; validar senha/e-mail server-side.
> - **Frontend:** reduzir tela de cadastro para o card "Dados de Acesso"; mover campos removidos para edição de perfil; revisar mensagens em pt-BR ABNT2.
> - **Infra/Config:** adicionar flag `structuralAuth` no mecanismo adotado pelo projeto.

**Definição de pronto**
> - [ ] Cadastro mínimo funcional em desktop e mobile.
> - [ ] Campos removidos aparecem somente em edição de perfil.
> - [ ] Validações de senha existem no frontend e no backend.
> - [ ] E-mail duplicado retorna mensagem clara.
> - [ ] Testes cobrindo cadastro mínimo, senha inválida e e-mail duplicado.
> - [ ] Scan de textos sem sequências típicas de mojibake concluído.

---

<a id="spec-02"></a>
## SPEC-02 — Autenticação local/social unificada por e-mail verificado

**Objetivo**
> Garantir que login local, Google e Facebook usem a mesma conta quando o e-mail for igual e verificado, sem exigir dados que provedores sociais não fornecem.

**Contexto**
> A autenticação foi migrada de Auth0 para nativa. O sistema precisa impedir duplicidade de contas, aceitar payload social próprio e evitar que endpoints públicos de auth sejam bloqueados por JWT, CSRF mal configurado ou interceptor Angular.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Visitante | Entra por e-mail/senha, Google ou Facebook | Usa uma única identidade por e-mail |
> | Usuário social | Autoriza provedor externo | Não informa CPF, telefone, endereço ou senha |
> | Usuário local | Pode vincular social login por e-mail verificado | Mantém histórico e dados da conta |
> | Backend dev | Valida token e vínculo | Evita tomada de conta |
> | Frontend dev | Envia payload correto | Não injeta Authorization indevido em rotas públicas |

**Comportamento esperado**
> Fluxo principal — login local:
> 1. Usuário informa e-mail e senha.
> 2. Backend autentica credenciais locais.
> 3. Backend emite access token e refresh token conforme política vigente.
>
> Fluxo principal — login social:
> 1. Frontend obtém token do Google ou Facebook.
> 2. Frontend envia payload social separado, sem CPF, telefone, endereço ou senha.
> 3. Backend valida token diretamente com o provedor ou biblioteca oficial.
> 4. Backend confere `audience/clientId`, `providerId`, e-mail e `email_verified` quando disponível.
> 5. Se e-mail verificado já existe, backend vincula provider à conta existente.
> 6. Se e-mail verificado não existe, backend cria conta parcial.
> 7. Backend emite a mesma sessão usada no login local.
>
> Fluxo alternativo — token inválido:
> 1. Backend rejeita login social.
> 2. Resposta informa motivo seguro, sem logar token completo.

**Regras de negócio**
> - Login social DEVE usar DTO separado do cadastro local.
> - Login social NÃO DEVE exigir senha, CPF, telefone ou endereço.
> - Backend DEVE validar token social antes de criar ou vincular conta.
> - Backend DEVE vincular conta por e-mail somente quando o provedor confirmar e-mail verificado.
> - Backend NÃO DEVE confiar em nome, avatar ou e-mail enviados como metadados pelo frontend sem validação do token.
> - Metadados sociais PODEM preencher nome/avatar inicial quando vierem do token validado.
> - Endpoint `/api/v1/auth/**` DEVE ser público no Spring Security.
> - Filtro JWT DEVE ignorar `OPTIONS`, `/api/v1/auth/**` e `/api/auth/**`.
> - Interceptor frontend NÃO DEVE enviar `Authorization` inválido para rotas públicas de autenticação.
> - Google Client ID usado no backend DEVE ser o mesmo do frontend.

**Contrato de API**
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------|-----------------|
> | POST | `/api/v1/auth/login` | Público + CSRF quando aplicável | `{ email, senha }` | `200` usuário + tokens/cookies | `400`, `401`, `403` |
> | POST | `/api/v1/auth/social/google` | Público + CSRF quando aplicável | `{ idToken }` ou `{ credential }` | `200` usuário + tokens/cookies | `400`, `401`, `422` |
> | POST | `/api/v1/auth/social/facebook` | Público + CSRF quando aplicável | `{ accessToken }` | `200` usuário + tokens/cookies | `400`, `401`, `422` |
> | POST | `/api/v1/auth/refresh` | Refresh válido | — | `200` nova sessão | `401` |
> | POST | `/api/v1/auth/logout` | Logado | — | `204` sessão encerrada | `401` |

**SLA e Performance**
> - Login local P95 até 500 ms.
> - Login social P95 até 1500 ms, considerando chamada ao provedor.
> - Timeout de validação externa até 5 s.
> - Falhas do provedor DEVEM retornar erro claro sem travar a UI.

**Observabilidade**
> - **Logar:** `auth_social_login_success` com `userId`, `provider`, `linkedExisting` — nível `info`.
> - **Logar:** `auth_social_login_failed` com `provider`, `reason`, `tokenFingerprint` — nível `warn`.
> - **Logar:** `social_account_linked` com `userId`, `provider` — nível `info`.
> - **Métrica:** `auth_social_login_success_total` por provider.
> - **Métrica:** `auth_social_link_total` por provider.
> - **Alerta:** sem alerta externo; logs devem revelar `clientId inválido`, `token ausente`, `audience inválida` ou `email não verificado`.

**Critérios de aceite**
> - DADO uma conta local existente com e-mail verificado pelo Google QUANDO o usuário entrar com Google ENTÃO o provider será vinculado à mesma conta.
> - DADO uma tentativa social sem token QUANDO chamar `/api/v1/auth/social/google` ENTÃO o backend retornará erro claro sem exigir JWT da aplicação.
> - DADO um token Google com audience diferente QUANDO tentar login ENTÃO o backend rejeitará com motivo seguro.
> - DADO uma rota `/api/v1/auth/**` QUANDO o interceptor processar a requisição ENTÃO nenhum `Authorization` inválido será anexado.
> - DADO uma requisição `OPTIONS` para auth QUANDO chegar no backend ENTÃO ela será permitida pela segurança.

**Estado atual**
> O projeto já possui autenticação nativa e endpoints sociais em produção, mas a feature precisa consolidar contratos, vínculo por e-mail verificado, comportamento do filtro JWT e payloads por provider.

**Mudanças necessárias**
> - **Banco de dados:** tabela `social_logins` com `provider`, `provider_id`, `user_id`, `email_verified`, unicidade por provider/providerId e por user/provider.
> - **Backend:** validar Google/Facebook, normalizar DTOs, garantir rotas públicas, logs seguros e client IDs por ambiente.
> - **Frontend:** enviar `idToken/credential` para Google e `accessToken` para Facebook; remover `Authorization` em auth; tratar erros por provider.
> - **Infra/Config:** variáveis `GOOGLE_CLIENT_ID`, `FACEBOOK_APP_ID`, secrets fortes e CORS de produção.

**Definição de pronto**
> - [ ] Login local, Google e Facebook criam ou reutilizam a mesma conta por e-mail verificado.
> - [ ] Payload social não contém CPF, telefone, endereço nem senha.
> - [ ] Rotas de auth públicas não são bloqueadas por JWT ou interceptor.
> - [ ] Logs não expõem tokens completos.
> - [ ] Testes cobrem conta existente, nova conta social, token ausente, audience inválida e e-mail não verificado.

---

<a id="spec-03"></a>
## SPEC-03 — Publicações com tipo, endereço obrigatório e coordenadas

**Objetivo**
> Migrar a regra de contratar/prestar para a publicação, exigindo endereço próprio da publicação e coordenadas suficientes para cálculo de distância.

**Contexto**
> `tipoUsuario` está acoplado ao usuário, mas o comportamento desejado é definir se cada publicação representa contratação ou prestação. Endereço de perfil passa a ser opcional, então a publicação precisa ter localização própria.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Publicador | Cria publicação de contratação ou prestação | Informa tipo e endereço da publicação |
> | Leitor | Vê cidade/UF e futuramente distância | Entende onde o serviço ocorre |
> | Backend dev | Ajusta entidade e validação | Separa regra nova de `tipoUsuario` legado |
> | Frontend dev | Ajusta card/formulário de publicar | Exige endereço no fluxo correto |

**Comportamento esperado**
> Fluxo principal:
> 1. Usuário logado abre o fluxo de publicar serviço.
> 2. Interface exige `tipoPublicacao`: `CONTRATACAO` ou `PRESTACAO`.
> 3. Interface exige endereço da publicação, independente do endereço do perfil.
> 4. Sistema obtém coordenadas por localização atual, endereço manual ou geocoding cacheado.
> 5. Backend valida tipo, endereço e coordenadas válidas.
> 6. Publicação é salva com endereço e lat/lng.
>
> Fluxo alternativo — publicação antiga sem coordenadas:
> 1. Card continua aparecendo.
> 2. Distância não é exibida até haver coordenadas.

**Regras de negócio**
> - `tipoUsuario` DEVE permanecer como legado.
> - Regra contratar/prestar DEVE ser definida em `tipoPublicacao`.
> - Publicação DEVE exigir endereço próprio.
> - Publicação DEVE armazenar latitude e longitude quando a feature de geo estiver ativa.
> - Backend DEVE validar latitude entre `-90` e `90` e longitude entre `-180` e `180`.
> - Backend NÃO DEVE usar endereço do perfil como substituto silencioso sem confirmação do usuário.
> - Geocoding externo DEVE usar cache e rate limit.
> - Nominatim DEVE respeitar política pública de uso, com User-Agent/Referer e limite mínimo de intervalo.

**Contrato de API**
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------|-----------------|
> | POST | `/api/v1/publicacoes` | Logado | `{ titulo, descricao, tipoPublicacao, enderecoPublicacao, cidadePublicacao, estadoPublicacao, latitude, longitude }` | `201` publicação | `400`, `401`, `422` |
> | PUT | `/api/v1/publicacoes/{id}` | Dono/admin | Dados editáveis da publicação | `200` publicação atualizada | `400`, `401`, `403`, `404` |
> | POST | `/api/v1/geocode` | Logado | `{ endereco, cidade, estado, cep }` | `200 { lat, lng, precision, source }` | `400`, `401`, `429`, `503` |

**SLA e Performance**
> - Criação de publicação sem geocoding externo P95 até 800 ms.
> - Geocoding externo P95 até 3 s com timeout controlado.
> - Cache de geocoding DEVE evitar chamadas repetidas para o mesmo endereço normalizado.
> - Nominatim DEVE respeitar intervalo mínimo configurável, recomendado `1100 ms`.

**Observabilidade**
> - **Logar:** `publication_created` com `publicationId`, `userId`, `tipoPublicacao`, `hasCoordinates` — nível `info`.
> - **Logar:** `geocode_cache_hit` com `addressHash`, `provider` — nível `debug`.
> - **Logar:** `geocode_failed` com `provider`, `reason`, `addressHash` — nível `warn`.
> - **Métrica:** `geocode_request_total` e `geocode_cache_hit_total`.
> - **Alerta:** sem alerta externo; flag `publicationGeo` deve permitir desligar geocoding.

**Critérios de aceite**
> - DADO um usuário logado QUANDO criar publicação sem endereço ENTÃO o backend rejeitará com erro de validação.
> - DADO um usuário logado QUANDO criar publicação com tipo, endereço e coordenadas válidas ENTÃO a publicação será persistida.
> - DADO uma publicação antiga sem coordenadas QUANDO exibida em listagem ENTÃO ela continuará visível sem texto de distância.
> - DADO duas publicações do mesmo usuário QUANDO uma for contratação e outra prestação ENTÃO cada uma manterá seu próprio `tipoPublicacao`.

**Estado atual**
> Publicações dependem de regras legadas associadas ao usuário e não possuem contrato completo de endereço/coords para distância. Endereço de perfil foi tornado opcional e não deve ser fonte única da publicação.

**Mudanças necessárias**
> - **Banco de dados:** adicionar `tipo_publicacao`, campos de endereço da publicação, `latitude`, `longitude`, `geocode_provider`, `geocode_precision`; criar `geocode_cache`.
> - **Backend:** validar publicação, normalizar endereço, integrar cache/geocoding, manter compatibilidade de leitura.
> - **Frontend:** exigir tipo e endereço no card/formulário de publicar; capturar coordenadas quando permitido.
> - **Infra/Config:** configurar provider, URL base, User-Agent e TTL de cache.

**Definição de pronto**
> - [ ] Publicação exige tipo e endereço.
> - [ ] Publicações antigas continuam listáveis.
> - [ ] Coordenadas são persistidas e validadas.
> - [ ] Geocoding externo possui cache, rate limit e rollback por flag.
> - [ ] Testes cobrem criação válida, endereço ausente, coordenada inválida e publicação legada.

---

<a id="spec-04"></a>
## SPEC-04 — Distância nos cards e listagem de 20 itens por rolagem

**Objetivo**
> Exibir distância em quilômetros nos cards de publicação quando houver localização do usuário e carregar a tela principal em páginas de 20 cards por rolagem.

**Contexto**
> Usuários precisam comparar serviços próximos. A solução deve usar localização atual mediante permissão ou localização manual, sem bloquear a listagem quando a permissão for negada.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Visitante | Navega publicações | Pode ver distância se permitir localização |
> | Usuário logado | Usa localização atual ou manual | Recebe listagem mais útil |
> | Publicador | Aparece em listagens por distância | Publicações com coordenadas têm mais contexto |
> | Frontend dev | Implementa estado de localização e rolagem | Evita chamadas hardcoded e overflow |
> | Backend dev | Calcula ou retorna distância | Mantém paginação estável |

**Comportamento esperado**
> Fluxo principal:
> 1. Listagem carrega primeiras 20 publicações.
> 2. Sistema solicita localização apenas por ação ou momento permitido pela UX.
> 3. Se usuário permitir, frontend envia `lat` e `lng` nas consultas.
> 4. Backend retorna `distanceKm` quando publicação e usuário têm coordenadas.
> 5. Card exibe "a X km de você".
> 6. Sentinel de scroll carrega a próxima página de até 20 itens.
>
> Fluxo alternativo — localização negada:
> 1. Sistema não exibe distância.
> 2. Usuário pode informar localização manual.
> 3. Após localização manual válida, cards passam a exibir distância.

**Regras de negócio**
> - Listagem principal DEVE usar page size 20.
> - Frontend DEVE ocultar distância quando `distanceKm` vier ausente.
> - Backend DEVE arredondar distância para formato adequado à UI, sem expor precisão excessiva do usuário.
> - Sistema DEVE permitir uso sem geolocalização.
> - Sistema DEVE oferecer localização manual quando permissão for negada ou indisponível.
> - Nenhuma URL de API no frontend DEVE apontar para `localhost` em build de produção.
> - Requisições DEVEM usar `environment.apiUrl` ou mecanismo equivalente do projeto.

**Contrato de API**
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------|-----------------|
> | GET | `/api/v1/publicacoes?page={n}&size=20&lat={lat}&lng={lng}&tipoPublicacao={tipo}` | Público | Query params | `200 { content, page, size, hasNext }` com `distanceKm` opcional | `400`, `500` |
> | GET | `/api/v1/publicacoes/{id}` | Público | — | `200` detalhe da publicação | `404` |
> | POST | `/api/v1/geocode` | Logado ou permitido pela política | Endereço manual | `200 { lat, lng }` | `400`, `429`, `503` |

**SLA e Performance**
> - Primeira página P95 até 1000 ms.
> - Próxima página P95 até 1000 ms.
> - Cálculo de distância DEVE usar fórmula estável, como Haversine, ou função geográfica equivalente.
> - Índices e limites DEVEM evitar retorno acima de 20 cards na tela principal.

**Observabilidade**
> - **Logar:** `publication_list_distance_requested` com `hasUserLocation`, `page`, `size` — nível `debug`.
> - **Métrica:** `publication_list_latency_ms` — latência da listagem.
> - **Métrica:** `publication_distance_available_ratio` — percentual de cards com distância.
> - **Alerta:** sem alerta externo; flag `publicationGeo` deve ocultar distância em rollback.

**Critérios de aceite**
> - DADO um usuário com localização permitida QUANDO abrir a listagem ENTÃO os cards com coordenadas exibirão "a X km de você".
> - DADO um usuário sem localização QUANDO abrir a listagem ENTÃO os cards serão exibidos sem distância e sem erro.
> - DADO a página principal QUANDO o usuário rolar até o final ENTÃO serão carregados no máximo 20 novos cards por requisição.
> - DADO uma build de produção QUANDO qualquer service Angular chamar API ENTÃO a URL base será `https://api.brjobs.com.br` ou config equivalente, nunca `localhost`.

**Estado atual**
> A listagem não possui distância confiável em cards e já houve ocorrência de rotas hardcoded para `localhost`. A nova solução precisa padronizar URL base, paginação e estados de localização.

**Mudanças necessárias**
> - **Banco de dados:** índices para coordenadas e campos usados em filtros.
> - **Backend:** retornar `distanceKm` opcional; limitar `size` máximo; validar queries de latitude/longitude.
> - **Frontend:** criar serviço de localização, fallback manual, estado persistido e infinite scroll com 20 itens.
> - **Infra/Config:** garantir environments de desenvolvimento e produção corretos.

**Definição de pronto**
> - [ ] Cards exibem distância somente quando dados são suficientes.
> - [ ] Localização negada não quebra listagem.
> - [ ] Scroll carrega 20 cards por página.
> - [ ] Produção não contém chamadas para `localhost` ou `127.0.0.1`.
> - [ ] Testes/manuais cobrem localização permitida, negada, manual e página seguinte.

---

<a id="spec-05"></a>
## SPEC-05 — Notificações dinâmicas, dropdown e chat responsivo

**Objetivo**
> Atualizar notificações sem depender de clique no sino, mostrar dropdown com as 5 últimas notificações e corrigir responsividade de notificações e chat em mobile/desktop.

**Contexto**
> Hoje notificações só aparecem ou atualizam após clique. A página de notificações também apresentou comportamento indevido em refresh. O chat e o dropdown têm problemas de layout responsivo.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Usuário logado | Recebe notificações e mensagens | Vê badge e lista atualizados automaticamente |
> | Usuário no chat | Lê e envia mensagens | Usa layout sem overflow em mobile |
> | Frontend dev | Ajusta estado global e responsividade | Evita depender de clique e corrige F5 |
> | Backend dev | Expõe contadores/listas estáveis | Reduz polling desnecessário |

**Comportamento esperado**
> Fluxo principal — notificações:
> 1. Após autenticação resolvida, um estado de notificações inicia atualização automática.
> 2. Badge do header reflete contador sem clique do usuário.
> 3. Clique no sino abre dropdown, sem navegar direto para a página.
> 4. Dropdown mostra até 5 notificações recentes e opção "Ver todas as notificações".
> 5. Estado vazio muda automaticamente para "Nenhuma notificação" quando a API retornar lista vazia.
>
> Fluxo principal — página de notificações:
> 1. Usuário acessa `/notificacoes`.
> 2. Refresh (`F5`) mantém rota protegida após restauração da sessão.
> 3. Tela não redireciona para login se `auth/me` ainda estiver carregando.
>
> Fluxo principal — chat:
> 1. Desktop mostra lista e conversa conforme layout existente.
> 2. Mobile separa lista/conversa sem overflow horizontal.
> 3. Chamadas de contador de chat não rodam continuamente fora de contexto sem necessidade.

**Regras de negócio**
> - Notificações DEVEM atualizar sem clique no sino quando usuário estiver logado.
> - Dropdown DEVE mostrar no máximo 5 notificações.
> - Dropdown DEVE ter ação para abrir todas as notificações.
> - Estado "Carregando..." NÃO DEVE depender de clique para sair.
> - Página protegida NÃO DEVE redirecionar para login enquanto sessão ainda está em verificação.
> - Polling DEVE pausar ou reduzir quando aba não estiver visível.
> - Contador de chat NÃO DEVE ser chamado continuamente fora do chat se a informação já estiver coberta pelo estado de notificações.
> - Dropdown DEVE usar largura máxima segura em mobile: `min(360px, calc(100vw - 16px))`.

**Contrato de API**
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------|-----------------|
> | GET | `/api/v1/notificacoes/recentes?limit=5` | Logado | — | `200` lista de notificações | `401`, `500` |
> | GET | `/api/v1/notificacoes/nao-lidas` | Logado | — | `200 { count }` | `401`, `500` |
> | GET | `/api/v1/notificacoes` | Logado | paginação/filtros | `200` página completa | `401`, `500` |
> | GET | `/api/v1/chat/nao-lidas` | Logado | — | `200 { count }` | `401`, `500` |

**SLA e Performance**
> - Polling inicial recomendado: 15 s, configurável.
> - Dropdown deve renderizar em menos de 100 ms após dados em cache.
> - Falha de polling deve usar backoff e não exibir alertas invasivos.
> - Mobile não deve ter scroll horizontal em chat/dropdown.

**Observabilidade**
> - **Logar:** `notification_poll_failed` com `userId`, `reason` — nível `debug/warn`.
> - **Métrica:** `notification_poll_latency_ms` — latência da atualização.
> - **Métrica:** `notification_unread_count` — contador retornado por usuário, quando aplicável.
> - **Alerta:** sem alerta externo; flag `dynamicNotifications` deve desligar polling.

**Critérios de aceite**
> - DADO um usuário logado QUANDO receber nova notificação ENTÃO o badge será atualizado sem clique no sino.
> - DADO um usuário logado QUANDO clicar no sino ENTÃO abrirá dropdown com até 5 notificações e link para ver todas.
> - DADO nenhuma notificação QUANDO a API retornar lista vazia ENTÃO a UI mudará automaticamente para "Nenhuma notificação".
> - DADO a página `/notificacoes` QUANDO o usuário pressionar F5 ENTÃO a sessão será restaurada antes de qualquer redirecionamento para login.
> - DADO um viewport mobile QUANDO abrir chat ou dropdown ENTÃO não haverá overflow horizontal nem texto vazando.

**Estado atual**
> Notificações dependem de clique para atualizar, estado carregando pode ficar preso até novo clique e a página protegida pode cair no login durante refresh. Chat e dropdown precisam de ajustes de layout.

**Mudanças necessárias**
> - **Banco de dados:** sem schema obrigatório, salvo otimização de consultas por usuário/data/leitura.
> - **Backend:** garantir endpoints de recentes, contador e página completa; padronizar respostas vazias.
> - **Frontend:** criar/ajustar `NotificationStateService`, guards aguardando auth loading, dropdown controlado e CSS responsivo do chat.
> - **Infra/Config:** flag `dynamicNotifications`, intervalo configurável e logs seguros.

**Definição de pronto**
> - [ ] Badge atualiza sem clique.
> - [ ] Dropdown abre sem navegar direto e mostra últimas 5 notificações.
> - [ ] Página de notificações sobrevive a refresh autenticado.
> - [ ] Chat e dropdown passam em teste visual mobile/desktop.
> - [ ] Polling tem backoff, pausa por visibilidade e não chama contador de chat sem necessidade.

---

<a id="spec-06"></a>
## SPEC-06 — Feature flags, rollback, testes e hardening

**Objetivo**
> Controlar a reestruturação por flags, garantir rollback por módulo, validar produção sem rotas locais e cobrir regressões de auth, cadastro, publicações, localização, notificações e responsividade.

**Contexto**
> A feature afeta fluxos centrais em produção. O rollout deve ser reversível, com mudanças de banco compatíveis, logs seguros e testes mínimos antes de ativação ampla.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Operação/admin técnico | Liga/desliga flags | Controla rollout e rollback |
> | Backend dev | Garante compatibilidade e logs | Reduz risco em produção |
> | Frontend dev | Valida URLs, ABNT2 e responsividade | Evita regressão visível |
> | QA/dev | Executa regressão | Confirma fluxos críticos |

**Comportamento esperado**
> Fluxo principal:
> 1. Backend e frontend leem flags de auth estrutural, geolocalização de publicações e notificações dinâmicas.
> 2. Flags desligadas mantêm comportamento legado seguro.
> 3. Flags ligadas ativam novas telas, contratos e respostas.
> 4. Testes automatizados e manuais validam os fluxos críticos.
> 5. Produção não contém URLs hardcoded para `localhost`.
>
> Fluxo de rollback:
> 1. Operação desliga flag afetada.
> 2. Sistema deixa de exibir/usar o módulo novo.
> 3. Dados novos permanecem no banco sem quebrar leitura legada.

**Regras de negócio**
> - `structuralAuth`, `publicationGeo` e `dynamicNotifications` DEVEM ser flags independentes.
> - Rollback por flag DEVE funcionar sem rollback imediato de banco.
> - Colunas novas DEVEM ser nullable quando não forem críticas.
> - Produção NÃO DEVE conter URL `localhost`, `localhost:8080` ou `127.0.0.1` em services frontend.
> - Textos visíveis no frontend DEVEM seguir pt-BR ABNT2 e UTF-8 correto.
> - Logs NÃO DEVEM conter senha, CPF completo, token completo ou coordenada precisa do usuário.
> - CORS e security config DEVEM permitir origens oficiais e `OPTIONS`.

**Contrato de API**
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------|-----------------|
> | GET | `/api/v1/config/public` | Público | — | `200` flags públicas permitidas | `500` |
> | OPTIONS | `/**` | Público | — | `200/204` preflight permitido | — |
> | GET | `/actuator/health` ou equivalente | Público/restrito conforme ambiente | — | `200` health | `503` |

**SLA e Performance**
> - Avaliação de flags deve ser local/in-memory ou config já carregada.
> - Healthcheck deve responder em até 500 ms.
> - Test suite de regressão crítica deve rodar em tempo aceitável para PR, idealmente abaixo de 10 min.

**Observabilidade**
> - **Logar:** `feature_flag_evaluated` com `flag`, `enabled` — nível `debug`.
> - **Logar:** falhas de CORS/auth pública com `path`, `method`, `origin`, sem tokens — nível `warn`.
> - **Métrica:** counters de sucesso/falha dos fluxos críticos já definidos nas specs anteriores.
> - **Alerta:** sem alerta externo nesta fase; documentação deve indicar como buscar logs por evento.

**Critérios de aceite**
> - DADO `structuralAuth=false` QUANDO o frontend carregar ENTÃO o fluxo legado seguro ficará disponível.
> - DADO `publicationGeo=false` QUANDO abrir listagem ENTÃO cards não exibirão distância e a listagem continuará funcionando.
> - DADO `dynamicNotifications=false` QUANDO usuário logado navegar ENTÃO polling novo será desativado sem quebrar a página.
> - DADO uma build de produção QUANDO buscar por `localhost` e `127.0.0.1` nos bundles/services ENTÃO nenhuma chamada de API hardcoded será encontrada.
> - DADO logs de auth/social QUANDO uma falha ocorrer ENTÃO o motivo será identificável sem expor token completo.

**Estado atual**
> Há ajustes prévios de produção, mas a nova reestruturação precisa padronizar flags, testes e checklist de rollout para evitar regressão em fluxos críticos.

**Mudanças necessárias**
> - **Banco de dados:** migrations compatíveis e reversíveis quando possível; não remover `tipoUsuario`.
> - **Backend:** flags, CORS, security pública para auth, logs seguros, testes de handlers/services.
> - **Frontend:** environments corretos, scan contra localhost, ABNT2/mojibake, testes de layout e smoke manual.
> - **Infra/Config:** documentar env vars, ordem de deploy, rollback e valores por ambiente.

**Definição de pronto**
> - [ ] Flags independentes implementadas e documentadas.
> - [ ] Rollback por flag validado.
> - [ ] Testes backend executados para auth, cadastro, publicações e notificações.
> - [ ] Build/lint/typecheck frontend executados quando houver mudança de UI.
> - [ ] Scan por `localhost`, `127.0.0.1` e sequências típicas de mojibake concluído.
> - [ ] Checklist de produção registrado no PR ou documento de rollout.

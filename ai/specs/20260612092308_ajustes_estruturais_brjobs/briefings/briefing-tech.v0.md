# Briefing Técnico — Ajustes estruturais no BRJobs

> **Versão:** 0.1
> **Status:** Rascunho
> **Gerado em:** 2026-06-12
> **Baseado em:**
>   - [`../discovery.md`](../discovery.md) — discovery de 2026-06-12

---

## 1. Contexto e Problema

BRJobs está em produção e possui fluxos centrais com fricção e inconsistência:

- cadastro exige dados pessoais, endereço e informações profissionais cedo demais;
- login social e login local precisam convergir para a mesma conta por email;
- `tipoUsuario` está acoplado ao usuário, mas o comportamento correto é definir contratar/prestar no ato da publicação;
- notificações não atualizam dinamicamente e dependem de clique no botão;
- chat e dropdown de notificações têm responsividade ruim;
- cards de publicação não exibem distância real do usuário;
- publicação deve ter endereço próprio obrigatório, independente do endereço do perfil.

Impacto:

- menor conversão no cadastro;
- risco de contas duplicadas;
- baixa confiança em notificações;
- navegação menos útil para quem busca serviços próximos;
- manutenção difícil por regras antigas misturadas com fluxos novos.

---

## 2. Solução Proposta

Construir uma reestruturação incremental dos fluxos de cadastro, autenticação, perfil, publicações, notificações, chat e localização.

Resumo:

- cadastro mínimo com nome, email, senha e confirmação;
- perfil passa a concentrar dados pessoais, endereço, foto e dados profissionais;
- conta única por email para login local e social;
- payload social separado, sem CPF/telefone/endereço/senha;
- `tipoUsuario` mantido como legado;
- publicação define `tipoPublicacao` como contratação ou prestação;
- publicação exige endereço e coordenadas para cálculo de distância;
- cards mostram distância "a X quilômetros de você";
- listagem principal carrega 20 cards por rolagem;
- notificações passam a atualizar sem clique;
- chat e dropdown de notificações corrigidos para mobile/desktop;
- feature flag controla rollout e rollback.

Retrocompatibilidade:

- dados antigos podem ser migrados sem preservar `tipoUsuario` como regra principal;
- campo `tipoUsuario` continua existindo para leitura/compatibilidade;
- endpoints antigos devem permanecer funcionando até substituição completa;
- novos campos devem ser nullable quando não forem críticos.

---

## 3. Personas e Papéis Afetados

| Papel | Ação que realiza | Impacto da feature |
|-------|------------------|--------------------|
| Visitante | Acessa cadastro, login e listagens públicas | Cadastro mais curto; pode ver distância se permitir localização |
| Usuário local | Entra com email/senha | Conta passa a ser única por email e compatível com social |
| Usuário social | Entra com Google/Facebook | Pode criar/vincular conta sem CPF, telefone ou senha |
| Usuário logado | Usa notificações, chat, perfil e publicações | Recebe contadores dinâmicos, chat responsivo e perfil editável |
| Publicador | Cria publicação de contratação/prestação | Deve informar endereço da publicação e tipo de publicação |
| Leitor de publicações | Navega cards/listagem | Vê distância em km por localização real ou manual |
| Operação/admin técnico | Mantém deploy, config e rollback | Usa feature flags e env vars para controlar ativação |
| Backend dev | Ajusta entidades, serviços, auth e endpoints | Deve preservar dados e evitar quebra de produção |
| Frontend dev | Ajusta telas, services e responsividade | Deve seguir ABNT2/pt-BR e usar `environment.apiUrl` |

---

## 4. Premissas, Restrições e Decisões Tomadas

- **Produto em produção:** mudanças precisam ser reversíveis por feature flag.
- **Todos os usuários podem ser afetados:** rollout deve ser controlado.
- **Cadastro mínimo:** somente nome completo, email, senha e confirmação são obrigatórios.
- **Perfil completo pós-cadastro:** dados pessoais, endereço, foto e dados profissionais saem do cadastro.
- **Conta única por email:** social/local convergem para mesmo `usuarios.email`.
- **Social payload separado:** Google/Facebook não enviam CPF, telefone, endereço nem senha.
- **Email social precisa ser verificado:** vinculação automática só pode ocorrer quando provedor confirmar email.
- **`tipoUsuario` legado:** campo permanece, mas regra de negócio migra para publicação.
- **Endereço de publicação obrigatório:** perfil pode não ter endereço; publicação deve ter localização própria.
- **Geo gratuito:** usar Geolocation API + fallback manual; Nominatim só cacheado e respeitando política.
- **Listagem principal:** 20 cards por rolagem.
- **Sem observabilidade dedicada:** logs/métricas mínimas entram na implementação, sem alertas externos nesta fase.
- **Textos frontend:** pt-BR ABNT2, UTF-8 correto, sem mojibake.
- **Produção:** nenhuma URL hardcoded para `localhost`.

---

## 5. Arquitetura e Fluxos

### 5.1 Fluxos principais

Cadastro local:

```text
Visitante
  -> RegisterComponent
  -> POST /api/usuarios/contratante ou endpoint v2 de cadastro mínimo
  -> UsuarioService valida nome/email/senha
  -> usuarios com perfil parcial
  -> AuthSessionService emite sessão
  -> Home
```

Login local/social unificado:

```text
Usuário
  -> LoginComponent
  -> email/senha OU Google/Facebook token
  -> backend localiza usuarios.email
  -> se social: valida provider token + email_verified
  -> cria/vincula social_logins
  -> AuthSessionService emite ACCESS_TOKEN/REFRESH_TOKEN
  -> mesma conta independente do provider
```

Publicação com endereço:

```text
Usuário logado
  -> PublicarComponent
  -> escolhe tipoPublicacao: CONTRATACAO | PRESTACAO
  -> informa endereço obrigatório
  -> frontend obtém lat/lng por geolocation ou geocoding
  -> POST /api/v1/publicacoes
  -> PublicacaoServicoService persiste endereço + coords
  -> card exibe distância quando user coords existem
```

Listagem com distância:

```text
Home/Listagem
  -> obter userLocation atual ou manual
  -> GET /api/v1/publicacoes?pageSize=20&lat=&lng=
  -> backend retorna cards + distanceKm
  -> frontend renderiza 20 cards
  -> scroll sentinel carrega próxima página
```

Notificações dinâmicas:

```text
Usuário logado
  -> HeaderComponent inicia NotificationStateService
  -> polling/SSE/WebSocket consulta notificações não lidas
  -> badge atualiza sem clique
  -> dropdown abre com últimos 5 itens já carregados/atualizados
```

### 5.2 Modelo de dados

| Campo | Entidade/Tabela | Tipo | Obrigatório | Descrição |
|-------|-----------------|------|-------------|-----------|
| `nome` | `usuarios` | varchar | Sim | Nome completo no cadastro mínimo |
| `email` | `usuarios` | varchar unique | Sim | Identificador principal da conta |
| `senha` | `usuarios` | varchar | Sim para local | Hash de senha; social pode usar marcador interno ou fluxo sem senha local |
| `telefone` | `usuarios` | varchar | Não | Editável no perfil |
| `cpf` | `usuarios` | varchar unique nullable | Não | Editável no perfil; não exigido em social |
| `genero` | `usuarios` | varchar | Não | Editável no perfil |
| `data_nascimento` | `usuarios` | date | Não | Editável no perfil |
| `endereco` | `usuarios` | varchar | Não | Endereço opcional do perfil |
| `cep` | `usuarios` | varchar | Não | Perfil |
| `rua` | `usuarios` | varchar | Não | Perfil |
| `bairro` | `usuarios` | varchar | Não | Perfil |
| `cidade` | `usuarios` | varchar | Não | Perfil |
| `estado` | `usuarios` | varchar(2) | Não | Perfil |
| `numero` | `usuarios` | varchar | Não | Perfil |
| `complemento` | `usuarios` | varchar | Não | Perfil |
| `tipo_usuario` | `usuarios` | enum/string | Legado | Mantido para compatibilidade |
| `provider` | `social_logins` | varchar | Sim | `google`, `facebook` |
| `provider_id` | `social_logins` | varchar | Sim | ID estável no provedor |
| `email_verified` | `social_logins` ou payload validado | boolean | Recomendado | Segurança para vínculo por email |
| `tipo_publicacao` | `publicacoes_servico` | enum/string | Sim | `CONTRATACAO` ou `PRESTACAO` |
| `endereco_publicacao` | `publicacoes_servico` | varchar | Sim | Endereço completo da publicação |
| `cep_publicacao` | `publicacoes_servico` | varchar | Não | Normalização do endereço |
| `cidade_publicacao` | `publicacoes_servico` | varchar | Sim | Exibição/filtro |
| `estado_publicacao` | `publicacoes_servico` | varchar(2) | Sim | Exibição/filtro |
| `latitude` | `publicacoes_servico` | decimal | Sim para distância | Latitude da publicação |
| `longitude` | `publicacoes_servico` | decimal | Sim para distância | Longitude da publicação |
| `geocode_provider` | `publicacoes_servico` | varchar | Não | `browser`, `nominatim`, `manual` |
| `geocode_precision` | `publicacoes_servico` | varchar | Não | `exact`, `city`, `approx` |
| `address_hash` | `geocode_cache` | varchar unique | Sim | Hash do endereço normalizado |
| `lat` | `geocode_cache` | decimal | Sim | Latitude cacheada |
| `lng` | `geocode_cache` | decimal | Sim | Longitude cacheada |
| `source` | `geocode_cache` | varchar | Sim | `nominatim` |
| `expires_at` | `geocode_cache` | timestamp | Não | Expiração opcional do cache |
| `enabled` | `feature_flags` ou env | boolean | Sim | Flags estruturais |

### 5.3 Endpoints

| Método | Path | Auth | Payload resumido | Resposta |
|--------|------|------|------------------|----------|
| POST | `/api/v1/auth/login` | Público + CSRF | `{ email, senha }` | Usuário + cookies de sessão |
| POST | `/api/v1/auth/social/google` | Público + CSRF | `{ token }` ou `{ idToken }` | Usuário + cookies de sessão |
| POST | `/api/v1/auth/social/facebook` | Público + CSRF | `{ token }` | Usuário + cookies de sessão |
| POST | `/api/v1/auth/register` ou manter `/api/usuarios/contratante` | Público + CSRF | `{ nome, email, senha, confirmacaoSenha }` | Usuário + cookies de sessão |
| GET | `/api/v1/auth/me` | Logado | - | Usuário autenticado |
| PUT | `/api/usuarios/{id}` | Dono/admin | Dados de perfil completo | Usuário atualizado |
| POST | `/api/v1/geocode` | Logado | Endereço normalizado | Coordenadas/cache |
| GET | `/api/v1/publicacoes` | Público | `page,size,lat,lng,tipoPublicacao` | Página de cards com `distanceKm` |
| POST | `/api/v1/publicacoes` | Logado | Dados publicação + endereço + tipo | Publicação criada |
| GET | `/api/v1/notificacoes/recentes?limit=5` | Logado | - | Últimas notificações |
| GET | `/api/v1/notificacoes/nao-lidas` | Logado | - | Contador |
| GET | `/api/v1/chat/nao-lidas` | Logado | - | Contador chat |

### 5.4 Integrações externas

| Integração | Uso | Restrição |
|------------|-----|-----------|
| Browser Geolocation API | Obter localização atual do usuário | HTTPS e permissão explícita |
| Notifications API | Notificação web opcional | Permissão por gesto usuário |
| OSM Nominatim | Geocoding gratuito fallback/cache | Máx. 1 req/s, User-Agent/Referer, atribuição, cache obrigatório |

---

## 6. UX e Comportamento da Interface

Não há briefing UX/UI separado. Esta seção define comportamento mínimo até existir Figma.

### 6.1 Estados da interface

```text
Cadastro vazio
  -> card "Dados de Acesso" com nome, email, senha, confirmação

Senha digitando
  -> texto fixo de requisitos sempre visível
  -> erro vermelho só se requisito falhar após interação/submissão

Social login
  -> botão Google/Facebook
  -> loading por provider
  -> erro claro se provider falhar

Listagem sem localização
  -> cards sem distância + CTA discreto "Usar minha localização"

Listagem com localização
  -> cards com "a X km de você"

Permissão negada
  -> oferecer "informar localização manualmente"

Dropdown notificações
  -> últimos 5 itens, contador dinâmico, link "Ver todas"

Chat mobile
  -> lista/conversa adaptadas sem overflow horizontal
```

### 6.2 Wireframes

Cadastro mínimo:

```text
┌──────────────────────────────────────┐
│ Dados de Acesso                      │
│ Nome Completo *                      │
│ [Seu nome]                           │
│ E-mail *                             │
│ [email@exemplo.com]                  │
│ Senha *                              │
│ [Digite sua senha]                   │
│ Use no mínimo 8 caracteres...        │
│ Confirme a Senha *                   │
│ [Repita a senha]                     │
│ [Criar conta]                        │
│ Google   Facebook                    │
└──────────────────────────────────────┘
```

Card de publicação:

```text
┌──────────────────────────────────────┐
│ [Contratação] Título                 │
│ Categoria · Cidade/UF                │
│ a 3,2 km de você                     │
│ Descrição curta...                   │
│ [Ver detalhes]                       │
└──────────────────────────────────────┘
```

Dropdown notificações:

```text
┌─────────────────────────────┐
│ Notificações             3  │
├─────────────────────────────┤
│ Nova mensagem de João       │
│ Publicação respondida       │
│ ... até 5 itens             │
├─────────────────────────────┤
│ Ver todas as notificações   │
└─────────────────────────────┘
```

### 6.3 Requisitos de responsividade

- Cadastro deve caber em mobile sem scroll horizontal.
- Dropdown de notificações deve usar largura máxima `min(360px, calc(100vw - 16px))`.
- Chat deve separar lista/conversa em mobile com navegação clara.
- Textos longos devem quebrar linha e não vazar containers.
- Botões devem manter área mínima de toque.

---

## 7. Regras de Negócio

1. Cadastro local DEVE exigir apenas nome completo, email, senha e confirmação de senha.
2. Cadastro local NÃO DEVE exigir CPF, telefone, gênero, data de nascimento, endereço, foto ou dados profissionais.
3. Senha DEVE cumprir requisitos existentes de segurança definidos no backend.
4. Frontend DEVE exibir requisitos de senha fixos abaixo do input.
5. Frontend DEVE exibir erro vermelho de senha apenas quando algum requisito não for atendido após interação/submissão.
6. Email DEVE ser único em `usuarios`.
7. Login local DEVE autenticar por email e senha.
8. Login social DEVE validar token no provedor antes de criar/vincular conta.
9. Login social DEVE usar payload separado do cadastro local.
10. Login social NÃO DEVE exigir CPF, telefone, endereço ou senha.
11. Social/local DEVEM apontar para a mesma conta quando email for igual e verificado.
12. Backend NÃO DEVE vincular social login por email não verificado.
13. `tipoUsuario` DEVE permanecer no banco como legado.
14. Regra de contratar/prestar DEVE ser definida em `tipoPublicacao`.
15. Publicação DEVE exigir endereço.
16. Publicação DEVE armazenar latitude/longitude para exibir distância.
17. Se usuário negar geolocalização, sistema DEVE permitir localização manual.
18. Cards DEVEM exibir distância somente quando houver coordenadas suficientes.
19. Listagem principal DEVE carregar 20 cards por página/rolagem.
20. Notificações DEVEM atualizar sem clique no sino quando usuário estiver logado.
21. Dropdown de notificações DEVE mostrar até 5 últimas notificações e ação para ver todas.
22. Chat e dropdown DEVEM ser responsivos em mobile e desktop.
23. Feature flag DEVE permitir desativar cadastro novo, geodistância e notificações dinâmicas separadamente.

---

## 8. Segurança e Privacidade

### 8.1 Controle de acesso

| Ação | Papel permitido | Papel bloqueado |
|------|-----------------|-----------------|
| Cadastro local | Anônimo | Usuário já logado pode ser redirecionado |
| Login local/social | Anônimo | Nenhum, mas rotas devem ignorar JWT inválido |
| Editar perfil | Dono da conta/admin | Outros usuários |
| Criar publicação | Usuário logado | Anônimo |
| Editar publicação | Dono/admin | Outros usuários |
| Ver publicações públicas | Todos | Nenhum |
| Ver notificações | Dono da conta | Outros usuários/anônimo |
| Ver chat | Participantes da conversa | Usuários fora da conversa/anônimo |
| Geocoding server-side | Usuário logado para criação; público somente para leitura cacheada se necessário | Abuso/rate limit |

### 8.2 Dados sensíveis

| Dado | Onde armazenar | Criptografia | Pode logar? |
|------|---------------|--------------|-------------|
| Senha | `usuarios.senha` | Hash forte via PasswordEncoder | Não |
| CPF | `usuarios.cpf` | Não definido; mínimo mascarar em UI/log | Não |
| Telefone | `usuarios.telefone` | Não obrigatório | Não completo |
| Email | `usuarios.email` | Não | Somente quando necessário; preferir userId |
| Token Google/Facebook | Nunca persistir bruto | Não persistir | Não |
| Access/refresh cookies | HttpOnly cookie | Assinado/aleatório | Não |
| Localização usuário | localStorage/session ou perfil se manual | Não obrigatório | Não com precisão completa |
| Coordenadas publicação | `publicacoes_servico` | Não | Pode logar arredondado se necessário |
| Endereço publicação | `publicacoes_servico` | Não | Não completo em logs |

### 8.3 Validação server-side

- Backend valida email, senha, confirmação e duplicidade.
- Backend valida `email_verified` social antes de vincular.
- Backend valida propriedade de perfil/publicação.
- Backend valida endereço obrigatório em publicação.
- Backend valida latitude/longitude dentro de ranges válidos.
- Backend aplica rate limit/cache em geocoding.
- Frontend não é fonte de verdade para email social, providerId ou provider email.

---

## 9. Tratamento de Erros e Resiliência

| Cenário | Causa | Comportamento esperado | Mensagem ao usuário |
|---------|-------|----------------------|---------------------|
| Email já cadastrado no cadastro local | `usuarios.email` existente | Sugerir login ou recuperação de senha | "Este e-mail já está cadastrado. Entre na sua conta." |
| Senha fraca | Requisito não atendido | Bloquear submit e destacar requisito | "A senha ainda não atende aos requisitos." |
| Confirmação diferente | Campos divergentes | Bloquear submit | "As senhas não coincidem." |
| Token social ausente | Payload inválido | 401 claro | "Token do Google ausente." |
| Token social inválido | Provedor rejeita | 401 claro | "Não foi possível validar sua conta Google." |
| Email social não verificado | Provider não confirmou email | Bloquear vínculo | "Confirme seu e-mail no provedor antes de entrar." |
| Provider social sem email | Facebook/Google não retorna email | Bloquear ou pedir email validado | "Não foi possível obter um e-mail verificado." |
| Conta local existente sem vínculo social | Primeiro login social com email verificado | Vincular provider e logar | Sem erro |
| Conta social existente | ProviderId já cadastrado | Logar usuário existente | Sem erro |
| Endereço publicação vazio | Falta campo obrigatório | Bloquear publicação | "Informe o endereço da publicação." |
| Geolocation negada | Usuário nega permissão | Mostrar fallback manual | "Informe sua localização manualmente para ver distâncias." |
| Geolocation indisponível | Navegador/OS bloqueia | Fallback manual | "Não foi possível obter sua localização." |
| Nominatim rate limit | Limite 1 req/s ou indisponível | Usar cache/fila/fallback manual | "Não foi possível validar o endereço agora." |
| Coordenadas ausentes no card | Publicação antiga sem lat/lng | Ocultar distância | Sem mensagem |
| Falha carregar próxima página | Rede/backend | Manter lista e permitir tentar novamente | "Não foi possível carregar mais publicações." |
| Notificação polling falha | Rede/backend | Manter contador anterior, retry backoff | Sem alerta invasivo |
| Dropdown sem notificações | Lista vazia | Estado vazio automático | "Nenhuma notificação." |
| Chat overflow mobile | Layout estreito | Layout responsivo sem quebra | Sem mensagem |
| Feature flag off | Rollback | Usar comportamento legado seguro | Sem mensagem técnica |

---

## 10. Observabilidade

Usuário confirmou que não quer observabilidade dedicada nesta fase. Mesmo assim, implementar logs técnicos mínimos para diagnóstico seguro.

### 10.1 Eventos a logar

| Evento | Campos obrigatórios | Nível |
|--------|--------------------|-------|
| `auth_register_success` | `userId`, `provider=local`, `featureFlagVersion` | info |
| `auth_register_failed` | `reason`, `provider=local`, `emailHash` | warn |
| `auth_social_login_success` | `userId`, `provider`, `linkedExisting` | info |
| `auth_social_login_failed` | `provider`, `reason`, `tokenFingerprint` | warn |
| `social_account_linked` | `userId`, `provider` | info |
| `publication_created` | `publicationId`, `userId`, `tipoPublicacao`, `hasCoordinates` | info |
| `geocode_cache_hit` | `addressHash`, `provider` | debug |
| `geocode_failed` | `provider`, `reason`, `addressHash` | warn |
| `notification_poll_failed` | `userId`, `reason` | debug/warn |
| `feature_flag_evaluated` | `flag`, `enabled` | debug |

### 10.2 Métricas

| Métrica | Tipo | O que mede |
|---------|------|-----------|
| `auth_register_success_total` | counter | Cadastros locais concluídos |
| `auth_social_login_success_total` | counter | Logins sociais concluídos por provider |
| `auth_social_link_total` | counter | Vínculos de provider criados |
| `geocode_request_total` | counter | Chamadas a geocoding |
| `geocode_cache_hit_total` | counter | Uso de cache geográfico |
| `publication_list_latency_ms` | histogram | Latência da listagem |
| `notification_poll_latency_ms` | histogram | Latência de atualização de notificações |
| `publication_distance_available_ratio` | gauge | Percentual de cards com distância disponível |

### 10.3 Alertas

Sem alertas externos nesta versão. Apenas logs suficientes para busca manual em produção.

| Condição | Threshold | Ação |
|----------|-----------|------|
| Falhas social login | Não aplicável | Consultar logs `auth_social_login_failed` |
| Falhas geocode | Não aplicável | Consultar logs `geocode_failed`; desativar flag de distância se necessário |
| Falhas notificação | Não aplicável | Consultar logs `notification_poll_failed`; desativar flag se necessário |

---

## 11. Variáveis de Ambiente e Configuração

```env
# Feature flags — backend
BRJOBS_FEATURE_STRUCTURAL_AUTH=true        # BE — ativa cadastro/login reestruturado
BRJOBS_FEATURE_PUBLICATION_GEO=true        # BE — ativa endereço/coords/distância em publicações
BRJOBS_FEATURE_DYNAMIC_NOTIFICATIONS=true  # BE — ativa notificações dinâmicas

# Feature flags — frontend
NG_APP_FEATURE_STRUCTURAL_AUTH=true        # FE — mostra cadastro mínimo e auth unificado
NG_APP_FEATURE_PUBLICATION_GEO=true        # FE — mostra localização/distância
NG_APP_FEATURE_DYNAMIC_NOTIFICATIONS=true  # FE — ativa polling/SSE de notificações

# Geocoding gratuito
GEOCODING_PROVIDER=nominatim               # BE — provider inicial
NOMINATIM_BASE_URL=https://nominatim.openstreetmap.org
NOMINATIM_USER_AGENT=BRJobs/1.0 contato@brjobs.com.br
NOMINATIM_MIN_INTERVAL_MS=1100             # BE — respeitar limite público
GEOCODING_CACHE_TTL_DAYS=365               # BE — reduzir chamadas externas

# Listagem
PUBLICACOES_DEFAULT_PAGE_SIZE=20           # BE — page size padrão
PUBLICACOES_MAX_PAGE_SIZE=20               # BE — limite por rolagem na tela principal

# Notificações
NOTIFICATION_POLL_INTERVAL_MS=15000        # FE/BE — intervalo inicial se polling
NOTIFICATION_RECENT_LIMIT=5                # BE — dropdown mostra últimas 5

# Auth já existente, manter forte
BRJOBS_JWT_SECRET=base64-ou-string-com-64-bytes-minimo
AUTH_ACCESS_TOKEN_TTL=PT2H
AUTH_REFRESH_TOKEN_TTL=PT168H
GOOGLE_CLIENT_ID=google-client-id
FACEBOOK_APP_ID=facebook-app-id
```

Observação: Angular normalmente usa `environment.ts`. Se o projeto não tiver suporte a `NG_APP_*`, aplicar feature flags via `environment` ou endpoint de config pública.

---

## 12. Estratégia de Rollout e Rollback

**Rollout:**

- Criar flags independentes:
  - `structuralAuth`;
  - `publicationGeo`;
  - `dynamicNotifications`.
- Deploy backend com schema backward-compatible.
- Deploy frontend lendo flags.
- Ativar primeiro em ambiente local/staging.
- Ativar em produção por módulo:
  1. cadastro mínimo/auth;
  2. publicação com endereço/coords;
  3. distância em listagem;
  4. notificações dinâmicas;
  5. responsividade chat/dropdown.

**Rollback:**

- Desativar flag afetada sem rollback de imagem.
- Manter colunas novas nullable.
- Não remover `tipoUsuario`.
- Se Nominatim falhar, desligar `publicationGeo` ou apenas geocoding externo, mantendo endereço textual.
- Se notificações gerarem carga, aumentar polling interval ou desligar `dynamicNotifications`.
- Se cadastro novo falhar, desligar `structuralAuth` e usar fluxo legado.

---

## 13. Fases de Entrega

### Fase 1 — Schema e auth unificado

- [ ] Ajustar entidade/tabela `usuarios` para perfil parcial.
- [ ] Garantir `email` como identidade única.
- [ ] Unificar local/social por email verificado.
- [ ] Diferenciar DTO local vs DTO social.
- [ ] Remover geração/armazenamento de dados inexistentes no social.
- [ ] Adicionar/validar feature flag `structuralAuth`.

### Fase 2 — Cadastro mínimo e perfil completo

- [ ] Reduzir tela de cadastro para card "Dados de Acesso".
- [ ] Mover campos removidos para edição de perfil.
- [ ] Ajustar validações visuais de senha.
- [ ] Garantir responsividade mobile/desktop.

### Fase 3 — Publicação com tipo e endereço

- [ ] Manter `tipoUsuario` legado.
- [ ] Adicionar `tipoPublicacao` obrigatório no fluxo de publicação.
- [ ] Exigir endereço na publicação.
- [ ] Persistir coordenadas e campos de endereço da publicação.

### Fase 4 — Geolocalização e distância

- [ ] Implementar serviço de localização no frontend.
- [ ] Implementar fallback manual.
- [ ] Implementar geocoding cacheado via Nominatim.
- [ ] Calcular distância Haversine.
- [ ] Exibir "a X km de você" nos cards.
- [ ] Carregar 20 cards por rolagem.

### Fase 5 — Notificações e chat responsivo

- [ ] Atualizar badge/notificações sem clique.
- [ ] Dropdown mostrar últimas 5 + "Ver todas".
- [ ] Corrigir estado vazio/carregando sem depender de clique.
- [ ] Ajustar responsividade do dropdown.
- [ ] Ajustar responsividade da tela de chat.

### Fase 6 — Hardening e regressão

- [ ] Testar fluxos locais e sociais.
- [ ] Testar migração de dados antigos.
- [ ] Testar flags on/off.
- [ ] Testar sem geolocation, com geolocation negada e fallback manual.
- [ ] Testar produção sem `localhost`.
- [ ] Corrigir mojibake e validar ABNT2.

---

## 14. Fora do Escopo (desta versão)

- App mobile nativo.
- Rotas em mapa.
- Google Maps pago.
- Geocoding massivo sem cache.
- Servidor próprio Nominatim.
- Observabilidade avançada com alertas externos.
- Mudança completa de design system.
- Remoção física de `tipoUsuario`.
- Backoffice/admin avançado para feature flags.
- Recomendação algorítmica de publicações além de distância.

---

## 15. Riscos e Pontos em Aberto

| # | Descrição | Probabilidade | Impacto | Mitigação |
|---|-----------|---------------|---------|-----------|
| R01 | Vincular social login por email não verificado pode permitir tomada de conta | Média | Alto | Exigir `email_verified` e providerId; logar vínculos |
| R02 | Campos obrigatórios antigos no DB podem quebrar cadastro social/local mínimo | Alta | Alto | Migration para nullable + testes de insert |
| R03 | `tipoUsuario` ainda usado em regras antigas pode conflitar com `tipoPublicacao` | Alta | Alto | Mapear usos de `tipoUsuario`; manter legado só onde necessário |
| R04 | Nominatim pode bloquear por excesso de chamadas | Média | Médio | Cache por endereço, rate limit 1 req/s, fallback manual |
| R05 | Geolocation negada reduz valor da distância | Alta | Médio | Fallback manual claro e persistência opcional |
| R06 | Infinite scroll pode degradar performance | Média | Médio | Page size 20, skeleton, preservar posição, evitar render pesado |
| R07 | Polling de notificações pode gerar carga | Média | Médio | Intervalo configurável, backoff, flag para desligar |
| R08 | Responsividade pode quebrar telas existentes | Média | Médio | Teste mobile/desktop com screenshots |
| R09 | Dados legados sem coordenadas não mostram distância | Alta | Baixo | Ocultar distância nesses cards e geocodificar quando editados |
| R10 | Mojibake/textos sem ABNT2 podem piorar UX | Média | Médio | Scan por `Ã`, `â`, caracteres corrompidos antes de finalizar |

**Pontos em aberto (bloqueadores):**

- Nenhum ponto em aberto bloqueante no discovery.

---

*Documento gerado para alinhamento técnico interno. Revisar com o time antes de iniciar o desenvolvimento.*

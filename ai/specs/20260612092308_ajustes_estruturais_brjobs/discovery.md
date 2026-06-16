# Discovery — Ajustes estruturais no BRJobs

> **Feature:** 20260612092308_ajustes_estruturais_brjobs
> **Data:** 2026-06-12
> **Autor:** Celso Borges / produto e engenharia
> **Status:** Completo

---

## Fontes de contexto utilizadas

| # | Fonte | Tipo | Conteúdo principal |
|---|-------|------|--------------------|
| 1 | `inputs/input-01.md` | paste | Lista de ajustes estruturais: notificações dinâmicas, cadastro mínimo, unificação social/local, tipo de usuário legado, localização/distância em publicações, responsividade de chat e notificações. |
| 2 | `AGENTS.md` e `CLAUDE.md` | arquivo | Convenções do repositório, comandos, estrutura, padrões de frontend/backend e regra ABNT2/pt-BR. |
| 3 | `brjobs-angular/package.json` e `brjobs-java/pom.xml` | arquivo | Stack confirmada: Angular 21, TypeScript 5.9, Spring Boot 4.0.6, Java 25, PostgreSQL. |
| 4 | MDN Notifications API — https://developer.mozilla.org/en-US/docs/Web/API/Notifications_API | URL | Notificações web exigem permissão do usuário e devem ser solicitadas por gesto explícito. |
| 5 | MDN Geolocation API — https://developer.mozilla.org/en-US/docs/Web/API/Geolocation/getCurrentPosition | URL | Geolocalização exige HTTPS, permissão explícita e pode ser bloqueada por política/permissão do navegador. |
| 6 | OSM Nominatim Usage Policy — https://operations.osmfoundation.org/policies/nominatim/ | URL | Geocoding gratuito com restrições: máximo absoluto de 1 req/s, identificação por User-Agent/Referer e atribuição. |
| 7 | Google/Internet Identity account linking — https://sites.google.com/site/oauthgoog/usability-research-on-federated-login/rp-best-practices/advanced-notes-on-account-linking | URL | Conta local deve manter lista de provedores vinculados; provedores novos para email existente exigem cuidado para evitar tomada de conta. |
| 8 | NN/g Infinite Scrolling — https://www.nngroup.com/articles/infinite-scrolling-tips/ | URL | Infinite scroll é adequado para descoberta, mas precisa preservar posição, manter performance e dar feedback de carregamento. |

---

## Tipo de projeto

**Brownfield**

> O BRJobs já existe e está em produção. Os ajustes mudam fluxos centrais de autenticação, cadastro, perfil, publicações, notificações e chat, com impacto em usuários e dados existentes.

---

## Resumo do entendimento

O BRJobs precisa simplificar a entrada do usuário, corrigir experiência de notificação e chat, e reposicionar a modelagem do usuário. O cadastro deve exigir apenas dados de acesso. Dados pessoais, endereço e informações profissionais devem sair do cadastro e ficar em edição de perfil. O antigo campo `tipoUsuario` deixa de direcionar a regra principal do produto e passa a ser legado; a intenção de "contratar" ou "prestar" passa a ser definida no momento de publicar.

Notificações devem ser dinâmicas, sem depender de clique no botão para atualizar. Publicações passam a exigir endereço próprio e devem exibir distância real em relação ao usuário, usando localização atual com permissão ou localização fornecida manualmente. Listagens devem carregar 20 cards por rolagem na tela principal.

---

## Problema e dor

**Dor principal:** Fluxos atuais exigem dados demais cedo, confundem papéis de usuário, não notificam em tempo real e não usam localização para melhorar relevância de publicações.

**Quem sente:** Visitantes, usuários cadastrando, usuários logados, quem publica serviços, quem busca publicações, usuários do chat e usuários que dependem de notificações.

**Frequência e impacto:** Ocorre em fluxos diários de cadastro, login, navegação, publicação e chat. O custo de não resolver é fricção no cadastro, contas duplicadas, baixa confiança nas notificações, menor conversão e menor utilidade das publicações por falta de distância/localização.

---

## Usuários e papéis afetados

| Papel | Relação com a feature | Impacto |
|-------|-----------------------|---------|
| Visitante | Acessa cadastro/login e listagens públicas. | Direto — cadastro fica mais curto e listagem mostra distância quando houver localização. |
| Usuário local | Entra com email e senha. | Direto — conta passa a ser unificada por email com social login. |
| Usuário social | Entra com Google/Facebook. | Direto — payload social segue separado e cria/vincula conta sem exigir CPF/telefone/endereço. |
| Usuário logado | Usa notificações, chat, perfil e publicações. | Direto — notificações dinâmicas, chat responsivo e perfil com dados completos opcionais. |
| Publicador | Cria publicação de contratação ou prestação. | Direto — tipo da publicação define intenção; endereço passa a ser obrigatório no card/form de publicar. |
| Leitor de publicações | Navega cards/listagem. | Direto — vê distância em quilômetros baseada em localização real ou informada. |
| Operação/admin técnico | Mantém deploy, env vars e rollback. | Indireto — precisa feature flag para ativar/desativar mudanças. |

---

## Solução proposta (rascunho)

**Construir:**

- Cadastro mínimo com apenas:
  - nome completo;
  - email;
  - senha;
  - confirmação de senha.
- Validação de senha com texto informativo fixo abaixo do input e erro vermelho apenas quando requisito não for atendido.
- Dados pessoais, endereço, dados profissionais e foto movidos para edição de perfil.
- Conta única por email:
  - login local e social apontam para mesmo usuário;
  - social login usa payload específico, sem campos que Google/Facebook não fornecem;
  - backend vincula provedor social somente quando email verificado for compatível.
- `tipoUsuario` mantido apenas como legado/compatibilidade.
- Publicação passa a declarar se é contratação ou prestação.
- Publicação exige endereço próprio e guarda dados suficientes para calcular distância.
- Cards de publicação exibem "a X quilômetros de você" quando houver localização do usuário ou localização manual.
- Listagem principal carrega 20 cards por página/rolagem.
- Notificações atualizam dinamicamente sem depender de clique no sino.
- Dropdown de notificações e tela de chat corrigidos para responsividade mobile/desktop.
- Feature flag para rollout e rollback das mudanças estruturais.

**Fora do escopo desta versão:**

- Aplicativo mobile nativo.
- Pagamento/monetização.
- Sistema completo de mapas com rotas.
- Geocoding massivo sem cache.
- Painel avançado de observabilidade/alertas, pois o usuário confirmou que não quer monitoramento dedicado nesta fase.

---

## Restrições e premissas

- Produto já está em produção.
- Mudança pode afetar todos os usuários.
- Dados legados podem migrar sem preservar `tipoUsuario` como regra principal.
- `tipoUsuario` deve continuar existindo como legado.
- Campos pessoais e endereço podem se tornar opcionais no cadastro/perfil.
- Endereço da publicação é obrigatório.
- Conta única por email é premissa confirmada.
- Social login não pode exigir CPF, telefone, endereço ou senha.
- Textos visíveis no frontend devem seguir ABNT2/pt-BR, com acentos corretos e sem mojibake.
- Nenhuma chamada de produção pode usar `localhost`.
- Geolocalização deve ser gratuita e priorizar usabilidade.
- Nominatim pode ser usado apenas com cache, baixa frequência e respeito à política de uso.
- Rollout deve ser por feature flag.
- Não haverá monitoramento/alerta dedicado nesta fase.

---

## Referências de mercado

| Referência | Decisão de design relevante | Ressoa? | Motivo |
|------------|-----------------------------|---------|--------|
| MDN Notifications API | Notificações exigem permissão e devem ser solicitadas em resposta a gesto do usuário. | Sim | Evita prompt abusivo e mantém compatibilidade com navegadores. |
| MDN Geolocation API | Localização exige HTTPS e permissão explícita; usuário pode negar. | Sim | BRJobs já usa HTTPS em produção e precisa fallback manual. |
| OSM Nominatim Usage Policy | Geocoding gratuito exige limite de 1 req/s, identificação e atribuição. | Parcialmente | Útil para MVP gratuito, mas exige cache e não serve para uso massivo sem servidor próprio ou provedor pago. |
| Google/Internet Identity account linking | Conta deve manter provedores vinculados; vinculação automática por email exige email verificado e cuidado. | Sim | Evita duplicidade e reduz risco de tomada de conta. |
| NN/g Infinite Scrolling | Infinite scroll serve para descoberta, com preservação de posição e feedback de carregamento. | Sim | Tela principal de publicações é fluxo de descoberta; limite de 20 por rolagem mantém performance. |

**Padrões extraídos das referências escolhidas:**

- Solicitar permissão de notificação/localização por gesto explícito e com texto de valor claro.
- Sempre oferecer fallback manual para localização.
- Calcular distância com coordenadas persistidas e fórmula Haversine, evitando geocoding repetido.
- Cachear resultado de geocoding por endereço normalizado.
- Vincular social/local por email somente quando o provedor confirmar email verificado.
- Usar página/cursor de 20 itens e feedback visual de carregamento.
- Preservar posição da listagem ao abrir/voltar de detalhes.

---

## Decisões de design tomadas

| Decisão | Alternativas consideradas | Justificativa |
|---------|--------------------------|---------------|
| Cadastro mínimo | Manter cadastro completo atual | Reduz fricção inicial e evita exigir dados que não são necessários para autenticar. |
| Perfil completo pós-cadastro | Coletar tudo no cadastro | Dados pessoais/profissionais são importantes, mas não devem bloquear entrada. |
| Conta única por email | Contas separadas por provedor | Evita duplicidade e permite mesmo usuário usar senha, Google ou Facebook. |
| Social login com payload separado | Exigir payload igual ao cadastro local | Google/Facebook não fornecem CPF/telefone/senha; exigir isso quebra UX e fluxo OAuth. |
| `tipoUsuario` legado | Remover coluna imediatamente | Evita quebra em dados/código existentes. Regra principal muda para tipo da publicação. |
| Tipo da publicação define contratação/prestação | Tipo fixo no usuário | Um usuário pode contratar e prestar em momentos diferentes. |
| Endereço obrigatório na publicação | Usar endereço do perfil | Publicações precisam localização própria e perfil pode não ter endereço. |
| Distância por localização real ou informada | Não mostrar distância | Distância melhora relevância e decisão do usuário. Fallback manual evita bloqueio por permissão negada. |
| Browser geolocation + Nominatim cacheado | Google Maps pago ou sem geocoding | Melhor equilíbrio gratuito/usabilidade. Nominatim exige respeito a limite e cache. |
| Listagem com 20 cards por rolagem | Carregar tudo ou paginação manual | Mantém performance e UX de descoberta. |
| Notificações dinâmicas | Atualizar só ao clicar | Corrige dor principal e melhora percepção de tempo real. |
| Feature flag | Big-bang sem controle | Permite rollback rápido em produção. |

---

## Lacunas e pontos em aberto

Nenhum ponto em aberto bloqueante foi identificado no discovery.

Decisões confirmadas:

- Geo gratuito com melhor usabilidade: browser geolocation + fallback manual + Nominatim cacheado.
- Listagem principal: 20 cards por rolagem.
- Observabilidade dedicada: fora do escopo.
- Rollout: feature flag.
- Env vars: permitidas se forem para serviço gratuito.

---

## Notas adicionais

- Módulos afetados confirmados:
  - auth/login/cadastro;
  - usuários/perfil;
  - publicações;
  - notificações/chat;
  - geolocalização/distância.
- Migração de dados aceita:
  - tornar campos pessoais/endereço opcionais;
  - adicionar campos de localização em publicação;
  - preservar `tipoUsuario` apenas como legado.
- Testes esperados na implementação:
  - cadastro local mínimo;
  - login local com conta existente;
  - social login criando conta sem CPF/telefone/endereço;
  - social login vinculando email existente verificado;
  - edição de perfil com campos movidos;
  - publicação exigindo endereço;
  - listagem com 20 cards por rolagem;
  - cálculo/exibição de distância com permissão concedida, negada e fallback manual;
  - notificações atualizando sem clique;
  - responsividade de chat e dropdown de notificações em mobile/desktop.

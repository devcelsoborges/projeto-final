<a id="spec-01"></a>
## SPEC-01 — Catálogo de planos e checkout de destaque

**Objetivo**
> Permitir que o autor de uma publicação escolha um plano de destaque, veja preço/duração e inicie o pagamento com Stripe sem sair do fluxo do BrJobs.

**Contexto**
> O discovery definiu monetização por destaque em brownfield, com Stripe como gateway principal, planos Básico/Plus/Premium e checkout hospedado. O produto ainda não tem usuários reais em produção, então não há migração histórica.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Autor da publicação | Seleciona plano e inicia checkout | Direto |
> | Frontend Engineer | Exibe planos, estados de carregamento e redirecionamento | Direto |
> | Backend Engineer | Cria sessão Stripe e valida ownership | Direto |
> | Produto | Define preço, duração e prioridade dos planos | Direto |

**Comportamento esperado**
> Fluxo principal:
> 1. Usuário autenticado abre a tela de destaque da sua publicação.
> 2. Frontend lista os planos disponíveis com preço, duração e prioridade.
> 3. Usuário seleciona um plano.
> 4. Frontend chama o endpoint de checkout.
> 5. Backend valida se a publicação pertence ao usuário e se não há pagamento ativo duplicado.
> 6. Backend cria um registro de pagamento `PENDING` e uma sessão Stripe.
> 7. Frontend redireciona para a URL de checkout da Stripe.
>
> Fluxo alternativo — plano inválido:
> 1. Usuário tenta iniciar checkout com plano inexistente.
> 2. Backend rejeita com erro de validação.
>
> Fluxo alternativo — publicação de terceiro:
> 1. Usuário tenta destacar publicação que não é sua.
> 2. Backend bloqueia a operação por ownership.

**Regras de negócio**
> - Usuário DEVE ser o autor da publicação para iniciar o checkout.
> - O sistema N�O DEVE criar mais de um pagamento ativo para a mesma publica��o.
> - O sistema DEVE expor os planos Básico, Plus e Premium.
> - SE o checkout for iniciado com sucesso ENT�O a sess�o Stripe DEVE ser gerada no backend.
> - O preço exibido DEVE corresponder ao plano salvo no banco.

**Contrato de API**
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------:|-----------------|
> | GET | /api/highlight/plans | não | - | 200 lista de planos | 500 |
> | POST | /api/highlight/checkout/{jobPostId} | sim | `{ planId }` | 200 `{ paymentId, stripeSessionId, checkoutUrl }` | 400, 401, 403, 409 |

**SLA e Performance**
> - Listagem de planos: P95 <= 300ms.
> - Início de checkout: P95 <= 1000ms.
> - Tempo externo da Stripe: até 10s antes de timeout de fluxo.

**Observabilidade**
> - **Logar:** `highlight_checkout_started` com campos `[user_id, job_post_id, plan_id, amount, timestamp]` — nível `info`
> - **Logar:** `highlight_checkout_created` com campos `[payment_id, stripe_session_id, job_post_id, timestamp]` — nível `info`
> - **Logar:** `highlight_checkout_rejected` com campos `[user_id, job_post_id, reason, timestamp]` — nível `warn`
> - **Métrica:** `highlight_checkout_success_rate`
> - **Alerta:** SE `highlight_checkout_success_rate < 95%` POR `10min` ENT�O notificar engenharia

**Critérios de aceite**
> - DADO uma publica��o do pr�prio usu�rio QUANDO ele escolher um plano v�lido ENT�O o checkout � criado e a URL da Stripe � retornada.
> - DADO um plano inv�lido QUANDO o usu�rio tentar iniciar checkout ENT�O o backend retorna erro de valida��o.
> - DADO uma publica��o que j� possui pagamento ativo QUANDO o usu�rio tentar novo checkout ENT�O o sistema retorna conflito.

**Estado atual**
> Ainda não existe módulo de monetização na base atual. A feature será adicionada sobre o ecossistema de publicações existente do BrJobs.

**Mudanças necessárias**
> - **Banco de dados:** tabela de planos de destaque e tabela de pagamentos.
> - **Backend:** controller/service de checkout, abstração de gateway e validação de ownership.
> - **Frontend:** tela/ação para seleção de plano e redirecionamento para Stripe.
> - **Infra/Config:** variáveis da Stripe e flags de habilitação.

**Definição de pronto**
> - [ ] Planos listados com preço e duração corretos
> - [ ] Checkout Stripe criado no backend
> - [ ] Ownership validado antes do pagamento
> - [ ] Erros de plano inválido e pagamento duplicado cobertos
> - [ ] Integração validada em homologação

---

<a id="spec-02"></a>
## SPEC-02 — Webhook, pagamento e ativação do destaque

**Objetivo**
> Garantir que a ativação do destaque ocorra apenas após confirmação válida do pagamento pela Stripe, com idempotência e prevenção de duplicidade.

**Contexto**
> O discovery definiu que o webhook é a fonte de verdade do fluxo. O sistema precisa aceitar retries da Stripe sem duplicar estado e deve atualizar a publicação rapidamente após o pagamento aprovado.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Stripe | Envia eventos de confirmação | Direto |
> | Backend Engineer | Processa webhook e ativa destaque | Direto |
> | DBA / Infra | Garante persistência consistente | Direto |
> | Autor da publicação | Espera a ativação após aprovação | Direto |

**Comportamento esperado**
> Fluxo principal:
> 1. Stripe envia evento `checkout.session.completed`.
> 2. Backend valida assinatura do webhook.
> 3. Backend localiza o pagamento pelo `stripe_session_id`.
> 4. Backend marca pagamento como `APPROVED`.
> 5. Backend ativa `isHighlighted` e define `highlightExpiresAt`.
> 6. Cache da listagem é invalidado.
>
> Fluxo alternativo — evento repetido:
> 1. Stripe reenviou o mesmo evento.
> 2. Backend reconhece idempotência.
> 3. Nenhuma dupla ativação é executada.
>
> Fluxo alternativo — assinatura inválida:
> 1. Evento chega com assinatura incorreta.
> 2. Backend rejeita a requisição.

**Regras de negócio**
> - O sistema DEVE validar a assinatura `Stripe-Signature`.
> - O sistema N�O DEVE ativar destaque antes do pagamento aprovado.
> - O webhook DEVE ser idempotente.
> - SE o pagamento j� estiver aprovado ENT�O o evento repetido N�O DEVE alterar o estado.
> - O sistema DEVE registrar data final do destaque a partir da duração do plano.

**Contrato de API**
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------:|-----------------|
> | POST | /api/webhook/stripe | não | evento Stripe | 200 OK | 400, 409, 500 |

**SLA e Performance**
> - Processamento do webhook: P95 <= 1000ms.
> - Ativação do destaque: percepção de atualização em até 5s no catálogo.
> - Idempotência: sem segunda escrita efetiva para o mesmo evento.

**Observabilidade**
> - **Logar:** `highlight_webhook_received` com campos `[event_id, event_type, stripe_session_id, timestamp]` — nível `info`
> - **Logar:** `highlight_webhook_approved` com campos `[payment_id, job_post_id, plan_id, amount, timestamp]` — nível `info`
> - **Logar:** `highlight_webhook_duplicate` com campos `[event_id, stripe_session_id, timestamp]` — nível `warn`
> - **Logar:** `highlight_activation_failed` com campos `[payment_id, job_post_id, error_code, timestamp]` — nível `error`
> - **Métrica:** `highlight_webhook_approved_rate`
> - **Alerta:** SE `highlight_webhook_approved_rate < 95%` POR `15min` ENT�O abrir incidente

**Critérios de aceite**
> - DADO webhook v�lido QUANDO Stripe confirmar pagamento ENT�O a publica��o fica destacada.
> - DADO webhook repetido QUANDO o mesmo evento chegar novamente ENT�O o estado n�o � duplicado.
> - DADO webhook com assinatura inv�lida QUANDO o endpoint for chamado ENT�O o backend rejeita o evento.

**Estado atual**
> Não existe pipeline de pagamento para destaque. A ativação ainda depende de implementação nova em backend e integração Stripe.

**Mudanças necessárias**
> - **Banco de dados:** tabela de pagamentos com status e referência à sessão Stripe.
> - **Backend:** controller de webhook, serviço idempotente e atualização da publicação.
> - **Frontend:** exibição de estado pós-pagamento.
> - **Infra/Config:** segredo do webhook e URL pública do endpoint.

**Definição de pronto**
> - [ ] Webhook assina e valida eventos da Stripe
> - [ ] Pagamento aprovado ativa destaque
> - [ ] Reenvio de evento não duplica ativação
> - [ ] Falhas de webhook são observáveis

---

<a id="spec-03"></a>
## SPEC-03 — Listagem destacada, Redis e expiração automática

**Objetivo**
> Ordenar publica��es com destaque ativo antes das org�nicas, cachear a listagem com Redis e remover destaque expirado automaticamente.

**Contexto**
> O discovery exigiu ordenação por destaque ativo, prioridade do plano e recência, com Redis no MVP e scheduler para expiração automática. O catálogo precisa refletir o estado quase em tempo real após webhook ou expiração.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Visitante da listagem | Navega pelas vagas ordenadas | Direto |
> | Autor da publicação | Ganha visibilidade ao destacar | Direto |
> | Backend Engineer | Implementa query, cache e scheduler | Direto |
> | Infra / DBA | Garante índice e consistência de performance | Direto |

**Comportamento esperado**
> Fluxo principal:
> 1. Usuário acessa GET /jobs.
> 2. Backend consulta cache Redis.
> 3. Se houver cache válido, retorna lista ordenada.
> 4. Se não houver cache, backend consulta banco com ordenação.
> 5. Publicações destacadas ativas aparecem primeiro.
> 6. Dentro dos destacados, Premium vem antes de Plus, que vem antes de Básico.
> 7. Empates são resolvidos por data de criação mais recente.
> 8. Scheduler remove o destaque após a expiração.
>
> Fluxo alternativo — cache expirado:
> 1. Redis não tem a chave da listagem.
> 2. Backend recarrega do banco.
> 3. Cache é reescrito.

**Regras de negócio**
> - A listagem DEVE ser paginada.
> - Publica��o destacada ativa DEVE aparecer antes de publica��o org�nica.
> - Entre publicações destacadas, o plano DEVE definir prioridade.
> - SE `highlightExpiresAt` vencer ENT�O o destaque DEVE ser removido.
> - O sistema DEVE invalidar cache após ativação e expiração.

**Contrato de API**
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------:|-----------------|
> | GET | /api/jobs?page=&size= | não | filtros/paginação | 200 lista ordenada | 400, 500 |

**SLA e Performance**
> - Listagem pública: P95 <= 800ms com cache.
> - Cache hit rate desejado: >= 70% no pico inicial.
> - Expiração de destaque: execução periódica curta, configurável por ambiente.

**Observabilidade**
> - **Logar:** `highlight_list_fetch` com campos `[page, size, cache_hit, latency_ms, timestamp]` — nível `info`
> - **Logar:** `highlight_cache_invalidated` com campos `[cache_key, reason, timestamp]` — nível `info`
> - **Logar:** `highlight_expiration_job_run` com campos `[processed_count, expired_count, timestamp]` — nível `info`
> - **Métrica:** `highlight_list_cache_hit_rate`
> - **Métrica:** `highlight_expired_count`
> - **Alerta:** SE o job n�o expirar itens por mais de um ciclo esperado ENT�O avisar engenharia

**Critérios de aceite**
> - DADO uma publica��o destacada ativa QUANDO o usu�rio abrir a listagem ENT�O ela aparece antes das org�nicas.
> - DADO destaque expirado QUANDO o scheduler rodar ENT�O a publica��o volta ao fluxo org�nico.
> - DADO cache v�lido QUANDO a listagem for consultada ENT�O a resposta vem do Redis.

**Estado atual**
> A listagem atual existe sem ordenação monetizada, sem cache Redis e sem expiração automática de destaque.

**Mudanças necessárias**
> - **Banco de dados:** campos de destaque na publicação e índices para ordenação.
> - **Backend:** query ordenada, cache Redis e scheduler de expiração.
> - **Frontend:** exibição visual de destaque na listagem.
> - **Infra/Config:** Redis, cron do scheduler e TTL de cache.

**Definição de pronto**
> - [ ] Ordenação monetizada implementada
> - [ ] Redis aplicado na listagem
> - [ ] Scheduler remove destaque expirado
> - [ ] Cache invalidado após mudança de estado

---

<a id="spec-04"></a>
## SPEC-04 — Hardening, observabilidade e rollout

**Objetivo**
> Garantir que o módulo de monetização esteja pronto para operação com logs, métricas, alertas, testes e rollback seguro.

**Contexto**
> O discovery definiu logs estruturados, observabilidade básica e rollout com baixo risco. Como a feature envolve pagamento, a qualidade operacional precisa ser alta desde o MVP.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | DevOps / SRE | Define alertas e acompanha saúde do fluxo | Direto |
> | Backend Engineer | Adiciona telemetria e resiliência | Direto |
> | Frontend Engineer | Valida feedback de erro e estados de carregamento | Direto |
> | Produto | Aprova política de destaque e rollout | Direto |

**Comportamento esperado**
> Fluxo principal:
> 1. Feature entra em ambiente com monitoramento ativo.
> 2. Logs e métricas passam a registrar checkout, webhook, cache e expiração.
> 3. Time acompanha sucesso de checkout e latência de ativação.
> 4. Se houver problema, rollback desativa a feature sem quebrar a listagem org�nica.

**Regras de negócio**
> - O sistema DEVE registrar eventos de negócio relevantes.
> - O sistema DEVE ter alertas para webhook, checkout e latência de ativação.
> - O sistema PODE ser desligado por configuração.
> - SE a feature for desativada ENT�O a listagem org�nica deve continuar funcionando.

**Contrato de API**
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------:|-----------------|
> | GET | /api/highlight/health (opcional) | interna | - | 200 status | 503 |

**SLA e Performance**
> - Checkout P95 <= 1000ms.
> - Webhook P95 <= 1000ms.
> - Ativação visível na listagem em <= 5s.
> - Taxa de falha crítica dos endpoints principais <= 5%.

**Observabilidade**
> - **Logar:** eventos `highlight_*` definidos nas specs anteriores — nível conforme cenário
> - **Métrica:** `highlight_checkout_success_rate`
> - **Métrica:** `highlight_webhook_approved_rate`
> - **Métrica:** `highlight_list_cache_hit_rate`
> - **Métrica:** `highlight_activation_latency_ms`
> - **Alerta:** SE qualquer m�trica cr�tica sair do limite por 10-15min ENT�O notificar engenharia

**Critérios de aceite**
> - DADO ambiente de homologa��o QUANDO o fluxo completo for executado ENT�O logs e m�tricas ficam vis�veis.
> - DADO falha simulada no checkout ou webhook QUANDO o erro ocorrer ENT�O h� alerta e mensagem adequada.
> - DADO rollback de feature QUANDO desativada ENT�O a listagem org�nica permanece �ntegra.

**Estado atual**
> Não há observabilidade específica de monetização nem rollout formal para a feature.

**Mudanças necessárias**
> - **Banco de dados:** sem novas mudanças além das já previstas nas specs anteriores.
> - **Backend:** instrumentação, healthcheck opcional e flag de habilitação.
> - **Frontend:** mensagens de erro e estados de carregamento.
> - **Infra/Config:** dashboard, alertas, variáveis de ambiente e flag de feature.

**Definição de pronto**
> - [ ] Logs estruturados ativos
> - [ ] Métricas e alertas configurados
> - [ ] Rollout e rollback definidos
> - [ ] Feature validada em homologação

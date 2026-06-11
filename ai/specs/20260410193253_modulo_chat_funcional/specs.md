<a id="spec-01"></a>
## SPEC-01 — Acesso e autorização do chat

**Objetivo**
> Garantir que apenas usuários autenticados e participantes legítimos consigam acessar e operar o chat 1:1.

**Contexto**
> O módulo de chat já existe parcialmente, mas o controle de acesso precisa ser fechado de ponta a ponta para evitar acesso indevido e inconsistências de sessão.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Contratante | Inicia conversa com prestador | Direto |
> | Prestador | Responde mensagens | Direto |
> | Backend Engineer | Implementa regras de autorização | Direto |
> | QA | Valida cenários de acesso permitido/bloqueado | Direto |

**Comportamento esperado**
> Fluxo principal:
> 1. Usuário autenticado acessa rota de chat.
> 2. Frontend valida sessão e solicita dados do chat.
> 3. Backend valida token e tenant_id.
> 4. Operação é autorizada apenas se usuário for participante da conversa.
>
> Fluxo alternativo — sessão ausente/expirada:
> 1. Usuário tenta acessar chat sem token válido.
> 2. Backend retorna 401.
> 3. Frontend redireciona para login.
>
> Fluxo alternativo — acesso indevido:
> 1. Usuário autenticado tenta abrir conversa de terceiros.
> 2. Backend detecta que não é participante.
> 3. Backend retorna 403 e frontend mostra erro de permissão.

**Regras de negócio**
> - Usuário DEVE estar autenticado para qualquer operação de chat.
> - Usuário NÃO DEVE acessar conversa onde não é participante.
> - Sistema DEVE retornar 401 para token ausente/inválido.
> - Sistema DEVE retornar 403 para acesso sem autorização de domínio.
> - SE sessão expirar ENTÃO frontend DEVE redirecionar para login.

**Contrato de API**
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------:|-----------------|
> | GET | /api/v1/chat/conversas | sim | — | 200 lista de conversas do usuário | 401, 403 |
> | GET | /api/v1/chat/conversa/{outroUsuarioId} | sim | query limit | 200 lista de mensagens da conversa | 401, 403, 404 |
> | PUT | /api/v1/chat/marcar-lida/{mensagemId} | sim | — | 204 | 401, 403, 404 |

**SLA e Performance**
> - Latência máxima de autorização: 300ms P95 por request de chat.
> - Operações de acesso devem manter consistência com até 200 RPS agregados no backend.
> - Timeout de request frontend: 10s.

**Observabilidade**
> - **Logar:** `chat_access_denied` com campos `[user_id, target_user_id, route, reason, timestamp]` — nível `warn`.
> - **Logar:** `chat_auth_failure` com campos `[route, auth_header_present, timestamp]` — nível `warn`.
> - **Métrica:** `chat_auth_error_rate` — taxa de 401/403.
> - **Alerta:** SE `chat_auth_error_rate > 5%` POR `10min` ENTÃO notificar engenharia.

**Critérios de aceite**
> - DADO usuário autenticado QUANDO acessar conversa da qual participa ENTÃO o histórico é retornado com 200.
> - DADO usuário sem token válido QUANDO acessar /chat/conversas ENTÃO recebe 401 e é redirecionado ao login.
> - DADO usuário autenticado QUANDO tentar acessar conversa de terceiros ENTÃO recebe 403 e não visualiza dados.

**Estado atual**
> Existe autenticação JWT e validações de tenant no backend, porém a cobertura de cenários de acesso do chat precisa ser consolidada como contrato de feature.

**Mudanças necessárias**
> - **Banco de dados:** sem alteração estrutural.
> - **Backend:** reforçar validações em endpoints de leitura/marcação de leitura.
> - **Frontend:** garantir redirecionamento consistente para login ao receber 401.
> - **Infra/Config:** sem novos serviços.

**Definição de pronto**
> - [ ] Regras de acesso validadas em todos os endpoints de chat
> - [ ] Cenários 401/403 cobertos em teste de integração
> - [ ] UX de sessão expirada validada no frontend
> - [ ] Logs de auditoria de acesso negado disponíveis

---

<a id="spec-02"></a>
## SPEC-02 — Contrato de conversas e mensagens

**Objetivo**
> Padronizar o contrato de dados do chat entre frontend e backend com DTOs explícitos para conversas e mensagens, eliminando acoplamento com entidade JPA.

**Contexto**
> O endpoint de conversas atualmente expõe estrutura de domínio sem contrato estável para o frontend. Isso gera fragilidade e risco de quebra em serialização/evolução.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Backend Engineer | Define e implementa DTOs e mapeamentos | Direto |
> | Frontend Engineer | Consome contrato estável no chat | Direto |
> | QA | Valida payloads e cenários de erro | Direto |

**Comportamento esperado**
> Fluxo principal:
> 1. Frontend solicita lista de conversas.
> 2. Backend retorna `ConversationListItem[]`.
> 3. Frontend solicita histórico de conversa por usuário.
> 4. Backend retorna `MessageItem[]` com formato padronizado.
> 5. Frontend envia mensagem.
> 6. Backend valida limite de 500 caracteres e persiste mensagem.
>
> Fluxo alternativo — payload inválido:
> 1. Frontend envia conteúdo vazio ou maior que 500.
> 2. Backend rejeita com 400 e erro semântico.

**Regras de negócio**
> - Endpoint de conversas DEVE retornar DTO, não entidade JPA.
> - Mensagem DEVE ter conteúdo entre 1 e 500 caracteres.
> - Sistema NÃO DEVE permitir envio para si mesmo.
> - SE destinatário não existir ENTÃO retornar erro de domínio apropriado.

**Contrato de API**
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------:|-----------------|
> | GET | /api/v1/chat/conversas | sim | — | 200 `ConversationListItem[]` | 401, 403 |
> | GET | /api/v1/chat/conversa/{outroUsuarioId}?limit=50 | sim | — | 200 `MessageItem[]` | 400, 401, 403, 404 |
> | POST | /api/v1/chat/enviar?destinatarioId={id} | sim | `{ conteudo, publicacaoId?, clientTempId? }` | 201 `MessageItem` | 400, 401, 403, 404 |
> | GET | /api/v1/chat/nao-lidas | sim | — | 200 `{ totalNaoLidas }` | 401 |

**SLA e Performance**
> - Envio de mensagem: <= 1000ms P95.
> - Leitura de histórico (limit 50): <= 1200ms P95.
> - Contagem de não lidas: <= 400ms P95.
> - Suporte inicial de pico: 400 RPS agregados sem degradação crítica.

**Observabilidade**
> - **Logar:** `chat_contract_response` com campos `[route, payload_version, item_count, latency_ms]` — nível `info`.
> - **Logar:** `chat_contract_validation_error` com `[route, reason, user_id]` — nível `warn`.
> - **Métrica:** `chat_contract_error_rate`.
> - **Alerta:** SE `chat_contract_error_rate > 2%` POR `10min` ENTÃO abrir incidente de contrato.

**Critérios de aceite**
> - DADO request para /chat/conversas QUANDO operação for bem-sucedida ENTÃO retorno segue exatamente contrato `ConversationListItem[]`.
> - DADO envio com 501 caracteres QUANDO chamar /chat/enviar ENTÃO backend retorna 400 com mensagem de validação.
> - DADO frontend consumindo payload QUANDO renderizar chat ENTÃO não depende de campos internos de entidade JPA.

**Estado atual**
> Existem endpoints e modelos de chat ativos, mas sem padronização completa de contrato para consumo estável no frontend.

**Mudanças necessárias**
> - **Banco de dados:** sem novas tabelas; apenas consumo das existentes.
> - **Backend:** criar DTO de conversa, ajustar mapeamentos, reforçar validação de conteúdo.
> - **Frontend:** adaptar interfaces e parsing para os novos DTOs.
> - **Infra/Config:** sem novos componentes.

**Definição de pronto**
> - [ ] DTOs de conversa e mensagem implementados e documentados
> - [ ] Endpoint /chat/conversas sem retorno de entidade JPA
> - [ ] Limite de 500 caracteres aplicado no backend
> - [ ] Testes de contrato passando (integração)

---

<a id="spec-03"></a>
## SPEC-03 — UX de chat, polling e sino no header

**Objetivo**
> Entregar experiência funcional de chat no frontend com atualização em até 5 segundos e notificação global de não lidas no header.

**Contexto**
> O chat atual possui componentes e serviços já implementados, mas o ciclo de atualização, feedback de erro e integração com header ainda está incompleto.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | Usuário autenticado (prestador/contratante) | Conversa, lê mensagens e vê não lidas | Direto |
> | Frontend Engineer | Implementa polling, estados e sino do header | Direto |
> | QA | Valida estados de UI e sincronismo de não lidas | Direto |

**Comportamento esperado**
> Fluxo principal:
> 1. Usuário abre tela de chat.
> 2. Frontend carrega conversas e inicia polling de 5s.
> 3. Usuário seleciona conversa e visualiza histórico.
> 4. Frontend marca mensagens recebidas como lidas ao abrir conversa.
> 5. Usuário envia mensagem e UI atualiza sem recarregar página.
> 6. Header exibe sino com badge atualizado no mesmo ciclo de polling.
>
> Fluxo alternativo — erro de rede:
> 1. Polling falha temporariamente.
> 2. Interface mantém último estado válido.
> 3. Próximo ciclo tenta novamente.
>
> Fluxo alternativo — envio falhou:
> 1. Usuário envia mensagem.
> 2. Backend retorna erro.
> 3. UI mantém conteúdo e exibe opção de reenviar.

**Regras de negócio**
> - Frontend DEVE atualizar conversas e não lidas com intervalo de 5s.
> - Header DEVE exibir badge de não lidas para usuário autenticado.
> - Sistema NÃO DEVE permitir envio de texto vazio.
> - Sistema DEVE limitar input visual a 500 caracteres.
> - SE usuário abrir conversa ENTÃO mensagens recebidas dessa conversa DEVEM ser marcadas como lidas.

**Contrato de API**
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------:|-----------------|
> | GET | /api/v1/chat/conversas | sim | — | 200 lista de conversas | 401, 403 |
> | GET | /api/v1/chat/conversa/{outroUsuarioId}?limit=50 | sim | — | 200 lista de mensagens | 401, 403 |
> | GET | /api/v1/chat/nao-lidas | sim | — | 200 total não lidas | 401 |
> | POST | /api/v1/chat/enviar?destinatarioId={id} | sim | conteúdo de mensagem | 201 mensagem criada | 400, 401, 404 |

**SLA e Performance**
> - Atualização perceptível da UI de chat: <= 5s.
> - Tempo de renderização de lista/histórico: <= 700ms P95 no frontend.
> - Consumo de polling deve manter estabilidade em sessões concorrentes sem travamento de interface.

**Observabilidade**
> - **Logar:** `chat_ui_poll_tick` com `[user_id, unread_total, conversations_count, latency_ms]` — nível `info`.
> - **Logar:** `chat_ui_send_error` com `[user_id, destinatario_id, error_code]` — nível `warn`.
> - **Métrica:** `chat_ui_poll_failure_rate`.
> - **Métrica:** `chat_header_badge_sync_drift` (diferença entre badge e fonte de verdade).
> - **Alerta:** SE `chat_ui_poll_failure_rate > 5%` POR `10min` ENTÃO notificar frontend owner.

**Critérios de aceite**
> - DADO usuário autenticado QUANDO permanecer na tela de chat ENTÃO novas mensagens chegam na UI em até 5 segundos.
> - DADO usuário com mensagens não lidas QUANDO abrir aplicação autenticada ENTÃO sino do header mostra badge correto.
> - DADO erro de envio QUANDO backend falhar ENTÃO UI mantém texto digitado e exibe feedback de erro.

**Estado atual**
> Existe componente de chat e serviço HTTP, com polling parcial de não lidas. A notificação global no header ainda não está integrada.

**Mudanças necessárias**
> - **Banco de dados:** sem alteração.
> - **Backend:** garantir contrato estável para consumo da UI.
> - **Frontend:** ajustar fluxo de polling, marcação de leitura e integração do sino no header.
> - **Infra/Config:** configurar variáveis de polling e limite de badge no frontend.

**Definição de pronto**
> - [ ] Polling de 5s aplicado e encerrado corretamente em destroy
> - [ ] Badge de sino integrado no header e sincronizado
> - [ ] Fluxo de erro/retry de envio implementado
> - [ ] Testes manuais multiusuário validados

---

<a id="spec-04"></a>
## SPEC-04 — Hardening, observabilidade operacional e readiness AWS

**Objetivo**
> Fechar qualidade operacional do chat para deploy em AWS com testes, métricas, alertas e plano de rollback seguro.

**Contexto**
> A feature precisa subir em ambiente cloud com custo controlado e boa capacidade de diagnóstico. O discovery apontou dois pontos em aberto: observabilidade mínima e anti-spam.

**Personas e Papéis**
> | Papel | O que faz nesta spec | Impacto |
> |-------|----------------------|---------|
> | DevOps/SRE | Acompanha métricas e saúde da feature | Direto |
> | Backend/Frontend Engineers | Corrigem incidentes e otimizam performance | Direto |
> | Produto | Define política de anti-spam e limites operacionais | Direto |

**Comportamento esperado**
> Fluxo principal:
> 1. Feature é implantada com logs estruturados e métricas mínimas.
> 2. Dashboards exibem latência, sucesso de envio e taxa de erro.
> 3. Alertas disparam automaticamente em degradação.
> 4. Time aplica rollback rápido em caso de incidente crítico.
>
> Fluxo alternativo — sobrecarga:
> 1. Polling e/ou erros sobem acima do limiar.
> 2. Alerta é disparado.
> 3. Time aplica mitigação (ajuste de intervalo, hotfix, rollback).

**Regras de negócio**
> - Sistema DEVE registrar eventos mínimos de envio, leitura, erro e polling.
> - Sistema DEVE ter alerta para aumento de erro e latência crítica.
> - Sistema PODE degradar frequência de atualização em modo de contingência.
> - Sistema NÃO DEVE avançar para produção pública sem política de rate limit definida.

**Contrato de API**
> | Método | Path | Auth | Payload | Resposta de sucesso | Erros esperados |
> |--------|------|------|---------|--------------------:|-----------------|
> | GET | /api/v1/chat/health (opcional) | interna | — | 200 status da feature | 503 |
> | GET | /api/v1/chat/nao-lidas | sim | — | 200 total não lidas | 401, 500 |

**SLA e Performance**
> - Latência p95 de envio de mensagem: <= 1500ms.
> - Taxa de sucesso de envio: >= 95% por janela de 10 minutos.
> - Taxa de erro total dos endpoints de chat: <= 5%.
> - Atualização de não lidas: <= 5s (SLA funcional).

**Observabilidade**
> - **Logar:** `chat_send_success`, `chat_send_failure`, `chat_poll_failure`, `chat_mark_read_failure`.
> - **Métricas:** `chat_send_success_rate`, `chat_send_latency_p95`, `chat_poll_latency_p95`, `chat_error_rate`.
> - **Alertas:**
>   - `chat_send_success_rate < 95% por 10min`
>   - `chat_error_rate > 5% por 10min`
>   - `chat_poll_latency_p95 > 2000ms por 15min`

**Critérios de aceite**
> - DADO ambiente de homologação QUANDO executar testes de carga moderada ENTÃO métricas de chat ficam visíveis em dashboard.
> - DADO degradação simulada QUANDO ultrapassar limiar configurado ENTÃO alerta operacional é disparado.
> - DADO incidente crítico QUANDO executar rollback ENTÃO a aplicação retorna ao estado estável anterior.

**Estado atual**
> Existem eventos e métricas sugeridos no briefing, mas ainda sem fechamento formal de dashboard e política de anti-spam.

**Mudanças necessárias**
> - **Banco de dados:** sem alteração.
> - **Backend:** instrumentação final e política de limite de envio.
> - **Frontend:** captura de falhas de polling e envio para telemetria.
> - **Infra/Config:** criação de dashboard/alertas e playbook de rollback.

**Definição de pronto**
> - [ ] Dashboard operacional mínimo definido e publicado
> - [ ] Alertas críticos configurados e testados
> - [ ] Política de rate limit por usuário aprovada e aplicada
> - [ ] Checklist de readiness para AWS assinado

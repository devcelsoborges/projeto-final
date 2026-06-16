<!-- Fonte: conteúdo colado em 2026-04-29 -->
<!-- Coletado durante o discovery de 20260429211111_monetizacao_publicacoes_destaque -->

# Documento base

Você é um desenvolvedor sênior especialista em Spring Boot, arquitetura de microsserviços e integrações de pagamento.

Sua tarefa é projetar e implementar um módulo de monetização para a plataforma BrJobs, onde usuários podem pagar para destacar suas publicações (vagas), similar ao modelo de destaque utilizado em marketplaces como a OLX.

Objetivo

Permitir que usuários promovam suas vagas mediante pagamento, fazendo com que essas publicações tenham maior visibilidade na listagem.

Requisitos funcionais
Planos de destaque
Criar planos como:
Básico (3 dias)
Premium (7 dias)
Top (15 dias)
Cada plano deve ter:
preço
duração
nível de prioridade
Integração com pagamento
Integrar com Stripe (principal)
Preparar arquitetura para suportar outros gateways futuramente
Fluxo:
Criar sessão de pagamento
Redirecionar usuário
Receber webhook de confirmação
Ativar destaque da publicação
Destaque de publicações
Campo isHighlighted
Campo highlightExpiresAt
Atualização automática após pagamento aprovado
Listagem de publicações
Criar endpoint com ordenação:
Publicações destacadas ativas primeiro
Ordenar por nível do plano (Top > Premium > Básico)
Depois ordenar por data de criação (mais recentes)
Paginação obrigatória
Expiração automática
Job (scheduler) para remover destaque após expiração
Requisitos técnicos
Backend: Spring Boot
Banco: PostgreSQL
Segurança: JWT
Arquitetura limpa (Clean Architecture / DDD)
Uso de DTOs
Separação de camadas (Controller, Service, Repository)
Modelagem sugerida

Tabela: job_posts

id
title
description
is_highlighted
highlight_expires_at
highlight_plan_id

Tabela: highlight_plans

id
name
price
duration_days
priority

Tabela: payments

id
job_post_id
stripe_session_id
status
amount
created_at

Endpoints esperados
POST /highlight/checkout/{jobPostId}
POST /webhook/stripe
GET /jobs (com ordenação inteligente)
GET /highlight/plans
Regras importantes
Não ativar destaque antes da confirmação do pagamento
Validar se a vaga pertence ao usuário
Evitar duplicidade de pagamentos ativos
Garantir idempotência no webhook
Extras (diferencial)
Cache com Redis para listagem
Logs estruturados
Testes unitários
Retry para falha no webhook
Entregáveis
Código completo
Explicação da arquitetura
Fluxo de pagamento detalhado
Exemplos de requisição/resposta

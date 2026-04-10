# Feature Flags & Rollout Plan

## Feature Flags (via Spring Cloud Config)

```properties
# application-features.properties

# WP-05: Chat Real-Time
feature.chat.enabled=true
feature.chat.notifications.enabled=true
feature.chat.socketio.enabled=true

# WP-06: Avaliações
feature.avaliacoes.enabled=true
feature.avaliacoes.filtro-palavras=true
feature.avaliacoes.min-rating=1
feature.avaliacoes.max-rating=5

# WP-07: Relatório Ganhos
feature.ganhos.relatorio.enabled=true
feature.ganhos.cache.ttl.hours=24

# WP-08: Publicar Serviço
feature.servicos.criar.enabled=true
feature.servicos.criarcategories=pintura,encanamento,eletrica,marcenaria,limpeza
```

## Rollout Strategy

### Phase 1: Canary (5% users, 2 dias)
- Deploy backend API v1.1
- Enable feature flags apenas para seleto 5% de prestadores
- Monitor: error_rate, latency, CPU, memory
- Success criteria: < 0.5% error rate

### Phase 2: Early Adopters (20% users, 3 dias)
- Gradual increase para 20% de usuário
- Coleta de feedback via in-app survey
- Success criteria: > 4.0/5.0 user satisfaction

### Phase 3: General Availability (100%, full release)
- Deploy para todos os usuários
- Ativa todas as features por default
- Marketing email campaign
- Monitor: Daily Active Users, feature adoption rate

## Deployment Checklist

Backend (brjobs-java):
- [ ] All unit tests passing (mvn clean test)
- [ ] All integration tests passing (mvn integration-test)  
- [ ] SonarQube quality gate passed
- [ ] Database migration tested (V2-V5 applied)
- [ ] CORS settings verified for frontend URL
- [ ] Secrets in .env, not in git
- [ ] AWS ECR image built with `./build-push.sh`
- [ ] Load test passed (500 concurrent users, p99 < 2s)

Frontend (brjobs-angular):
- [ ] npm run lint (ESLint) passing
- [ ] npm run typecheck (TypeScript) passing
- [ ] npm run build (production build) successful
- [ ] dist/ bundle size < 500KB (gzipped)
- [ ] No console errors in production build
- [ ] Smoke tests on live deployment passed

Database:
- [ ] Backup taken before migration
- [ ] Flyway migrations applied (V1 baseline through V5)
- [ ] Indexes created for tenant_id queries
- [ ] Trigger for earnings cache working
- [ ] Foreign keys configured with CASCADE DELETE

## Monitoring & Alerts

Dashboard (Prometheus + Grafana):
- API response time (p50, p95, p99)
- Error rate by endpoint
- Database connection pool usage
- JVM heap memory
- Chat message throughput
- Frontend bundle performance

Alerts (critical, requires immediate action):
- Error rate > 1%
- API latency p99 > 5s
- DB connection errors
- OOM (Out of Memory)
- Feature flag service down

## Rollback Plan

If issues detected:

1. **Immediate (< 5 min):**
   - Set feature flags to false in Spring Cloud Config
   - This disables new features without redeploying

2. **Short-term (< 30 min):**
   - Reroute traffic to previous version via load balancer
   - Do not modify database

3. **Manual steps if needed:**
   ```bash
   # Rollback ECS task to previous image
   aws ecs update-service --cluster prod --service brjobs-api \
     --force-new-deployment \
     --image brjobs:v1.0.0
   ```

## Success Metrics (Post-Launch)

| Metric | Target | Frequency |
|--------|--------|-----------|
| Feature Adoption Rate | > 40% within 2 weeks | Daily |
| User Satisfaction | > 4.2/5.0 | Weekly survey |
| Error Rate | < 0.3% | Continuous |
| API Latency p99 | < 2s | Real-time monitoring |
| Chat Message Delivery | > 99.5% success | Hourly |
| Avaliação Creation Success | > 98% | Daily |
| Ganhos Report Generation | < 500ms | Real-time |


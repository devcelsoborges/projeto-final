# Social Login Setup - Configuração de Provedores OAuth2

## Resumo

Este documento guia a configuração **100% GRATUITA** de autenticação social via Google, Facebook e Apple para o brjobs. Todos os provedores oferecem contas de desenvolvedor sem custo.

---

## 1. Google Sign-In (GRATUITO)

### Configuração Rápida

1. **Acessar Google Cloud Console:**
   - URL: https://console.cloud.google.com
   - Login com conta Google

2. **Criar Projeto:**
   - Novo Projeto → Nome: "brjobs"
   - Aguardar criação (~1-2 min)

3. **Ativar Google+ API:**
   - Menu: APIs & Services → Library
   - Buscar: "Google+ API"
   - Clicar: "ENABLE"

4. **Criar Credenciais OAuth2:**
   - Menu: APIs & Services → Credentials
   - Create Credentials → OAuth 2.0 Client ID
   - Application Type: Web application
   - Name: "brjobs-webapp"
   - **Authorized redirect URIs:**
     ```
     http://localhost:4200/login
     http://localhost:4200/
     http://localhost:8080/login/oauth2/code/google
     https://seu-dominio-producao.com/login
     ```
   - Clique: Create
   - Copiar: Client ID e Client Secret

5. **Salvar Credenciais:**
   ```bash
   # Em backend/.env ou docker-compose.yml
   GOOGLE_CLIENT_ID=xxx-yyy.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=GOCSPX-zzz
   ```

### Teste Local

```bash
# Backend encontra GOOGLE_CLIENT_ID automaticamente
# Frontend atualizar:
# brjobs-angular/src/app/service/social-auth.service.ts
const clientId = 'xxx-yyy.apps.googleusercontent.com'; // linha ~45
```

---

## 2. Facebook Login (GRATUITO com App Grátis)

### Configuração Rápida

1. **Acessar Meta Developers:**
   - URL: https://developers.facebook.com
   - Login com conta Facebook (criar se necessário)

2. **Criar App:**
   - My Apps → Create App
   - App Type: Consumer
   - App Name: "brjobs"
   - Next, selecionar "Facebook Login"

3. **Configurar Facebook Login:**
   - Products → Facebook Login → Settings
   - **Redirect URIs:**
     ```
     http://localhost:4200/login
     http://localhost:8080/login/oauth2/code/facebook
     https://seu-dominio-producao.com/login
     ```
   - Save

4. **Obter Credenciais:**
   - Settings → Basic
   - Copiar: App ID e App Secret

5. **Salvar Credenciais:**
   ```bash
   # Em backend/.env ou docker-compose.yml
   FACEBOOK_APP_ID=123456789
   FACEBOOK_APP_SECRET=abc123def456
   ```

### Teste Local

```bash
# Frontend atualizar:
# brjobs-angular/src/app/service/social-auth.service.ts
appId: '123456789' // linha ~67
```

---

## 3. Apple Sign-In (GRATUITO com Apple Developer Account)

### Configuração Rápida

1. **Acessar Apple Developer:**
   - URL: https://developer.apple.com
   - Login com Apple ID

2. **Registrar App ID:**
   - Certificates, Identifiers & Profiles → Identifiers
   - Clique: + (novo)
   - Type: App IDs
   - Descrever o app: "brjobs"
   - Identifier: com.brjobs.app (ou seu domínio reverso)
   - Capabilities: Sign in with Apple (ativar)
   - Register

3. **Configurar Service ID:**
   - Certificates, Identifiers & Profiles → Identifiers
   - Clique: + (novo)
   - Type: Service IDs
   - Description: "brjobs-web"
   - Identifier: com.brjobs.app.service (legal identifier)
   - Clicar: Configure
   - **Web Redirect URIs:**
     ```
     http://localhost:4200/login
     http://localhost:8080/login/oauth2/code/apple
     https://seu-dominio-producao.com/login
     ```
   - Save

4. **Gerar Private Key:**
   - Certificates, Identifiers & Profiles → Keys
   - Clique: + (nova)
   - Key Name: "brjobs-signin"
   - Ativar: Sign in with Apple
   - Clicar: Configure
   - Primary App ID: selecionar "brjobs"
   - Clicar: Save
   - Clicar: Create Key
   - **Baixar arquivo .p8** (salvar em local seguro)

5. **Obter IDs:**
   - Team ID: Canto superior direito (Membership)
   - Key ID: No arquivo .p8 ou na lista de chaves
   - Service ID: Com.brjobs.app.service
   - Client ID: O mesmo que Service ID

6. **Salvar Credenciais:**
   ```bash
   # Em backend/.env ou docker-compose.yml
   APPLE_CLIENT_ID=com.brjobs.app.service
   APPLE_TEAM_ID=ABC123XYZ    # 10 caracteres, numéricos
   APPLE_KEY_ID=AAAABBBBCC    # 10 caracteres, alfanuméricos
   APPLE_KEY_FILE=/path/to/private/key.p8
   ```

### Teste Local

```bash
# Frontend atualizar:
# brjobs-angular/src/app/service/social-auth.service.ts
clientId: 'com.brjobs.app.service'    // linha ~109
teamId: 'ABC123XYZ'                    // linha ~110
keyId: 'AAAABBBBCC'                    // linha ~111
```

---

## Integração com Backend (Docker Compose)

### arquivo: docker-compose.yml

```yaml
version: '3.9'

services:
  backend:
    build: ./brjobs-java
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/brjobsdb
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: 160788
      BRJOBS_JWT_SECRET: mySecretKeyForJWTTokenGenerationAndValidationMustBeAtLeast256BitsLongForHS512Algorithm
      BRJOBS_JWT_EXPIRATION: 3600000
      # ===== OAuth2 Social Login =====
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
      FACEBOOK_APP_ID: ${FACEBOOK_APP_ID}
      FACEBOOK_APP_SECRET: ${FACEBOOK_APP_SECRET}
      APPLE_CLIENT_ID: ${APPLE_CLIENT_ID}
      APPLE_TEAM_ID: ${APPLE_TEAM_ID}
      APPLE_KEY_ID: ${APPLE_KEY_ID}
    depends_on:
      - postgres

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_PASSWORD: 160788
      POSTGRES_DB: brjobsdb
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  web:
    build: ./brjobs-angular
    ports:
      - "4200:4200"
    environment:
      NEXT_PUBLIC_API_URL: http://localhost:8080/api/v1

volumes:
  postgres_data:
```

### arquivo: .env

```bash
# Copiar este arquivo para variáveis de ambiente

# Google OAuth2
GOOGLE_CLIENT_ID=xxx-yyy.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-zzz

# Facebook OAuth2
FACEBOOK_APP_ID=123456789
FACEBOOK_APP_SECRET=abc123def456

# Apple Sign-In
APPLE_CLIENT_ID=com.brjobs.app.service
APPLE_TEAM_ID=ABC123XYZ
APPLE_KEY_ID=AAAABBBBCC
```

### Executar Stack com OAuth2:

```bash
# Carregar variáveis
source .env

# Iniciar todos os serviços
docker compose up --build

# Backend com OAuth2 ativo em:
# http://localhost:8080/api/v1/auth/social/google
# http://localhost:8080/api/v1/auth/social/facebook
# http://localhost:8080/api/v1/auth/social/apple
```

---

## Fluxo de Autenticação

### 1. Frontend (Angular):

```
User clica "Login com Google"
  ↓
JavaScript Google SDK abre popup
  ↓
User autoriza em accounts.google.com
  ↓
Google retorna ID Token ao navegador
  ↓
Frontend envia ID Token via POST /api/v1/auth/social/google
```

### 2. Backend (Java/Spring):

```
Recebe ID Token
  ↓
Valida assinatura (via Google Public Keys)
  ↓
Extrai claims (email, nome, foto, providerId)
  ↓
Busca ou cria Usuario no BD
  ↓
Busca ou cria SocialLogin linking usuario ↔ provedor
  ↓
Gera JWT próprio do brjobs
  ↓
Retorna { token, refreshToken, usuarioId, email, nome }
```

### 3. Frontend (Angular):

```
Armazena JWT em localStorage
  ↓
Redireciona para /home
  ↓
AuthService envia JWT em Authorization header
```

---

## Endpoints da API

### Documentação Swagger

```
http://localhost:8080/swagger-ui.html
```

### Endpoints Social Login

| Método | URL | Corpo | Resposta |
|--------|-----|-------|----------|
| POST | /api/v1/auth/social/google | `{ "idToken": "..." }` | `{ "token", "refreshToken", "usuarioId", "email", "nome" }` |
| POST | /api/v1/auth/social/facebook | `{ "accessToken": "..." }` | `{ "token", "refreshToken", "usuarioId", "email", "nome" }` |
| POST | /api/v1/auth/social/apple | `{ "idToken": "...", "code": "..." }` | `{ "token", "refreshToken", "usuarioId", "email", "nome" }` |
| DELETE | /api/v1/auth/social/{provider} | Header: `Authorization: Bearer <jwt>` | 204 No Content |

---

## Segurança & Boas Práticas

### ✅ Feito Neste Projeto

- **Validação de tokens**: Backend valida ID Tokens/Access Tokens
- **Tenant isolation**: Todos os endpoints validam tenant via JWT
- **Rate limiting**: Provedores nativas (Google/Facebook/Apple) já possuem quotas
- **HTTPS em produção**: Configure seu domínio com SSL/TLS
- **CORS**: Backend restringe requisições ao domínio frontend
- **JWT próprio**: Nunca armazenamos OAuth tokens no BD, apenas ID do provedor

### 🚀 Para Produção

1. **Atualizar Redirect URIs:**
   - Google, Facebook, Apple: Alterar localhost para seu domínio

2. **Ativar HTTPS:**
   ```bash
   # Certificado Let's Encrypt gratuito
   certbot certonly --standalone -d seu-dominio.com
   ```

3. **Variáveis de Ambiente:**
   ```bash
   exporte GOOGLE_CLIENT_ID=...
   export GOOGLE_CLIENT_SECRET=...
   # Não armazenar em Git!
   ```

4. **Monitoramento:**
   - Logs de OAuth2 em: `backend/logs/`
   - Métricas: `/api/v1/metrics`

---

## Troubleshooting

### Erro: "cors_error" no console do navegador

**Causa:** Frontend tentando chamar endpoint OAuth2 do provedor sem credenciais

**Solução:**
```bash
# Verificar se scripts estão carregando:
# GET https://accounts.google.com (?)
# GET https://connect.facebook.net (?)
# GET https://appleid.cdn-apple.com (?)

# Abrir DevTools → Network → verificar


3xx status codes
```

### Erro: "Invalid client ID" no backend

**Causa:** GOOGLE_CLIENT_ID não corresponde ao usado no frontend

**Solução:**
```bash
# Atualizar ambos:
# 1. brjobs-angular/src/app/service/social-auth.service.ts
# 2. .env ou docker-compose.yml

# Reiniciar backend/frontend
```

### Erro: "Token validation failed" no backend

**Causa:** Token expirado (Google tokens expiram em ~1 hora)

**Solução:** Frontend gera novo token ao fazer login novamente

---

## Referências

- [Google OAuth2 Docs](https://developers.google.com/identity/protocols/oauth2)
- [Facebook Login Docs](https://developers.facebook.com/docs/facebook-login)
- [Apple Sign-In Docs](https://developer.apple.com/sign-in-with-apple/)
- [Spring OAuth2 Docs](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)

---

## Suporte

Dúvidas sobre configuração?

1. Verificar logs do backend:
   ```bash
   docker logs brjobs-java
   ```

2. Verificar console do navegador:
   ```
   DevTools → Console → filtrar "oauth"
   ```

3. Testar endpoint direto:
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/social/google \
     -H "Content-Type: application/json" \
     -d '{"idToken": "teste"}'
   ```

---

**Criado em:** 2024-12-19  
**Versão:** 1.0  
**Status:** Production-Ready (100% Gratuito)

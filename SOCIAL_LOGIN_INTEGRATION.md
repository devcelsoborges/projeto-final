# Social Login Integration Guide

## Resumo

Este guia descreve como integrar o novo componente `SocialLoginComponent` (Google, Facebook, Apple) na página de login e registro existente do brjobs.

---

## 1. Componentes Criados

### `SocialLoginComponent` 
**Localização:** `brjobs-angular/src/app/components/social-login/social-login.component.ts`

**Funcionalidade:**
- Exibe 3 botões: Google, Facebook, Apple
- Gerencia fluxo de autenticação social
- Retorna JWT via evento `loginSucesso`

**Propriedades:**
- `@Input() redirecionar: boolean` - Auto-redireciona para /home após login
- `@Output() loginSucesso: EventEmitter<AuthResponseDTO>` - Emite após sucesso
- `@Output() loginErro: EventEmitter<string>` - Emite mensagem de erro

---

## 2. Integração com LoginComponent Existente

### Editar: `brjobs-angular/src/app/components/login/login.component.ts`

```typescript
import { SocialLoginComponent } from '../social-login/social-login.component';
import { AuthResponseDTO } from '../../service/social-auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    NgIf,
    ReactiveFormsModule,
    RouterModule,
    FormsModule,
    SocialLoginComponent  // ← ADICIONAR AQUI
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit, OnDestroy {
  // ... código existente ...

  /**
   * Callback quando login social é bem-sucedido
   */
  onLoginSocialSucesso(response: AuthResponseDTO): void {
    // Já armazenado pelo SocialLoginComponent
    // Apenas logar sucesso
    console.log('Login social bem-sucedido:', response.email);
  }

  /**
   * Callback quando login social falha
   */
  onLoginSocialErro(erro: string): void {
    this.errorMessage = erro;
  }
}
```

### Editar: `brjobs-angular/src/app/components/login/login.component.html`

```html
<div class="login-container">
  <div class="login-card">
    <h2>Login</h2>

    <!-- FORMULÁRIO DE LOGIN TRADICIONAL (existente) -->
    <form [formGroup]="form" (ngSubmit)="onSubmit()" *ngIf="!loading">
      <div class="form-group">
        <label for="email">Email:</label>
        <input
          id="email"
          type="email"
          formControlName="email"
          placeholder="seu-email@example.com"
          class="form-control"
        />
        <!-- validadores existentes ... -->
      </div>

      <div class="form-group">
        <label for="senha">Senha:</label>
        <input
          id="senha"
          type="password"
          formControlName="senha"
          placeholder="Sua senha segura"
          class="form-control"
        />
      </div>

      <button type="submit" class="btn btn-primary btn-block">
        Entrar
      </button>
    </form>

    <!-- ===== NOVO: SOCIAL LOGIN COMPONENT ===== -->
    <app-social-login
      [redirecionar]="true"
      (loginSucesso)="onLoginSocialSucesso($event)"
      (loginErro)="onLoginSocialErro($event)"
    ></app-social-login>

    <!-- Link para registro -->
    <div class="text-center margin-top-20">
      <p>
        Não tem conta?
        <a routerLink="/register">Registre-se aqui</a>
      </p>
      <a routerLink="/forgot-password" class="forgot-password-link">
        Esqueceu sua senha?
      </a>
    </div>
  </div>
</div>
```

---

## 3. Integração com RegisterComponent

### Adicionar ao RegisterComponent

```typescript
import { SocialLoginComponent } from '../social-login/social-login.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    SocialLoginComponent  // ← ADICIONAR
  ],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  // ...

  onRegistroSocialSucesso(response: AuthResponseDTO): void {
    // Usuário criado automaticamente via OAuth2
    console.log('Registro social bem-sucedido:', response.email);
  }
}
```

### Adicionar ao template de registro

```html
<div class="register-container">
  <h2>Criar Conta</h2>

  <!-- Formulário tradicional existente aqui -->
  <form [formGroup]="form" (ngSubmit)="onSubmit()">
    <!-- ... campos de registro existentes ... -->
  </form>

  <!-- ===== OU REGISTRE COM SOCIAL ===== -->
  <p class="text-center">Ou registre-se rapidamente com:</p>
  <app-social-login
    [redirecionar]="true"
    (loginSucesso)="onRegistroSocialSucesso($event)"
  ></app-social-login>

  <!-- Link para login -->
  <p class="text-center">
    Já tem conta? <a routerLink="/login">Faça login</a>
  </p>
</div>
```

---

## 4. Configurar Credenciais OAuth2 no Frontend

### Editar: `brjobs-angular/src/app/service/social-auth.service.ts`

Localize e atualize as seguintes linhas:

**Google:**
```typescript
// Linha ~47
(window as any).google.accounts.id.initialize({
  client_id: 'SEU_GOOGLE_CLIENT_ID',  // ← SUBSTITUIR
  // ...
});
```

**Facebook:**
```typescript
// Linha ~70
FB.init({
  appId: 'SEU_FACEBOOK_APP_ID',  // ← SUBSTITUIR
  // ...
});
```

**Apple:**
```typescript
// Linha ~110
(window as any).AppleID.auth.init({
  clientId: 'SEU_APPLE_CLIENT_ID',      // ← SUBSTITUIR
  teamId: 'SEU_APPLE_TEAM_ID',           // ← SUBSTITUIR
  keyId: 'SEU_APPLE_KEY_ID',             // ← SUBSTITUIR
  // ...
});
```

### Melhor: Usar variáveis de ambiente

```typescript
// Criar arquivo: brjobs-angular/.env
VITE_GOOGLE_CLIENT_ID=seu-google-client-id
VITE_FACEBOOK_APP_ID=seu-facebook-app-id
VITE_APPLE_CLIENT_ID=seu-apple-client-id

// Em social-auth.service.ts
const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
// ...
```

---

## 5. Fluxo de Autenticação

### Diagrama Completo

```
┌─────────────────┐
│  Angular Login  │
└────────┬────────┘
         │
         ├─→ Email/Senha
         │      │
         │      └─→ POST /api/v1/auth/login
         │           ↓
         │      Retorna JWT
         │      Armazena em localStorage
         │      Redireciona /home
         │
         └─→ Social Login (NOVO)
                  │
                  ├─→ Google Sign-In
                  │      │
                  │      └─→ ID Token
                  │
                  ├─→ Facebook Login
                  │      │
                  │      └─→ Access Token
                  │
                  └─→ Apple Sign-In
                         │
                         └─→ Identity Token
                               ↓
                         POST /api/v1/auth/social/{provider}
                               ↓
                         Backend valida token
                         Cria/atualiza Usuario
                         Cria/busca SocialLogin
                               ↓
                         Retorna JWT
                         Armazena em localStorage
                         Redireciona /home
```

### Unificação de contas pelo e-mail

O e-mail é a chave de identidade entre todos os métodos de autenticação. Cadastros e
logins com o mesmo e-mail — via Google, Facebook ou formulário do site — acessam a
**mesma conta**:

| Cenário | Comportamento |
|---------|---------------|
| Login social com e-mail já cadastrado no site | Vincula o provedor à conta existente e entra nela |
| Login social com e-mail já usado em outro provedor | Mesma conta; cada provedor vira uma linha em `social_logins` |
| Login social com e-mail novo | Cria a conta (CONTRATANTE) já com o e-mail normalizado |
| Cadastro no site com e-mail que já existe (conta local **ou** social) | HTTP 409 — orienta a entrar pelo provedor ou usar "Esqueci minha senha" |
| Login com senha em conta social que ainda não definiu senha | HTTP 401 com mensagem orientando a entrar com o provedor ou definir senha via "Esqueci minha senha" |
| Definir senha local numa conta criada via social | Via "Esqueci minha senha" (`/api/auth/forgot-password/*`) — verifica a posse do e-mail |

Princípio de segurança: o e-mail é a chave única (case-insensitive) de conta. A
vinculação "mesmo e-mail = mesma conta" acontece **no login social** (o provedor já
provou a posse do e-mail) e na ação **autenticada/verificada** de definir senha
(reset por código no e-mail). O cadastro anônimo **nunca** define senha numa conta
existente — caso contrário, qualquer um que soubesse o e-mail de uma conta Google
poderia assumi-la (account takeover).

Detalhes de implementação:

- Normalização: `UsuarioValidator.normalizarEmail()` (trim + lowercase) em toda escrita;
  busca por `UsuarioRepository.findByEmailIgnoreCase()` em toda leitura/login.
- Defesa no banco: índice único funcional `ux_usuarios_email_lower` (migração `V17`).
- Conta "somente social" é detectada por `Usuario.isSomenteLoginSocial()` (senha
  placeholder `OAUTH2_<PROVIDER>`/`SOCIAL_LOGIN`, nunca um hash BCrypt).
- Apple está desabilitado (`SocialAuthService.loginComApple` lança
  `UnsupportedOperationException`) até que a validação de assinatura do token seja implementada.
- Testes: `UsuarioServiceContaUnificadaTest` e `AuthSessionServiceLoginSocialTest`.

---

## 6. Backend API (Endpoints)

Todos os endpoints retornam:

```json
{
  "token": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "refreshToken": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "usuarioId": 123,
  "email": "user@example.com",
  "nome": "João Silva"
}
```

### Endpoints Disponíveis

| Método | URL | Descrição |
|--------|-----|-----------|
| POST | `/api/v1/auth/social/google` | Login com Google |
| POST | `/api/v1/auth/social/facebook` | Login com Facebook |
| POST | `/api/v1/auth/social/apple` | Login com Apple |
| DELETE | `/api/v1/auth/social/{provider}` | Desconectar provedor |

---

## 7. Tratamento de Erros

### Frontend

```typescript
// Se login/reg social falha
onLoginSocialErro(erro: string): void {
  // Exibir toast/alert
  alert(`Erro: ${erro}`);
  
  // OU redirecionar para página de ajuda
  this.router.navigate(['/ajuda', 'oauth-error'], {
    queryParams: { erro }
  });
}
```

### Backend

Todos os erros retornam com `HttpStatus.UNAUTHORIZED` (401):

```json
{
  "error": "Google login falhou: Token inválido ou expirado"
}
```

---

## 8. Teste Local

### Pré-requisitos

```bash
# 1. Configurar credenciais OAuth2 (ver SOCIAL_LOGIN_SETUP.md)
# 2. Iniciar backend
cd brjobs-java
mvn spring-boot:run

# 3. Iniciar frontend
cd brjobs-angular
npm run dev
```

### Teste Manual

1. Acessar: http://localhost:4200/login
2. Clicar botão "Google"
3. Autorizar na popup
4. Verificar se redireciona para /home
5. Verificar localStorage:
   ```
   DevTools → Application → localStorage
   - token: JWT armazenado
   - refreshToken: Refresh token
   - usuarioId: ID do usuário criado
   ```

### Teste com cURL (Backend)

```bash
# Simular envio de ID Token Google
curl -X POST http://localhost:8080/api/v1/auth/social/google \
  -H "Content-Type: application/json" \
  -d '{"idToken": "eyJ0eXAiOiJKV1QiLCJhbGc..."}'

# Resposta esperada:
{
  "token": "...",
  "refreshToken": "...",
  "usuarioId": 1,
  "email": "user@example.com",
  "nome": "João Silva"
}
```

---

## 9. Problemas Comuns & Soluções

### Erro: "Google is not defined"

```
❌ Solução: Scripts não carregaram
✅ Verificar se https://accounts.google.com/gapi/client:platform.js retorna status 200
```

### Erro: "CORS error" no console

```
❌ Causa: CORS não configurado no backend
✅ Verificar application.properties:
   spring.web.cors.allowed-origins=http://localhost:4200
```

### ID Token não valida

```
❌ Causa: Token expirado (Google tokens expiram em ~1 hora)
   OU Client ID não corresponde ao configurado
✅ Fazer login novamente para gerar novo token
✅ Verificar Client ID em:
   - Google Cloud Console
   - brjobs-angular/src/app/service/social-auth.service.ts
```

### Usuário criado mas não consegue fazer login novamente

```
❌ Causa: SocialLogin não está sendo encontrado no banco
✅ Verificar:
   - Migração V6 foi executada (tabela social_logins existe)
   - SocialLoginRepository retorna encontrado(provider, providerId)
```

---

## 10. Estrutura de Arquivos

```
brjobs-angular/src/app/
├── components/
│   ├── login/
│   │   ├── login.component.ts
│   │   ├── login.component.html (ATUALIZAR)
│   │   └── login.component.css
│   ├── register/
│   │   ├── register.component.ts
│   │   ├── register.component.html (ATUALIZAR)
│   │   └── register.component.css
│   └── social-login/                   (NOVO)
│       └── social-login.component.ts
├── service/
│   ├── auth.service.ts                 (EXISTENTE)
│   └── social-auth.service.ts          (NOVO)

brjobs-java/src/main/java/ads/uninassau/brjobs/
├── controller/
│   └── SocialAuthController.java       (NOVO)
├── service/
│   └── SocialAuthService.java          (NOVO)
├── model/
│   └── SocialLogin.java                (NOVO)
└── repository/
    └── SocialLoginRepository.java      (NOVO)

brjobs-java/src/main/resources/db/migration/
└── V6__create_social_logins_table.sql  (NOVO)
```

---

## 11. Deployment em Produção

### Checklist

- [ ] Atualizar Redirect URIs em Google, Facebook, Apple
- [ ] Configurar variáveis de ambiente (.env)
- [ ] Ativar HTTPS no servidor
- [ ] Verificar CORS em produção
- [ ] Atualizar domínio em application.properties
- [ ] Criar backup do banco de dados
- [ ] Executar migração V6
- [ ] Testar login social em produção
- [ ] Monitorar logs por 24 horas

### Variáveis de Ambiente (Exemplo)

```bash
export GOOGLE_CLIENT_ID=xxx-yyy.apps.googleusercontent.com
export GOOGLE_CLIENT_SECRET=GOCSPX-zzz
export FACEBOOK_APP_ID=123456789
export FACEBOOK_APP_SECRET=abc123def456
export APPLE_CLIENT_ID=com.brjobs.app.service
export APPLE_TEAM_ID=ABC123XYZ
export APPLE_KEY_ID=AAAABBBBCC
```

---

## Referências

- [SOCIAL_LOGIN_SETUP.md](../SOCIAL_LOGIN_SETUP.md) - Configuração detalhada
- [Google OAuth2 Docs](https://developers.google.com/identity/protocols/oauth2)
- [Facebook Login Docs](https://developers.facebook.com/docs/facebook-login)
- [Apple Sign-In Docs](https://developer.apple.com/sign-in-with-apple/)

---

**Criado:** 2024-12-19  
**Versão:** 1.0  
**Status:** Ready for Integration

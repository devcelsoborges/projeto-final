# 🔐 Configuração OAuth2 - Google, Facebook e Apple

Para que o login social funcione, você precisa configurar os **Client IDs** dos provedores OAuth. Siga as instruções abaixo:

---

## 1️⃣ Google Sign-In

### 1.1 Criar um projeto no Google Cloud Console

1. Acesse: https://console.cloud.google.com
2. Clique em **"Selecionar um projeto"** → **"Novo projeto"**
3. Nomeie como `brjobs` e clique em **"Criar"**
4. Aguarde alguns segundos para o projeto ser criado

### 1.2 Criar credenciais OAuth2

1. No painel esquerdo, clique em **"APIs e serviços"** → **"Credenciais"**
2. Clique em **"Criar credenciais"** → **"ID do cliente OAuth"**
3. Se solicitado, clique em **"Configurar tela de consentimento"**:
   - Selecione **"Externo"** como tipo de usuário
   - Preencha os campos obrigatórios (nome do app, email de suporte)
   - Pule as etapas de escopos
   - Adicione sua conta como testador
   - Salve e continue

4. Volte para **"Credenciais"** → **"Criar credenciais"** → **"ID do cliente OAuth"**
5. Selecione **"Aplicação Web"** como tipo:
   - Digite `http://localhost:4200` em **"URIs de origem JavaScript autorizado"**
   - Digite `http://localhost:4200/callback` em **"URIs de redirecionamento autorizado"**
6. Clique em **"Criar"**
7. Copie o **"ID do Cliente"** (algo como `123456789-abcdefg.apps.googleusercontent.com`)

### 1.3 Adicionar o ID no arquivo de configuração

Edite: `brjobs-angular/src/app/environments/environment.ts`

```typescript
export const environment = {
  // ...
  oauth: {
    google: {
      clientId: 'SEU_CLIENT_ID_AQUI', // Cole aqui
    },
    // ...
  }
};
```

---

## 2️⃣ Facebook Login

### 2.1 Criar um app no Facebook Developer

1. Acesse: https://developers.facebook.com
2. Clique em **"Meus Apps"** → **"Criar App"**
3. Selecione **"Consumidor"** como tipo
4. Preencha os dados:
   - Nome da App: `brjobs`
   - Email: seu email
   - Clique em **"Criar App"**

### 2.2 Configurar Facebook Login

1. No dashboard, clique em **"Adicionar produto"**
2. Procure por **"Facebook Login"** e clique em **"Configurar"**
3. Clique em **"Configurações"** → **"Configurações básicas do login do Facebook"**
4. Em **"URIs de redirecionamento válidos"**, adicione:
   - `http://localhost:4200`
   - `http://localhost:4200/callback`
5. Salve as alterações

### 2.3 Obter o App ID

1. Vá para **"Configurações"** → **"Básico"**
2. Copie o **"ID do App"** (números longos)
3. Cole em `brjobs-angular/src/app/environments/environment.ts`

```typescript
export const environment = {
  // ...
  oauth: {
    facebook: {
      appId: 'SEU_APP_ID_AQUI', // Cole aqui (somente números)
    },
    // ...
  }
};
```

---

## 3️⃣ Apple Sign In (Opcional)

Apple requer configuração mais complexa no Apple Developer. Para agora, deixe os placeholders:

```typescript
apple: {
  teamId: 'YOUR_APPLE_TEAM_ID',
  clientId: 'YOUR_APPLE_CLIENT_ID',
}
```

---

## ✅ Testando o Login Social

1. Certifique-se de que o arquivo `environment.ts` foi atualizado com os IDs reais
2. Abra o **Console do navegador** (F12 → Console)
3. Vá para `http://localhost:4200/login`
4. Clique em **"Google"** ou **"Facebook"**
5. Verifique os logs do console:
   - Se ver `🔓 Iniciando login com GOOGLE...` → clique está funcionando
   - Se ver erro "client_id inválido" → verifique se o ID foi copiado corretamente
   - Se ver erro "CORS" → o backend precisa estar configurado

---

## 🐛 Troubleshooting

### "client_id inválido"
- Verifique se o ID foi copiado corretamente em `environment.ts`
- Certifique-se de que digitou com aspas simples ou duplas

### "Popup foi bloqueado"
- O browser bloqueou a janela popup do Google
- Permita popups para `localhost:4200` nas configurações do navegador

### "CORS error"
- O backend está respondendo com erro CORS
- Verifique se o backend está rodando em `http://localhost:8080`
- Verifique se `application.properties` tem a origem correta:
  ```
  app.cors.allowed-origins=http://localhost:4200
  ```

### "SDK não carregado"
- Veja o Console (F12 → Console → tab Network)
- Verifique se o script (`https://accounts.google.com/gsi/client`) foi carregado
- Pode haver problema de internet ou bloqueio

---

## 📚 Referências

- **Google OAuth 2.0**: https://developers.google.com/identity/protocols/oauth2
- **Facebook Login**: https://developers.facebook.com/docs/facebook-login
- **Google Sign-In for Web**: https://developers.google.com/identity/sign-in/web

---

## 🚀 Próximos Passos

Uma vez que os IDs estejam configurados corretamente, você pode:

1. Testar o login social no frontend
2. Verificar se o backend está respondendo em `/api/v1/auth/social-login/{google|facebook|apple}`
3. Implementar a lógica de registro automático no backend (se não existir)
4. Configurar variáveis de ambiente em produção (não faça hardcode de secrets!)

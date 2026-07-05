// ConfiguraÃ§Ãµes de ambiente
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  stripePublishableKey: 'pk_test_51TRiFRLCyLGNKGa5saHOfAfCDTEGFhOxjHUVBFmax0aMumrDbDfXIJVMhgW0028mOW48qEb1mqoZloqwR8uWXWnk008vxfcNp1',
  chat: {
    // Conversa aberta (mensagens que o usuário está lendo): mantém a cadência atual.
    activePollIntervalMs: 15000,
    // Badge de não-lidas em segundo plano (todas as telas): mais lento e barato.
    unreadPollIntervalMs: 30000,
    maxMessageLength: 500,
    headerBadgeMax: 99
  },
  features: {
    structuralAuth: true,
    publicationGeo: true,
    dynamicNotifications: true
  },
  defaultTheme: 'system' as 'system' | 'light' | 'dark',
  uxTelemetryEnabled: true,
  perfSampleRate: 0.2,
  oauth: {
    google: {
      clientId: '562205988451-gu1vcc47c8nffla5eabhk6o3p00k8s1n.apps.googleusercontent.com',
    },
    facebook: {
      appId: 'YOUR_FACEBOOK_APP_ID', // Obtenha em: https://developers.facebook.com
    }
  }
};

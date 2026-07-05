// ConfiguraÃ§Ãµes de ambiente
export const environment = {
  production: true,
  apiUrl: 'https://api.brjobs.com.br',
  stripePublishableKey: 'pk_live_51TRiEzLBtI5b9UJx2SrkaZDDwPpLwePGZFG34pCqCNdfJtcAGkBjf09hCwyEu6UNqOR0NBW8OU6rAwGQxtWRCkWt00bEP7Y8mU',
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

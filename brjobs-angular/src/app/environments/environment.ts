// Configurações de ambiente
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  defaultTheme: 'system' as 'system' | 'light' | 'dark',
  uxTelemetryEnabled: true,
  perfSampleRate: 0.2,
  oauth: {
    google: {
      clientId: '562205988451-gu1vcc47c8nffla5eabhk6o3p00k8s1n.apps.googleusercontent.com',
    },
    facebook: {
      appId: 'YOUR_FACEBOOK_APP_ID', // Obtenha em: https://developers.facebook.com
      // Exemplo: '1234567890123456'
    },
    apple: {
      teamId: 'YOUR_APPLE_TEAM_ID', // Obtenha em: https://developer.apple.com
      clientId: 'YOUR_APPLE_CLIENT_ID',
    }
  }
};

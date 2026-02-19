import { createI18n } from 'vue-i18n';
import en from './locales/en.json';
import ar from './locales/ar.json';

// Get saved language from localStorage or default to English
const savedLanguage = localStorage.getItem('appLanguage') || 'en';

const i18n = createI18n({
  legacy: false,
  locale: savedLanguage,
  fallbackLocale: 'en',
  messages: {
    en,
    ar
  }
});

export default i18n;


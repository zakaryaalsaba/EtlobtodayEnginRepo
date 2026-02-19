import { createI18n } from 'vue-i18n'
import en from './locales/en.json'
import ar from './locales/ar.json'

const i18n = createI18n({
  legacy: false,
  locale: localStorage.getItem('locale') || 'en',
  fallbackLocale: 'en',
  messages: { en, ar }
})

export default i18n

export function setDocumentDirection(locale) {
  const dir = locale === 'ar' ? 'rtl' : 'ltr'
  const lang = locale === 'ar' ? 'ar' : 'en'
  document.documentElement.setAttribute('dir', dir)
  document.documentElement.setAttribute('lang', lang)
}

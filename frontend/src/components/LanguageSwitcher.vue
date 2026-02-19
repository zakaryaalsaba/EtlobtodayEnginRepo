<template>
  <div class="relative">
    <button
      @click="showDropdown = !showDropdown"
      class="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-gray-100 transition-colors"
      :class="{ 'bg-gray-100': showDropdown }"
    >
      <span class="text-lg">{{ currentLanguageFlag }}</span>
      <span class="text-sm font-medium text-gray-700">{{ currentLanguageName }}</span>
      <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
      </svg>
    </button>

    <div
      v-if="showDropdown"
      @click.stop
      class="absolute top-full mt-2 w-40 bg-white rounded-lg shadow-lg border border-gray-200 py-2 z-50"
      :class="currentLocale === 'ar' ? 'left-0' : 'right-0'"
    >
      <button
        v-for="lang in languages"
        :key="lang.code"
        @click="switchLanguage(lang.code)"
        class="w-full px-4 py-2 text-left hover:bg-gray-100 transition-colors flex items-center gap-3"
        :class="{ 'bg-indigo-50 text-indigo-600': currentLocale === lang.code }"
      >
        <span class="text-lg">{{ lang.flag }}</span>
        <span class="text-sm font-medium">{{ lang.name }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useI18n } from 'vue-i18n';

const props = defineProps({
  availableLanguages: {
    type: Array,
    default: () => [
      { code: 'en', name: 'English', flag: '🇺🇸' },
      { code: 'ar', name: 'العربية', flag: '🇸🇦' }
    ]
  }
});

const { locale } = useI18n();

const showDropdown = ref(false);

const languages = computed(() => {
  // If availableLanguages prop is provided, use it; otherwise use default
  return props.availableLanguages && props.availableLanguages.length > 0
    ? props.availableLanguages
    : [
        { code: 'en', name: 'English', flag: '🇺🇸' },
        { code: 'ar', name: 'العربية', flag: '🇸🇦' }
      ];
});

const currentLocale = computed(() => locale.value);

const currentLanguageFlag = computed(() => {
  const lang = languages.value.find(l => l.code === locale.value);
  return lang ? lang.flag : '🇺🇸';
});

const currentLanguageName = computed(() => {
  const lang = languages.value.find(l => l.code === locale.value);
  return lang ? lang.name : 'English';
});

const switchLanguage = (langCode) => {
  locale.value = langCode;
  localStorage.setItem('appLanguage', langCode);
  
  // Update HTML dir and lang attributes
  document.documentElement.setAttribute('lang', langCode);
  document.documentElement.setAttribute('dir', langCode === 'ar' ? 'rtl' : 'ltr');
  
  showDropdown.value = false;
};

const handleClickOutside = (event) => {
  if (showDropdown.value && !event.target.closest('.relative')) {
    showDropdown.value = false;
  }
};

onMounted(() => {
  // Set initial dir and lang
  const savedLang = localStorage.getItem('appLanguage') || 'en';
  document.documentElement.setAttribute('lang', savedLang);
  document.documentElement.setAttribute('dir', savedLang === 'ar' ? 'rtl' : 'ltr');
  
  // Close dropdown when clicking outside
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>


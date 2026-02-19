import { onMounted, onUnmounted } from 'vue';

/**
 * SEO composable for managing meta tags, Open Graph, and structured data
 */
export function useSEO() {
  const updateMetaTag = (name, content, attribute = 'name') => {
    if (!content) return;
    
    let element = document.querySelector(`meta[${attribute}="${name}"]`);
    if (!element) {
      element = document.createElement('meta');
      element.setAttribute(attribute, name);
      document.head.appendChild(element);
    }
    element.setAttribute('content', content);
  };

  const updateTitle = (title) => {
    if (title) {
      document.title = title;
    }
  };

  const updateLink = (rel, href) => {
    if (!href) return;
    
    let element = document.querySelector(`link[rel="${rel}"]`);
    if (!element) {
      element = document.createElement('link');
      element.setAttribute('rel', rel);
      document.head.appendChild(element);
    }
    element.setAttribute('href', href);
  };

  const setSEO = (options = {}) => {
    const {
      title,
      description,
      keywords,
      image,
      url,
      type = 'website',
      siteName = 'RestaurantAI',
      locale = 'en',
      alternateLocale,
      canonical,
      noindex = false,
      structuredData,
    } = options;

    // Title
    if (title) {
      updateTitle(title);
      updateMetaTag('og:title', title, 'property');
      updateMetaTag('twitter:title', title);
    }

    // Description
    if (description) {
      updateMetaTag('description', description);
      updateMetaTag('og:description', description, 'property');
      updateMetaTag('twitter:description', description);
    }

    // Keywords
    if (keywords) {
      updateMetaTag('keywords', Array.isArray(keywords) ? keywords.join(', ') : keywords);
    }

    // Image
    if (image) {
      const fullImageUrl = image.startsWith('http') ? image : `${window.location.origin}${image}`;
      updateMetaTag('og:image', fullImageUrl, 'property');
      updateMetaTag('twitter:image', fullImageUrl);
      updateMetaTag('twitter:card', 'summary_large_image');
    }

    // URL
    if (url) {
      const fullUrl = url.startsWith('http') ? url : `${window.location.origin}${url}`;
      updateMetaTag('og:url', fullUrl, 'property');
      updateLink('canonical', fullUrl);
    } else if (canonical) {
      const fullCanonical = canonical.startsWith('http') ? canonical : `${window.location.origin}${canonical}`;
      updateLink('canonical', fullCanonical);
    }

    // Type
    updateMetaTag('og:type', type, 'property');

    // Site Name
    updateMetaTag('og:site_name', siteName, 'property');

    // Locale
    updateMetaTag('og:locale', locale, 'property');
    if (alternateLocale) {
      updateMetaTag('og:locale:alternate', alternateLocale, 'property');
    }

    // Robots
    if (noindex) {
      updateMetaTag('robots', 'noindex, nofollow');
    } else {
      updateMetaTag('robots', 'index, follow');
    }

    // Structured Data (JSON-LD)
    if (structuredData) {
      let script = document.querySelector('script[type="application/ld+json"]');
      if (!script) {
        script = document.createElement('script');
        script.setAttribute('type', 'application/ld+json');
        document.head.appendChild(script);
      }
      script.textContent = JSON.stringify(structuredData, null, 2);
    }
  };

  const clearSEO = () => {
    // Reset to defaults
    updateTitle('RestaurantAI - Restaurant Website Builder Platform');
    updateMetaTag('description', 'Create beautiful restaurant websites with online ordering. Start your restaurant website in minutes.');
    updateMetaTag('og:title', 'RestaurantAI - Restaurant Website Builder Platform', 'property');
    updateMetaTag('og:description', 'Create beautiful restaurant websites with online ordering. Start your restaurant website in minutes.', 'property');
    updateMetaTag('robots', 'index, follow');
    
    // Remove structured data
    const script = document.querySelector('script[type="application/ld+json"]');
    if (script) {
      script.remove();
    }
  };

  return {
    setSEO,
    clearSEO,
    updateMetaTag,
    updateTitle,
    updateLink,
  };
}


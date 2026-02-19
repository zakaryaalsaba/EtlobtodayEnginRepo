import { pool } from '../db/init.js';

/**
 * Domain-based routing middleware
 * Detects subdomain or custom domain and sets website context
 */
export async function domainRouter(req, res, next) {
  try {
    // Skip domain routing for API routes
    if (req.path.startsWith('/api/')) {
      return next();
    }
    
    const host = req.get('host') || '';
    const protocol = req.protocol;
    
    // Remove port if present (e.g., localhost:3000 -> localhost)
    const hostname = host.split(':')[0];
    
    // Get the base domain from environment (e.g., yourplatform.com)
    const baseDomain = process.env.BASE_DOMAIN || 'localhost';
    
    // Check if it's a custom domain (not the base domain and not a subdomain of base domain)
    let website = null;
    
    if (hostname !== baseDomain && !hostname.endsWith('.' + baseDomain)) {
      // This might be a custom domain
      const [customDomainWebsites] = await pool.execute(
        'SELECT * FROM restaurant_websites WHERE custom_domain = ? AND is_published = 1',
        [hostname]
      );
      
      if (customDomainWebsites.length > 0) {
        website = customDomainWebsites[0];
        req.websiteContext = {
          website: website,
          type: 'custom_domain',
          domain: hostname
        };
        return next();
      }
    }
    
    // Check for subdomain (e.g., restaurant1.yourplatform.com)
    if (hostname.includes('.')) {
      const parts = hostname.split('.');
      
      // If it's a subdomain of the base domain
      if (hostname.endsWith('.' + baseDomain) || baseDomain === 'localhost') {
        const subdomain = parts[0];
        
        // Skip 'www' subdomain
        if (subdomain !== 'www' && subdomain !== 'api' && subdomain !== 'admin') {
          const [subdomainWebsites] = await pool.execute(
            'SELECT * FROM restaurant_websites WHERE subdomain = ? AND is_published = 1',
            [subdomain]
          );
          
          if (subdomainWebsites.length > 0) {
            website = subdomainWebsites[0];
            req.websiteContext = {
              website: website,
              type: 'subdomain',
              subdomain: subdomain
            };
            return next();
          }
        }
      }
    }
    
    // No domain match found, continue with normal routing
    req.websiteContext = null;
    next();
  } catch (error) {
    console.error('Error in domain router:', error);
    // On error, continue with normal routing
    req.websiteContext = null;
    next();
  }
}

/**
 * Get website by domain or subdomain
 */
export async function getWebsiteByDomain(domain) {
  try {
    // Try custom domain first
    const [customDomainWebsites] = await pool.execute(
      'SELECT * FROM restaurant_websites WHERE custom_domain = ? AND is_published = 1',
      [domain]
    );
    
    if (customDomainWebsites.length > 0) {
      return customDomainWebsites[0];
    }
    
    // Try subdomain
    const [subdomainWebsites] = await pool.execute(
      'SELECT * FROM restaurant_websites WHERE subdomain = ? AND is_published = 1',
      [domain]
    );
    
    if (subdomainWebsites.length > 0) {
      return subdomainWebsites[0];
    }
    
    return null;
  } catch (error) {
    console.error('Error getting website by domain:', error);
    return null;
  }
}


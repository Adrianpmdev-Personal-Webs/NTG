/**
 * API_BASE_URL
 *
 * Base URL of the backend API. It is always relative, so the browser only talks to the site's own domain:
 * in production Vercel forwards /api/* to the backend (vercel.json) and in development `ng serve` does the same
 * (proxy.conf.json). The session cookie is first-party in both cases.
 *
 * @author @adrianpmdev
 */

export const API_BASE_URL = '/api/v1';

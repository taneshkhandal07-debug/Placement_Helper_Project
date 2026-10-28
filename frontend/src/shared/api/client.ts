import axios, { AxiosError } from 'axios';

/**
 * Shared Axios client configured for same-origin session credentials,
 * SPA CSRF token propagation, and unified error handling.
 */
export const apiClient = axios.create({
  baseURL: '/api',
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
});

/**
 * Request Interceptor:
 * Ensures the X-XSRF-TOKEN header is attached on all mutating HTTP methods
 * (POST, PUT, DELETE, PATCH) by reading the XSRF-TOKEN cookie if present.
 */
apiClient.interceptors.request.use((config) => {
  const method = (config.method || 'get').toLowerCase();
  if (['post', 'put', 'delete', 'patch'].includes(method)) {
    if (typeof document !== 'undefined') {
      const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
      if (match && match[1]) {
        config.headers['X-XSRF-TOKEN'] = decodeURIComponent(match[1]);
      }
    }
  }
  return config;
});

/**
 * Response Interceptor:
 * Handles HTTP 401 Unauthorized (session expiry) and 403 Forbidden cleanly.
 */
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<{ error?: string; message?: string }>) => {
    if (error.response?.status === 401) {
      const url = error.config?.url || '';
      // Don't trigger session-expired event on initial auth probe or credentials check
      if (!url.includes('/auth/me') && !url.includes('/auth/login')) {
        console.warn('[API] Session expired or unauthorized on', url);
        if (typeof window !== 'undefined') {
          window.dispatchEvent(new CustomEvent('auth:session-expired'));
        }
      }
    } else if (error.response?.status === 403) {
      console.warn('[API] Access forbidden on', error.config?.url);
    }
    return Promise.reject(error);
  }
);

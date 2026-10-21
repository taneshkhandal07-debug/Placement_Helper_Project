import axios, { AxiosError } from 'axios';

export const apiClient = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<{ error?: string; message?: string }>) => {
    // Standardized API error handling
    if (error.response?.status === 401) {
      // Unauthenticated session
      console.debug('[API] Unauthenticated session on', error.config?.url);
    }
    return Promise.reject(error);
  }
);

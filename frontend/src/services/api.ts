import axios from 'axios';

// API Configuration
// Check if running in production (built app) or development (vite dev server)
const isProduction = import.meta.env.PROD;

// Production: Use relative path (proxied by Nginx)
// Development: Use Vite proxy
const API_BASE_URL = isProduction 
  ? '/api'  // Nginx will proxy to http://52.221.245.143:8080
  : '/api/aws';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptors for auth
api.interceptors.request.use((config: any) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptors for error handling
api.interceptors.response.use(
  (response: any) => response,
  (error: any) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('authToken');
      localStorage.removeItem('userId');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
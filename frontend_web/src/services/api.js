import axios from 'axios';

// Backend URL for production - use environment variable if available
const BACKEND_URL = import.meta.env.VITE_API_URL || 'https://judify-795422705086.asia-east1.run.app';

// Determine if we're in production based on environment or URL
const isProduction = () => {
  return import.meta.env.PROD || 
    (typeof window !== 'undefined' && 
     !window.location.hostname.includes('localhost') && 
     !window.location.hostname.includes('127.0.0.1'));
};

// Configure axios defaults - ALWAYS use the full backend URL
const api = axios.create({
  baseURL: BACKEND_URL,
  timeout: 15000, // 15 seconds timeout
  headers: {
    'Content-Type': 'application/json'
  }
});

// Log the current baseURL configuration
console.log(`API configured with baseURL: ${api.defaults.baseURL}`);

// Add request interceptor to include auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token') || localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    
    // Log API requests in development
    if (!isProduction()) {
      console.log(`🚀 API Request: ${config.method?.toUpperCase()} ${config.baseURL}${config.url}`);
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add response interceptor to handle common errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Handle 401 Unauthorized errors (token expired or invalid)
    if (error.response && error.response.status === 401) {
      // Clear storage and redirect to login
      localStorage.removeItem('auth_token');
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      
      // Only redirect if we're in a browser context
      if (typeof window !== 'undefined') {
        window.location.href = '/login';
      }
    }
    
    return Promise.reject(error);
  }
);

export default api; 
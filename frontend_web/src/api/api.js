/**
 * API Client Module
 * 
 * This file re-exports all API functionality from services/api.js
 * to maintain a single source of truth for API configuration and endpoints.
 * 
 * This approach eliminates duplication and ensures consistency across the application.
 */

// Import all exports from services/api.js
import api from '../services/api';

// Re-export everything from services/api.js
export * from '../services/api';

// Export the default API instance
export default api;

// Log API configuration
console.log(`API client using baseURL: ${api.defaults.baseURL} (imported from services/api.js)`);

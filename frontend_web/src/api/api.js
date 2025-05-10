/**
 * API Compatibility Layer
 * 
 * This file exists to support legacy imports from "api/api.js" while we transition
 * to the new modular API structure. It re-exports all API functions from their
 * individual modules.
 * 
 * IMPORTANT: For new code, import directly from the dedicated API modules or
 * use the central exports from 'api/index.js'.
 * 
 * Example:
 *   // Preferred approach
 *   import { messageApi, userApi } from '../api';
 *   
 *   // Legacy approach (still supported)
 *   import { messageApi } from '../api/api';
 */

// Import the base API instance
import api from './baseApi';

// Import all API modules
import { authApi } from './authApi';
import { userApi } from './userApi';
import { tutorProfileApi } from './tutorApi';
import { studentProfileApi } from './studentApi';
import { tutoringSessionApi } from './sessionApi';
import { messageApi } from './messageApi';
import { conversationApi } from './conversationApi';
import { reviewApi } from './reviewApi';
import { tutorAvailabilityApi, calendarApi } from './availabilityApi';
import { paymentApi } from './paymentApi';
import { notificationApi } from './notificationApi';

// Re-export everything
export {
  api,
  authApi,
  userApi,
  tutorProfileApi,
  studentProfileApi,
  tutoringSessionApi,
  messageApi,
  conversationApi,
  reviewApi,
  tutorAvailabilityApi,
  calendarApi,
  paymentApi,
  notificationApi
};

// Legacy default export for maximum compatibility
export default api;

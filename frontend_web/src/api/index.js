// Export the API instance
export { default as api } from './baseApi';

// Export all API modules
export * from './authApi';
export * from './userApi';
export * from './tutorApi';
export * from './studentApi';
export * from './sessionApi';
export * from './messageApi';
export * from './conversationApi';
export * from './reviewApi';
export * from './availabilityApi';
export * from './paymentApi';
export * from './notificationApi';

// Export compatibility layer for legacy code
export * from './api'; 
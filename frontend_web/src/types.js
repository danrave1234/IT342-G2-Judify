/**
 * User role constants
 * LEARNER is mapped to STUDENT for backend compatibility (matching mobile app)
 */
export const USER_ROLES = {
  STUDENT: 'STUDENT',
  LEARNER: 'STUDENT', // Map LEARNER to STUDENT for backend compatibility
  TUTOR: 'TUTOR',
  ADMIN: 'ADMIN'
};

/**
 * Session status constants
 */
export const SESSION_STATUS = {
  PENDING: 'PENDING',
  SCHEDULED: 'SCHEDULED',
  CONFIRMED: 'CONFIRMED',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED',
  CANCELED: 'CANCELED'
};

/**
 * Payment status constants
 */
export const PAYMENT_STATUS = {
  PENDING: 'PENDING',
  COMPLETED: 'COMPLETED',
  REFUNDED: 'REFUNDED',
  FAILED: 'FAILED'
};

/**
 * Message type constants
 */
export const MESSAGE_TYPE = {
  TEXT: 'TEXT',
  IMAGE: 'IMAGE',
  FILE: 'FILE',
  SYSTEM: 'SYSTEM'
};

/**
 * Notification type constants
 */
export const NOTIFICATION_TYPE = {
  BOOKING: 'BOOKING',
  MESSAGE: 'MESSAGE',
  REVIEW: 'REVIEW',
  PAYMENT: 'PAYMENT',
  SYSTEM: 'SYSTEM'
}; 

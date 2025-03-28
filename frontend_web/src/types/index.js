/**
 * Type definitions for the Judify app
 */

// User types
export const USER_ROLES = {
  STUDENT: 'STUDENT',
  TUTOR: 'TUTOR',
  ADMIN: 'ADMIN'
};

export const SESSION_STATUS = {
  SCHEDULED: 'SCHEDULED',
  ONGOING: 'ONGOING',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED'
};

export const PAYMENT_STATUS = {
  PENDING: 'PENDING',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED',
  REFUNDED: 'REFUNDED'
};

// User type
export class User {
  userId;
  username;
  email;
  firstName;
  lastName;
  role;
  profilePicture;
  contactDetails;
  createdAt;
  updatedAt;
}

// Authentication response
export class AuthResponse {
  userId;
  username;
  email;
  firstName;
  lastName;
  role;
  token;
  isAuthenticated;
}

// Tutor Profile
export class TutorProfile {
  profileId;
  userId;
  username;
  bio;
  expertise;
  hourlyRate;
  subjects = [];
  rating;
  totalReviews;
  createdAt;
}

// Tutoring Session
export class TutoringSession {
  sessionId;
  tutorId;
  studentId;
  startTime;
  endTime;
  subject;
  status; // SCHEDULED, ONGOING, COMPLETED, CANCELLED
  price;
  notes;
  locationData;  // For in-person sessions
  meetingLink;   // For online sessions
  createdAt;
  updatedAt;
}

// Review
export class Review {
  reviewId;
  sessionId;
  tutorId;
  studentId;
  rating;
  comment;
  createdAt;
}

// Payment Transaction
export class PaymentTransaction {
  transactionId;
  sessionId;
  payerId;
  payeeId;
  amount;
  status;
  paymentStatus;
  paymentGatewayReference;
  transactionReference;
  createdAt;
  updatedAt;
}

// Message
export class Message {
  messageId;
  conversationId;
  senderId;
  receiverId;
  content;
  isRead;
  createdAt;
}

// Conversation
export class Conversation {
  conversationId;
  user1Id;
  user2Id;
  lastMessageTime;
  createdAt;
}

// Notification
export class Notification {
  notificationId;
  userId;
  type;
  message;
  isRead;
  relatedEntityId;
  createdAt;
}

// Tutor Availability
export class TutorAvailability {
  availabilityId;
  tutorId;
  day;
  startTime;
  endTime;
  isRecurring;
  createdAt;
} 
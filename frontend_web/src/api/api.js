import axios from 'axios';

// Create an axios instance with defaults
const API = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for adding auth token
API.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for handling errors
API.interceptors.response.use(
  (response) => response,
  (error) => {
    // Handle 401 Unauthorized - redirect to login
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }

    // Log details of server errors for debugging
    if (error.response && error.response.status >= 500) {
      console.error('Server error:', error.response.data);
    }

    return Promise.reject(error);
  }
);

// User/Auth API endpoints
export const authApi = {
  login: (email, password) => API.post(`/users/authenticate?email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`),
  register: (userData) => API.post('/users/addUser', userData),
  verify: () => API.get('/users/verify'),
  resetPassword: (email) => API.post('/users/reset-password', { email }),
};

// User API endpoints
export const userApi = {
  getCurrentUser: () => API.get('/users/me'),
  updateUser: (userId, userData) => API.put(`/users/${userId}`, userData),
  uploadProfilePicture: (userId, formData) => 
    API.post(`/users/${userId}/profile-picture`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }),
};

// Tutor Profiles API endpoints
export const tutorProfileApi = {
  getProfiles: (params) => API.get('/tutor-profiles', { params }),
  getProfileById: (profileId) => API.get(`/tutor-profiles/${profileId}`),
  getProfileByUserId: (userId) => API.get(`/tutor-profiles/user/${userId}`),
  createProfile: (profileData) => API.post('/tutor-profiles', profileData),
  updateProfile: (profileId, profileData) => API.put(`/tutor-profiles/${profileId}`, profileData),
  searchProfiles: (params) => API.get('/tutor-profiles/search', { params }),
};

// Tutoring Sessions API endpoints
export const tutoringSessionApi = {
  getSessions: (params) => API.get('/tutoring-sessions', { params }),
  getSessionById: (sessionId) => API.get(`/tutoring-sessions/${sessionId}`),
  createSession: (sessionData) => API.post('/tutoring-sessions/createSession', sessionData),
  updateSession: (sessionId, sessionData) => API.put(`/tutoring-sessions/${sessionId}`, sessionData),
  updateSessionStatus: (sessionId, status) => 
    API.patch(`/tutoring-sessions/${sessionId}/status`, { status }),
  getTutorSessions: (tutorId, params) => 
    API.get(`/tutoring-sessions/findByTutor/${tutorId}`, { params }),
  getStudentSessions: (studentId, params) => 
    API.get(`/tutoring-sessions/student/${studentId}`, { params }),
  // New endpoints for negotiation
  acceptSession: (sessionId) => API.put(`/tutoring-sessions/acceptSession/${sessionId}`),
  rejectSession: (sessionId) => API.put(`/tutoring-sessions/rejectSession/${sessionId}`),
  negotiateSession: (sessionId, sessionData) => API.put(`/tutoring-sessions/negotiateSession/${sessionId}`, sessionData),
};

// Reviews API endpoints
export const reviewApi = {
  getReviews: (params) => API.get('/reviews', { params }),
  getReviewById: (reviewId) => API.get(`/reviews/${reviewId}`),
  createReview: (reviewData) => API.post('/reviews', reviewData),
  updateReview: (reviewId, reviewData) => API.put(`/reviews/${reviewId}`, reviewData),
  getTutorReviews: (tutorId, params) => API.get(`/reviews/tutor/${tutorId}`, { params }),
};

// Payment Transactions API endpoints
export const paymentApi = {
  getTransactions: (params) => API.get('/payments', { params }),
  getTransactionById: (transactionId) => API.get(`/payments/${transactionId}`),
  createTransaction: (paymentData) => API.post('/payments', paymentData),
  updateTransaction: (transactionId, paymentData) => 
    API.put(`/payments/${transactionId}`, paymentData),
};

// Message API endpoints
export const messageApi = {
  getMessages: (conversationId, params) => 
    API.get(`/messages/conversation/${conversationId}`, { params }),
  sendMessage: (messageData) => API.post('/messages', messageData),
  markAsRead: (messageId) => API.patch(`/messages/${messageId}/read`),
};

// Conversation API endpoints
export const conversationApi = {
  getConversations: (userId) => API.get(`/conversations/user/${userId}`),
  getConversation: (conversationId) => API.get(`/conversations/${conversationId}`),
  createConversation: (data) => API.post('/conversations', data),
};

// Notification API endpoints
export const notificationApi = {
  getNotifications: (userId, params) => 
    API.get(`/notifications/user/${userId}`, { params }),
  markAsRead: (notificationId) => API.patch(`/notifications/${notificationId}/read`),
  markAllAsRead: (userId) => API.patch(`/notifications/user/${userId}/read-all`),
};

// Tutor Availability API endpoints
export const tutorAvailabilityApi = {
  getAvailabilities: (tutorId) => API.get(`/tutor-availability/tutor/${tutorId}`),
  createAvailability: (availabilityData) => API.post('/tutor-availability', availabilityData),
  updateAvailability: (availabilityId, availabilityData) => 
    API.put(`/tutor-availability/${availabilityId}`, availabilityData),
  deleteAvailability: (availabilityId) => API.delete(`/tutor-availability/${availabilityId}`),
};

export default API; 

import axios from 'axios';

// Backend URL configuration - use environment variables with appropriate fallbacks
// In development: Use VITE_API_URL or the development URL (matching mobile app)
// In production: Use VITE_API_URL or the deployed Cloud Run URL
const BACKEND_URL = import.meta.env.VITE_API_URL || 
  (import.meta.env.PROD 
    ? 'https://judify-795422705086.asia-east1.run.app' 
    : 'http://localhost:8080');

// Add /api to the base URL for API endpoints
const API_BASE_URL = `${BACKEND_URL}/api`;

// Determine if we're in production based on environment or URL
const isProduction = () => {
  return import.meta.env.PROD || 
    (typeof window !== 'undefined' && 
     !window.location.hostname.includes('localhost') && 
     !window.location.hostname.includes('127.0.0.1'));
};

// Configure axios defaults - ALWAYS use the full backend URL with /api path
const api = axios.create({
  baseURL: API_BASE_URL,
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

    // Log details of server errors for debugging
    if (error.response && error.response.status >= 500) {
      console.error('Server error:', error.response.data);
    }

    return Promise.reject(error);
  }
);

// =====================================================
// API ENDPOINT DEFINITIONS 
// =====================================================

// User/Auth API endpoints
export const authApi = {
  login: async (email, password) => {
    console.log(`Attempting login for email: ${email}`);

    // Try multiple API formats since the backend might expect different formats
    try {
      // First attempt: Use params in a POST request (most likely format)
      return await api.post(`/users/authenticate`, null, {
        params: {
          email: email,
          password: password
        }
      });
    } catch (error) {
      console.log('First login attempt failed, trying alternative format:', error);

      try {
        // Second attempt: Use query string in URL (fallback)
        return await api.post(`/users/authenticate?email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`);
      } catch (error2) {
        console.log('Second login attempt failed, trying sending in body:', error2);

        try {
          // Third attempt: Send credentials in request body
          return await api.post('/users/authenticate', { email, password });
        } catch (error3) {
          console.error('All login attempts failed:', error3);

          // In case of error, we should not provide mock data
          console.error('All login attempts failed - authentication required');

          throw error3;
        }
      }
    }
  },
  register: (userData) => {
    console.log('API: Registering user with data:', userData);

    // CRITICAL: Ensure password is not null or empty
    if (!userData.password || userData.password.trim() === '') {
      console.error('API: Registration error - password is missing or empty');
      return Promise.reject(new Error('Password is required for registration'));
    }

    // Create a clean copy of userData to prevent manipulation of the original
    const registrationData = { ...userData };

    // Ensure all required fields are present and not empty
    const requiredFields = ['firstName', 'lastName', 'email', 'username', 'password', 'role'];
    const missingFields = requiredFields.filter(field => !registrationData[field] || registrationData[field].trim?.() === '');

    if (missingFields.length > 0) {
      console.error('API: Registration error - missing fields', missingFields);
      return Promise.reject(new Error(`Missing required fields: ${missingFields.join(', ')}`));
    }

    // Convert any null values to empty strings to avoid database nullability issues
    Object.keys(registrationData).forEach(key => {
      if (registrationData[key] === null) {
        registrationData[key] = '';
      }
    });

    // Make an explicit log of the request about to be sent
    console.log('API: Sending registration request to backend:', {
      url: '/users/register',
      method: 'POST',
      data: registrationData,
      serialized: JSON.stringify(registrationData)
    });

    // Send the registration request to the new endpoint
    return api.post('/users/register', registrationData, {
      headers: {
        'Content-Type': 'application/json'
      }
    })
    .catch(error => {
      // Log detailed error information
      console.error('API: Registration request failed:', {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status
      });

      // If backend is available but returns a 400, try to provide more specific error
      if (error.response && error.response.status === 400) {
        console.error('API: Bad request - field validation failed', error.response.data);
        throw new Error(`Validation failed: ${error.response.data}`);
      }

      throw error;
    });
  },
  getCurrentUserFromToken: () => api.get('/users/findById/' + JSON.parse(localStorage.getItem('user'))?.userId || 0),
  resetPassword: (email) => api.post('/users/reset-password', { email }),
};

// User API endpoints
export const userApi = {
  getCurrentUser: () => {
    const userData = localStorage.getItem('user');
    if (!userData) return Promise.reject("No user logged in");
    const user = JSON.parse(userData);
    return api.get(`/users/findById/${user.userId}`);
  },
  updateUser: (userId, userData) => api.put(`/users/updateUser/${userId}`, userData),
  uploadProfilePicture: (userId, formData) => 
    api.post(`/users/${userId}/profile-picture`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }),
  // New function to get users by role
  getUsersByRole: (role, params = {}) => {
    console.log(`Fetching users with role: ${role}`, params);
    return api.get(`/users/findByRole/${role}`, { params });
  },
  // New function to search users by name or username with role filter
  searchUsers: (query = '', role = 'TUTOR', params = {}) => {
    console.log(`Searching users with query: "${query}" and role: ${role}`, params);
    // Use server-side filtering if available, or client-side as fallback
    return userApi.getUsersByRole(role, { query, ...params });
  }
};

// Tutor Profiles API endpoints
export const tutorProfileApi = {
  getProfiles: () => api.get('/tutors/getAllProfiles'),
  getProfileById: (profileId) => api.get(`/tutors/findById/${profileId}`),
  getProfileByUserId: (userId) => api.get(`/tutors/findByUserId/${userId}`),
  createProfile: (profileData) => api.post(`/tutors/createProfile/user/${profileData.userId}`, profileData),
  updateProfile: (profileId, profileData) => api.put(`/tutors/updateProfile/${profileId}`, profileData),
  searchProfiles: (params) => api.get('/tutors/searchBySubject', { params }),
  getAllProfilesPaginated: (params) => api.get('/tutors/getAllProfilesPaginated', { params }),
};

// Tutoring Sessions API endpoints
export const tutoringSessionApi = {
  getSessions: (params) => api.get('/tutoring-sessions', { params }),
  getSessionById: (sessionId) => api.get(`/tutoring-sessions/${sessionId}`),
  createSession: (sessionData) => {
    console.log('API: Creating session with data:', sessionData);
    return api.post('/tutoring-sessions/createSession', sessionData);
  },
  updateSession: (sessionId, sessionData) => api.put(`/tutoring-sessions/${sessionId}`, sessionData),
  updateSessionStatus: (sessionId, status) => 
    api.patch(`/tutoring-sessions/${sessionId}/status`, { status }),
  getTutorSessions: (tutorId, params) => {
    // tutorId should be the profile ID, not the user ID
    console.log(`Fetching tutoring sessions for tutor profile ID: ${tutorId}`);
    
    // Try the endpoint with "tutor" path
    return api.get(`/tutoring-sessions/findByTutor/${tutorId}`, { params })
      .catch(error => {
        console.log('Error with tutor endpoint, trying alternative format');
        // Try alternative endpoint format if first one fails
        return api.get(`/tutoring-sessions`, { params: { tutorId, ...params } });
      });
  },
  getStudentSessions: (studentId, params) => {
    console.log(`Fetching tutoring sessions for student ID: ${studentId}`);
    // Try the endpoint with "student" path
    return api.get(`/tutoring-sessions/student/${studentId}`, { params })
      .catch(error => {
        console.log('Error with student endpoint, trying alternative format');
        // Try alternative endpoint format if first one fails
        return api.get(`/tutoring-sessions`, { params: { studentId, ...params } });
      });
  },
};

// Reviews API endpoints
export const reviewApi = {
  getReviews: (params) => api.get('/reviews', { params }),
  getReviewById: (reviewId) => api.get(`/reviews/${reviewId}`),
  createReview: (reviewData) => api.post('/reviews', reviewData),
  updateReview: (reviewId, reviewData) => api.put(`/reviews/${reviewId}`, reviewData),
  getTutorReviews: (tutorId, params) => api.get(`/reviews/tutor/${tutorId}`, { params }),
};

// Payment Transactions API endpoints
export const paymentApi = {
  getTransactions: (params) => {
    // Ensure we're using the correct endpoint structure based on backend implementation
    // Remove userId parameter if it's not part of the API specification
    const cleanParams = { ...params };
    // Create a new copy of params without userId if both payerId and payeeId exist
    if (cleanParams.userId && (cleanParams.payerId || cleanParams.payeeId)) {
      delete cleanParams.userId;
    }
    console.log('Fetching payment transactions with params:', cleanParams);
    // Use more standard endpoint pattern
    return api.get('/payment-transactions', { params: cleanParams });
  },
  getTransactionById: (transactionId) => api.get(`/payment-transactions/${transactionId}`),
  createTransaction: (paymentData) => api.post('/payment-transactions', paymentData),
  updateTransaction: (transactionId, paymentData) => 
    api.put(`/payment-transactions/${transactionId}`, paymentData),
  // Add fallback methods that try both endpoint patterns
  getAllTransactions: (params) => {
    // Try the standard endpoint first
    return api.get('/payment-transactions', { params })
      .catch(error => {
        console.log('Error with /payment-transactions, trying /payments fallback');
        // Fall back to the old endpoint if the first one fails
        return api.get('/payments', { params });
      });
  }
};

// Message API endpoints
export const messageApi = {
  getMessages: (conversationId, params) => 
    api.get(`/messages/conversation/${conversationId}`, { params }),
  sendMessage: (messageData) => api.post('/messages', messageData),
  markAsRead: (messageId) => api.patch(`/messages/${messageId}/read`),
  getNewMessages: (conversationId, since, params = {}) => 
    api.get(`/messages/conversation/${conversationId}/new`, { 
      params: { since, ...params } 
    }),
};

// Conversation API endpoints
export const conversationApi = {
  getConversations: (userId) => api.get(`/conversations/user/${userId}`),
  getConversation: (conversationId) => api.get(`/conversations/${conversationId}`),
  createConversation: (data) => api.post('/conversations', data),
  getConversationMessages: (conversationId, params) => 
    api.get(`/conversations/${conversationId}/messages`, { params }),
};

// Notification API endpoints
export const notificationApi = {
  getNotifications: (userId, params = {}, userRole = null) => {
    if (!userId) {
      console.error('Cannot fetch notifications: userId is undefined');
      return Promise.reject(new Error('User ID is required for notifications'));
    }

    // Get user role from localStorage if not provided
    if (!userRole) {
      try {
        const storedUser = JSON.parse(localStorage.getItem('user') || '{}');
        userRole = storedUser.role;
      } catch (e) {
        console.error('Error parsing user from localStorage:', e);
      }
    }

    // Use different endpoints based on user role
    const isTutor = userRole && userRole.toUpperCase() === 'TUTOR';

    console.log(`Fetching notifications for ${isTutor ? 'tutor' : 'user'} ${userId}`);

    // For tutors, use the tutor-specific endpoint
    if (isTutor) {
      return api.get(`/notifications/tutor/${userId}`, { params })
        .catch(error => {
          console.log('Error with tutor-specific endpoint, trying generic endpoint');
          return api.get(`/notifications/user/${userId}`, { params });
        });
    }

    // For students and other users, use the standard endpoint
    return api.get(`/notifications/user/${userId}`, { params })
      .catch(error => {
        console.log('Error with /notifications/user endpoint, trying fallback endpoint');
        // Fall back to original endpoint if first attempt fails
        return api.get(`/notifications/findByUser/${userId}`, { params });
      });
  },
  markAsRead: (notificationId) => {
    if (!notificationId) {
      console.error('Cannot mark notification as read: notificationId is undefined');
      return Promise.reject(new Error('Notification ID is required'));
    }
    return api.patch(`/notifications/${notificationId}/read`);
  },
  markAllAsRead: (userId, userRole = null) => {
    if (!userId) {
      console.error('Cannot mark all notifications as read: userId is undefined');
      return Promise.reject(new Error('User ID is required'));
    }

    // Get user role from localStorage if not provided
    if (!userRole) {
      try {
        const storedUser = JSON.parse(localStorage.getItem('user') || '{}');
        userRole = storedUser.role;
      } catch (e) {
        console.error('Error parsing user from localStorage:', e);
      }
    }

    // Use different endpoints based on user role
    const isTutor = userRole && userRole.toUpperCase() === 'TUTOR';

    console.log(`Marking all notifications as read for ${isTutor ? 'tutor' : 'user'} ${userId}`);

    // For tutors, use the tutor-specific endpoint
    if (isTutor) {
      return api.patch(`/notifications/tutor/${userId}/read-all`)
        .catch(error => {
          console.log('Error with tutor-specific endpoint, trying generic endpoint');
          return api.patch(`/notifications/user/${userId}/read-all`);
        });
    }

    // For students and other users, use the standard endpoint
    return api.patch(`/notifications/user/${userId}/read-all`);
  }
};

// Tutor Availability API endpoints
export const tutorAvailabilityApi = {
  getAvailabilities: (tutorId) => {
    // tutorId should be the tutor's profile ID, not the user ID
    if (!tutorId) {
      console.warn('No tutorId provided for getAvailabilities');
      // Return an empty array to prevent errors
      return Promise.resolve({ data: [] });
    }

    // Log the request being made
    console.log(`Fetching availabilities for tutor profile ID: ${tutorId}`);

    // Make the request and return
    return api.get(`/tutor-availability/findByTutor/${tutorId}`)
      .then(response => {
        console.log('Availability API response successful:', response);
        return response;
      })
      .catch(error => {
        console.error('Error fetching availabilities:', error);
        throw error;
      });
  },
  createAvailability: (availabilityData) => {
    // Ensure we have a tutorId in the data (should be profile ID, not user ID)
    if (!availabilityData.tutorId) {
      console.error('Missing tutorId in availability data');
      return Promise.reject(new Error('Missing tutorId in availability data'));
    }
    
    console.log('Creating availability with tutor profile ID:', availabilityData.tutorId);
    return api.post('/tutor-availability/createAvailability', availabilityData);
  },
  updateAvailability: (availabilityId, availabilityData) => {
    return api.put(`/tutor-availability/updateAvailability/${availabilityId}`, availabilityData);
  },
  deleteAvailability: (availabilityId) => {
    console.log(`Deleting availability with ID: ${availabilityId}`);
    return api.delete(`/tutor-availability/deleteAvailability/${availabilityId}`);
  },
  // Get available time slots for a specific date
  getAvailableTimeSlots: (tutorId, date) => {
    if (!tutorId || !date) {
      console.warn('Missing tutorId or date for getAvailableTimeSlots');
      return Promise.resolve({ data: [] });
    }
    
    console.log(`Fetching available time slots for tutor ${tutorId} on ${date}`);
    return api.get(`/tutor-availability/timeslots/${tutorId}`, { params: { date } });
  }
};

// Google Calendar API endpoints
export const calendarApi = {
  checkConnection: (userId) => api.get(`/calendar/check-connection`, { params: { userId } }),
  connect: (userId) => api.get(`/calendar/connect`, { params: { userId } }),
  getEvents: (userId, date) => api.get(`/calendar/events`, { params: { userId, date } }),
  getAvailableSlots: (tutorId, date, durationMinutes = 60) => 
    api.get(`/calendar/available-slots`, { params: { tutorId, date, durationMinutes } }),
  checkAvailability: (tutorId, date, startTime, endTime) => 
    api.get(`/calendar/check-availability`, { params: { tutorId, date, startTime, endTime } }),
  createEvent: (sessionId) => api.post(`/calendar/create-event`, { sessionId }),
};

// Student Profiles API endpoints
export const studentProfileApi = {
  getProfileByUserId: (userId) => {
    if (!userId) {
      console.warn('Attempted to get student profile with undefined userId');
      return Promise.reject(new Error('User ID is required'));
    }
    console.log(`Fetching student profile for user ID: ${userId}`);
    // Try to get the profile, handle 404s gracefully
    return api.get(`/student-profiles/user/${userId}`)
      .catch(error => {
        if (error.response && error.response.status === 404) {
          console.log(`No existing profile found - profile needs to be created`);
          // Return an empty profile object instead of rejecting
          return { data: null, status: 404 };
        }
        throw error;
      });
  },
  updateProfile: (userId, profileData) => {
    if (!userId) {
      console.warn('Attempted to update student profile with undefined userId');
      return Promise.reject(new Error('User ID is required'));
    }
    console.log(`Updating student profile for user ${userId} with data:`, profileData);
    return api.put(`/student-profiles/${userId}`, profileData);
  },
  createProfile: (profileData) => {
    if (!profileData.userId) {
      console.warn('Attempted to create student profile without userId');
      return Promise.reject(new Error('User ID is required in profile data'));
    }
    console.log('Creating new student profile with data:', profileData);
    return api.post('/student-profiles', profileData);
  }
};

// Export both the api instance and all endpoint collections
export { api as default, api }; 

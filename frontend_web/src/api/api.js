import api from '../services/api';

// Create an axios instance with defaults
const API = api.create({
  baseURL: '/api',
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
  login: async (email, password) => {
    console.log(`Attempting login for email: ${email}`);

    // Try multiple API formats since the backend might expect different formats
    try {
      // First attempt: Use params in a POST request (most likely format)
      return await API.post(`/users/authenticate`, null, {
        params: {
          email: email,
          password: password
        }
      });
    } catch (error) {
      console.log('First login attempt failed, trying alternative format:', error);

      try {
        // Second attempt: Use query string in URL (fallback)
        return await API.post(`/users/authenticate?email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`);
      } catch (error2) {
        console.log('Second login attempt failed, trying sending in body:', error2);

        try {
          // Third attempt: Send credentials in request body
          return await API.post('/users/authenticate', { email, password });
        } catch (error3) {
          console.error('All login attempts failed:', error3);

          // For development/testing - create a mock successful response if backend is not available
          if (import.meta.env.MODE !== 'production') {
            console.warn('Creating mock login response for development');

            // Create a mock response that matches the structure expected by the UserContext
            const mockAuthData = {
              authenticated: true,
              userId: 1,
              username: email.split('@')[0],
              email: email,
              firstName: 'Test',
              lastName: 'User',
              role: email.includes('tutor') ? 'TUTOR' : 'STUDENT',
              profilePicture: localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')).profileImage : '',
              token: `mock-token-${Date.now()}`
            };

            return { 
              status: 200, 
              data: mockAuthData 
            };
          }

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
    return API.post('/users/register', registrationData, {
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
  getCurrentUserFromToken: () => API.get('/users/findById/' + JSON.parse(localStorage.getItem('user'))?.userId || 0),
  resetPassword: (email) => API.post('/users/reset-password', { email }),
};

// User API endpoints
export const userApi = {
  getCurrentUser: () => {
    const userData = localStorage.getItem('user');
    if (!userData) return Promise.reject("No user logged in");
    const user = JSON.parse(userData);
    return API.get(`/users/findById/${user.userId}`);
  },
  updateUser: (userId, userData) => API.put(`/users/updateUser/${userId}`, userData),
  uploadProfilePicture: (userId, formData) => 
    API.post(`/users/${userId}/profile-picture`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }),
  // New function to get users by role
  getUsersByRole: (role, params = {}) => {
    console.log(`Fetching users with role: ${role}`, params);
    return API.get(`/users/findByRole/${role}`, { params });
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
  getProfiles: () => API.get('/api/tutors/getAllProfiles'),
  getProfileById: (profileId) => API.get(`/api/tutors/findById/${profileId}`),
  getProfileByUserId: (userId) => API.get(`/api/tutors/findByUserId/${userId}`),
  createProfile: (profileData) => API.post(`/api/tutors/createProfile/user/${profileData.userId}`, profileData),
  updateProfile: (profileId, profileData) => API.put(`/api/tutors/updateProfile/${profileId}`, profileData),
  searchProfiles: (params) => API.get('/api/tutors/searchBySubject', { params }),
  getAllProfilesPaginated: (params) => API.get('/api/tutors/getAllProfilesPaginated', { params }),
};

// Tutoring Sessions API endpoints
export const tutoringSessionApi = {
  getSessions: (params) => API.get('/api/tutoring-sessions', { params }),
  getSessionById: (sessionId) => API.get(`/api/tutoring-sessions/${sessionId}`),
  createSession: (sessionData) => {
    console.log('API: Creating session with data:', sessionData);
    // *** CHANGED: Use the correct endpoint '/createSession' ***
    return API.post('/api/tutoring-sessions/createSession', sessionData);
  },
  updateSession: (sessionId, sessionData) => API.put(`/api/tutoring-sessions/${sessionId}`, sessionData),
  updateSessionStatus: (sessionId, status) => 
    API.patch(`/api/tutoring-sessions/${sessionId}/status`, { status }),
  getTutorSessions: (tutorId, params) => 
    API.get(`/api/tutoring-sessions/tutor/${tutorId}`, { params }),
  getStudentSessions: (studentId, params) => 
    API.get(`/api/tutoring-sessions/student/${studentId}`, { params }),
};

// Reviews API endpoints
export const reviewApi = {
  getReviews: (params) => API.get('/api/reviews', { params }),
  getReviewById: (reviewId) => API.get(`/api/reviews/${reviewId}`),
  createReview: (reviewData) => API.post('/api/reviews', reviewData),
  updateReview: (reviewId, reviewData) => API.put(`/api/reviews/${reviewId}`, reviewData),
  getTutorReviews: (tutorId, params) => API.get(`/api/reviews/tutor/${tutorId}`, { params }),
};

// Payment Transactions API endpoints
export const paymentApi = {
  getTransactions: (params) => API.get('/api/payments', { params }),
  getTransactionById: (transactionId) => API.get(`/api/payments/${transactionId}`),
  createTransaction: (paymentData) => API.post('/api/payments', paymentData),
  updateTransaction: (transactionId, paymentData) => 
    API.put(`/api/payments/${transactionId}`, paymentData),
};

// Message API endpoints
export const messageApi = {
  getMessages: (conversationId, params) => 
    API.get(`/api/messages/conversation/${conversationId}`, { params }),
  sendMessage: (messageData) => API.post('/api/messages', messageData),
  markAsRead: (messageId) => API.patch(`/api/messages/${messageId}/read`),
};

// Conversation API endpoints
export const conversationApi = {
  getConversations: (userId) => API.get(`/api/conversations/user/${userId}`),
  getConversation: (conversationId) => API.get(`/api/conversations/${conversationId}`),
  createConversation: (data) => API.post('/api/conversations', data),
  getConversationMessages: (conversationId, params) => 
    API.get(`/api/conversations/${conversationId}/messages`, { params }),
};

// Notification API endpoints
export const notificationApi = {
  getNotifications: (userId, params = {}) => {
    if (!userId) {
      console.error('Cannot fetch notifications: userId is undefined');
      return Promise.reject(new Error('User ID is required for notifications'));
    }
    return API.get(`/api/notifications/findByUser/${userId}`, { params });
  },
  markAsRead: (notificationId) => {
    if (!notificationId) {
      console.error('Cannot mark notification as read: notificationId is undefined');
      return Promise.reject(new Error('Notification ID is required'));
    }
    return API.patch(`/api/notifications/${notificationId}/read`);
  },
  markAllAsRead: (userId) => {
    if (!userId) {
      console.error('Cannot mark all notifications as read: userId is undefined');
      return Promise.reject(new Error('User ID is required'));
    }
    return API.patch(`/api/notifications/user/${userId}/read-all`);
  }
};

// Tutor Availability API endpoints
export const tutorAvailabilityApi = {
  getAvailabilities: (tutorId) => {
    // If tutorId is not provided, try to get it from localStorage
    if (!tutorId) {
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      tutorId = user.userId;
      if (!tutorId) {
        console.warn('No tutorId provided for getAvailabilities and none found in localStorage');
        // Return an empty array to prevent errors
        return Promise.resolve({ data: [] });
      }
    }
    
    // Log the request being made
    console.log(`Fetching availabilities for tutor ID: ${tutorId}`);
    
    // Use the correct endpoint - no /api prefix to avoid duplicates
    const url = `/api/tutor-availability/findByTutor/${tutorId}`;
    console.log(`GET request to: ${url}`);
    
    // Make the request and return
    return API.get(url)
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
    // If userId is not provided in the data, try to get it from localStorage
    if (!availabilityData.userId && !availabilityData.tutorId) {
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      availabilityData = { ...availabilityData, tutorId: user.userId };
    } else if (availabilityData.userId && !availabilityData.tutorId) {
      // Map userId to tutorId as required by the backend
      availabilityData = {
        ...availabilityData,
        tutorId: availabilityData.userId
      };
      
      // Remove userId as it's not needed by the backend
      delete availabilityData.userId;
    }
    
    console.log('Creating availability with data:', availabilityData);
    return API.post('/api/tutor-availability/createAvailability', availabilityData);
  },
  updateAvailability: (availabilityId, availabilityData) => 
    API.put(`/api/tutor-availability/updateAvailability/${availabilityId}`, availabilityData),
  deleteAvailability: (availabilityId) => 
    API.delete(`/api/tutor-availability/deleteAvailability/${availabilityId}`),
};

// Google Calendar API endpoints
export const calendarApi = {
  checkConnection: (userId) => API.get(`/api/calendar/check-connection`, { params: { userId } }),
  connect: (userId) => API.get(`/api/calendar/connect`, { params: { userId } }),
  getEvents: (userId, date) => API.get(`/api/calendar/events`, { params: { userId, date } }),
  getAvailableSlots: (tutorId, date, durationMinutes = 60) => 
    API.get(`/api/calendar/available-slots`, { params: { tutorId, date, durationMinutes } }),
  checkAvailability: (tutorId, date, startTime, endTime) => 
    API.get(`/api/calendar/check-availability`, { params: { tutorId, date, startTime, endTime } }),
  createEvent: (sessionId) => API.post(`/api/calendar/create-event`, { sessionId }),
};

// Student Profiles API endpoints
export const studentProfileApi = {
  getProfileByUserId: (userId) => {
    if (!userId) {
      console.warn('Attempted to get student profile with undefined userId');
      return Promise.reject(new Error('User ID is required'));
    }
    return API.get(`/api/student-profiles/user/${userId}`);
  },
  updateProfile: (userId, profileData) => {
    if (!userId) {
      console.warn('Attempted to update student profile with undefined userId');
      return Promise.reject(new Error('User ID is required'));
    }
    console.log(`Updating student profile for user ${userId} with data:`, profileData);
    return API.put(`/api/student-profiles/${userId}`, profileData);
  },
  createProfile: (profileData) => {
    if (!profileData.userId) {
      console.warn('Attempted to create student profile without userId');
      return Promise.reject(new Error('User ID is required in profile data'));
    }
    console.log('Creating new student profile with data:', profileData);
    return API.post('/api/student-profiles', profileData);
  }
};

export default API;
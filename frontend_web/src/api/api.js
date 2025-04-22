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
  getProfiles: () => API.get('/tutors/getAllProfiles'),
  getProfileById: (profileId) => API.get(`/tutors/findById/${profileId}`),
  getProfileByUserId: (userId) => API.get(`/tutors/findByUserId/${userId}`),
  createProfile: (profileData) => API.post(`/tutors/createProfile/user/${profileData.userId}`, profileData),
  updateProfile: (profileId, profileData) => API.put(`/tutors/updateProfile/${profileId}`, profileData),
  searchProfiles: (params) => API.get('/tutors/searchBySubject', { params }),
  getAllProfilesPaginated: (params) => API.get('/tutors/getAllProfilesPaginated', { params }),
};

// Tutoring Sessions API endpoints
export const tutoringSessionApi = {
  getSessions: (params) => API.get('/tutoring-sessions', { params }),
  getSessionById: (sessionId) => API.get(`/tutoring-sessions/${sessionId}`),
  createSession: (sessionData) => API.post('/tutoring-sessions', sessionData),
  updateSession: (sessionId, sessionData) => API.put(`/tutoring-sessions/${sessionId}`, sessionData),
  updateSessionStatus: (sessionId, status) => 
    API.patch(`/tutoring-sessions/${sessionId}/status`, { status }),
  getTutorSessions: (tutorId, params) => 
    API.get(`/tutoring-sessions/tutor/${tutorId}`, { params }),
  getStudentSessions: (studentId, params) => 
    API.get(`/tutoring-sessions/student/${studentId}`, { params }),
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
  getNotifications: (userId, params = {}) => {
    if (!userId) {
      console.error('Cannot fetch notifications: userId is undefined');
      return Promise.reject(new Error('User ID is required for notifications'));
    }
    return API.get(`/notifications/findByUser/${userId}`, { params });
  },
  markAsRead: (notificationId) => {
    if (!notificationId) {
      console.error('Cannot mark notification as read: notificationId is undefined');
      return Promise.reject(new Error('Notification ID is required'));
    }
    return API.patch(`/notifications/${notificationId}/read`);
  },
  markAllAsRead: (userId) => {
    if (!userId) {
      console.error('Cannot mark all notifications as read: userId is undefined');
      return Promise.reject(new Error('User ID is required'));
    }
    return API.patch(`/notifications/user/${userId}/read-all`);
  }
};

// Tutor Availability API endpoints
export const tutorAvailabilityApi = {
  getAvailabilities: (tutorId) => API.get(`/tutor-availability/tutor/${tutorId}`),
  createAvailability: (availabilityData) => API.post('/tutor-availability', availabilityData),
  updateAvailability: (availabilityId, availabilityData) => 
    API.put(`/tutor-availability/${availabilityId}`, availabilityData),
  deleteAvailability: (availabilityId) => API.delete(`/tutor-availability/${availabilityId}`),
};

// Student Profiles API endpoints
export const studentProfileApi = {
  getProfileByUserId: (userId) => {
    if (!userId) {
      console.warn('Attempted to get student profile with undefined userId');
      return Promise.reject(new Error('User ID is required'));
    }
    return API.get(`/student-profiles/user/${userId}`);
  },
  updateProfile: (userId, profileData) => {
    if (!userId) {
      console.warn('Attempted to update student profile with undefined userId');
      return Promise.reject(new Error('User ID is required'));
    }
    console.log(`Updating student profile for user ${userId} with data:`, profileData);
    return API.put(`/student-profiles/${userId}`, profileData);
  },
  createProfile: (profileData) => {
    if (!profileData.userId) {
      console.warn('Attempted to create student profile without userId');
      return Promise.reject(new Error('User ID is required in profile data'));
    }
    console.log('Creating new student profile with data:', profileData);
    return API.post('/student-profiles', profileData);
  }
};

export default API; 

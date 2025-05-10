import api from './baseApi';

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
  
  getCurrentUser: async () => {
    // Try multiple approaches to get the current user
    // First try to use the token in localStorage to authenticate
    const token = localStorage.getItem('token');
    if (!token) {
      return Promise.reject(new Error('No authentication token found'));
    }

    try {
      // First try the user/me endpoint (common REST pattern)
      return await api.get('/users/me');
    } catch (error) {
      console.log('First getCurrentUser attempt failed, trying alternative endpoint', error);
      
      try {
        // Second try - get from stored userId
        const userId = JSON.parse(localStorage.getItem('user'))?.userId;
        if (!userId) {
          throw new Error('No user ID found in localStorage');
        }
        return await api.get(`/users/findById/${userId}`);
      } catch (error2) {
        console.log('Second getCurrentUser attempt failed, trying profile endpoint', error2);
        
        try {
          // Third try - profile endpoint
          return await api.get('/users/profile');
        } catch (error3) {
          console.error('All getCurrentUser attempts failed', error3);
          throw error3;
        }
      }
    }
  },
  
  updateProfile: async (profileData) => {
    const userId = profileData.userId || JSON.parse(localStorage.getItem('user'))?.userId;
    if (!userId) {
      return Promise.reject(new Error('Cannot update profile: No user ID provided'));
    }
    
    console.log(`Updating profile for user ${userId}`, profileData);
    
    try {
      // First try the PUT request to update user profile
      return await api.put(`/users/${userId}`, profileData);
    } catch (error) {
      console.log('First update profile attempt failed, trying alternative endpoint', error);
      
      try {
        // Second try - updateProfile endpoint
        return await api.post('/users/updateProfile', profileData);
      } catch (error2) {
        console.error('All update profile attempts failed', error2);
        throw error2;
      }
    }
  },
  
  getCurrentUserFromToken: () => api.get('/users/findById/' + JSON.parse(localStorage.getItem('user'))?.userId || 0),
  resetPassword: (email) => api.post('/users/reset-password', { email }),
}; 
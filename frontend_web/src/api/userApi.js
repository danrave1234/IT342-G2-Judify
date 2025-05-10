import api from './baseApi';

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
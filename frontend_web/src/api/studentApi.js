import api from './baseApi';

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
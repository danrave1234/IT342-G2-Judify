import api from './baseApi';

// Tutor Profiles API endpoints
export const tutorProfileApi = {
  getProfiles: (params) => api.get('/tutor-profiles', { params }),
  
  // Get profile by ID
  getProfile: (profileId) => api.get(`/tutor-profiles/${profileId}`),
  
  // Find profile by user ID
  getProfileByUserId: (userId) => api.get(`/tutor-profiles/findByUserId/${userId}`),
  
  // Create a new tutor profile
  createProfile: (profileData) => api.post('/tutor-profiles', profileData),
  
  // Update an existing tutor profile
  updateProfile: (profileId, profileData) => api.put(`/tutor-profiles/${profileId}`, profileData),
  
  // Update specific fields of a tutor profile
  updateProfileFields: (profileId, fields) => api.patch(`/tutor-profiles/${profileId}`, fields),
  
  // Delete a tutor profile
  deleteProfile: (profileId) => api.delete(`/tutor-profiles/${profileId}`),
  
  // Search for tutors using query parameters
  searchProfiles: (searchParams) => api.get('/tutor-profiles/search', { params: searchParams }),

  // Get userId from tutorId (profile ID)
  getUserIdFromTutorId: (tutorId) => {
    console.log(`Getting userId from tutorId: ${tutorId}`);
    // Try tutor-profiles endpoint first
    return api.get(`/tutor-profiles/getUserId/${tutorId}`)
      .catch(error => {
        console.log('First endpoint failed, trying alternative endpoint');
        // Try alternative endpoint if first one fails
        return api.get(`/api/tutor-profiles/getUserId/${tutorId}`);
      })
      .catch(error => {
        console.log('Second endpoint failed, trying third alternative endpoint');
        // Try another alternative
        return api.get(`/tutorProfiles/getUserId/${tutorId}`);
      });
  },
  
  // Get tutorId (profile ID) from userId
  getTutorIdFromUserId: (userId) => {
    console.log(`Getting tutorId from userId: ${userId}`);
    // Try tutor-profiles endpoint first
    return api.get(`/tutor-profiles/getTutorId/${userId}`)
      .catch(error => {
        console.log('First endpoint failed, trying alternative endpoint');
        // Try alternative endpoint if first one fails
        return api.get(`/api/tutor-profiles/getTutorId/${userId}`);
      })
      .catch(error => {
        console.log('Second endpoint failed, trying third alternative endpoint');
        // Try another alternative
        return api.get(`/tutorProfiles/getTutorId/${userId}`);
      });
  }
}; 
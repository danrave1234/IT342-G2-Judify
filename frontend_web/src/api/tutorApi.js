import api from './baseApi';

// Tutor Profiles API endpoints
export const tutorProfileApi = {
  getProfiles: (params) => api.get('/tutor-profiles', { params }),
  
  // Get profile by ID with fallbacks
  getProfileById: (profileId) => {
    console.log(`Trying to fetch tutor profile by profileId: ${profileId}`);
    
    // Try multiple endpoints in sequence
    return api.get(`/tutor-profiles/${profileId}`)
      .catch(error => {
        console.log(`First endpoint failed with status ${error?.response?.status}, trying alternative endpoint`);
        return api.get(`/tutors/${profileId}`);
      })
      .catch(error => {
        console.log(`Second endpoint failed with status ${error?.response?.status}, trying third endpoint`);
        return api.get(`/api/tutors/${profileId}`);
      });
  },
  
  // Get profile by ID
  getProfile: (profileId) => api.get(`/tutor-profiles/${profileId}`),
  
  // Find profile by user ID
  getProfileByUserId: (userId) => {
    console.log(`Trying to fetch tutor profile for userId: ${userId}`);
    
    // Try multiple endpoints in sequence
    return api.get(`/tutor-profiles/findByUserId/${userId}`)
      .catch(error => {
        console.log(`First endpoint failed with status ${error?.response?.status}, trying alternative endpoint`);
        return api.get(`/tutors/findByUserId/${userId}`);
      })
      .catch(error => {
        console.log(`Second endpoint failed with status ${error?.response?.status}, trying third endpoint`);
        return api.get(`/api/tutors/findByUserId/${userId}`);
      });
  },
  
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
      })
      .catch(error => {
        console.log('All getUserId endpoints failed, falling back to direct profile lookup');
        // As a last resort, try to get the full profile and extract the userId
        return api.get(`/tutor-profiles/${tutorId}`)
          .then(response => {
            if (response?.data?.userId) {
              return { data: response.data.userId };
            }
            throw new Error('Could not extract userId from profile');
          });
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
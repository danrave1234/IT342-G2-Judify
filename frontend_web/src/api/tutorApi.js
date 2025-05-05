import api from './baseApi';

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
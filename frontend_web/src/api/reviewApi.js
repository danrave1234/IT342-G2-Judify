import api from './baseApi';

// Reviews API endpoints
export const reviewApi = {
  getReviews: (params) => api.get('/reviews', { params }),
  getReviewById: (reviewId) => api.get(`/reviews/${reviewId}`),
  createReview: (reviewData) => api.post('/reviews', reviewData),
  updateReview: (reviewId, reviewData) => api.put(`/reviews/${reviewId}`, reviewData),
  getTutorReviews: (tutorId, params) => api.get(`/reviews/tutor/${tutorId}`, { params }),
}; 
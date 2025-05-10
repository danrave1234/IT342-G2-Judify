import api from './baseApi';

// Tutor Availability API endpoints
export const tutorAvailabilityApi = {
  getAvailabilities: (tutorId, isUserId = false) => {
    // tutorId can be either userId or profileId based on isUserId flag
    if (!tutorId) {
      console.warn('No tutorId provided for getAvailabilities');
      // Return an empty array to prevent errors
      return Promise.resolve({ data: [] });
    }

    // Log the request being made with proper context
    console.log(`Fetching availabilities for tutor ${isUserId ? 'user' : 'profile'} ID: ${tutorId}`);

    // Determine the correct endpoint based on whether we have a userId or profileId
    const endpoint = isUserId 
      ? `/tutor-availability/findByUser/${tutorId}` 
      : `/tutor-availability/findByTutor/${tutorId}`;

    // Make the request and return
    return api.get(endpoint)
      .then(response => {
        console.log('Availability API response successful:', response);
        return response;
      })
      .catch(error => {
        console.error(`Error fetching availabilities for ${isUserId ? 'user' : 'profile'} ID ${tutorId}:`, error);
        
        // Try the alternative endpoints in sequence
        if (isUserId) {
          // If using userId failed, try these alternative endpoints
          console.log(`Attempting direct API call to /api${endpoint}`);
          return api.get(`/api${endpoint}`)
            .catch(err => {
              console.log(`API endpoint failed, trying fallback to legacy endpoint`);
              return api.get(`/tutors/${tutorId}/availability`);
            });
        } else {
          // If using profileId fails, try to fetch by tutorId directly as fallback
          console.log(`Attempting fallback request to /tutors/${tutorId}/availability`);
          return api.get(`/tutors/${tutorId}/availability`);
        }
      });
  },
  createAvailability: (availabilityData) => {
    // Check for either tutorId or userId in the data
    if (!availabilityData.tutorId && !availabilityData.userId) {
      console.error('Missing tutorId or userId in availability data');
      return Promise.reject(new Error('Missing tutorId or userId in availability data'));
    }
    
    // Map userId to tutorId if needed for backward compatibility
    const dataToSend = { ...availabilityData };
    
    // If userId is provided but tutorId is not, use userId as tutorId
    if (availabilityData.userId && !availabilityData.tutorId) {
      console.log(`Using userId ${availabilityData.userId} as tutorId for createAvailability`);
      dataToSend.tutorId = availabilityData.userId;
    }
    
    console.log('Creating availability with data:', dataToSend);
    return api.post('/tutor-availability/createAvailability', dataToSend);
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
import api from './baseApi';

// Tutor Availability API endpoints
export const tutorAvailabilityApi = {
  getAvailabilities: (tutorId) => {
    // tutorId should be the tutor's profile ID, not the user ID
    if (!tutorId) {
      console.warn('No tutorId provided for getAvailabilities');
      // Return an empty array to prevent errors
      return Promise.resolve({ data: [] });
    }

    // Log the request being made
    console.log(`Fetching availabilities for tutor profile ID: ${tutorId}`);

    // Make the request and return
    return api.get(`/tutor-availability/findByTutor/${tutorId}`)
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
    // Ensure we have a tutorId in the data (should be profile ID, not user ID)
    if (!availabilityData.tutorId) {
      console.error('Missing tutorId in availability data');
      return Promise.reject(new Error('Missing tutorId in availability data'));
    }
    
    console.log('Creating availability with tutor profile ID:', availabilityData.tutorId);
    return api.post('/tutor-availability/createAvailability', availabilityData);
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
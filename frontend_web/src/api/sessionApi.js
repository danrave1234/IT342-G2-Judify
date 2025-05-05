import api from './baseApi';

// Tutoring Sessions API endpoints
export const tutoringSessionApi = {
  getSessions: (params) => api.get('/tutoring-sessions', { params }),
  getSessionById: (sessionId) => {
    if (!sessionId) {
      console.error('Cannot fetch session details: sessionId is undefined');
      return Promise.reject(new Error('Session ID is required'));
    }
    
    console.log(`Fetching session details for session ID: ${sessionId}`);
    
    // Try multiple endpoints in sequence
    // This is because different backend implementations might use different endpoint patterns
    const endpointsToTry = [
      `/tutoring-sessions/${sessionId}`,
      `/sessions/${sessionId}`,
      `/tutoring-sessions/session/${sessionId}`,
      `/api/tutoring-sessions/${sessionId}` // Add API prefix as fallback
    ];
    
    // Use a recursive function to try each endpoint until one works
    const tryEndpoint = (index) => {
      if (index >= endpointsToTry.length) {
        // All endpoints failed
        return Promise.reject(new Error(`Failed to fetch session details after trying ${endpointsToTry.length} endpoints`));
      }
      
      const endpoint = endpointsToTry[index];
      console.log(`Trying endpoint: ${endpoint}`);
      
      return api.get(endpoint)
        .then(response => {
          console.log(`Success with endpoint: ${endpoint}`, response.data);
          return response;
        })
        .catch(error => {
          console.log(`Endpoint ${endpoint} failed: ${error.message}`);
          // Try the next endpoint
          return tryEndpoint(index + 1);
        });
    };
    
    // Start trying endpoints from the first one
    return tryEndpoint(0);
  },
  createSession: (sessionData) => {
    console.log('API: Creating session with data:', sessionData);
    return api.post('/tutoring-sessions/createSession', sessionData);
  },
  updateSession: (sessionId, sessionData) => api.put(`/tutoring-sessions/${sessionId}`, sessionData),
  updateSessionStatus: (sessionId, status) => 
    api.patch(`/tutoring-sessions/${sessionId}/status`, { status }),
  getTutorSessions: (tutorId, params) => {
    // tutorId should be the profile ID, not the user ID
    console.log(`Fetching tutoring sessions for tutor profile ID: ${tutorId}`);
    
    // Try the endpoint with "tutor" path
    return api.get(`/tutoring-sessions/findByTutor/${tutorId}`, { params })
      .catch(error => {
        console.log('Error with tutor endpoint, trying alternative format');
        // Try alternative endpoint format if first one fails
        return api.get(`/tutoring-sessions`, { params: { tutorId, ...params } });
      });
  },
  getStudentSessions: (studentId, params) => {
    console.log(`Fetching tutoring sessions for student ID: ${studentId}`);
    // Try the endpoint with "student" path
    return api.get(`/tutoring-sessions/student/${studentId}`, { params })
      .catch(error => {
        console.log('Error with student endpoint, trying alternative format');
        // Try alternative endpoint format if first one fails
        return api.get(`/tutoring-sessions`, { params: { studentId, ...params } });
      });
  },
}; 
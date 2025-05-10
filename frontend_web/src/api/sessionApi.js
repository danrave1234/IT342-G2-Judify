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
    
    // Make a direct API call using the session data as-is
    return api.post('/tutoring-sessions/createSession', sessionData)
      .catch(error => {
        console.log('First session creation endpoint failed, trying with modified data');
        
        // If first attempt fails, try the alternative approach
        // Map tutorId to userId for backend compatibility if needed
        const updatedSessionData = {
          ...sessionData,
          userId: sessionData.tutorId, // Map tutorId to userId for backend compatibility
        };
        
        // Delete the tutorId field to avoid confusion
        if (updatedSessionData.tutorId) {
          delete updatedSessionData.tutorId;
        }
        
        console.log('API: Trying with modified session data:', updatedSessionData);
        return api.post('/tutoring-sessions/createSession', updatedSessionData);
      })
      .catch(error => {
        console.log('Second session creation attempt failed, trying direct tutor endpoint');
        
        // If second attempt fails, try the direct tutor endpoint
        return api.post('/tutoring-sessions/createWithTutor', sessionData);
      });
  },
  updateSession: (sessionId, sessionData) => api.put(`/tutoring-sessions/${sessionId}`, sessionData),
  updateSessionStatus: (sessionId, status) => 
    api.patch(`/tutoring-sessions/${sessionId}/status`, { status }),
  getTutorSessions: (tutorId, params) => {
    // tutorId should be the profile ID, not the user ID
    console.log(`Fetching tutoring sessions for tutor profile ID: ${tutorId}`);
    
    // Try the endpoint with "user" path
    return api.get(`/tutoring-sessions/findByUser/${tutorId}`, { params })
      .catch(error => {
        console.log('Error with user endpoint, trying alternative format');
        // Try alternative endpoint format if first one fails
        return api.get(`/tutoring-sessions`, { params: { tutorId, ...params } });
      });
  },
  getStudentSessions: (studentId, params) => {
    console.log(`Fetching tutoring sessions for student ID: ${studentId}`);
    // Try the endpoint with "user" path
    return api.get(`/tutoring-sessions/findByUser/${studentId}`, { params })
      .catch(error => {
        console.log('Error with user endpoint, trying alternative format');
        // Try alternative endpoint format if first one fails
        return api.get(`/tutoring-sessions/student/${studentId}`, { params })
          .catch(error2 => {
            console.log('Error with student endpoint, trying findByStudent endpoint');
            // Try the findByStudent endpoint as a last resort
            return api.get(`/tutoring-sessions/findByStudent/${studentId}`, { params });
          });
      });
  },
  getTutorSessionsByUserId: (userId) => {
    if (!userId) {
      console.error('Cannot fetch tutor sessions: userId is undefined');
      return Promise.reject(new Error('User ID is required'));
    }
    
    console.log(`Fetching tutoring sessions for tutor user ID: ${userId}`);
    
    // Try multiple endpoints in sequence
    return api.get(`/tutoring-sessions/tutor/${userId}`)
      .catch(error => {
        console.log('Error with first endpoint, trying alternative format');
        return api.get(`/tutoring-sessions/findByTutor/${userId}`);
      })
      .catch(error => {
        console.log('Error with second endpoint, trying findByUser endpoint');
        return api.get(`/tutoring-sessions/findByUser/${userId}`);
      });
  },
  
  // Accept a tutoring session
  acceptSession: (sessionId) => {
    if (!sessionId) {
      console.error('Cannot accept session: sessionId is undefined');
      return Promise.reject(new Error('Session ID is required'));
    }
    
    console.log(`Accepting tutoring session with ID: ${sessionId}`);
    
    // Try multiple endpoints in sequence
    return api.put(`/tutoring-sessions/${sessionId}/accept`, { tutorAccepted: true })
      .catch(error => {
        console.log('Error with first endpoint, trying alternative format');
        return api.patch(`/tutoring-sessions/${sessionId}/status`, { 
          status: 'APPROVED',
          tutorAccepted: true 
        });
      })
      .catch(error => {
        console.log('Error with second endpoint, trying updateStatus endpoint');
        return api.put(`/tutoring-sessions/updateStatus/${sessionId}`, {
          status: 'APPROVED',
          tutorAccepted: true
        });
      });
  },
}; 
import api from './baseApi';

// Cache successful endpoint patterns to avoid trying endpoints that don't work
const endpointCache = {
  getConversation: null,
  getConversations: null
};

// Conversation API endpoints
export const conversationApi = {
  getConversations: (userId) => {
    if (!userId) {
      console.error('Cannot fetch conversations: userId is undefined');
      return Promise.reject(new Error('User ID is required'));
    }
    
    // Try cached endpoint first if available
    if (endpointCache.getConversations) {
      const endpoint = endpointCache.getConversations.replace('{userId}', userId);
      return api.get(endpoint)
        .catch(error => {
          // If cached endpoint fails, reset cache and try full sequence
          console.log('Cached endpoint failed, trying all endpoints');
          endpointCache.getConversations = null;
        });
    }
    
    console.log(`Fetching conversations for user: ${userId}`);
    
    // Try multiple endpoints in sequence to match mobile implementation
    const endpointsToTry = [
      `/conversations/findByUser/${userId}`,
      `/conversations/findByUser/participant/${userId}`,
      `/conversations/user/${userId}`,
      `/users/${userId}/conversations`
    ];
    
    // Use a recursive function to try each endpoint until one works
    const tryEndpoint = (index) => {
      if (index >= endpointsToTry.length) {
        // Try POST approach as last resort (mobile app does this)
        console.log('All GET endpoints failed, trying POST approach');
        return api.post('/conversations/findByUserId', { userId })
          .then(response => {
            // Cache successful POST endpoint
            endpointCache.getConversations = '/conversations/findByUserId';
            return response;
          })
          .catch(postError => {
            console.log('POST approach also failed, no more fallbacks available');
            // All endpoints failed
            return Promise.reject(new Error(`Failed to fetch conversations after trying all endpoints`));
          });
      }
      
      const endpoint = endpointsToTry[index];
      
      // Minimize logging to reduce console spam
      if (index === 0) {
        console.log(`Trying to fetch conversations with endpoint: ${endpoint}`);
      }
      
      return api.get(endpoint)
        .then(response => {
          // Cache successful endpoint pattern
          endpointCache.getConversations = endpoint.replace(userId, '{userId}');
          return response;
        })
        .catch(error => {
          // Only log first failure
          if (index === 0) {
            console.log(`Primary conversations endpoint failed, trying fallbacks`);
          }
          // Try the next endpoint
          return tryEndpoint(index + 1);
        });
    };
    
    // Start trying endpoints from the first one
    return tryEndpoint(0);
  },

  getConversation: (conversationId) => {
    if (!conversationId) {
      return Promise.reject(new Error('Conversation ID is required'));
    }
    
    // Try cached endpoint first if available
    if (endpointCache.getConversation) {
      const endpoint = endpointCache.getConversation.replace('{conversationId}', conversationId);
      return api.get(endpoint)
        .catch(error => {
          // If cached endpoint fails, reset cache and try full sequence
          console.log('Cached conversation endpoint failed, trying all endpoints');
          endpointCache.getConversation = null;
        });
    }
    
    // Try multiple endpoints in sequence
    const endpointsToTry = [
      `/conversations/findById/${conversationId}`,  // Most reliable endpoint first based on logs
      `/conversations/${conversationId}`,           // Simplest format
      `/conversations/get/${conversationId}`,
      `/conversations/conversation/${conversationId}`,
      `/conversations/getConversation/${conversationId}`
    ];
    
    // Use a recursive function to try each endpoint
    const tryEndpoint = (index) => {
      if (index >= endpointsToTry.length) {
        // All endpoints failed
        return Promise.reject(new Error(`Failed to fetch conversation ${conversationId} after trying all endpoints`));
      }
      
      const endpoint = endpointsToTry[index];
      
      // Only log the first attempt to reduce console spam
      if (index === 0) {
        console.log(`Trying to get conversation with endpoint: ${endpoint}`);
      }
      
      return api.get(endpoint)
        .then(response => {
          // Cache the successful endpoint pattern
          endpointCache.getConversation = endpoint.replace(conversationId, '{conversationId}');
          console.log(`Success fetching conversation with endpoint: ${endpoint}`);
          return response;
        })
        .catch(error => {
          // Only log the first failure
          if (index === 0) {
            console.log(`Conversation endpoint failed, trying fallbacks`);
          }
          // Try the next endpoint
          return tryEndpoint(index + 1);
        });
    };
    
    // Start trying endpoints from the first one
    return tryEndpoint(0);
  },

  createConversation: (data) => {
    // Transform data to match the expected format on the backend
    const payload = {
      user1Id: data.studentId, // Student ID
      user2Id: data.tutorId,   // Tutor ID
      sessionId: data.sessionId,
      lastMessageTime: data.lastMessageTime
    };
    
    console.log('Creating conversation with payload:', payload);
    
    // Try multiple approaches in sequence, matching the mobile implementation
    
    // First try: Use the createConversation endpoint with direct data
    return api.post('/conversations/createConversation', {
      studentId: data.studentId,
      tutorId: data.tutorId,
      sessionId: data.sessionId
    })
    .catch(error => {
      console.log('First createConversation attempt failed, trying with user1/user2 format...');
      
      // Second try: Use createConversation with user1/user2 format
      return api.post('/conversations/createConversation', payload);
    })
    .catch(error => {
      console.log('Second attempt failed, trying createWithTutor endpoint...');
      
      // Third try: Use createWithTutor endpoint with path variables
      return api.post(`/conversations/createWithTutor/${payload.user1Id}/${payload.user2Id}`, null, {
        params: { sessionId: payload.sessionId }
      });
    })
    .catch(error => {
      console.log('Third attempt failed, trying generic conversations endpoint...');
      
      // Final fallback - try the root endpoint
      return api.post('/conversations', payload);
    });
  },

  getConversationMessages: (conversationId, params) => {
    // This is now handled by the messageApi.getMessages method
    console.log('Using getConversationMessages - consider switching to messageApi.getMessages');
    return api.get(`/conversations/${conversationId}/messages`, { params });
  },
  
  updateConversationNames: (conversationId) => {
    if (!conversationId) {
      return Promise.reject(new Error('Conversation ID is required'));
    }
    
    // This endpoint exists in the mobile app to update participant names
    return api.put(`/conversations/updateNames/${conversationId}`)
      .catch(error => {
        console.log('Error updating conversation names, continuing with conversation');
        // Return original conversation ID even if the update fails
        return { data: { id: conversationId } };
      });
  }
}; 
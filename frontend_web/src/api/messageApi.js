import api from './baseApi';

// Message API endpoints
export const messageApi = {
  getMessages: (conversationId, params) => {
    if (!conversationId) {
      return Promise.reject(new Error('Conversation ID is required'));
    }
    
    // Try multiple endpoints in sequence to match mobile implementation
    const endpointsToTry = [
      `/messages/findByConversationPaginated/${conversationId}`, // Mobile primary endpoint
      `/messages/conversation/${conversationId}`,               // Current web endpoint
      `/conversations/${conversationId}/messages`,              // Alternative format
      `/messages?conversationId=${conversationId}`              // Query parameter format
    ];
    
    // Use a recursive function to try each endpoint until one works
    const tryEndpoint = (index) => {
      if (index >= endpointsToTry.length) {
        // All endpoints failed
        return Promise.reject(new Error(`Failed to fetch messages after trying ${endpointsToTry.length} endpoints`));
      }
      
      const endpoint = endpointsToTry[index];
      if (index === 0) {
        // Only log for the first attempt to reduce console spam
        console.log(`Trying to fetch messages for conversation ${conversationId}`);
      }
      
      return api.get(endpoint, { params })
        .then(response => {
          // If the response is paginated (has a 'content' property), extract it
          if (response.data && response.data.content) {
            return { 
              ...response, 
              data: response.data.content 
            };
          }
          
          return response;
        })
        .catch(error => {
          // Only log for the first failed attempt
          if (index === 0) {
            console.log(`Primary endpoint failed for conversation ${conversationId}, trying fallback endpoints`);
          }
          // Try the next endpoint
          return tryEndpoint(index + 1);
        });
    };
    
    // Start trying endpoints from the first one
    return tryEndpoint(0);
  },
  
  sendMessage: (messageData) => {
    // Make sure we have required fields
    if (!messageData.conversationId || !messageData.senderId || !messageData.content) {
      return Promise.reject(new Error('Missing required message data'));
    }
    
    console.log('Sending message to conversation:', messageData.conversationId);
    
    // Try the standard endpoint first
    return api.post('/messages/sendMessage', messageData)
      .catch(error => {
        console.log('Error with primary sendMessage endpoint, trying fallback');
        // Fall back to the simpler endpoint if the first one fails
        return api.post('/messages', messageData);
      });
  },
  
  markAsRead: (messageId) => {
    if (!messageId) {
      return Promise.reject(new Error('Message ID is required'));
    }
    
    // Try the mobile endpoint first
    return api.put(`/messages/markAsRead/${messageId}`)
      .catch(error => {
        // Fall back to PATCH method if the PUT method fails
        return api.patch(`/messages/${messageId}/read`);
      });
  },
  
  markAllAsRead: (conversationId, userId) => {
    if (!conversationId || !userId) {
      return Promise.reject(new Error('Conversation ID and User ID are required'));
    }
    
    // Use the mobile app's endpoint format
    return api.put(`/messages/markAllAsRead/${conversationId}?userId=${userId}`)
      .catch(error => {
        // Try alternative endpoint format if first one fails
        return api.patch(`/conversations/${conversationId}/read-all?userId=${userId}`);
      });
  },
  
  getNewMessages: (conversationId, since, params = {}) => {
    if (!conversationId) {
      return Promise.reject(new Error('Conversation ID is required'));
    }
    
    if (!since) {
      console.warn('No timestamp provided for new messages, using current time');
      since = new Date().toISOString();
    }
    
    // Add debug info for timestamp comparison
    console.log(`Fetching messages for conversation ${conversationId} since ${since}`);
    
    // Don't make additional API calls for new messages,
    // reuse the existing getMessages endpoint and filter on client side
    // This avoids making 404 requests to endpoints that don't exist
    return messageApi.getMessages(conversationId, params)
      .then(response => {
        const allMessages = response.data || [];
        console.log(`Got ${allMessages.length} total messages, filtering for new ones since ${since}`);
        
        // Parse the 'since' timestamp once
        const sinceTime = new Date(since).getTime();
        
        // Filter messages that are newer than the since timestamp
        const newMessages = allMessages.filter(message => {
          // Get the message timestamp (use the first available timestamp field)
          const messageTimestamp = message.timestamp || message.createdAt || message.sentAt;
          
          if (!messageTimestamp) {
            console.warn('Message has no timestamp, skipping:', message);
            return false;
          }
          
          const messageTime = new Date(messageTimestamp).getTime();
          
          // For debugging
          if (messageTime > sinceTime) {
            console.log(`Found new message: ID=${message.messageId || message.id}, time=${messageTimestamp} (after ${since})`);
          }
          
          return messageTime > sinceTime;
        });
        
        // Only log if we actually found new messages to reduce console spam
        console.log(`Filtered to ${newMessages.length} new messages for conversation ${conversationId}`);
        
        return { ...response, data: newMessages };
      })
      .catch(error => {
        // Log but don't propagate to avoid UI disruption
        console.error(`Error fetching new messages for conversation ${conversationId}`);
        return { data: [] };
      });
  }
};
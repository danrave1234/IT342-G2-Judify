import axios from 'axios';

// Make sure global is defined for browsers
if (typeof window !== 'undefined' && typeof window.global === 'undefined') {
  window.global = window;
}

class MessageService {
  constructor() {
    this.isConnected = false;
    this.messageCallbacks = new Map();
    this.readReceiptCallbacks = new Map();
    this.conversationPollingIntervals = new Map(); // Track polling intervals
    this.pollingInterval = 5000; // Poll every 5 seconds
    this.lastMessageTimestamps = new Map(); // Track timestamps for efficient polling
    this.isPolling = false; // Global polling flag
  }

  connect(userId, token, onConnected, onError) {
    try {
      // Set token for all future axios requests
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
      
      // We're not really connecting a websocket here, 
      // just setting up the service
      this.userId = userId;
      this.token = token;
      this.isConnected = true;
      
      console.log('REST API message service initialized');
      
      if (onConnected) {
        onConnected();
      }

      return true;
    } catch (error) {
      console.error('Error initializing message service:', error);
      if (onError) {
        onError('Failed to initialize message service');
      }
      return false;
    }
  }

  disconnect() {
    // Clean up polling intervals
    this.conversationPollingIntervals.forEach((interval) => {
      clearInterval(interval);
    });
    this.conversationPollingIntervals.clear();
    this.messageCallbacks.clear();
    this.readReceiptCallbacks.clear();
    this.lastMessageTimestamps.clear();
    this.isConnected = false;
    console.log('Message service disconnected');
  }

  // Check if a conversation is being actively viewed
  isConversationActive(conversationId) {
    return this.messageCallbacks.has(`conversation.${conversationId}`);
  }

  // Subscribe to a conversation by starting polling for new messages
  subscribeToConversation(conversationId, callback) {
    if (!this.isConnected) {
      console.error('Not connected to message service');
      return null;
    }

    try {
      // Store the callback
      this.messageCallbacks.set(`conversation.${conversationId}`, callback);
      
      // Get last message timestamp to only fetch new messages
      const lastTimestamp = this.getLastTimestampForConversation(conversationId);
      
      // Start polling for new messages in this conversation
      const intervalId = setInterval(async () => {
        if (this.isPolling) return; // Skip if already polling
        
        this.isPolling = true;
        try {
          await this.pollForNewMessages(conversationId, callback);
        } finally {
          this.isPolling = false;
        }
      }, this.pollingInterval);
      
      // Store interval ID for cleanup
      this.conversationPollingIntervals.set(conversationId, intervalId);
      
      // Do initial poll immediately
      this.pollForNewMessages(conversationId, callback);
      
      return {
        conversationId,
        unsubscribe: () => this.unsubscribeFromConversation(conversationId)
      };
    } catch (e) {
      console.error(`Error subscribing to conversation ${conversationId}:`, e);
      return null;
    }
  }

  async pollForNewMessages(conversationId, callback) {
    try {
      const lastTimestamp = this.getLastTimestampForConversation(conversationId);
      console.log(`Polling for new messages since ${new Date(lastTimestamp).toISOString()}`);
      
      // Get new messages since last timestamp using the API
      // Use query parameter to specify "since" timestamp
      const response = await axios.get(`/api/messages/findByConversation/${conversationId}`, {
        params: {
          since: lastTimestamp > 0 ? new Date(lastTimestamp).toISOString() : undefined
        }
      });
      
      const newMessages = response.data;
      
      if (newMessages && newMessages.length > 0) {
        console.log(`Received ${newMessages.length} new messages`);
        
        // Update last timestamp from the newest message
        const latestTimestamp = Math.max(
          ...newMessages.map(msg => new Date(msg.timestamp).getTime())
        );
        this.updateLastTimestampForConversation(conversationId, latestTimestamp);
        
        // Process all new messages
        newMessages.forEach(message => {
          callback(message);
        });
      }
    } catch (error) {
      console.error(`Error polling conversation ${conversationId}:`, error);
    }
  }

  unsubscribeFromConversation(conversationId) {
    try {
      // Clear polling interval
      const intervalId = this.conversationPollingIntervals.get(conversationId);
      if (intervalId) {
        clearInterval(intervalId);
        this.conversationPollingIntervals.delete(conversationId);
      }
      
      // Remove callback
      this.messageCallbacks.delete(`conversation.${conversationId}`);
    } catch (e) {
      console.error(`Error unsubscribing from conversation ${conversationId}:`, e);
    }
  }

  getLastTimestampForConversation(conversationId) {
    // First check our in-memory map
    if (this.lastMessageTimestamps.has(conversationId)) {
      return this.lastMessageTimestamps.get(conversationId);
    }
    
    // Then check localStorage as fallback
    const storageKey = `last_message_timestamp_${conversationId}`;
    const storedTimestamp = localStorage.getItem(storageKey);
    const timestamp = storedTimestamp ? parseInt(storedTimestamp, 10) : 0;
    
    // Update in-memory map
    this.lastMessageTimestamps.set(conversationId, timestamp);
    
    return timestamp;
  }

  updateLastTimestampForConversation(conversationId, timestamp) {
    if (!timestamp || timestamp <= this.getLastTimestampForConversation(conversationId)) {
      return; // Don't update if timestamp is older or same
    }
    
    // Update in-memory map
    this.lastMessageTimestamps.set(conversationId, timestamp);
    
    // Update localStorage for persistence across page reloads
    const storageKey = `last_message_timestamp_${conversationId}`;
    localStorage.setItem(storageKey, timestamp.toString());
  }

  async sendMessage(message) {
    if (!this.isConnected) {
      console.error('Not connected to message service');
      return false;
    }

    try {
      // Call the API to send the message
      const response = await axios.post('/api/messages/sendMessage', message);
      const sentMessage = response.data;
      
      // Update timestamp for the conversation
      const msgTimestamp = new Date(sentMessage.timestamp).getTime();
      this.updateLastTimestampForConversation(message.conversationId, msgTimestamp);
      
      // Manually trigger the callback for our own message
      // This saves us from having to wait for the next polling interval
      const callback = this.messageCallbacks.get(`conversation.${message.conversationId}`);
      if (callback) {
        callback(sentMessage);
      }
      
      return true;
    } catch (e) {
      console.error('Error sending message:', e);
      return false;
    }
  }

  async markMessageAsRead(messageId, senderId, conversationId) {
    if (!this.isConnected) {
      console.error('Not connected to message service');
      return false;
    }

    try {
      await axios.put(`/api/messages/markAsRead/${messageId}`);
      
      // Notify any callbacks
      const callback = this.readReceiptCallbacks.get(`message.${messageId}`);
      if (callback) {
        callback({
          messageId: messageId,
          senderId: senderId,
          conversationId: conversationId,
          timestamp: new Date().toISOString()
        });
      }
      
      return true;
    } catch (e) {
      console.error('Error marking message as read:', e);
      return false;
    }
  }

  onMessageRead(messageId, callback) {
    this.readReceiptCallbacks.set(`message.${messageId}`, callback);
  }

  // Load conversation messages once (not via polling) with pagination
  async loadConversationMessages(conversationId, page = 0, size = 20) {
    try {
      // Use pagination to load only necessary messages
      const response = await axios.get(`/api/messages/findByConversationPaginated/${conversationId}`, {
        params: {
          page,
          size,
          order: 'DESC' // Newest first
        }
      });
      
      const messages = response.data.content || [];
      
      // Update last message timestamp if we have messages
      if (messages.length > 0) {
        const latestTimestamp = Math.max(
          ...messages.map(msg => new Date(msg.timestamp).getTime())
        );
        this.updateLastTimestampForConversation(conversationId, latestTimestamp);
      }
      
      return {
        success: true,
        messages: messages,
        hasMore: response.data.totalPages > page + 1,
        totalPages: response.data.totalPages,
        totalElements: response.data.totalElements
      };
    } catch (error) {
      console.error(`Error loading messages for conversation ${conversationId}:`, error);
      
      // Try localStorage backup in case of network failure
      try {
        const storageKey = `judify_messages_${conversationId}`;
        const storedMessages = localStorage.getItem(storageKey);
        if (storedMessages) {
          const parsedMessages = JSON.parse(storedMessages);
          return {
            success: true,
            messages: parsedMessages,
            hasMore: false,
            totalPages: 1,
            totalElements: parsedMessages.length,
            fromLocalStorage: true
          };
        }
      } catch (e) {
        console.error('Error accessing localStorage backup:', e);
      }
      
      return {
        success: false,
        messages: [],
        hasMore: false,
        totalPages: 0,
        totalElements: 0
      };
    }
  }

  // Load more messages for pagination (older messages)
  async loadMoreMessages(conversationId, page, size = 20) {
    try {
      const response = await axios.get(`/api/messages/findByConversationPaginated/${conversationId}`, {
        params: {
          page,
          size,
          order: 'DESC'
        }
      });
      
      return {
        success: true,
        messages: response.data.content || [],
        hasMore: response.data.totalPages > page + 1,
        totalPages: response.data.totalPages,
        totalElements: response.data.totalElements
      };
    } catch (error) {
      console.error(`Error loading more messages for conversation ${conversationId}:`, error);
      return {
        success: false,
        messages: [],
        hasMore: false
      };
    }
  }

  joinConversation(conversationId, userId, username) {
    // REST API doesn't need join/leave notification, but we'll keep the interface
    return true;
  }

  leaveConversation(conversationId, userId, username) {
    // REST API doesn't need join/leave notification, but we'll keep the interface
    return true;
  }

  isActive() {
    return this.isConnected;
  }
}

// Create a singleton instance
const webSocketService = new MessageService();
export default webSocketService; 
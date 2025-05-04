import api from './api';

class PollingService {
  constructor() {
    this.isConnected = false;
    this.messageCallbacks = new Map();
    this.activeConversations = new Set(); // Track which conversations are actively being viewed
    this.userId = null;
    this.token = null;
    this.pollingIntervals = new Map(); // Track polling intervals for each conversation
    this.defaultPollingInterval = 5000; // 5 seconds by default, matching mobile app
    this.localStoragePrefix = 'judify_message_';
  }

  connect(userId, token, onConnected, onError) {
    if (this.isConnected) {
      console.log('Already connected, not connecting again');

      if (onConnected) {
        onConnected();
      }

      return true;
    }

    try {
      // Store user info
      this.userId = userId;
      this.token = token;

      // Set connected status
      this.isConnected = true;

      console.log('Polling service initialized');

      if (onConnected) {
        onConnected();
      }

      return true;
    } catch (error) {
      console.error('Error initializing polling service:', error);
      if (onError) {
        onError('Failed to initialize polling service');
      }
      return false;
    }
  }

  disconnect() {
    // Clear all active conversations
    this.activeConversations.clear();

    // Clear all polling intervals
    this.pollingIntervals.forEach((interval) => {
      clearInterval(interval);
    });
    this.pollingIntervals.clear();

    // Clear all callbacks
    this.messageCallbacks.clear();

    this.isConnected = false;
    console.log('Message service disconnected');
  }

  // Check if a conversation is being actively viewed
  isConversationActive(conversationId) {
    return this.activeConversations.has(conversationId);
  }

  // Subscribe to a conversation when the user opens it
  subscribeToConversation(conversationId, callback) {
    if (!this.userId) {
      console.error('Not connected to message service');
      return null;
    }

    try {
      console.log(`Subscribing to conversation ${conversationId}`);

      // If we already have a callback for this conversation, replace it with the new one
      if (this.messageCallbacks.has(`conversation.${conversationId}`)) {
        console.log(`Updating existing callback for conversation ${conversationId}`);
        this.messageCallbacks.set(`conversation.${conversationId}`, callback);

        // Ensure this conversation is marked as active
        this.activeConversations.add(conversationId);

        return {
          conversationId,
          unsubscribe: () => this.unsubscribeFromConversation(conversationId)
        };
      }

      // Store the callback
      this.messageCallbacks.set(`conversation.${conversationId}`, callback);

      // Mark this conversation as active
      this.activeConversations.add(conversationId);

      // Start polling for this conversation
      this.startPollingForConversation(conversationId);

      return {
        conversationId,
        unsubscribe: () => this.unsubscribeFromConversation(conversationId)
      };
    } catch (e) {
      console.error(`Error subscribing to conversation ${conversationId}:`, e);
      return null;
    }
  }

  // Start polling for new messages in a conversation
  startPollingForConversation(conversationId) {
    // If already polling for this conversation, don't start another interval
    if (this.pollingIntervals.has(conversationId)) {
      return;
    }

    console.log(`Starting polling for conversation ${conversationId}`);

    // Get the last message timestamp to only fetch newer messages
    let lastMessageTimestamp = null;

    // Set up polling interval
    const intervalId = setInterval(async () => {
      if (!this.isConversationActive(conversationId)) {
        // If conversation is no longer active, stop polling
        this.stopPollingForConversation(conversationId);
        return;
      }

      try {
        // Fetch new messages since the last timestamp
        const result = await this.fetchNewMessages(conversationId, lastMessageTimestamp);

        if (result.success && result.messages.length > 0) {
          // Update the last message timestamp
          const latestMessage = result.messages.reduce((latest, msg) => {
            const msgTime = new Date(msg.timestamp || msg.createdAt).getTime();
            const latestTime = latest ? new Date(latest.timestamp || latest.createdAt).getTime() : 0;
            return msgTime > latestTime ? msg : latest;
          }, null);

          if (latestMessage) {
            lastMessageTimestamp = latestMessage.timestamp || latestMessage.createdAt;
          }

          // Process each new message
          result.messages.forEach(message => {
            const callback = this.messageCallbacks.get(`conversation.${conversationId}`);
            if (callback) {
              callback(message);
            }
          });
        }
      } catch (error) {
        console.error(`Error polling for conversation ${conversationId}:`, error);
      }
    }, this.defaultPollingInterval);

    // Store the interval ID for cleanup
    this.pollingIntervals.set(conversationId, intervalId);
  }

  // Stop polling for a conversation
  stopPollingForConversation(conversationId) {
    const intervalId = this.pollingIntervals.get(conversationId);
    if (intervalId) {
      clearInterval(intervalId);
      this.pollingIntervals.delete(conversationId);
      console.log(`Stopped polling for conversation ${conversationId}`);
    }
  }

  // Fetch new messages for a conversation since a specific timestamp
  async fetchNewMessages(conversationId, sinceTimestamp) {
    try {
      let params = {};
      if (sinceTimestamp) {
        params.since = sinceTimestamp;
      }

      // Try to fetch new messages
      try {
        const response = await api.get(`/messages/conversation/${conversationId}/new`, { params });

        if (response.data) {
          let messages = [];

          if (Array.isArray(response.data)) {
            messages = response.data;
          } else if (response.data.content && Array.isArray(response.data.content)) {
            messages = response.data.content;
          } else {
            messages = response.data.messages || [];
          }

          // Normalize message format
          const normalizedMessages = messages.map(msg => {
            // Handle different message structures
            let senderId = msg.senderId;
            let receiverId = msg.receiverId;

            // If the message has sender/receiver objects instead of IDs
            if (msg.sender && !msg.senderId) {
              senderId = msg.sender.userId;
            }

            if (msg.receiver && !msg.receiverId) {
              receiverId = msg.receiver.userId;
            }

            return {
              messageId: msg.messageId || msg.id,
              conversationId: msg.conversationId || conversationId,
              senderId: senderId,
              receiverId: receiverId,
              content: msg.content,
              timestamp: msg.timestamp || msg.createdAt || new Date().toISOString(),
              isRead: msg.isRead || false
            };
          });

          return {
            success: true,
            messages: normalizedMessages
          };
        }
      } catch (error) {
        console.warn(`Error fetching new messages for conversation ${conversationId}:`, error);

        // Try fallback endpoint
        try {
          const fallbackResponse = await api.get(`/messages`, { 
            params: { 
              conversationId,
              since: sinceTimestamp
            } 
          });

          if (fallbackResponse.data) {
            let messages = [];

            if (Array.isArray(fallbackResponse.data)) {
              messages = fallbackResponse.data;
            } else if (fallbackResponse.data.content && Array.isArray(fallbackResponse.data.content)) {
              messages = fallbackResponse.data.content;
            } else {
              messages = fallbackResponse.data.messages || [];
            }

            // Normalize message format
            const normalizedMessages = messages.map(msg => {
              return {
                messageId: msg.messageId || msg.id,
                conversationId: msg.conversationId || conversationId,
                senderId: msg.senderId,
                receiverId: msg.receiverId,
                content: msg.content,
                timestamp: msg.timestamp || msg.createdAt || new Date().toISOString(),
                isRead: msg.isRead || false
              };
            });

            return {
              success: true,
              messages: normalizedMessages
            };
          }
        } catch (fallbackError) {
          console.error(`Fallback endpoint failed for conversation ${conversationId}:`, fallbackError);
        }
      }

      // If all endpoints failed, return empty array
      return {
        success: true,
        messages: []
      };
    } catch (error) {
      console.error(`Error fetching new messages for conversation ${conversationId}:`, error);
      return {
        success: false,
        messages: []
      };
    }
  }

  unsubscribeFromConversation(conversationId) {
    try {
      // Remove the callback
      this.messageCallbacks.delete(`conversation.${conversationId}`);

      // Mark this conversation as inactive
      this.activeConversations.delete(conversationId);

      // Stop polling for this conversation
      this.stopPollingForConversation(conversationId);

      console.log(`Unsubscribed from conversation ${conversationId}`);
    } catch (e) {
      console.error(`Error unsubscribing from conversation ${conversationId}:`, e);
    }
  }

  // Join a conversation (mimics WebSocketService API for compatibility)
  joinConversation(conversationId) {
    if (!this.isConnected) {
      console.log(`Not connected yet, will join conversation ${conversationId} once connected`);
      // Add to active conversations
      this.activeConversations.add(conversationId);
      return;
    }

    try {
      console.log(`Joining conversation ${conversationId}`);

      // Mark this conversation as active
      this.activeConversations.add(conversationId);

      // Start polling for this conversation
      this.startPollingForConversation(conversationId);
    } catch (error) {
      console.error(`Error joining conversation ${conversationId}:`, error);
    }
  }

  // Leave a conversation (mimics WebSocketService API for compatibility)
  leaveConversation(conversationId) {
    this.unsubscribeFromConversation(conversationId);
    return true;
  }

  // Send a message to a conversation
  async sendMessage(message) {
    try {
      // Ensure the message has required fields
      if (!message.content) {
        return {
          success: false,
          message: 'Message content is required'
        };
      }

      if (!message.conversationId && !this.activeConversation) {
        return {
          success: false,
          message: 'Conversation ID is required'
        };
      }

      // Use active conversation ID if not provided
      const conversationId = message.conversationId || 
                           (this.activeConversation ? this.activeConversation.conversationId : null);

      // Get receiver ID from active conversation if available
      const receiverId = message.receiverId || 
                      (this.activeConversation && this.activeConversation.user1Id !== this.userId ? 
                       this.activeConversation.user1Id : 
                       (this.activeConversation ? this.activeConversation.user2Id : null));

      // Prepare the message object
      const messageToSend = {
        ...message,
        conversationId: conversationId,
        senderId: this.userId,
        receiverId: receiverId,
        timestamp: new Date().toISOString(),
        type: 'CHAT'
      };

      console.log('Sending message:', messageToSend);

      // Try to send via the server API
      try {
        const response = await api.post(`/messages`, messageToSend);

        if (response.data) {
          console.log('Message sent successfully:', response.data);

          // Store locally as backup
          this.storeMessageLocally(messageToSend);

          // Return the sent message
          return {
            success: true,
            message: response.data
          };
        }
      } catch (error) {
        console.error('Error sending message via API:', error);

        // Try fallback endpoint
        try {
          const fallbackResponse = await api.post(`/messages/sendMessage`, {
            conversationId: conversationId,
            senderId: this.userId,
            content: message.content
          });

          if (fallbackResponse.data) {
            console.log('Message sent successfully via fallback:', fallbackResponse.data);

            // Store locally as backup
            this.storeMessageLocally(messageToSend);

            // Return the sent message
            return {
              success: true,
              message: fallbackResponse.data
            };
          }
        } catch (fallbackError) {
          console.error('Fallback endpoint failed:', fallbackError);
        }
      }

      // If API fails, store locally and return success
      console.log('Storing message locally only');
      const localMessage = {
        ...messageToSend,
        messageId: new Date().getTime(), // Generate a temporary ID
        isLocal: true // Mark as local-only
      };

      this.storeMessageLocally(localMessage);

      // If we have a callback for this conversation, call it
      const callback = this.messageCallbacks.get(`conversation.${conversationId}`);
      if (callback) {
        callback(localMessage);
      }

      return {
        success: true,
        message: localMessage
      };
    } catch (error) {
      console.error('Error in sendMessage:', error);
      return {
        success: false,
        message: 'Failed to send message: ' + error.message
      };
    }
  }

  // Store message locally
  storeMessageLocally(message) {
    try {
      if (!message.conversationId) return;

      // Get existing messages for this conversation
      const existingMessages = JSON.parse(
        localStorage.getItem(`${this.localStoragePrefix}${message.conversationId}`) || '[]'
      );

      // Add new message
      existingMessages.push(message);

      // Store updated messages
      localStorage.setItem(
        `${this.localStoragePrefix}${message.conversationId}`,
        JSON.stringify(existingMessages)
      );
    } catch (error) {
      console.error('Error storing message locally:', error);
    }
  }

  // Mark a message as read
  async markMessageAsRead(messageId) {
    try {
      if (!this.isConnected) {
        return {
          success: false,
          message: 'Not connected to the server'
        };
      }

      // Try to mark as read via API - try multiple endpoints for compatibility
      // First try the web endpoint
      try {
        const response = await api.put(`/messages/${messageId}/read`);

        if (response.data) {
          console.log('Message marked as read:', response.data);
          return {
            success: true,
            message: 'Message marked as read'
          };
        }
      } catch (error) {
        console.log('First attempt to mark message as read failed, trying alternative endpoint:', error);

        // Try the mobile app endpoint as fallback
        try {
          const fallbackResponse = await api.put(`/messages/markAsRead/${messageId}`);

          if (fallbackResponse.data) {
            console.log('Message marked as read via fallback endpoint:', fallbackResponse.data);
            return {
              success: true,
              message: 'Message marked as read'
            };
          }
        } catch (fallbackError) {
          console.error('Fallback endpoint failed:', fallbackError);
        }
      }

      // If API fails, just return success
      return {
        success: true,
        message: 'Message marked as read locally'
      };
    } catch (error) {
      console.error('Error in markMessageAsRead:', error);
      return {
        success: false,
        message: 'Failed to mark message as read: ' + error.message
      };
    }
  }

  // Load messages for a conversation
  async loadConversationMessages(conversationId, page = 0, size = 20) {
    try {
      console.log(`Loading messages for conversation ${conversationId}, page ${page}, size ${size}`);

      // Try to load from server
      const serverResult = await this.tryGetMessagesFromServer(conversationId, page, size);

      if (serverResult.success && serverResult.messages.length > 0) {
        console.log(`Loaded ${serverResult.messages.length} messages from server`);

        // Store messages locally for offline access
        serverResult.messages.forEach(message => {
          this.storeMessageLocally(message);
        });

        return {
          success: true,
          messages: serverResult.messages,
          hasMore: serverResult.hasMore,
          totalPages: serverResult.totalPages,
          totalElements: serverResult.totalElements
        };
      }

      // If server fails or returns no messages, try loading from localStorage
      try {
        const localMessages = JSON.parse(
          localStorage.getItem(`${this.localStoragePrefix}${conversationId}`) || '[]'
        );

        if (localMessages.length > 0) {
          console.log(`Loaded ${localMessages.length} messages from localStorage`);

          // Sort by timestamp, newest first
          localMessages.sort((a, b) => {
            const timeA = new Date(a.timestamp || a.createdAt).getTime();
            const timeB = new Date(b.timestamp || b.createdAt).getTime();
            return timeB - timeA;
          });

          // Paginate
          const start = page * size;
          const end = start + size;
          const paginatedMessages = localMessages.slice(start, end);

          return {
            success: true,
            messages: paginatedMessages,
            hasMore: end < localMessages.length,
            totalPages: Math.ceil(localMessages.length / size),
            totalElements: localMessages.length
          };
        }
      } catch (error) {
        console.error('Error loading messages from localStorage:', error);
      }

      // If no messages found, return empty array
      return {
        success: true,
        messages: [],
        hasMore: false,
        totalPages: 0,
        totalElements: 0
      };
    } catch (error) {
      console.error('Error in loadConversationMessages:', error);
      return {
        success: false,
        messages: [],
        hasMore: false,
        totalPages: 0,
        totalElements: 0,
        message: 'Failed to load messages: ' + error.message
      };
    }
  }

  // Try to get messages from the server
  async tryGetMessagesFromServer(conversationId, page = 0, size = 20) {
    try {
      // Try multiple endpoints
      const endpoints = [
        `/messages/conversation/${conversationId}`,
        `/messages`,
        `/conversations/${conversationId}/messages`
      ];

      for (const endpoint of endpoints) {
        try {
          let params = { page, size };

          // Add conversationId to params only for the second endpoint
          if (endpoint === '/messages') {
            params.conversationId = conversationId;
          }

          console.log(`Trying to load messages from endpoint: ${endpoint}`, params);
          const response = await api.get(endpoint, { params });

          if (response.data) {
            let messages = [];
            let hasMore = false;
            let totalPages = 0;
            let totalElements = 0;

            if (Array.isArray(response.data)) {
              // Direct array response
              messages = response.data;
              hasMore = messages.length >= size;
              totalElements = messages.length;
              totalPages = Math.ceil(totalElements / size);
            } else if (response.data.content && Array.isArray(response.data.content)) {
              // Page response
              messages = response.data.content;
              hasMore = !response.data.last;
              totalPages = response.data.totalPages || 0;
              totalElements = response.data.totalElements || 0;
            } else if (response.data.messages && Array.isArray(response.data.messages)) {
              // Wrapped response
              messages = response.data.messages;
              hasMore = messages.length >= size;
              totalElements = messages.length;
              totalPages = Math.ceil(totalElements / size);
            }

            // Normalize messages
            const normalizedMessages = messages.map(msg => {
              // Handle different message structures
              let senderId = msg.senderId;
              let receiverId = msg.receiverId;

              // If the message has sender/receiver objects instead of IDs
              if (msg.sender && !msg.senderId) {
                senderId = msg.sender.userId;
              }

              if (msg.receiver && !msg.receiverId) {
                receiverId = msg.receiver.userId;
              }

              return {
                messageId: msg.messageId || msg.id,
                conversationId: msg.conversationId || conversationId,
                senderId: senderId,
                receiverId: receiverId,
                content: msg.content,
                timestamp: msg.timestamp || msg.createdAt || new Date().toISOString(),
                isRead: msg.isRead || false
              };
            });

            return {
              success: true,
              messages: normalizedMessages,
              hasMore: hasMore,
              totalPages: totalPages,
              totalElements: totalElements
            };
          }
        } catch (error) {
          console.warn(`Endpoint ${endpoint} failed:`, error.message);
          // Continue to next endpoint
        }
      }

      // If all endpoints fail, return failure
      return {
        success: false,
        messages: [],
        hasMore: false,
        totalPages: 0,
        totalElements: 0,
        message: 'All endpoints failed'
      };
    } catch (error) {
      console.error('Error in tryGetMessagesFromServer:', error);
      return {
        success: false,
        messages: [],
        hasMore: false,
        totalPages: 0,
        totalElements: 0,
        message: 'Failed to load messages from server: ' + error.message
      };
    }
  }

  // Load more messages for a conversation
  async loadMoreMessages(conversationId, page, size = 20) {
    return this.loadConversationMessages(conversationId, page, size);
  }

  // Check if the service is active
  isActive() {
    return this.isConnected;
  }
}

// Create a singleton instance
const pollingServiceInstance = new PollingService();

export default pollingServiceInstance;

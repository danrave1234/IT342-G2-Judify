import axios from 'axios';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

// Make sure global is defined for browsers
if (typeof window !== 'undefined' && typeof window.global === 'undefined') {
  window.global = window;
}

class MessageService {
  constructor() {
    this.isConnected = false;
    this.messageCallbacks = new Map();
    this.readReceiptCallbacks = new Map();
    this.activeConversations = new Set(); // Track which conversations are actively being viewed
    this.stompClient = null;
    this.userId = null;
    this.token = null;
  }

  connect(userId, token, onConnected, onError) {
    if (this.isConnected && this.stompClient) {
      console.log('Already connected, not connecting again');
      
      if (onConnected) {
        onConnected();
      }
      
      return true;
    }
    
    try {
      // Set token for all future axios requests
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
      
      // Store user info
      this.userId = userId;
      this.token = token;
      
      // Create and configure the connection
      this.setupStompConnection();
      
      console.log('WebSocket service initialized');
      
      // We'll consider ourselves connected once the STOMP connection is established
      this.onConnectedCallback = onConnected;
      this.onErrorCallback = onError;
      
      return true;
    } catch (error) {
      console.error('Error initializing WebSocket service:', error);
      if (onError) {
        onError('Failed to initialize WebSocket service');
      }
      return false;
    }
  }
  
  setupStompConnection() {
    // Only create a connection if we don't already have one
    if (this.stompClient) {
      return;
    }
    
    console.log('Setting up WebSocket connection...');
    
    // Create WebSocket connection
    const socket = new SockJS('/ws');
    this.stompClient = Stomp.over(socket);
    
    // Disable heartbeat (optional, helps with debugging)
    // this.stompClient.heartbeat.outgoing = 0;
    // this.stompClient.heartbeat.incoming = 0;
    
    // For debugging, uncomment this
    // this.stompClient.debug = function(str) {
    //   console.log('STOMP: ' + str);
    // };
    
    // Set smaller reconnect delay
    this.stompClient.reconnect_delay = 5000;
    
    // Connect to the WebSocket server
    this.stompClient.connect(
      {
        'Authorization': `Bearer ${this.token}`,
        'userId': this.userId
      },
      this.onStompConnected.bind(this),
      this.onStompError.bind(this)
    );
  }
  
  onStompConnected(frame) {
    this.isConnected = true;
    console.log('WebSocket Connected: ' + frame);
    
    // Subscribe to user-specific message queue
    this.stompClient.subscribe(`/user/${this.userId}/queue/messages`, this.onMessageReceived.bind(this));
    
    // Subscribe to read receipts
    this.stompClient.subscribe(`/user/${this.userId}/queue/receipts`, this.onReceiptReceived.bind(this));
    
    // Reconnect to all active conversations
    this.activeConversations.forEach(conversationId => {
      this.joinConversation(conversationId);
    });
    
    // Notify the caller that we're connected
    if (this.onConnectedCallback) {
      this.onConnectedCallback();
    }
  }
  
  onStompError(error) {
    console.error('WebSocket Error:', error);
    
    // Close the connection
    if (this.stompClient) {
      this.stompClient.disconnect();
      this.stompClient = null;
    }
    
    this.isConnected = false;
    
    // Notify the caller of the error
    if (this.onErrorCallback) {
      this.onErrorCallback('Failed to connect to WebSocket');
    }
    
    // Try to reconnect after a delay
    setTimeout(() => {
      if (this.activeConversations.size > 0) {
        console.log('Attempting to reconnect WebSocket...');
        this.setupStompConnection();
      }
    }, 5000);
  }
  
  onMessageReceived(payload) {
    const message = JSON.parse(payload.body);
    console.log('Received message:', message);
    
    // Forward the message to the appropriate conversation callback
    const conversationId = message.conversationId;
    const callback = this.messageCallbacks.get(`conversation.${conversationId}`);
    
    if (callback) {
      callback(message);
    } else {
      console.log(`No callback registered for conversation ${conversationId}`);
    }
  }
  
  onReceiptReceived(payload) {
    const receipt = JSON.parse(payload.body);
    console.log('Received read receipt:', receipt);
    
    // Forward the receipt to the registered callback
    const callback = this.readReceiptCallbacks.get(`message.${receipt.messageId}`);
    
    if (callback) {
      callback(receipt);
    }
  }

  disconnect() {
    // Clear all active conversations
    this.activeConversations.clear();
    
    // Disconnect the WebSocket connection
    if (this.stompClient) {
      this.stompClient.disconnect();
      this.stompClient = null;
    }
    
    // Clear all callbacks
    this.messageCallbacks.clear();
    this.readReceiptCallbacks.clear();
    
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
      
      // Ensure we have a WebSocket connection
      if (!this.stompClient) {
        this.setupStompConnection();
      } else if (this.isConnected) {
        // If already connected, join the conversation
        this.joinConversation(conversationId);
      }
      
      return {
        conversationId,
        unsubscribe: () => this.unsubscribeFromConversation(conversationId)
      };
    } catch (e) {
      console.error(`Error subscribing to conversation ${conversationId}:`, e);
      return null;
    }
  }
  
  // Join a conversation via WebSocket
  joinConversation(conversationId) {
    if (!this.isConnected || !this.stompClient) {
      console.log(`Not connected yet, will join conversation ${conversationId} once connected`);
      return;
    }
    
    try {
      console.log(`Joining conversation ${conversationId}`);
      
      // Send JOIN message to the server - no need to convert conversationId to number
      this.stompClient.send(`/app/chat.join/${conversationId}`, {}, JSON.stringify({
        senderId: this.userId,
        conversationId: conversationId, // Keep as string
        type: 'JOIN'
      }));
    } catch (e) {
      console.error(`Error joining conversation ${conversationId}:`, e);
    }
  }

  unsubscribeFromConversation(conversationId) {
    try {
      // Remove the callback
      this.messageCallbacks.delete(`conversation.${conversationId}`);
      
      // Mark this conversation as inactive
      this.activeConversations.delete(conversationId);
      
      // Send LEAVE message if connected
      if (this.isConnected && this.stompClient) {
        this.stompClient.send(`/app/chat.leave/${conversationId}`, {}, JSON.stringify({
          senderId: this.userId,
          conversationId: conversationId,
          type: 'LEAVE'
        }));
      }
      
      console.log(`Unsubscribed from conversation ${conversationId}`);
    } catch (e) {
      console.error(`Error unsubscribing from conversation ${conversationId}:`, e);
    }
  }

  async sendMessage(message) {
    if (!this.isConnected || !this.stompClient) {
      console.error('Not connected to WebSocket service');
      return false;
    }

    try {
      // Check if the conversationId is numeric or client-generated
      const isNumericId = /^\d+$/.test(message.conversationId.toString());
      
      // Ensure the message has the correct format - but DON'T convert conversationId to number
      const messageToSend = {
        ...message,
        // Don't convert conversationId to Number, keep it as string
        senderId: message.senderId ? Number(message.senderId) : message.senderId,
        receiverId: message.receiverId ? Number(message.receiverId) : message.receiverId,
        type: 'CHAT'
      };
      
      console.log('Sending message:', messageToSend);
      
      // If this is a numeric conversationId, also try to save via REST API
      // This helps ensure the message is stored even if WebSocket fails
      if (isNumericId) {
        try {
          // Try to save the message via REST API as backup
          const response = await axios.post('/api/messages/sendMessage', {
            conversationId: Number(message.conversationId),
            senderId: message.senderId,
            content: message.content
          });
          console.log('Message saved via REST API:', response.data);
        } catch (apiError) {
          console.warn('Could not save message via REST API:', apiError);
          // Continue with WebSocket anyway
        }
      }
      
      // Send the message via WebSocket
      this.stompClient.send(`/app/chat.send/${messageToSend.conversationId}`, {}, JSON.stringify(messageToSend));
      
      return true;
    } catch (e) {
      console.error('Error sending message:', e);
      
      // Log more details about the error for debugging
      if (e.response) {
        console.error('Error response:', e.response.data);
        console.error('Error status:', e.response.status);
      }
      
      return false;
    }
  }

  async markMessageAsRead(messageId, senderId, conversationId) {
    if (!this.isConnected || !this.stompClient) {
      console.error('Not connected to WebSocket service');
      return false;
    }

    try {
      // Send a read receipt via WebSocket
      this.stompClient.send(`/app/chat.read/${messageId}`, {}, JSON.stringify({
        messageId: messageId,
        senderId: senderId,
        receiverId: this.userId,
        conversationId: conversationId,
        isRead: true
      }));
      
      return true;
    } catch (e) {
      console.error('Error marking message as read:', e);
      return false;
    }
  }

  onMessageRead(messageId, callback) {
    this.readReceiptCallbacks.set(`message.${messageId}`, callback);
  }

  // Load conversation messages once
  async loadConversationMessages(conversationId, page = 0, size = 20) {
    try {
      console.log(`Loading messages for conversation ${conversationId}, page ${page}`);
      
      // Check if user is a tutor (tutors should always use the server endpoint)
      const userStr = localStorage.getItem('user');
      const user = userStr ? JSON.parse(userStr) : null;
      const isTutor = user?.role === 'TUTOR';
      
      // Check if the conversationId is numeric or client-generated
      const isNumericId = /^\d+$/.test(conversationId.toString());
      
      let response;
      let messagesFromServer = false;
      
      // For tutors, always try the backend endpoints first
      if (isTutor || isNumericId) {
        // Try multiple approaches to ensure tutors can see student messages
        const endpoints = [
          // Primary endpoint
          {
            url: `/api/messages/findByConversationPaginated/${conversationId}`,
            params: { page, size, order: 'ASC' } // ASC is oldest first, which is what we want
          },
          // Backup endpoint that might have different permission structure
          {
            url: `/api/conversations/${conversationId}/messages`,
            params: { page, size, order: 'ASC' }
          },
          // Direct user messages endpoint for tutors
          {
            url: `/api/messages/conversation/${conversationId}`,
            params: { page, size, order: 'ASC' }
          }
        ];
      
        // Try each endpoint until one works
        for (const endpoint of endpoints) {
          try {
            console.log(`Trying to fetch messages from endpoint: ${endpoint.url}`);
            response = await axios.get(endpoint.url, { params: endpoint.params });
            
            if (response?.data) {
              messagesFromServer = true;
              console.log(`Successfully fetched messages using endpoint: ${endpoint.url}`);
              break;
            }
          } catch (err) {
            console.warn(`Endpoint ${endpoint.url} failed:`, err.message);
            // Continue to the next endpoint
          }
        }
      }
      
      // If we have server data, process it
      if (messagesFromServer && response?.data) {
        // Handle different response formats
        let messages = [];
        let hasMore = false;
        let totalPages = 1;
        let totalElements = 0;
        
        if (Array.isArray(response.data)) {
          // Direct array of messages
          messages = response.data;
          hasMore = false;
          totalElements = messages.length;
        } else if (response.data.content) {
          // Paginated response
          messages = response.data.content;
          hasMore = response.data.totalPages > page + 1;
          totalPages = response.data.totalPages;
          totalElements = response.data.totalElements;
        } else {
          // Unknown format, try to extract messages
          messages = response.data.messages || response.data;
        }
        
        // Store messages in localStorage as backup
        try {
          const storageKey = `judify_messages_${conversationId}`;
          localStorage.setItem(storageKey, JSON.stringify(messages));
        } catch (e) {
          console.warn('Failed to store messages in localStorage:', e);
        }
        
        return {
          success: true,
          messages: messages,
          hasMore: hasMore,
          totalPages: totalPages,
          totalElements: totalElements
        };
      }
      
      // For client-generated IDs or if server request failed, check localStorage
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
      
      // If no stored messages, return empty array
      return {
        success: true,
        messages: [],
        hasMore: false,
        totalPages: 0,
        totalElements: 0
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

  // Load older messages (pagination)
  async loadMoreMessages(conversationId, page, size = 20) {
    try {
      console.log(`Loading more messages for conversation ${conversationId}, page ${page}`);
      
      // Check if user is a tutor
      const userStr = localStorage.getItem('user');
      const user = userStr ? JSON.parse(userStr) : null;
      const isTutor = user?.role === 'TUTOR';
      
      // Check if conversationId is numeric
      const isNumericId = /^\d+$/.test(conversationId.toString());
      
      let response;
      let messagesFromServer = false;
      
      // For tutors or numeric IDs, always try server endpoints
      if (isTutor || isNumericId) {
        // Try multiple approaches to ensure tutors can see student messages
        const endpoints = [
          // Primary endpoint
          {
            url: `/api/messages/findByConversationPaginated/${conversationId}`,
            params: { page, size, order: 'DESC' }
          },
          // Backup endpoint
          {
            url: `/api/conversations/${conversationId}/messages`,
            params: { page, size, order: 'DESC' }
          },
          // Direct user messages endpoint
          {
            url: `/api/messages/conversation/${conversationId}`,
            params: { page, size, order: 'DESC' }
          }
        ];
        
        // Try each endpoint until one works
        for (const endpoint of endpoints) {
          try {
            console.log(`Trying to fetch more messages from endpoint: ${endpoint.url}`);
            response = await axios.get(endpoint.url, { params: endpoint.params });
            
            if (response?.data) {
              messagesFromServer = true;
              console.log(`Successfully fetched more messages using endpoint: ${endpoint.url}`);
              break;
            }
          } catch (err) {
            console.warn(`Endpoint ${endpoint.url} failed when loading more messages:`, err.message);
            // Continue to the next endpoint
          }
        }
      }
      
      // If we have server data, process it
      if (messagesFromServer && response?.data) {
        // Handle different response formats
        let messages = [];
        let hasMore = false;
        let totalPages = 1;
        let totalElements = 0;
        
        if (Array.isArray(response.data)) {
          // Direct array of messages
          messages = response.data;
          hasMore = false;
          totalElements = messages.length;
        } else if (response.data.content) {
          // Paginated response
          messages = response.data.content;
          hasMore = response.data.totalPages > page + 1;
          totalPages = response.data.totalPages;
          totalElements = response.data.totalElements;
        } else {
          // Unknown format, try to extract messages
          messages = response.data.messages || response.data;
        }
        
        // Update the local storage with the merged messages
        try {
          const storageKey = `judify_messages_${conversationId}`;
          const existingMessagesStr = localStorage.getItem(storageKey);
          
          if (existingMessagesStr) {
            const existingMessages = JSON.parse(existingMessagesStr);
            
            // Filter out duplicates (based on messageId)
            const messageIds = new Set(existingMessages.map(m => m.messageId));
            const uniqueNewMessages = messages.filter(m => !messageIds.has(m.messageId));
            
            // Merge and sort
            const mergedMessages = [...existingMessages, ...uniqueNewMessages]
              .sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
            
            localStorage.setItem(storageKey, JSON.stringify(mergedMessages));
          } else {
            localStorage.setItem(storageKey, JSON.stringify(messages));
          }
        } catch (e) {
          console.warn('Failed to update messages in localStorage:', e);
        }
        
        return {
          success: true,
          messages: messages,
          hasMore: hasMore,
          totalPages: totalPages,
          totalElements: totalElements
        };
      }
      
      // For client-generated IDs, we already have all messages in localStorage
      return {
        success: false,
        messages: [],
        hasMore: false,
        totalPages: 0,
        totalElements: 0,
        error: 'No more messages available'
      };
      
    } catch (error) {
      console.error(`Error loading more messages for conversation ${conversationId}:`, error);
      return {
        success: false,
        messages: [],
        hasMore: false,
        totalPages: 0,
        totalElements: 0,
        error: error.message
      };
    }
  }

  leaveConversation(conversationId, userId, username) {
    this.unsubscribeFromConversation(conversationId);
    return true;
  }

  isActive() {
    return this.isConnected;
  }
}

// Create a singleton instance
const webSocketService = new MessageService();
export default webSocketService; 